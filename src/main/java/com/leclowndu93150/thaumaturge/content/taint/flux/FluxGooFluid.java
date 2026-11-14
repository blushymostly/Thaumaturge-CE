package com.leclowndu93150.thaumaturge.content.taint.flux;

import com.leclowndu93150.thaumaturge.registry.TTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

public abstract class FluxGooFluid extends ForgeFlowingFluid {
    protected FluxGooFluid() {
        super(TTFluids.GOO_PROPERTIES);
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        if (!(level instanceof ServerLevel server)) return;
        BlockState block = level.getBlockState(pos);
        if (block.getBlock() instanceof BlockFluxGoo goo) FluxFlow.spread(server, pos, block, goo, Direction.DOWN, level.random);
    }

    @Override protected boolean isRandomlyTicking() { return true; }

    @Override
    protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (level instanceof ServerLevel server && random.nextInt(4) == 0 && PhysicalFlux.placeGas(server, pos.above(), 1)) {
            PhysicalFlux.reduce(server, pos, 1);
        }
    }

    public static final class Source extends FluxGooFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static final class Flowing extends FluxGooFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
