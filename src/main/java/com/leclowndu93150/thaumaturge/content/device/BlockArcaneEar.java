package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class BlockArcaneEar extends BaseEntityBlock {
    private static final VoxelShape EAR = Shapes.or(
            box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0),
            box(5.0, 2.0, 5.0, 11.0, 3.0, 11.0),
            box(7.0, 3.0, 7.0, 9.0, 6.0, 9.0),
            box(6.0, 6.0, 6.0, 10.0, 7.0, 10.0),
            box(5.0, 7.0, 5.0, 11.0, 8.0, 11.0),
            box(4.0, 8.0, 4.0, 5.0, 10.0, 12.0),
            box(11.0, 8.0, 5.0, 12.0, 10.0, 11.0),
            box(5.0, 8.0, 4.0, 12.0, 10.0, 5.0),
            box(5.0, 8.0, 11.0, 12.0, 10.0, 12.0));
    private static final VoxelShape SWITCH_OFF = Shapes.or(box(5.0, 2.0, 12.0, 6.0, 5.0, 13.0), box(6.0, 2.0, 12.0, 9.0, 3.0, 13.0));
    private static final VoxelShape SWITCH_ON = Shapes.or(box(5.0, 2.0, 12.0, 9.0, 3.0, 13.0), box(8.0, 3.0, 12.0, 9.0, 5.0, 13.0));
    private static final Map<Direction, VoxelShape> EAR_SHAPES = DeviceShapes.facingShapesFromUp(EAR);
    private static final Map<Direction, VoxelShape> TOGGLE_OFF_SHAPES = DeviceShapes.facingShapesFromUp(Shapes.or(EAR, SWITCH_OFF));
    private static final Map<Direction, VoxelShape> TOGGLE_ON_SHAPES = DeviceShapes.facingShapesFromUp(Shapes.or(EAR, SWITCH_ON));

    private static final int FULL_SIGNAL = 15;
    private static final int NO_SIGNAL = 0;

    private final boolean toggle;

    public BlockArcaneEar(boolean toggle) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
        this.toggle = toggle;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.UP).setValue(BlockStateProperties.ENABLED, false));
    }

    public boolean isToggle() {
        return toggle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.ENABLED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeTable(state).get(state.getValue(BlockStateProperties.FACING));
    }

    private Map<Direction, VoxelShape> shapeTable(BlockState state) {
        if (!toggle) {
            return EAR_SHAPES;
        }
        return state.getValue(BlockStateProperties.ENABLED) ? TOGGLE_ON_SHAPES : TOGGLE_OFF_SHAPES;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.FACING, context.getClickedFace());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction toward = state.getValue(BlockStateProperties.FACING).getOpposite();
        BlockPos base = pos.relative(toward);
        return level.getBlockState(base).isFaceSturdy(level, base, toward.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? super.updateShape(state, direction, neighborState, level, pos, neighborPos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityArcaneEar(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        refreshTone(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        refreshTone(level, pos);
    }

    private static void refreshTone(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BlockEntityArcaneEar ear) {
            ear.updateTone();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityArcaneEar ear) {
            ear.changePitch();
            ear.playNote();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(BlockStateProperties.ENABLED) ? FULL_SIGNAL : NO_SIGNAL;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, TTBlockEntities.ARCANE_EAR.get(), BlockEntityArcaneEar::serverTick);
    }
}
