package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFluxIndex;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TaintBootstrap {
    private TaintBootstrap() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(TaintBootstrap::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TTEntities.TAINT_SEED.get(), TaintSeedEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        PhysicalFluxIndex.rebuild(chunk);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            PhysicalFluxIndex.unload(chunk);
        }
    }
}
