package com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class FurnacePartBlock extends BaseEntityBlock {
    private final boolean nozzle;

    public FurnacePartBlock(boolean nozzle) {
        super(Properties.of().strength(3).noOcclusion());
        this.nozzle = nozzle;
    }

    @Override
    public FurnaceNozzle newBlockEntity(BlockPos pos, BlockState state) {
        return nozzle ? new FurnaceNozzle(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !(replacement.getBlock() instanceof FurnacePartBlock)) {
            FurnaceStructure.disassemble(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
