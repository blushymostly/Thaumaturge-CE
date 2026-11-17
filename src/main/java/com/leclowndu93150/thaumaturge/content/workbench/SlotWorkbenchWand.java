package com.leclowndu93150.thaumaturge.content.workbench;

import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.WandParts;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SlotWorkbenchWand extends Slot {
    SlotWorkbenchWand(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof WandItem && !WandParts.rod(stack).staff();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
