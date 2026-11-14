package com.leclowndu93150.thaumaturge.api.items;

import com.leclowndu93150.thaumaturge.content.item.equipment.GogglesItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/** Revealing gear may be worn as armor or as a Curio. */
public final class GogglesAccess {
    private GogglesAccess() {}

    public static boolean isRevealing(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GogglesItem;
    }

    public static boolean wearsRevealingGear(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return isRevealing(entity.getItemBySlot(EquipmentSlot.HEAD))
                || CuriosApi.getCuriosInventory(entity).map(inventory -> inventory.isEquipped(GogglesAccess::isRevealing)).orElse(false);
    }
}
