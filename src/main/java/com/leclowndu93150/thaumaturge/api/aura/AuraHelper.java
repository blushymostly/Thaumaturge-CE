package com.leclowndu93150.thaumaturge.api.aura;

import com.leclowndu93150.thaumaturge.content.aura.AuraHelperBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Static facade over the per-chunk aura (vis, flux, base capacity). Server-side only; forwards to
 * {@link AuraHelperBindings} (no bind step is needed on Forge). Mirrors the CE 26 facade minus the transaction overload.
 */
public final class AuraHelper {
    private AuraHelper() {}

    public static IAuraChunk of(ServerLevel level, ChunkPos pos) {
        return AuraHelperBindings.chunkLookup(level, pos);
    }

    public static IAuraChunk of(Level level, BlockPos pos) {
        return AuraHelperBindings.blockLookup(level, pos);
    }

    public static float getVis(Level level, BlockPos pos) {
        return AuraHelperBindings.getVis(level, pos);
    }

    public static float getFlux(Level level, BlockPos pos) {
        return AuraHelperBindings.getFlux(level, pos);
    }

    public static int getAuraBase(Level level, BlockPos pos) {
        return AuraHelperBindings.getAuraBase(level, pos);
    }

    public static float getTotalAura(Level level, BlockPos pos) {
        return AuraHelperBindings.getTotalAura(level, pos);
    }

    public static float getFluxSaturation(Level level, BlockPos pos) {
        return AuraHelperBindings.getFluxSaturation(level, pos);
    }

    public static float capacityRemaining(Level level, BlockPos pos) {
        return AuraHelperBindings.capacityRemaining(level, pos);
    }

    public static boolean canAcceptVis(Level level, BlockPos pos, float amount) {
        return AuraHelperBindings.canAcceptVis(level, pos, amount);
    }

    public static boolean shouldPreserveAura(Level level, @Nullable Player player, BlockPos pos) {
        return AuraHelperBindings.shouldPreserveAura(level, player, pos);
    }

    public static void addVis(Level level, BlockPos pos, float amount) {
        AuraHelperBindings.addVis(level, pos, amount);
    }

    public static void addFlux(Level level, BlockPos pos, float amount) {
        AuraHelperBindings.addFlux(level, pos, amount);
    }

    public static void polluteAura(Level level, BlockPos pos, float fluxAmount, boolean withCue) {
        AuraHelperBindings.polluteAura(level, pos, fluxAmount, withCue);
    }

    public static float drainFlux(Level level, BlockPos pos, float requested, boolean dryRun) {
        return AuraHelperBindings.drainFlux(level, pos, requested, dryRun);
    }

    public static float drainVis(Level level, BlockPos pos, float requested, boolean dryRun) {
        return AuraHelperBindings.drainVis(level, pos, requested, dryRun);
    }
}
