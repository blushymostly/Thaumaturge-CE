package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Chunk load/unload tracking plus biome-based base aura generation (Forge port of CE's AuraGenHandler).
 * Registered on the Forge event bus from {@code Thaumaturge}.
 */
public final class AuraGenHandler {
    private static final int SAMPLE_HEIGHT = 50;
    private static final int CHUNK_CENTER_OFFSET = 8;
    private static final int CHUNK_WIDTH = 16;
    private static final double VARIANCE_FACTOR = 0.1;
    private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;
    private static final int KEY_Z_SHIFT = 32;
    private static final int[][] SAMPLE_OFFSETS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private AuraGenHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            AuraManager.onChunkLoaded(level, chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraManager.onChunkUnloaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AuraManager.resetSession();
    }

    /** Generates the chunk's base aura from surrounding biomes the first time it is seen. */
    public static void initializeIfNeeded(ServerLevel level, LevelChunk chunk, AuraData data) {
        if (data.isInitialized()) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        boolean pristine = data.getVis() == 0.0F && data.getFlux() == 0.0F;
        short base = generateBase(level, pos);
        data.setBase(base);
        data.setChunkPos(pos);
        if (pristine) {
            data.setVis(base);
            if (BiomeAuraModifiers.isTainted(biomeId(level, pos.x, pos.z))) {
                data.setFlux(base * ThaumaturgeCommonConfig.TAINTED_LANDS_STARTING_FLUX.get().floatValue());
            }
        }
        chunk.setUnsaved(true);
    }

    private static short generateBase(ServerLevel level, ChunkPos pos) {
        double modifierSum = 0.0;
        for (int[] offset : SAMPLE_OFFSETS) {
            modifierSum += BiomeAuraModifiers.get(biomeId(level, pos.x + offset[0], pos.z + offset[1]));
        }
        double mean = modifierSum / SAMPLE_OFFSETS.length;
        long key = (pos.x & UNSIGNED_INT_MASK) | ((pos.z & UNSIGNED_INT_MASK) << KEY_Z_SHIFT);
        double gaussian = new Random(level.getSeed() ^ key).nextGaussian();
        int base = (int) (mean * AuraManager.AURA_CEILING * (1.0 + VARIANCE_FACTOR * gaussian));
        return (short) Mth.clamp(base, 0, AuraManager.AURA_CEILING);
    }

    private static ResourceLocation biomeId(ServerLevel level, int chunkX, int chunkZ) {
        int blockX = chunkX * CHUNK_WIDTH + CHUNK_CENTER_OFFSET;
        int blockZ = chunkZ * CHUNK_WIDTH + CHUNK_CENTER_OFFSET;
        Holder<Biome> biome = level.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(SAMPLE_HEIGHT), QuartPos.fromBlock(blockZ));
        return biome.unwrapKey().map(ResourceKey::location).orElseGet(() -> new ResourceLocation("minecraft", "plains"));
    }
}
