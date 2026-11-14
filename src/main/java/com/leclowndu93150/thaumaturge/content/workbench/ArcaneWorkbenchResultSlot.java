package com.leclowndu93150.thaumaturge.content.workbench;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ArcaneWorkbenchResultSlot extends Slot {
    private final ArcaneWorkbenchMenu menu;

    ArcaneWorkbenchResultSlot(ResultContainer result, ArcaneWorkbenchMenu menu, int x, int y) {
        super(result, 0, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (menu.craftsOnServer()) {
            return menu.takeCraft();
        }
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
    }
}
