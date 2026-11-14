package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** MiW hungry chest — swallows nearby item entities into chest slots. */
public final class BlockEntityHungryChest extends ChestBlockEntity {
    private static final double HORIZONTAL_GROWTH = 0.1;
    private static final double VERTICAL_REACH = 0.3;
    private static final int LID_EVENT = 1;
    private static final int LID_OPEN = 1;
    private static final int LID_CLOSED = 0;
    private static final int LID_CLOSE_DELAY = 4;
    private static final float EAT_VOLUME = 0.25F;

    private final AABB swallowBox;
    private int lidCloseTimer;

    public BlockEntityHungryChest(BlockPos pos, BlockState state) {
        super(TTBlockEntities.HUNGRY_CHEST.get(), pos, state);
        swallowBox = new AABB(pos).inflate(HORIZONTAL_GROWTH, 0.0, HORIZONTAL_GROWTH).expandTowards(0.0, VERTICAL_REACH, 0.0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityHungryChest chest) {
        if (chest.lidCloseTimer > 0 && --chest.lidCloseTimer == 0) {
            level.blockEvent(pos, state.getBlock(), LID_EVENT, LID_CLOSED);
        }
        boolean swallowed = false;
        RandomSource random = level.getRandom();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, chest.swallowBox)) {
            if (!item.isRemoved() && chest.swallow(item, random)) {
                swallowed = true;
            }
        }
        if (swallowed) {
            chest.setChanged();
            chest.lidCloseTimer = LID_CLOSE_DELAY;
            level.blockEvent(pos, state.getBlock(), LID_EVENT, LID_OPEN);
        }
    }

    private boolean swallow(ItemEntity item, RandomSource random) {
        ItemStack stack = item.getItem();
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack inSlot = getItem(slot);
            if (inSlot.isEmpty()) {
                int take = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                setItem(slot, remaining.split(take));
            } else if (ItemStack.isSameItemSameTags(inSlot, remaining)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                if (space > 0) {
                    int take = Math.min(space, remaining.getCount());
                    inSlot.grow(take);
                    remaining.shrink(take);
                    setItem(slot, inSlot);
                }
            }
        }
        int accepted = stack.getCount() - remaining.getCount();
        if (accepted <= 0) {
            return false;
        }
        if (remaining.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remaining);
        }
        item.playSound(SoundEvents.GENERIC_EAT, EAT_VOLUME, 0.9F + random.nextFloat() * 0.2F);
        return true;
    }
}
