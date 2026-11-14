package com.leclowndu93150.thaumaturge.content.world.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** CE sapling: vanilla growth, but bonemeal only succeeds 25% of the time. */
public final class BlockSaplingTT extends SaplingBlock {
    private static final float BONEMEAL_SUCCESS_CHANCE = 0.25F;

    public BlockSaplingTT(AbstractTreeGrower treeGrower, BlockBehaviour.Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }
}
