package com.leclowndu93150.thaumaturge.content.crucible;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class CrucibleBlock extends BaseEntityBlock {
    /** CE hollow cauldron collision so thrown items fall inside and hit {@link #entityInside}. */
    private static final double[][] SHAPE_BOXES = {
            {0.0, 0.1875, 0.0, 0.125, 1.0, 1.0},
            {0.125, 0.1875, 0.125, 0.875, 0.19, 0.875},
            {0.875, 0.1875, 0.0, 1.0, 1.0, 1.0},
            {0.125, 0.1875, 0.0, 0.875, 1.0, 0.125},
            {0.125, 0.1875, 0.875, 0.875, 1.0, 1.0},
            {0.0, 0.0, 0.0, 0.1875, 0.1875, 0.125},
            {0.0, 0.0, 0.125, 0.125, 0.1875, 0.1875},
            {0.8125, 0.0, 0.0, 1.0, 0.1875, 0.125},
            {0.875, 0.0, 0.125, 1.0, 0.1875, 0.1875},
            {0.0, 0.0, 0.875, 0.1875, 0.1875, 1.0},
            {0.0, 0.0, 0.8125, 0.125, 0.1875, 0.875},
            {0.8125, 0.0, 0.875, 1.0, 0.1875, 1.0},
            {0.875, 0.0, 0.8125, 1.0, 0.1875, 0.875}
    };
    public static final VoxelShape SHAPE = buildShape();

    public CrucibleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static VoxelShape buildShape() {
        VoxelShape shape = Shapes.empty();
        for (double[] box : SHAPE_BOXES) {
            shape = Shapes.or(shape, Shapes.box(box[0], box[1], box[2], box[3], box[4], box[5]));
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TTBlockEntities.CRUCIBLE.get(), CrucibleBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)) {
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            crucible.emptyOut();
            return InteractionResult.CONSUME;
        }
        if (hit.getDirection().getAxis().isVertical() && hit.getDirection().getStepY() > 0
                && crucible.isBoiling() && crucible.tryInsertFromPlayer(player, held)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity itemEntity
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible && crucible.isBoiling()) {
            crucible.absorbItem(itemEntity);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CrucibleBlockEntity crucible) {
                crucible.emptyOut();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
