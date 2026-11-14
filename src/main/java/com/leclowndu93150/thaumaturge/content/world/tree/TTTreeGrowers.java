package com.leclowndu93150.thaumaturge.content.world.tree;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/** Vanilla {@link AbstractTreeGrower}s that feed the sapling blocks (single-tree only, like CE). */
public final class TTTreeGrowers {
    public static final ResourceKey<ConfiguredFeature<?, ?>> GREATWOOD_TREE_GROWN = key("greatwood_tree_grown");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD_TREE_GROWN = key("silverwood_tree_grown");

    public static final AbstractTreeGrower GREATWOOD = new Grower(GREATWOOD_TREE_GROWN);
    public static final AbstractTreeGrower SILVERWOOD = new Grower(SILVERWOOD_TREE_GROWN);

    private TTTreeGrowers() {}

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, TTIds.rl(path));
    }

    private static final class Grower extends AbstractTreeGrower {
        private final ResourceKey<ConfiguredFeature<?, ?>> feature;

        private Grower(ResourceKey<ConfiguredFeature<?, ?>> feature) {
            this.feature = feature;
        }

        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return feature;
        }
    }
}
