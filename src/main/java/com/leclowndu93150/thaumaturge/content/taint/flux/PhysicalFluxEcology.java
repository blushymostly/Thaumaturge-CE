package com.leclowndu93150.thaumaturge.content.taint.flux;

import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.taint.EtherealBloom;
import com.leclowndu93150.thaumaturge.content.taint.TaintSeedEntity;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

public final class PhysicalFluxEcology {
    private PhysicalFluxEcology() {}

    public static float auraFloor(LevelChunk chunk, List<BlockPos> cells, int base) {
        float floor = 0.0F;
        for (BlockPos pos : cells) {
            var state = chunk.getBlockState(pos);
            if (state.getBlock() instanceof PhysicalFluxBlock flux) floor += flux.fluxAmount(state) * flux.auraFloorPerQuantum();
        }
        return Math.min(floor, Math.min(30.0F, base * 0.3F));
    }

    public static void tryOutbreak(ServerLevel level, LevelChunk chunk, List<BlockPos> cells, RandomSource random) {
        if (cells.isEmpty() || ThaumaturgeCommonConfig.WUSS_MODE.get() || !ThaumaturgeCommonConfig.PHYSICAL_FLUX_TAINT_OUTBREAKS.get()
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
        float total = 0;
        float[] weights = new float[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            var state = chunk.getBlockState(cells.get(i));
            if (state.getBlock() instanceof PhysicalFluxBlock flux) {
                weights[i] = flux.fluxAmount(state) * flux.taintWeightPerQuantum();
                total += weights[i];
            }
        }
        if (total <= 10 || random.nextFloat() >= Math.min(0.18F, (total - 10) / 300)) return;
        float roll = random.nextFloat() * total;
        int first = 0;
        while (first < weights.length - 1 && (roll -= weights[first]) > 0) first++;
        for (int offset = 0; offset < cells.size(); offset++) {
            BlockPos source = cells.get((first + offset) % cells.size());
            if (EtherealBloom.protects(level, source)) continue;
            var state = level.getBlockState(source);
            if (!(state.getBlock() instanceof PhysicalFluxBlock flux)) continue;
            for (int depth = 0; depth <= 24; depth++) {
                BlockPos at = source.below(depth);
                if (level.isOutsideBuildHeight(at.below())) break;
                if ((level.isEmptyBlock(at) || PhysicalFlux.isPhysicalFlux(level.getBlockState(at)))
                        && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
                    if (plantSeed(level, at)) {
                        PhysicalFlux.reduce(level, source, flux.outbreakCost());
                        return;
                    }
                    break;
                }
            }
        }
    }

    public static boolean plantSeed(ServerLevel level, BlockPos pos) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || EtherealBloom.protects(level, pos)
                || !level.getEntitiesOfClass(TaintSeedEntity.class, new AABB(pos).inflate(12)).isEmpty()) return false;
        TaintSeedEntity seed = TTEntities.TAINT_SEED.get().create(level);
        if (seed == null) return false;
        seed.setPersistenceRequired();
        seed.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return level.addFreshEntity(seed);
    }
}
