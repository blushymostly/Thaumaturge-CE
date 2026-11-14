package com.leclowndu93150.thaumaturge.content.workbench;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WorkbenchContainer implements Container {
    private final ArcaneWorkbenchBlockEntity workbench;
    private Runnable changeListener = () -> {};

    public WorkbenchContainer(ArcaneWorkbenchBlockEntity workbench) {
        this.workbench = workbench;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener == null ? () -> {} : changeListener;
    }

    public static final int CRAFTING_SLOTS = 9;
    public static final int CRYSTAL_SLOTS = 6;
    public static final int WAND_SLOT = CRAFTING_SLOTS + CRYSTAL_SLOTS;
    public static final int SIZE = WAND_SLOT + 1;

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!workbench.items().get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return workbench.items().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = workbench.items().get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        workbench.setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = workbench.items().get(slot);
        workbench.items().set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        workbench.items().set(slot, stack);
        workbench.setChanged();
    }

    @Override
    public void setChanged() {
        workbench.setChanged();
        changeListener.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            workbench.items().set(i, ItemStack.EMPTY);
        }
    }
}
