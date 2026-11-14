package com.leclowndu93150.thaumaturge.compat.curios;

import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public final class ThaumaturgeCuriosCompat {
    private ThaumaturgeCuriosCompat() {}

    public static void init(IEventBus modBus) {
        Thaumaturge.LOGGER.info("Curios integration enabled");
    }

    @Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void registerRenderer(FMLClientSetupEvent event) {
            event.enqueueWork(() -> CuriosRendererRegistry.register(TTItems.GOGGLES_REVEALING.get(), GogglesCurioRenderer::new));
        }
    }
}
