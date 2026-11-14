package com.leclowndu93150.thaumaturge.mixin.compat.expatternprovider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ExtendedAE leaves {@code EPPConfig.infCellFluid/Item} null until {@code ModConfigEvent}.
 * Opening creative (esp. after ModernFix tab rebuild when another mod adds a tab) NPEs in
 * {@code InfinityCell.addToMainCreativeTab}.
 *
 * <p>Must live in the primary {@code thaumaturge.mixins.json}: Forge 1.20.1 only registered the
 * first {@code [[mixins]]} entry from mods.toml, so the optional secondary config never loaded.
 */
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.common.items.InfinityCell", remap = false)
public class InfinityCellCreativeTabMixin {
    @Inject(method = "addToMainCreativeTab", at = @At("HEAD"), remap = false, require = 0, cancellable = true)
    private void thaumaturge$guardNullInfCellLists(CreativeModeTab.Output output, CallbackInfo ci) {
        try {
            Class<?> config = Class.forName("com.glodblock.github.extendedae.config.EPPConfig");
            Field fluid = config.getField("infCellFluid");
            Field item = config.getField("infCellItem");
            if (fluid.get(null) != null && item.get(null) != null) {
                return;
            }
            try {
                Class<?> eventType = Class.forName("net.minecraftforge.fml.event.config.ModConfigEvent");
                Method onLoad = config.getDeclaredMethod("onLoad", eventType);
                onLoad.setAccessible(true);
                onLoad.invoke(null, new Object[] {null});
            } catch (Throwable ignored) {
                // fall through to empty lists
            }
            if (fluid.get(null) == null) {
                fluid.set(null, new ArrayList<>());
            }
            if (item.get(null) == null) {
                item.set(null, new ArrayList<>());
            }
            // If still somehow broken, skip the original body rather than crash the inventory.
            if (fluid.get(null) == null || item.get(null) == null) {
                ci.cancel();
            }
        } catch (ClassNotFoundException ignored) {
            // ExtendedAE absent
        } catch (Throwable ignored) {
            ci.cancel();
        }
    }
}
