package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class EssentiaCrystalFactory {
    private EssentiaCrystalFactory() {}

    public static ItemStack of(ResourceLocation aspect, int count) {
        ItemStack stack = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), count);
        TTItemData.setCrystalAspect(stack, new AspectInstance(aspect, 1));
        return stack;
    }

    public static ItemStack aer() {
        return of(TTAspects.AER, 1);
    }

    public static ItemStack ignis() {
        return of(TTAspects.IGNIS, 1);
    }

    public static ItemStack aqua() {
        return of(TTAspects.AQUA, 1);
    }

    public static ItemStack terra() {
        return of(TTAspects.TERRA, 1);
    }

    public static ItemStack ordo() {
        return of(TTAspects.ORDO, 1);
    }

    public static ItemStack perditio() {
        return of(TTAspects.PERDITIO, 1);
    }
}
