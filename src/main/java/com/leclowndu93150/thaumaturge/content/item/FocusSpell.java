package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Saved focus topology. The starter parts use the same identifiers as CE26. */
record FocusSpell(Node root) {
    private static final String GRAPH = "Spell";
    private static final ResourceLocation ORIGIN = TTIds.rl("origin");
    private static final ResourceLocation TOUCH = TTIds.rl("touch");
    private static final ResourceLocation BOLT = TTIds.rl("bolt");
    private static final ResourceLocation BRANCH = TTIds.rl("branch");
    private static final ResourceLocation FIRE = TTIds.rl("fire");
    private static final ResourceLocation FROST = TTIds.rl("frost");

    static @Nullable FocusSpell read(ItemStack stack) {
        CompoundTag data = stack.getTagElement(TTItemData.ROOT);
        if (data == null) return null;
        if (!data.contains(GRAPH, Tag.TAG_COMPOUND)) return starter(data.getString("StarterSpell"));
        CompoundTag graph = data.getCompound(GRAPH);
        if (graph.contains("style") && !graph.getString("style").equals("instant")) return null;
        Node root = Node.read(graph.getCompound("root"), 0);
        if (root == null || !root.part().equals(ORIGIN) || root.children().size() != 1 || !root.settings().isEmpty()) return null;
        Node flow = root.children().get(0);
        boolean valid = flow.part().equals(BRANCH)
                ? flow.settings().isEmpty() && flow.children().size() >= 2 && flow.children().size() <= 3 && flow.children().stream().allMatch(FocusSpell::isDelivery)
                : isDelivery(flow);
        return valid ? new FocusSpell(root) : null;
    }

    private static boolean isDelivery(Node delivery) {
        if (!(delivery.part().equals(TOUCH) || delivery.part().equals(BOLT)) || delivery.children().size() != 1 || !delivery.settings().isEmpty()) return false;
        Node effect = delivery.children().get(0);
        if (!effect.children().isEmpty() || !(effect.part().equals(FIRE) || effect.part().equals(FROST))) return false;
        int duration = effect.settings().getOrDefault("duration", effect.part().equals(FIRE) ? 1 : 2);
        int power = effect.settings().getOrDefault("power", 1);
        return effect.settings().keySet().stream().allMatch(key -> key.equals("duration") || key.equals("power"))
                && power >= 1 && power <= 5
                && (effect.part().equals(FIRE) ? duration >= 0 && duration <= 5 : duration >= 2 && duration <= 10);
    }

    static @Nullable FocusSpell starter(String effect) {
        ResourceLocation part = switch (effect) {
            case "fire" -> FIRE;
            case "frost" -> FROST;
            default -> null;
        };
        return part == null ? null : new FocusSpell(new Node(ORIGIN, List.of(delivery(TOUCH, part))));
    }

    static FocusSpell bolt(String effect) {
        return new FocusSpell(new Node(ORIGIN, List.of(delivery(BOLT, TTIds.rl(effect)))));
    }

    static FocusSpell split() {
        return new FocusSpell(new Node(ORIGIN, List.of(new Node(BRANCH, List.of(delivery(TOUCH, FIRE), delivery(TOUCH, FROST))))));
    }

    private static Node delivery(ResourceLocation delivery, ResourceLocation effect) {
        return new Node(delivery, List.of(new Node(effect, List.of())));
    }

    record Cast(boolean bolt, String effect, int power, int duration) {
        boolean fire() {
            return effect.equals("fire");
        }

        ResourceLocation deliveryAspect() {
            return bolt ? TTAspects.POTENTIA : TTAspects.AVERSIO;
        }

        ResourceLocation effectAspect() {
            return fire() ? TTAspects.IGNIS : TTAspects.GELUM;
        }
    }

    boolean branched() {
        return root.children().get(0).part().equals(BRANCH);
    }

    List<Cast> casts() {
        Node flow = root.children().get(0);
        List<Node> deliveries = branched() ? flow.children() : List.of(flow);
        return deliveries.stream().map(node -> {
            Node effect = node.children().get(0);
            return new Cast(node.part().equals(BOLT), effect.part().getPath(), effect.settings().getOrDefault("power", 1),
                    effect.settings().getOrDefault("duration", effect.part().equals(FIRE) ? 1 : 2));
        }).toList();
    }

    List<ResourceLocation> aspects() {
        var aspects = new ArrayList<ResourceLocation>();
        if (branched()) aspects.add(TTAspects.PERMUTATIO);
        for (Cast cast : casts()) {
            aspects.add(cast.deliveryAspect());
            aspects.add(cast.effectAspect());
        }
        return aspects;
    }

    int complexity() {
        var uses = new HashMap<String, Integer>();
        float total = branched() ? 4 : 0;
        for (Cast cast : casts()) {
            int deliveryUses = uses.merge(cast.bolt() ? "bolt" : "touch", 1, Integer::sum);
            int effectUses = uses.merge(cast.effect(), 1, Integer::sum);
            total += (cast.bolt() ? 5 : 2) * (deliveryUses + 1) / 2F;
            int effectComplexity = 2 + (cast.power() - 1) * 2 + cast.duration() - (cast.fire() ? 0 : 2);
            total += effectComplexity * (effectUses + 1) / 2F;
        }
        return Math.round(total);
    }

    int visCost() {
        return Math.round(complexity() * 0.2F * WandCharge.CENTIVIS_PER_VIS);
    }

    int cooldown() {
        return Math.round(5F + (float) Math.pow(complexity(), 1.5) / 6F);
    }

    String effect() {
        return casts().get(0).effect();
    }

    void write(ItemStack stack) {
        CompoundTag graph = new CompoundTag();
        graph.putString("style", "instant");
        graph.put("root", root.save());
        CompoundTag data = TTItemData.root(stack);
        data.put(GRAPH, graph);
        data.remove("StarterSpell");
    }

    private record Node(ResourceLocation part, Map<String, Integer> settings, List<Node> children) {
        private Node(ResourceLocation part, List<Node> children) {
            this(part, Map.of(), children);
        }

        private static @Nullable Node read(CompoundTag tag, int depth) {
            ResourceLocation part = ResourceLocation.tryParse(tag.getString("part"));
            if (part == null || depth > 3 || tag.contains("children") && !tag.contains("children", Tag.TAG_LIST)) return null;
            ListTag children = tag.getList("children", Tag.TAG_COMPOUND);
            if (children.size() > 3) return null;
            if (tag.contains("settings") && !tag.contains("settings", Tag.TAG_COMPOUND)) return null;
            var configured = tag.getCompound("settings");
            if (configured.size() > 2) return null;
            var settings = new HashMap<String, Integer>();
            for (String key : configured.getAllKeys()) {
                if (!configured.contains(key, Tag.TAG_INT)) return null;
                settings.put(key, configured.getInt(key));
            }
            var nodes = new ArrayList<Node>();
            for (int i = 0; i < children.size(); i++) {
                Node child = read(children.getCompound(i), depth + 1);
                if (child == null) return null;
                nodes.add(child);
            }
            return new Node(part, Map.copyOf(settings), List.copyOf(nodes));
        }

        private CompoundTag save() {
            CompoundTag result = new CompoundTag();
            result.putString("part", part.toString());
            if (!settings.isEmpty()) {
                var configured = new CompoundTag();
                settings.forEach(configured::putInt);
                result.put("settings", configured);
            }
            ListTag nested = new ListTag();
            for (Node child : children) nested.add(child.save());
            result.put("children", nested);
            return result;
        }
    }
}
