package com.leclowndu93150.thaumaturge.registry;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Remaps NeoForge {@code c:} item tags (CE datapacks) to Forge 1.20.1 {@code forge:} tags.
 */
public final class TTCommonTags {
    private static final Map<String, String> PATH_ALIASES = Map.ofEntries(
            Map.entry("glass_blocks", "glass"),
            Map.entry("cobblestones", "cobblestone"),
            Map.entry("gravels", "gravel"),
            Map.entry("netherracks", "netherrack"),
            Map.entry("obsidians/normal", "obsidian"),
            Map.entry("obsidians/crying", "obsidian"),
            Map.entry("leathers", "leather"),
            Map.entry("bones", "bones"),
            Map.entry("end_stones", "end_stones"),
            Map.entry("ender_pearls", "ender_pearls"),
            Map.entry("feathers", "feathers"),
            Map.entry("gunpowders", "gunpowder"),
            Map.entry("nether_stars", "nether_stars"),
            Map.entry("eggs", "eggs"),
            Map.entry("music_discs", "music_discs"),
            Map.entry("fence_gates/wooden", "fence_gates/wooden"),
            Map.entry("chests/wooden", "chests/wooden"),
            Map.entry("buds", "buds"),
            Map.entry("clusters", "clusters"),
            Map.entry("bricks/nether", "ingots/nether_brick"),
            Map.entry("concrete_powders", "concrete_powders"),
            Map.entry("concretes", "concretes"),
            Map.entry("glazed_terracottas", "glazed_terracotta")
    );

    private TTCommonTags() {}

    public static ResourceLocation remapItemTag(ResourceLocation id) {
        if (id == null || !"c".equals(id.getNamespace())) {
            return id;
        }
        String path = PATH_ALIASES.getOrDefault(id.getPath(), id.getPath());
        return new ResourceLocation("forge", path);
    }
}
