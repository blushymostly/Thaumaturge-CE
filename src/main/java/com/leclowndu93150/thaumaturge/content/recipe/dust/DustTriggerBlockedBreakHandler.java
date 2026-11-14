package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps blocks with a pending dust swap from being broken or blown up before the swap lands.
 * Forge 1.20.1 has no generic break-block event, so player breaks and explosions are vetoed separately.
 */
@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DustTriggerBlockedBreakHandler {
    private DustTriggerBlockedBreakHandler() {}

    @SubscribeEvent
    public static void vetoSwapPendingBreak(BlockEvent.BreakEvent event) {
        if (DustTriggerSwapQueue.isBlocked(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void vetoSwapPendingExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> DustTriggerSwapQueue.isBlocked(event.getLevel(), pos));
    }
}
