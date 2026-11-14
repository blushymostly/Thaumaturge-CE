package com.leclowndu93150.thaumaturge.content.essentia.tube;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Forge port of CE26 {@code BlockTube} / {@code BlockEssentiaTransport}: six connection flags, geometry from CE. */
public class TubeBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        // CE TubeGeometry.ARM: flange + stem, authored pointing down, rotated onto each side.
        double[][] downBoxes = {{5, 0, 5, 11, 2, 11}, {6, 2, 6, 10, 6, 10}};
        for (Direction dir : Direction.values()) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : downBoxes) {
                shape = Shapes.or(shape, rotated(dir, b));
            }
            ARMS.put(dir, shape);
        }
    }

    public TubeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    private static VoxelShape rotated(Direction dir, double[] b) {
        double[] a = {b[0], b[1], b[2]};
        double[] c = {b[3], b[4], b[5]};
        double[] lo = new double[3];
        double[] hi = new double[3];
        double[] p = transform(dir, a);
        double[] q = transform(dir, c);
        for (int i = 0; i < 3; i++) {
            lo[i] = Math.min(p[i], q[i]);
            hi[i] = Math.max(p[i], q[i]);
        }
        return Block.box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    private static double[] transform(Direction dir, double[] v) {
        double x = v[0];
        double y = v[1];
        double z = v[2];
        return switch (dir) {
            case DOWN -> new double[]{x, y, z};
            case UP -> new double[]{x, 16 - y, z};
            case NORTH -> new double[]{x, z, y};
            case SOUTH -> new double[]{x, z, 16 - y};
            case WEST -> new double[]{y, z, x};
            case EAST -> new double[]{16 - y, z, x};
        };
    }

    public static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(propertyFor(direction))) {
                shape = Shapes.or(shape, ARMS.get(direction));
            }
        }
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TTBlockEntities.TUBE.get(), TubeBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return recomputeConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), connects(level, pos, direction));
    }

    public static BlockState recomputeConnections(BlockState state, LevelReader level, BlockPos pos) {
        BlockState result = state;
        for (Direction side : Direction.values()) {
            result = result.setValue(propertyFor(side), connects(level, pos, side));
        }
        return result;
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction side) {
        BlockPos neighbour = pos.relative(side);
        if (!level.hasChunkAt(neighbour)) {
            return false;
        }
        Direction touching = side.getOpposite();
        IEssentiaHandler peer = EssentiaAccess.handler(level, neighbour, touching);
        return peer != null && peer.isConnectable(touching);
    }
}
