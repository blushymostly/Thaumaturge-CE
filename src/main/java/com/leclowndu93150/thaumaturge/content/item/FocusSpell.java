package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Saved focus topology. The starter parts use the same identifiers as CE26. */
public record FocusSpell(Node root) {
    private static final String GRAPH = "Spell";
    private static final ResourceLocation ORIGIN = TTIds.rl("origin");
    private static final ResourceLocation TOUCH = TTIds.rl("touch");
    private static final ResourceLocation BOLT = TTIds.rl("bolt");
    private static final ResourceLocation FIRE = TTIds.rl("fire");
    private static final ResourceLocation FROST = TTIds.rl("frost");

    static @Nullable FocusSpell read(ItemStack stack) {
        CompoundTag data = stack.getTagElement(TTItemData.ROOT);
        if (data == null)
            return null;
        if (!data.contains(GRAPH, Tag.TAG_COMPOUND))
            return starter(data.getString("StarterSpell"));
        return readGraph(data.getCompound(GRAPH));
    }

    static @Nullable FocusSpell readGraph(CompoundTag graph) {
        FocusSpell draft = readDraft(graph);
        return draft != null && draft.complete() ? draft : null;
    }

    public void writeDraft(FriendlyByteBuf buffer) {
        root.writeDraft(buffer);
    }

    public static FocusSpell readDraft(FriendlyByteBuf buffer) {
        Node root = Node.readDraft(buffer, 0);
        if (root.kind() != Part.ORIGIN || !validDraft(root))
            throw new DecoderException("Invalid focus draft");
        return new FocusSpell(root);
    }

    static @Nullable FocusSpell readDraft(CompoundTag graph) {
        if (graph.contains("style") && !graph.getString("style").equals("instant"))
            return null;
        Node root = Node.read(graph.getCompound("root"), 0);
        return root == null || root.kind() != Part.ORIGIN || !validDraft(root) ? null : new FocusSpell(root);
    }

    private static boolean validDraft(Node node) {
        Part part = node.kind();
        if (part == null || node.children().size() > part.capacity() || !node.settings().keySet().stream().allMatch(part::hasSetting))
            return false;
        if (part.isEffect() && (node.power() < 1 || node.power() > 5 || node.duration() < part.minDuration() || node.duration() > part.maxDuration()))
            return false;
        return node.children().stream().allMatch(child -> part.accepts(child.kind()) && validDraft(child));
    }

    public static FocusSpell empty() {
        return new FocusSpell(Part.ORIGIN.node());
    }

    public boolean complete() {
        if (root.children().isEmpty())
            return false;
        Node first = root.children().get(0);
        if (!branched())
            return !first.children().isEmpty();
        return first.children().size() >= 2 && first.children().stream().allMatch(node -> !node.children().isEmpty());
    }

    public boolean researched(Player player) {
        return researched(root, TTAttachments.knowledge(player));
    }

    private static boolean researched(Node node, PlayerKnowledge knowledge) {
        return node.kind().unlocked(knowledge) && node.children().stream().allMatch(child -> researched(child, knowledge));
    }

    static @Nullable FocusSpell starter(String effect) {
        ResourceLocation part = switch (effect) {
            case "fire" -> FIRE;
            case "frost" -> FROST;
            default -> null;
        };
        return part == null ? null : new FocusSpell(new Node(ORIGIN, List.of(delivery(TOUCH, part))));
    }

    private static Node delivery(ResourceLocation delivery, ResourceLocation effect) {
        return new Node(delivery, List.of(new Node(effect, List.of())));
    }

    public record Cast(boolean bolt, String effect, int power, int duration) {
        boolean fire() {
            return effect.equals("fire");
        }

    }

    boolean branched() {
        return !root.children().isEmpty() && root.children().get(0).kind() == Part.BRANCH;
    }

    public List<Cast> casts() {
        if (!complete())
            return List.of();
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
        collectAspects(root, aspects);
        return aspects;
    }

    private static void collectAspects(Node node, List<ResourceLocation> aspects) {
        if (node.kind().aspect != null)
            aspects.add(node.kind().aspect);
        node.children().forEach(child -> collectAspects(child, aspects));
    }

    public int complexity() {
        return Math.round(complexity(root, new HashMap<>()));
    }

    private static float complexity(Node node, Map<Part, Integer> uses) {
        Part kind = node.kind();
        int count = uses.merge(kind, 1, Integer::sum);
        float total = node.complexity() * (count + 1) / 2F;
        for (Node child : node.children())
            total += complexity(child, uses);
        return total;
    }

    public Map<ResourceLocation, Integer> crystals() {
        var crystals = new LinkedHashMap<ResourceLocation, Integer>();
        for (var aspect : aspects())
            crystals.merge(aspect, 1, Integer::sum);
        return Collections.unmodifiableMap(crystals);
    }

    public int inscriptionLevels() {
        return Math.max(1, Math.round((float) Math.sqrt(complexity())));
    }

