package com.leclowndu93150.thaumaturge.content.world.crystal;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** World-gen vis crystal cluster cell. Drops aspected essentia_crystal stacks. */
public final class BlockCrystal extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
    /** Bitmask of sturdy support faces (Direction.ordinal bits). Forge 1.20.1 has no block ModelData. */
    public static final IntegerProperty SUPPORT = IntegerProperty.create("support", 0, 63);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ResourceLocation aspect;

    public BlockCrystal(Properties properties, ResourceLocation aspect) {
        super(properties);
        this.aspect = aspect;
        registerDefaultState(stateDefinition.any().setValue(SIZE, 0).setValue(SUPPORT, 1 << Direction.DOWN.ordinal()).setValue(WATERLOGGED, false));
    }

    public ResourceLocation aspect() {
        return aspect;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, SUPPORT, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CrystalShapes.shapeFor(level, pos);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction side : DIRECTIONS) {
            if (CrystalShards.supports(level, pos, side)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER)
                .setValue(SUPPORT, CrystalShards.supportMask(context.getLevel(), context.getClickedPos()));
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        int mask = CrystalShards.supportMask(level, pos);
        if (state.getValue(SUPPORT) != mask) {
            return state.setValue(SUPPORT, mask);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, neighborPos, movedByPiston);
        if (!canSurvive(state, level, pos)) {
            return;
        }
        int mask = CrystalShards.supportMask(level, pos);
        if (state.getValue(SUPPORT) != mask && !level.isClientSide()) {
            level.setBlock(pos, state.setValue(SUPPORT, mask), Block.UPDATE_ALL);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int count = Math.max(1, state.getValue(SIZE));
        ItemStack drop = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), count);
        TTItemData.setCrystalAspect(drop, new AspectInstance(aspect, 1));
        return Collections.singletonList(drop);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get());
        TTItemData.setCrystalAspect(stack, new AspectInstance(aspect, 1));
        return stack;
    }
}
