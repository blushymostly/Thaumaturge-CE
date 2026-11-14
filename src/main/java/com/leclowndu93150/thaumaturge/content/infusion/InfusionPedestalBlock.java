package com.leclowndu93150.thaumaturge.content.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class InfusionPedestalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 3, 16), box(4, 3, 4, 12, 13, 12), box(0, 13, 0, 16, 16, 16));

    public InfusionPedestalBlock() {
        super(Properties.of().strength(2).noOcclusion());
    }

    @Override
    public InfusionPedestal newBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionPedestal(pos, state);
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
        if (level.getBlockEntity(pos) instanceof InfusionPedestal pedestal) {
            if (!level.isClientSide) {
                if (!pedestal.item().isEmpty()) {
                    ItemStack item = pedestal.item();
                    pedestal.setItem(ItemStack.EMPTY);
                    if (!player.addItem(item)) {
                        player.drop(item, false);
                    }
                } else if (!player.getItemInHand(hand).isEmpty()) {
                    ItemStack held = player.getItemInHand(hand);
                    pedestal.setItem(held.copyWithCount(1));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!level.isClientSide && !state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof InfusionPedestal pedestal) {
            Block.popResource(level, pos, pedestal.item());
        }
        super.onRemove(state, level, pos, replacement, moved);
    }
}
