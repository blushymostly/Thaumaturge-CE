package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.api.aura.IAuraChunk;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Forge implementation behind {@code AuraHelper}: thin delegation to {@link AuraManager} / {@link AuraGenHandler}.
 * {@link #getAura(Level, BlockPos)} is kept for existing callers (workbench, crucible, aura packets).
 */
public final class AuraHelperBindings {
    private AuraHelperBindings() {}

    /** Chunk aura record at a position; on the server it is lazily initialised from biomes (forces the chunk load). */
    public static AuraData getAura(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        AuraData data = TTAttachments.aura(chunk);
        if (level instanceof ServerLevel serverLevel) {
            ensureInitialized(serverLevel, chunk, data);
        }
        return data;
    }

    public static void ensureInitialized(ServerLevel level, LevelChunk chunk, AuraData data) {
        data.setChunkPos(chunk.getPos());
        AuraGenHandler.initializeIfNeeded(level, chunk, data);
    }

    public static IAuraChunk chunkLookup(ServerLevel level, ChunkPos pos) {
        AuraData data = AuraManager.chunkAt(level, pos);
        if (data != null) {
            return data;
        }
        AuraData empty = new AuraData();
        empty.setChunkPos(pos);
        return empty;
    }

    public static IAuraChunk blockLookup(Level level, BlockPos pos) {
        AuraData data = AuraManager.chunkAt(level, pos);
        return data != null ? data : new AuraData();
    }

    public static float getVis(Level level, BlockPos pos) {
        return AuraManager.visAt(level, pos);
    }

    public static float getFlux(Level level, BlockPos pos) {
        return AuraManager.fluxAt(level, pos);
    }

    public static int getAuraBase(Level level, BlockPos pos) {
        return AuraManager.baseCapacity(level, pos);
    }

    public static float getTotalAura(Level level, BlockPos pos) {
        return AuraManager.combinedLevel(level, pos);
    }

    public static float getFluxSaturation(Level level, BlockPos pos) {
        return AuraManager.fluxRatio(level, pos);
    }

    public static float capacityRemaining(Level level, BlockPos pos) {
        return AuraManager.capacityRemaining(level, pos);
    }

    public static boolean canAcceptVis(Level level, BlockPos pos, float amount) {
        return AuraManager.capacityRemaining(level, pos) >= amount;
    }

    public static boolean shouldPreserveAura(Level level, @Nullable Player player, BlockPos pos) {
        return AuraManager.isPreservationDue(level, player, pos);
    }

    public static void addVis(Level level, BlockPos pos, float amount) {
        AuraManager.creditVis(level, pos, amount);
    }

    public static void addFlux(Level level, BlockPos pos, float amount) {
        AuraManager.creditFlux(level, pos, amount);
    }

    public static float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraManager.withdrawVis(level, pos, amount, simulate);
    }

    public static float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraManager.withdrawFlux(level, pos, amount, simulate);
    }

    public static void polluteAura(Level level, BlockPos pos, float amount, boolean showEffect) {
        AuraManager.taint(level, pos, amount, showEffect);
    }
}
