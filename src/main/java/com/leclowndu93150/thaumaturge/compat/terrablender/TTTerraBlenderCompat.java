package com.leclowndu93150.thaumaturge.compat.terrablender;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;

/**
 * Lithostitched biome_injector requires 1.6+ (not on Forge 1.20.1 / Lithostitched 1.4.x).
 * TerraBlender places datapack biomes into the overworld climate instead.
 */
public final class TTTerraBlenderCompat {
    private TTTerraBlenderCompat() {}

    public static void init() {
        Regions.register(new MagicalForestRegion(TTIds.rl("magical_forest"), 3));
        Regions.register(new TaintedLandsRegion(TTIds.rl("tainted_lands"), 2));
    }

    private static final class MagicalForestRegion extends Region {
        MagicalForestRegion(net.minecraft.resources.ResourceLocation name, int weight) {
            super(name, RegionType.OVERWORLD, weight);
        }

        @Override
        public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
            this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
                builder.replaceBiome(Biomes.FOREST, TTBiomeKeys.MAGICAL_FOREST);
                builder.replaceBiome(Biomes.FLOWER_FOREST, TTBiomeKeys.MAGICAL_FOREST);
                builder.replaceBiome(Biomes.LUSH_CAVES, TTBiomeKeys.MAGICAL_FOREST_CAVES);
            });
        }
    }

    private static final class TaintedLandsRegion extends Region {
        TaintedLandsRegion(net.minecraft.resources.ResourceLocation name, int weight) {
            super(name, RegionType.OVERWORLD, weight);
        }

        @Override
        public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
            this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
                builder.replaceBiome(Biomes.SWAMP, TTBiomeKeys.TAINTED_LANDS);
                builder.replaceBiome(Biomes.MANGROVE_SWAMP, TTBiomeKeys.TAINTED_LANDS);
            });
        }
    }
}
