package com.leclowndu93150.thaumaturge.content.workbench;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SlotWorkbenchWand extends Slot {
    SlotWorkbenchWand(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        return path.contains("wand") || path.contains("staff") || path.contains("scepter");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
