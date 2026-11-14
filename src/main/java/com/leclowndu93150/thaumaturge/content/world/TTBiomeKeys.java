package com.leclowndu93150.thaumaturge.content.world;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class TTBiomeKeys {
    public static final ResourceKey<Biome> MAGICAL_FOREST = key("magical_forest");
    public static final ResourceKey<Biome> MAGICAL_FOREST_CAVES = key("magical_forest_caves");
    public static final ResourceKey<Biome> TAINTED_LANDS = key("tainted_lands");

    private TTBiomeKeys() {}

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, TTIds.rl(path));
    }
}
