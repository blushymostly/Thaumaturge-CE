package com.leclowndu93150.thaumaturge.content.aspect;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AspectIndexBootstrap {
    private AspectIndexBootstrap() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            refresh(event.getPlayerList().getServer());
        }
    }

    private static void refresh(net.minecraft.server.MinecraftServer server) {
        com.leclowndu93150.thaumaturge.api.aspect.AspectIndex.invalidate();
        com.leclowndu93150.thaumaturge.api.aspect.AspectIndex.rebuildDerived(
                server.getRecipeManager(), server.registryAccess(), LogicalSide.SERVER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AspectIndex());
    }
}
