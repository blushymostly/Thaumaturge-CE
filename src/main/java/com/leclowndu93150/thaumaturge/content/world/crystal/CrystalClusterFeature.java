package com.leclowndu93150.thaumaturge.content.world.crystal;

import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalClusterConfig.Entry;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraftforge.common.Tags;

/** Places small underground patches of primal vis crystals. */
public final class CrystalClusterFeature extends Feature<CrystalClusterConfig> {
    private static final int CLUSTER_SPREAD = 6;
    private static final int CLUSTER_OFFSET = 8;
    private static final int SURFACE_MARGIN = 5;
    private static final int CELL_REACH = 1;
    private static final int CELL_SKIP_ONE_IN = 3;
    private static final int SIZE_VARIATION = 3;
    private static final int PLACE_FLAGS = 19;
    private static final int OVERSHOOT_ALLOWANCE = 1;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final List<Vec3i> RADIAL_OFFSETS = buildRadialOffsets();

    public CrystalClusterFeature(Codec<CrystalClusterConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CrystalClusterConfig> context) {
        CrystalClusterConfig config = context.config();
        if (config.crystals().isEmpty()) {
            return false;
        }
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int attemptsLeft = config.attempts();
        int sizeLeft = config.maxTotal() + OVERSHOOT_ALLOWANCE;
        boolean placed = false;
        while (attemptsLeft-- > 0 && sizeLeft > 0) {
            BlockPos center = config.atOrigin()
                    ? context.origin()
                    : pickUnderground(level, random, context.origin());
            if (center == null) {
                continue;
            }
            Entry entry = config.crystals().get(random.nextInt(config.crystals().size()));
            BlockState base = entry.block().defaultBlockState();
            if (!base.hasProperty(BlockCrystal.SIZE)) {
                continue;
            }
            List<BlockPos> cells = planCells(level, random, center);
            int added = placeCells(level, random, base, cells);
            sizeLeft -= added;
            placed |= added > 0;
        }
        return placed;
    }

    private static BlockPos pickUnderground(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int z = origin.getZ() + CLUSTER_OFFSET + random.nextInt(2 * CLUSTER_SPREAD + 1) - CLUSTER_SPREAD;
        int x = origin.getX() + CLUSTER_OFFSET + random.nextInt(2 * CLUSTER_SPREAD + 1) - CLUSTER_SPREAD;
        int floor = level.getMinBuildHeight();
        int ceiling = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - SURFACE_MARGIN;
        if (ceiling < floor) {
            return null;
        }
        return new BlockPos(x, floor + random.nextInt(ceiling - floor + 1), z);
    }

    private static List<Vec3i> buildRadialOffsets() {
        List<Vec3i> offsets = new ArrayList<>();
        for (int dx = -CELL_REACH; dx <= CELL_REACH; dx++) {
            for (int dy = -CELL_REACH; dy <= CELL_REACH; dy++) {
                for (int dz = -CELL_REACH; dz <= CELL_REACH; dz++) {
                    offsets.add(new Vec3i(dx, dy, dz));
                }
            }
        }
        offsets.sort(Comparator.comparingDouble(offset -> offset.distSqr(Vec3i.ZERO)));
        return List.copyOf(offsets);
    }

    private static List<BlockPos> planCells(WorldGenLevel level, RandomSource random, BlockPos center) {
        List<BlockPos> candidates = new ArrayList<>(RADIAL_OFFSETS.size());
        for (Vec3i offset : RADIAL_OFFSETS) {
            candidates.add(center.offset(offset));
        }
        candidates.removeIf(pos -> !canHold(level, pos));
        candidates.removeIf(pos -> random.nextInt(CELL_SKIP_ONE_IN) == 0);
        return candidates;
    }

    private static int placeCells(WorldGenLevel level, RandomSource random, BlockState base, List<BlockPos> cells) {
        int total = 0;
        for (BlockPos pos : cells) {
            int size = 1 + random.nextInt(SIZE_VARIATION);
            int support = CrystalShards.supportMask(level, pos);
            if (support == 0) {
                support = 1 << net.minecraft.core.Direction.DOWN.ordinal();
            }
            level.setBlock(pos, base
                    .setValue(BlockCrystal.SIZE, Math.min(3, size))
                    .setValue(BlockCrystal.SUPPORT, support), PLACE_FLAGS);
            total += size;
        }
        return total;
    }

    private static boolean canHold(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty() || !(state.isAir() || state.canBeReplaced())) {
            return false;
        }
        for (Direction direction : DIRECTIONS) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (neighbour.is(BlockTags.BASE_STONE_OVERWORLD) || neighbour.is(Tags.Blocks.COBBLESTONE)
                    || neighbour.is(Tags.Blocks.ORES) || neighbour.is(BlockTags.BASE_STONE_NETHER)
                    || neighbour.is(BlockTags.STONE_ORE_REPLACEABLES) || neighbour.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                return true;
            }
        }
        return false;
    }
}
