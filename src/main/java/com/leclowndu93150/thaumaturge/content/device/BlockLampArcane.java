package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mountable arcane lamp with the CE light and redstone states. */
public final class BlockLampArcane extends Block {
    private static final int LIGHT_INTERVAL = 5;
    private static final int LIGHT_RADIUS = 15;
    private static final VoxelShape BODY = box(4, 2, 4, 12, 14, 12);
    private static final VoxelShape SIDE_X = box(2, 4, 4, 14, 12, 12);
    private static final VoxelShape SIDE_Z = box(4, 4, 2, 12, 12, 14);

    public BlockLampArcane() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(BlockStateProperties.ENABLED) ? 15 : 0).noOcclusion().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(BlockStateProperties.ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.FACING, context.getClickedFace().getOpposite())
                .setValue(BlockStateProperties.ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        boolean enabled = !level.hasNeighborSignal(pos);
        if (!level.isClientSide && enabled != state.getValue(BlockStateProperties.ENABLED)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.ENABLED, enabled), UPDATE_ALL);
            if (enabled) {
                level.scheduleTick(pos, this, LIGHT_INTERVAL);
            } else {
                clearLights(level, pos);
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && !oldState.is(this) && state.getValue(BlockStateProperties.ENABLED)) {
            level.scheduleTick(pos, this, LIGHT_INTERVAL);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!level.isClientSide && !replacement.is(this)) {
            clearLights(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(BlockStateProperties.ENABLED);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Lamps placed by older versions have no scheduled tick in their saved chunk.
        level.scheduleTick(pos, this, LIGHT_INTERVAL);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.ENABLED)) {
            return;
        }
        level.scheduleTick(pos, this, LIGHT_INTERVAL);
        BlockPos target = pos.offset(spread(random), spread(random), spread(random));
        if (!level.hasChunkAt(target)) {
            return;
        }
        int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target).getY();
        int y = Math.max(level.getMinBuildHeight() + 5, Math.min(target.getY(), surface + 4));
        tryLight(level, pos, target.atY(y));
    }

    private static int spread(RandomSource random) {
        return random.nextInt(LIGHT_RADIUS + 1) - random.nextInt(LIGHT_RADIUS + 1);
    }

    private static void tryLight(ServerLevel level, BlockPos origin, BlockPos target) {
        if (level.isOutsideBuildHeight(target) || !level.isEmptyBlock(target)
                || level.getBrightness(LightLayer.BLOCK, target) >= 11) {
            return;
        }
        Vec3 from = Vec3.atCenterOf(origin);
        Vec3 to = Vec3.atCenterOf(target);
        boolean clear = BlockGetter.traverseBlocks(from, to, level, (world, cell) -> {
            if (!world.hasChunkAt(cell)) {
                return Boolean.FALSE;
            }
            if (cell.equals(origin) || cell.equals(target)) {
                return null;
            }
            var shape = world.getBlockState(cell).getCollisionShape(world, cell);
            return shape.clip(from, to, cell) == null ? null : Boolean.FALSE;
        }, world -> Boolean.TRUE);
        if (clear) {
            level.setBlock(target, glimmer().defaultBlockState(), UPDATE_ALL);
        }
    }

    private static void clearLights(Level level, BlockPos origin) {
        Block glimmer = glimmer();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-LIGHT_RADIUS, -LIGHT_RADIUS, -LIGHT_RADIUS),
                origin.offset(LIGHT_RADIUS, LIGHT_RADIUS, LIGHT_RADIUS))) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(glimmer)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
            }
        }
    }

    private static Block glimmer() {
        return TTBulkContent.BLOCKS.get("effect_glimmer").get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.FACING).getAxis()) {
            case X -> SIDE_X;
            case Y -> BODY;
            case Z -> SIDE_Z;
        };
    }
}
