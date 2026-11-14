package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.TTIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Static biome -> aura modifier table (Forge replacement for CE's {@code BIOME_AURA_MODIFIER} data map).
 * Values mirror CE's AuraModifierProvider; biomes not present fall back to {@link #DEFAULT_MODIFIER}.
 */
public final class BiomeAuraModifiers {
    public static final float DEFAULT_MODIFIER = 0.5F;
    private static final Map<ResourceLocation, Float> VALUES = new HashMap<>();

    static {
        // Overworld
        vanilla("plains", 0.3F);
        vanilla("sunflower_plains", 0.3F);
        vanilla("snowy_plains", 0.275F);
        vanilla("ice_spikes", 0.25F);
        vanilla("desert", 0.25F);
        vanilla("swamp", 0.5F);
        vanilla("mangrove_swamp", 0.55F);
        vanilla("forest", 0.5F);
        vanilla("flower_forest", 0.5F);
        vanilla("birch_forest", 0.5F);
        vanilla("dark_forest", 0.5F);
        vanilla("old_growth_birch_forest", 0.5F);
        vanilla("old_growth_pine_taiga", 0.4F);
        vanilla("old_growth_spruce_taiga", 0.4F);
        vanilla("taiga", 0.4F);
        vanilla("snowy_taiga", 0.35F);
        vanilla("savanna", 0.25F);
        vanilla("savanna_plateau", 0.25F);
        vanilla("windswept_hills", 0.3F);
        vanilla("windswept_gravelly_hills", 0.3F);
        vanilla("windswept_forest", 0.4F);
        vanilla("windswept_savanna", 0.275F);
        vanilla("jungle", 0.6F);
        vanilla("sparse_jungle", 0.4F);
        vanilla("bamboo_jungle", 0.65F);
        vanilla("badlands", 0.33F);
        vanilla("eroded_badlands", 0.33F);
        vanilla("wooded_badlands", 0.4F);
        vanilla("meadow", 0.4F);
        vanilla("cherry_grove", 0.6F);
        vanilla("grove", 0.4F);
        vanilla("snowy_slopes", 0.275F);
        vanilla("frozen_peaks", 0.275F);
        vanilla("jagged_peaks", 0.3F);
        vanilla("stony_peaks", 0.3F);
        vanilla("river", 0.4F);
        vanilla("frozen_river", 0.35F);
        vanilla("beach", 0.3F);
        vanilla("snowy_beach", 0.275F);
        vanilla("stony_shore", 0.3F);
        vanilla("warm_ocean", 0.33F);
        vanilla("lukewarm_ocean", 0.33F);
        vanilla("deep_lukewarm_ocean", 0.33F);
        vanilla("ocean", 0.33F);
        vanilla("deep_ocean", 0.33F);
        vanilla("cold_ocean", 0.33F);
        vanilla("deep_cold_ocean", 0.33F);
        vanilla("frozen_ocean", 0.3F);
        vanilla("deep_frozen_ocean", 0.3F);
        vanilla("mushroom_fields", 0.75F);
        vanilla("dripstone_caves", 0.3F);
        vanilla("lush_caves", 0.5F);
        vanilla("deep_dark", 0.1F);
        // Nether / End
        vanilla("nether_wastes", 0.125F);
        vanilla("warped_forest", 0.125F);
        vanilla("crimson_forest", 0.125F);
        vanilla("soul_sand_valley", 0.125F);
        vanilla("basalt_deltas", 0.125F);
        vanilla("the_end", 0.125F);
        vanilla("end_highlands", 0.125F);
        vanilla("end_midlands", 0.125F);
        vanilla("small_end_islands", 0.125F);
        vanilla("end_barrens", 0.125F);
        vanilla("the_void", 0.0F);
        // Thaumaturge
        VALUES.put(TTIds.rl("magical_forest"), 0.625F);
        VALUES.put(TTIds.rl("magical_forest_caves"), 0.625F);
        VALUES.put(TTIds.rl("eerie"), 0.625F);
        VALUES.put(TTIds.rl("eldritch"), 0.458F);
        VALUES.put(TTIds.rl("tainted_lands"), 0.45F);
    }

    private BiomeAuraModifiers() {}

    private static void vanilla(String path, float value) {
        VALUES.put(new ResourceLocation("minecraft", path), value);
    }

    public static float get(ResourceLocation biomeId) {
        return VALUES.getOrDefault(biomeId, DEFAULT_MODIFIER);
    }

    public static boolean isTainted(ResourceLocation biomeId) {
        return TTIds.rl("tainted_lands").equals(biomeId);
    }
}
