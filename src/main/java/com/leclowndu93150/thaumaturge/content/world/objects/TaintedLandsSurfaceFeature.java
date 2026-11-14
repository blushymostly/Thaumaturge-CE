package com.leclowndu93150.thaumaturge.content.world.objects;

import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

public final class TaintedLandsSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int LANDMARK_RADIUS_CHUNKS = 4;
    private static final int LANDMARK_ATTEMPTS = 48;

    public TaintedLandsSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        var crust = TTBulkContent.BLOCKS.get("taint_crust").get().defaultBlockState();
        var fibre = TTBulkContent.BLOCKS.get("taint_fibre").get().defaultBlockState();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        int blobs = random.nextInt(3);
        for (int i = 0; i < blobs; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int radius = 1 + random.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + random.nextInt(2)) continue;
                    var ground = ground(level, x + dx, z + dz);
                    if (ground != null && level.getBlockState(ground).getDestroySpeed(level, ground) >= 0) {
                        placed |= level.setBlock(ground, crust, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        for (int i = 0; i < 18; i++) {
            var ground = ground(level, minX + random.nextInt(16), minZ + random.nextInt(16));
            if (ground != null && (i >= 10 || level.getBlockState(ground).is(BlockTags.DIRT))
                    && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                var surface = ground.above();
                var state = level.getBlockState(surface);
                if (state.canBeReplaced() && state.getFluidState().isEmpty()) {
                    placed |= level.setBlock(surface, fibre, Block.UPDATE_CLIENTS);
                }
            }
        }
        if (!ThaumaturgeCommonConfig.WUSS_MODE.get() && isLandmarkChunk(level, minX >> 4, minZ >> 4)) {
            placed |= placeLandmark(level, random, minX, minZ, NodeType.TAINTED);
            placed |= placeLandmark(level, random, minX, minZ, NodeType.HUNGRY);
        }
        return placed;
    }

    private static boolean placeLandmark(WorldGenLevel level, RandomSource random, int minX, int minZ, NodeType type) {
        for (int attempt = 0; attempt < LANDMARK_ATTEMPTS; attempt++) {
            var ground = ground(level, minX + random.nextInt(16), minZ + random.nextInt(16));
            if (ground == null || ground.getY() + 1 >= level.getMaxBuildHeight()) continue;
            var pos = ground.above();
            var state = level.getBlockState(pos);
            if (state.canBeReplaced() && state.getFluidState().isEmpty() && level.getBiome(pos).is(TTBiomeKeys.TAINTED_LANDS)
                    && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                return NodeGenerator.createLandmarkNodeAt(level, pos, random, type);
            }
        }
        return false;
    }

    private static boolean isLandmarkChunk(WorldGenLevel level, int x, int z) {
        var chunkSource = level.getLevel().getChunkSource();
        var source = chunkSource.getGenerator().getBiomeSource();
        var sampler = chunkSource.randomState().sampler();
        int y = QuartPos.fromBlock(level.getSeaLevel());
        if (!source.getNoiseBiome(QuartPos.fromSection(x) + 2, y, QuartPos.fromSection(z) + 2, sampler).is(TTBiomeKeys.TAINTED_LANDS)) return false;
        long seed = level.getSeed();
        long rank = landmarkRank(seed, x, z);
        for (int dx = -LANDMARK_RADIUS_CHUNKS; dx <= LANDMARK_RADIUS_CHUNKS; dx++) {
            for (int dz = -LANDMARK_RADIUS_CHUNKS; dz <= LANDMARK_RADIUS_CHUNKS; dz++) {
                if (Long.compareUnsigned(landmarkRank(seed, x + dx, z + dz), rank) < 0
                        && source.getNoiseBiome(QuartPos.fromSection(x + dx) + 2, y, QuartPos.fromSection(z + dz) + 2, sampler).is(TTBiomeKeys.TAINTED_LANDS)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long landmarkRank(long seed, int x, int z) {
        long rank = seed ^ x * 341873128712L ^ z * 132897987541L;
        rank = (rank ^ rank >>> 30) * 0xbf58476d1ce4e5b9L;
        rank = (rank ^ rank >>> 27) * 0x94d049bb133111ebL;
        return rank ^ rank >>> 31;
    }

    private static @Nullable BlockPos ground(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 32); y--) {
            var pos = new BlockPos(x, y, z);
            if (!level.getBiome(pos).is(TTBiomeKeys.TAINTED_LANDS)) return null;
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return null;
            if (!state.canBeReplaced() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) return pos;
        }
        return null;
    }
}
