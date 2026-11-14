package com.leclowndu93150.thaumaturge.content.infusion;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class InfusionInlay extends Block {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public InfusionInlay() {
        super(Properties.of().strength(0.2F).noCollission().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        return side == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, side, neighbor, level, pos, other);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0 && source(level, pos, 1) != null) {
            level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + 0.5, pos.getY() + 0.08, pos.getZ() + 0.5, 0, 0, 0);
        }
    }

    public static boolean isPowered(Level level, BlockPos pos) {
        return source(level, pos, 1) != null;
    }

    static @Nullable InfusionStabilizer.BlockEntity source(Level level, BlockPos pos, int minimum) {
        var seen = new HashSet<BlockPos>();
        var pending = new ArrayDeque<Step>();
        seen.add(pos);
        pending.add(new Step(pos, 0));
        InfusionStabilizer.BlockEntity best = null;
        int bestCharge = minimum - 1;
        while (!pending.isEmpty()) {
            Step step = pending.removeFirst();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = step.pos().relative(direction);
                if (!level.hasChunkAt(neighbor) || !seen.add(neighbor)) continue;
                if (level.getBlockEntity(neighbor) instanceof InfusionStabilizer.BlockEntity stabilizer) {
                    int charge = stabilizer.energy() - step.distance();
                    if (charge > bestCharge) {
                        best = stabilizer;
                        bestCharge = charge;
                    }
                } else if (step.distance() < 14 && (level.getBlockState(neighbor).getBlock() instanceof InfusionInlay
                        || level.getBlockState(neighbor).getBlock() instanceof InfusionPedestalBlock)) {
                    pending.addLast(new Step(neighbor, step.distance() + 1));
                }
            }
        }
        return best;
    }

    private record Step(BlockPos pos, int distance) {}
}
