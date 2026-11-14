package com.leclowndu93150.thaumaturge.compat.expatternprovider;

import com.leclowndu93150.thaumaturge.TTIds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Belt-and-suspenders for ExtendedAE creative NPE: ensure {@code EPPConfig.infCell*} lists exist
 * before inventory/creative screens rebuild tabs (covers cases where the InfinityCell mixin misses).
 */
@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExtendedAeCreativeTabGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger(TTIds.MODID);
    private static final String EPP_MODID = "expatternprovider";

    private ExtendedAeCreativeTabGuard() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof InventoryScreen || event.getNewScreen() instanceof CreativeModeInventoryScreen) {
            ensureInfCellLists();
        }
    }

    public static void ensureInfCellLists() {
        if (!ModList.get().isLoaded(EPP_MODID)) {
            return;
        }
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
                // empty lists below
            }
            if (fluid.get(null) == null) {
                fluid.set(null, new ArrayList<>());
            }
            if (item.get(null) == null) {
                item.set(null, new ArrayList<>());
            }
        } catch (Throwable t) {
            LOGGER.debug("Could not prime ExtendedAE infinity-cell creative lists", t);
        }
    }

    @Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientSetup {
        private ClientSetup() {}

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(ExtendedAeCreativeTabGuard::ensureInfCellLists);
        }
    }
}
