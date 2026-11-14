package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import org.jetbrains.annotations.Nullable;

/** Pedestal that seats a wand and slowly refills it from nearby nodes. */
public class BlockRechargePedestal extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
            box(2.0, 2.0, 2.0, 14.0, 4.0, 14.0),
            box(5.0, 4.0, 5.0, 11.0, 12.0, 11.0),
            box(4.0, 12.0, 4.0, 12.0, 14.0, 12.0),
            box(4.0, 14.0, 4.0, 6.0, 16.0, 6.0),
            box(4.0, 14.0, 10.0, 6.0, 16.0, 12.0),
            box(10.0, 14.0, 4.0, 12.0, 16.0, 6.0),
            box(10.0, 14.0, 10.0, 12.0, 16.0, 12.0),
            box(6.0, 14.0, 6.0, 10.0, 15.0, 10.0));
    private static final float PICKUP_VOLUME = 0.2F;
    private static final float PITCH_SPREAD = 0.7F;
    private static final float PITCH_BASE = 1.0F;
    private static final float PLACE_PITCH = 1.6F;
    private static final float TAKE_PITCH = 1.5F;

    public BlockRechargePedestal(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRechargePedestal(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.RECHARGE_PEDESTAL.get(), BlockEntityRechargePedestal::serverTick);
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityRechargePedestal pedestal)) {
            return InteractionResult.PASS;
        }
        ItemStack resting = pedestal.getItem();
        boolean placing = resting.isEmpty();
        ItemStack held = player.getItemInHand(hand);
        if (placing && !BlockEntityRechargePedestal.accepts(held)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (placing) {
                pedestal.setItem(held.split(1));
            } else {
                ItemStack taken = resting.copy();
                pedestal.setItem(ItemStack.EMPTY);
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            }
            playPickupSound(level, pos, placing ? PLACE_PITCH : TAKE_PITCH);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlockEntityRechargePedestal pedestal) {
            Block.popResource(level, pos, pedestal.getItem());
            pedestal.setItem(ItemStack.EMPTY);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void playPickupSound(Level level, BlockPos pos, float pitchFactor) {
        RandomSource random = level.getRandom();
        float variation = (random.nextFloat() - random.nextFloat()) * PITCH_SPREAD + PITCH_BASE;
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, PICKUP_VOLUME, variation * pitchFactor);
    }
}
