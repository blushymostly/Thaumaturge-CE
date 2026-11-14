package com.leclowndu93150.thaumaturge.content.workbench;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.content.item.VisCrystalItem;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SlotCrystalEssentia extends Slot {
    private final ResourceLocation requiredAspect;

    SlotCrystalEssentia(Container container, int index, int x, int y, ResourceLocation requiredAspect) {
        super(container, index, x, y);
        this.requiredAspect = requiredAspect;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!(stack.getItem() instanceof VisCrystalItem)) {
            return false;
        }
        AspectInstance aspect = TTItemData.getCrystalAspect(stack);
        return aspect != null && requiredAspect.equals(aspect.aspectId());
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
