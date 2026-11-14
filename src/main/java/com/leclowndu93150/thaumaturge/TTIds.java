package com.leclowndu93150.thaumaturge;

import net.minecraft.resources.ResourceLocation;

public final class TTIds {
    public static final String MODID = "thaumaturge";
    public static final String CURIOS = "curios";
    public static final String DISTANT_HORIZONS = "distanthorizons";
    public static final String FTB_LIBRARY = "ftblibrary";
    public static final String IRIS = "iris";
    public static final String DYNAMIC_TREES = "dynamictrees";
    public static final String BIOMES_O_PLENTY = "biomesoplenty";

    private TTIds() {}

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static ResourceLocation rl(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