    public int inscriptionVis(int budget) {
        return complexity() * 10 + budget / 5;
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

    public CompoundTag save() {
        CompoundTag graph = new CompoundTag();
        graph.putString("style", "instant");
        graph.put("root", root.save());
        return graph;
    }

    void write(ItemStack stack) {
        CompoundTag data = TTItemData.root(stack);
        data.put(GRAPH, save());
        data.remove("StarterSpell");
    }

    public enum Part {
        ORIGIN("origin", "root", "flow", "", 0, null), BRANCH("branch", "split_trajectory", "flow", "focus_split", 4, TTAspects.PERMUTATIO), TOUCH("touch", "touch", "delivery", "base_auromancy", 2,
                TTAspects.AVERSIO), BOLT("bolt", "bolt", "delivery", "focus_bolt", 5,
                        TTAspects.POTENTIA), FIRE("fire", "fire", "effect", "base_auromancy", 2, TTAspects.IGNIS), FROST("frost", "frost", "effect", "focus_elemental", 2, TTAspects.GELUM);

        private final ResourceLocation id;
        private final String glyph;
        private final String category;
        private final String research;
        private final int complexity;
        private final ResourceLocation aspect;

        Part(String name, String glyph, String category, String research, int complexity, ResourceLocation aspect) {
            this.id = TTIds.rl(name);
            this.glyph = glyph;
            this.category = category;
            this.research = research;
            this.complexity = complexity;
            this.aspect = aspect;
        }

        public String key() {
            return id.getPath();
        }
        public String glyph() {
            return glyph;
        }
        public String category() {
            return category;
        }
        public int minDuration() {
            return this == FROST ? 2 : 0;
        }
        public int maxDuration() {
            return this == FROST ? 10 : 5;
        }
        public boolean isEffect() {
            return this == FIRE || this == FROST;
        }

        public boolean unlocked(PlayerKnowledge knowledge) {
            return research.isEmpty() || knowledge.isComplete(TTIds.rl(research));
        }

        public int capacity() {
            return switch (this) {
                case ORIGIN, TOUCH, BOLT -> 1;
                case BRANCH -> 3;
                case FIRE, FROST -> 0;
            };
        }

        public boolean accepts(@Nullable Part child) {
            return switch (this) {
                case ORIGIN -> child == BRANCH || child == TOUCH || child == BOLT;
                case BRANCH -> child == TOUCH || child == BOLT;
                case TOUCH, BOLT -> child == FIRE || child == FROST;
                case FIRE, FROST -> false;
            };
        }

        boolean hasSetting(String setting) {
            return isEffect() && (setting.equals("power") || setting.equals("duration"));
        }

        public Node node() {
            return new Node(id, List.of());
        }

        static @Nullable Part of(ResourceLocation id) {
            for (Part part : values()) {
                if (part.id.equals(id))
                    return part;
            }
            return null;
        }
    }

    public record Node(ResourceLocation part, Map<String, Integer> settings, List<Node> children) {
        public Node {
            settings = Map.copyOf(settings);
            children = List.copyOf(children);
        }

        public Part kind() {
            return Part.of(part);
        }

        public int power() {
            return settings.getOrDefault("power", 1);
        }

        public int duration() {
            return settings.getOrDefault("duration", kind() == Part.FROST ? 2 : 1);
        }

        public int complexity() {
            return kind().complexity + (kind().isEffect() ? (power() - 1) * 2 + duration() - kind().minDuration() : 0);
        }

        public Node withChildren(List<Node> replacement) {
            return new Node(part, settings, replacement);
        }

        public Node withSetting(String name, int value) {
            var replacement = new HashMap<>(settings);
            replacement.put(name, value);
            return new Node(part, replacement, children);
        }

        private Node(ResourceLocation part, List<Node> children) {
            this(part, Map.of(), children);
        }

        private void writeDraft(FriendlyByteBuf buffer) {
            buffer.writeByte(kind().ordinal());
            buffer.writeByte(settings.getOrDefault("power", -1) + 1);
            buffer.writeByte(settings.getOrDefault("duration", -1) + 1);
            buffer.writeByte(children.size());
            for (Node child : children)
                child.writeDraft(buffer);
        }

        private static Node readDraft(FriendlyByteBuf buffer, int depth) {
            int id = buffer.readUnsignedByte();
            if (id >= Part.values().length || depth > 3)
                throw new DecoderException("Invalid focus node");
            Part part = Part.values()[id];
            int power = buffer.readUnsignedByte() - 1;
            int duration = buffer.readUnsignedByte() - 1;
            int count = buffer.readUnsignedByte();
            if (count > part.capacity())
                throw new DecoderException("Too many focus children");

            var settings = new HashMap<String, Integer>();
            if (power >= 0)
                settings.put("power", power);
            if (duration >= 0)
                settings.put("duration", duration);
            var children = new ArrayList<Node>(count);
            for (int index = 0; index < count; index++)
                children.add(readDraft(buffer, depth + 1));
            return new Node(part.id, settings, children);
        }

        private static @Nullable Node read(CompoundTag tag, int depth) {
            ResourceLocation part = ResourceLocation.tryParse(tag.getString("part"));
            if (part == null || depth > 3 || tag.contains("children") && !tag.contains("children", Tag.TAG_LIST))
                return null;
            ListTag children = tag.getList("children", Tag.TAG_COMPOUND);
            if (children.size() > 3)
                return null;
            if (tag.contains("settings") && !tag.contains("settings", Tag.TAG_COMPOUND))
                return null;
            var configured = tag.getCompound("settings");
            if (configured.size() > 2)
                return null;
            var settings = new HashMap<String, Integer>();
            for (String key : configured.getAllKeys()) {
                if (!configured.contains(key, Tag.TAG_INT))
                    return null;
                settings.put(key, configured.getInt(key));
            }
            var nodes = new ArrayList<Node>();
            for (int i = 0; i < children.size(); i++) {
                Node child = read(children.getCompound(i), depth + 1);
                if (child == null)
                    return null;
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
            for (Node child : children)
                nested.add(child.save());
            result.put("children", nested);
            return result;
        }
    }
}
