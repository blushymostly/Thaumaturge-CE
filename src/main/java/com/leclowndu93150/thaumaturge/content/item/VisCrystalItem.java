package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class VisCrystalItem extends Item {
    public VisCrystalItem() {
        super(new Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        AspectInstance aspect = TTItemData.getCrystalAspect(stack);
        if (aspect == null) {
            return Component.translatable("item.thaumaturge.essentia_crystal.unknown");
        }
        return Component.translatable("item.thaumaturge.essentia_crystal", AspectNames.name(aspect.aspectId()));
    }

    public static int tintColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFF;
        }
        AspectInstance aspect = TTItemData.getCrystalAspect(stack);
        if (aspect == null) {
            return 0xFFFFFF;
        }
        IAspect def = TTAspects.get(aspect.aspectId());
        return def != null ? def.color() : 0xFFFFFF;
    }
}
