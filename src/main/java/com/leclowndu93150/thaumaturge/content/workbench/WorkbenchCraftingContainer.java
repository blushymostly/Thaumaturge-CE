package com.leclowndu93150.thaumaturge.content.workbench;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.StackedContents;

public final class WorkbenchCraftingContainer implements CraftingContainer {
    private static final int SIZE = 9;
    private static final int WIDTH = 3;

    private final Container delegate;

    public WorkbenchCraftingContainer(Container delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return delegate.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return delegate.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return delegate.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        delegate.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        delegate.setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return delegate.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return WIDTH;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            items.set(i, getItem(i));
        }
        return items;
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack stack : getItems()) {
            contents.accountStack(stack);
        }
    }
}
