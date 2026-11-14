package com.leclowndu93150.thaumaturge.content.infernalfurnace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class InfernalShellBlock extends Block {
    public InfernalShellBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (level.hasChunkAt(at) && level.getBlockState(at).getBlock() instanceof InfernalFurnaceBlock) {
                    level.setBlock(at, Blocks.LAVA.defaultBlockState(), UPDATE_ALL);
                    break;
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
