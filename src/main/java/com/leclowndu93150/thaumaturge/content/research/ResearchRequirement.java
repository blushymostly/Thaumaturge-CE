package com.leclowndu93150.thaumaturge.content.research;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.registry.TTCommonTags;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/**
 * An obtain or craft requirement on a research stage.
 *
 * <p>The JSON {@code items} field is either a single string or a list of strings; entries starting with {@code #}
 * name an item tag, anything else is an item id. Supported CE components match Forge item NBT.
 */
public record ResearchRequirement(List<ResourceLocation> items, List<ResourceLocation> tags, int amount, Components components) {
    private static final Codec<List<String>> SELECTOR_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(single -> List.of(single), list -> list), list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public static final Codec<ResearchRequirement> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(SELECTOR_CODEC.fieldOf("items").forGetter(ResearchRequirement::selectors),
                    Codec.INT.fieldOf("amount").forGetter(ResearchRequirement::amount),
                    Components.CODEC.optionalFieldOf("components", Components.EMPTY).forGetter(ResearchRequirement::components))
            .apply(instance, ResearchRequirement::fromSelectors));

    public ResearchRequirement(List<ResourceLocation> items, List<ResourceLocation> tags, int amount) {
        this(items, tags, amount, Components.EMPTY);
    }

    private static ResearchRequirement fromSelectors(List<String> selectors, int amount, Components components) {
        List<ResourceLocation> items = new ArrayList<>();
        List<ResourceLocation> tags = new ArrayList<>();
        for (String selector : selectors) {
            boolean tag = selector.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? selector.substring(1) : selector);
            if (id != null) {
                (tag ? tags : items).add(tag ? TTCommonTags.remapItemTag(id) : id);
            }
        }
        return new ResearchRequirement(List.copyOf(items), List.copyOf(tags), amount, components);
    }

    private List<String> selectors() {
        List<String> out = new ArrayList<>();
        for (ResourceLocation tag : tags) {
            out.add("#" + tag);
        }
        for (ResourceLocation item : items) {
            out.add(item.toString());
        }
        return out;
    }

    /** Item ids named directly plus the current members of every referenced tag. */
    public List<ResourceLocation> candidateIds() {
        if (tags.isEmpty()) {
            return items;
        }
        Set<ResourceLocation> ids = new LinkedHashSet<>(items);
        for (ResourceLocation tag : tags) {
            BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                    .ifPresent(named -> named.forEach(holder -> ids.add(BuiltInRegistries.ITEM.getKey(holder.value()))));
        }
        return List.copyOf(ids);
    }

    public boolean isEmpty() {
        return candidateIds().isEmpty();
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !components.matches(stack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (items.contains(id)) {
            return true;
        }
        for (ResourceLocation tag : tags) {
            if (stack.is(TagKey.create(Registries.ITEM, tag))) {
                return true;
            }
        }
        return false;
    }

    public boolean obtainMet(Player player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matches(stack)) {
                total += stack.getCount();
            }
        }
        return total >= amount;
    }

    /** The {@code index}-th candidate item (wrapping), or {@link ItemStack#EMPTY} when nothing resolves. */
    public ItemStack stackAt(long index) {
        List<ResourceLocation> ids = candidateIds();
        if (ids.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(ids.get((int) Math.floorMod(index, (long) ids.size())));
        ItemStack stack = new ItemStack(item);
        components.crystal().ifPresent(aspect -> TTItemData.setCrystalAspect(stack, new AspectInstance(aspect, 1)));
        if (!components.aspects().isEmpty()) {
            TTItemData.setAspects(stack, components.aspects());
        }
        return stack;
    }

    public record Components(Optional<ResourceLocation> crystal, AspectList aspects) {
        private static final Components EMPTY = new Components(Optional.empty(), AspectList.EMPTY);
        private static final Codec<Components> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("aspect").codec().optionalFieldOf("thaumaturge:crystal_aspect").forGetter(Components::crystal),
                AspectList.CODEC.optionalFieldOf("thaumaturge:aspects", AspectList.EMPTY).forGetter(Components::aspects)
        ).apply(instance, Components::new));

        private boolean matches(ItemStack stack) {
            if (crystal.isPresent()) {
                AspectInstance stored = TTItemData.getCrystalAspect(stack);
                if (stored == null || !stored.aspectId().equals(crystal.get())) {
                    return false;
                }
            }
            AspectList stored = TTItemData.getAspects(stack);
            return aspects.isEmpty() || stored.size() == aspects.size() && aspects.entries().stream()
                    .allMatch(entry -> stored.amountOf(entry.aspectId()) == entry.amount());
        }
    }
}
