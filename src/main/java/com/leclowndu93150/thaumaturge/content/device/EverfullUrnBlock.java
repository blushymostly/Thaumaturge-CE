package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class EverfullUrnBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(3, 1, 3, 13, 9, 13), box(4, 0, 4, 12, 1, 12),
            box(4, 9, 4, 12, 10, 12), box(5, 10, 5, 11, 15, 11), box(4, 13, 4, 5, 16, 12),
            box(5, 13, 4, 12, 16, 5), box(5, 13, 11, 12, 16, 12), box(11, 13, 5, 12, 16, 11));

    public EverfullUrnBlock() {
        super(Properties.of().strength(1.5F).noOcclusion());
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
    public EverfullUrnBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EverfullUrnBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EverfullUrnBlockEntity urn)) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return held.is(Items.GLASS_BOTTLE) || FluidUtil.getFluidHandler(held).isPresent()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            if (!held.is(Items.GLASS_BOTTLE) || urn.water() < EverfullUrnBlockEntity.PORTION) return InteractionResult.PASS;
            urn.tank().drain(EverfullUrnBlockEntity.PORTION, FluidAction.EXECUTE);
            var bottle = PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.WATER);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, bottle));
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F, 1);
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.EVERFULL_URN.get(), EverfullUrnBlockEntity::tick);
    }
}
