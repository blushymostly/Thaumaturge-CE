package com.leclowndu93150.thaumaturge.content.essentia.smeltery;

import com.leclowndu93150.thaumaturge.registry.TTMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SmelterMenu extends AbstractContainerMenu {
    static final int DATA_COUNT = 5;
    private static final int MACHINE_SLOTS = 2;
    private final Container items;
    private final ContainerData data;

    public SmelterMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    SmelterMenu(int id, Inventory inventory, Container items, ContainerData data) {
        super(TTMenus.SMELTER.get(), id);
        this.items = items;
        this.data = data;
        addSlot(new Slot(items, SmelterBlockEntity.INPUT, 80, 7) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SmelterBlockEntity.validInput(stack);
            }
        });
        addSlot(new Slot(items, SmelterBlockEntity.FUEL, 80, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SmelterBlockEntity.burnTime(stack) > 0;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return items.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SmelterBlockEntity.FUEL, MACHINE_SLOTS, false)
                && !moveItemStackTo(stack, SmelterBlockEntity.INPUT, SmelterBlockEntity.FUEL, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    public int fuelHeight(int height) {
        return scaled(data.get(0), data.get(1), height);
    }

    public int progressHeight(int height) {
        return scaled(data.get(2), data.get(3), height);
    }

    public int stockHeight(int height) {
        return scaled(data.get(4), SmelterBlockEntity.CAPACITY, height);
    }

    private static int scaled(int amount, int capacity, int height) {
        return capacity <= 0 ? 0 : Math.min(height, amount * height / capacity);
    }
}
