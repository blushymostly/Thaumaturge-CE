package com.leclowndu93150.thaumaturge.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;

public final class FocalManipulatorBlock extends BaseEntityBlock {
    public FocalManipulatorBlock() {
        super(Properties.of().strength(3).noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FocalManipulatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.FOCAL_MANIPULATOR.get(), (world, pos, blockState, table) -> table.tick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table) {
            if (!FocalManipulatorBlockEntity.canOpen(player)) {
                player.displayClientMessage(Component.translatable("message.thaumaturge.device.not_understood"), true);
                return InteractionResult.FAIL;
            }
            NetworkHooks.openScreen(serverPlayer, table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table) {
            Containers.dropContents(level, pos, table.focus());
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
