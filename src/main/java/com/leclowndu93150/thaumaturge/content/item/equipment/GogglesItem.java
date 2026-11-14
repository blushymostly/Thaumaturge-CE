package com.leclowndu93150.thaumaturge.content.item.equipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Goggles of Revealing — helmet slot revealing gear (Wave 3a MiW). */
public final class GogglesItem extends ArmorItem {
    public GogglesItem(Item.Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumaturge:textures/entity/equipment/humanoid/goggles_revealing.png";
    }
}
