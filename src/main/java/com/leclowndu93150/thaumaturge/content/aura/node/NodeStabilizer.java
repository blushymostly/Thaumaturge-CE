package com.leclowndu93150.thaumaturge.content.aura.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NodeStabilizer extends Block {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);
    private final int tier;

    public NodeStabilizer(int tier) {
        super(Properties.of().strength(3).noOcclusion());
        this.tier = tier;
    }

    int tier() {
        return tier;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
