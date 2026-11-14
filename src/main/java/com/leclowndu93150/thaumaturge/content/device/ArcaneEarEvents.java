package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArcaneEarEvents {
    private static final double MAX_RANGE_SQ = 4096.0;

    private ArcaneEarEvents() {}

    @SubscribeEvent
    public static void onNotePlayed(NoteBlockEvent.Play event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos source = event.getPos();
        for (BlockPos earPos : ArcaneEarIndex.ears(level)) {
            if (earPos.distToCenterSqr(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5) > MAX_RANGE_SQ) {
                continue;
            }
            if (level.getBlockEntity(earPos) instanceof BlockEntityArcaneEar ear && ear.matches(event.getInstrument(), event.getVanillaNoteId())) {
                ear.trigger();
            }
        }
    }
}
