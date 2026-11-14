package com.leclowndu93150.thaumaturge.content.taint.flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

final class FluxFlow {
    private FluxFlow() {}

    static void spread(ServerLevel level, BlockPos pos, BlockState state, PhysicalFluxBlock flux, Direction vertical, RandomSource random) {
        int amount = flux.fluxAmount(state);
        BlockPos next = pos.relative(vertical);
        if (level.isOutsideBuildHeight(next)) {
            set(level, pos, flux, 0);
            return;
        }
        BlockState target = level.getBlockState(next);
        if (target.getBlock() instanceof LiquidBlock && !PhysicalFlux.isPhysicalFlux(target)) {
            set(level, next, flux, amount);
            level.setBlock(pos, target, Block.UPDATE_ALL);
            return;
        }
        if (accepts(state, target)) {
            int held = target.is(state.getBlock()) ? flux.fluxAmount(target) : 0;
            int moved = Math.min(amount, PhysicalFlux.MAX_QUANTA - held);
            if (moved > 0) {
                set(level, next, flux, held + moved);
                amount -= moved;
                set(level, pos, flux, amount);
            }
        }
        if (amount < 2) return;

        List<BlockPos> cells = new ArrayList<>(5);
        cells.add(pos);
        int total = amount;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos other = pos.relative(side);
            if (!level.hasChunkAt(other)) continue;
            BlockState otherState = level.getBlockState(other);
            int held = otherState.is(state.getBlock()) ? flux.fluxAmount(otherState) : 0;
            if (held <= amount - 2 && accepts(state, otherState)) {
                cells.add(other);
                total += held;
            }
        }
        if (cells.size() == 1) return;
        Collections.shuffle(cells, new java.util.Random(random.nextLong()));
        for (int i = 0; i < cells.size(); i++) {
            BlockPos cell = cells.get(i);
            int share = total / cells.size() + (i < total % cells.size() ? 1 : 0);
            if (PhysicalFlux.amount(level, cell) != share) set(level, cell, flux, share);
        }
    }

    private static boolean accepts(BlockState source, BlockState target) {
        return target.is(source.getBlock()) || !PhysicalFlux.isPhysicalFlux(target)
                && target.getFluidState().isEmpty() && target.canBeReplaced();
    }

    private static void set(ServerLevel level, BlockPos pos, PhysicalFluxBlock flux, int amount) {
        level.setBlock(pos, amount > 0 ? flux.withFluxAmount(amount) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (amount > 0) flux.scheduleFluxTick(level, pos);
    }
}
