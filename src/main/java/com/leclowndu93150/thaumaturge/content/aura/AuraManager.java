package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTParticles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side aura bookkeeping: tracks loaded chunks per dimension and provides vis/flux read/withdraw/credit helpers.
 * Forge port of the CE AuraManager, without NeoForge transactions: mutations apply directly and the chunk is marked dirty.
 */
public final class AuraManager {
    public static final int AURA_CEILING = 500;
    public static final ResourceLocation AURA_PRESERVE_RESEARCH = TTIds.rl("aura_preserve");
    private static final float PRESERVE_RATIO = 0.1F;
    private static final int CHUNK_SHIFT = 4;
    private static final double BLOCK_CENTER = 0.5;
    private static final Map<ResourceKey<Level>, DimensionState> STATES = new ConcurrentHashMap<>();

    private AuraManager() {}

    public static void onChunkLoaded(ServerLevel level, ChunkPos pos) {
        STATES.computeIfAbsent(level.dimension(), key -> new DimensionState()).loaded.add(pos);
    }

    public static void onChunkUnloaded(ServerLevel level, ChunkPos pos) {
        DimensionState state = STATES.get(level.dimension());
        if (state != null) {
            state.loaded.remove(pos);
        }
    }

    public static Set<ChunkPos> loadedChunksSnapshot(ServerLevel level) {
        DimensionState state = STATES.get(level.dimension());
        return state == null ? new HashSet<>() : new HashSet<>(state.loaded);
    }

    public static void resetSession() {
        STATES.clear();
    }

    // --- chunk lookup ---

    /** Aura of the already-loaded chunk at a block position (server only); initialises it lazily. */
    public static @Nullable AuraData chunkAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        return chunkAt(server, chunkOf(pos));
    }

    /** Aura of the already-loaded chunk; never forces a chunk load. */
    public static @Nullable AuraData chunkAt(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk == null) {
            return null;
        }
        AuraData found = TTAttachments.aura(chunk);
        found.setChunkPos(pos);
        AuraGenHandler.initializeIfNeeded(level, chunk, found);
        return found;
    }

    // --- reads ---

    public static int baseCapacity(Level level, BlockPos pos) {
        AuraData found = chunkAt(level, pos);
        return found == null ? 0 : found.getBase();
    }

    public static float visAt(Level level, BlockPos pos) {
        AuraData found = chunkAt(level, pos);
        return found == null ? 0.0F : found.getVis();
    }

    public static float fluxAt(Level level, BlockPos pos) {
        AuraData found = chunkAt(level, pos);
        return found == null ? 0.0F : found.getFlux();
    }

    public static float combinedLevel(Level level, BlockPos pos) {
        return visAt(level, pos) + fluxAt(level, pos);
    }

    public static float fluxRatio(Level level, BlockPos pos) {
        int capacity = baseCapacity(level, pos);
        return capacity == 0 ? 0.0F : fluxAt(level, pos) / capacity;
    }

    public static float capacityRemaining(Level level, BlockPos pos) {
        return Math.max(0.0F, baseCapacity(level, pos) - combinedLevel(level, pos));
    }

    public static boolean isPreservationDue(Level level, @Nullable Player player, BlockPos pos) {
        AuraData found = chunkAt(level, pos);
        if (found == null || found.getBase() == 0) {
            return false;
        }
        boolean starved = found.getVis() / found.getBase() < PRESERVE_RATIO;
        if (!starved) {
            return false;
        }
        return player == null || TTAttachments.knowledge(player).isComplete(AURA_PRESERVE_RESEARCH);
    }

    // --- mutations ---

    public static float withdrawVis(Level level, BlockPos pos, float amount, boolean simulate) {
        AuraData found = chunkAt(level, pos);
        if (found == null || amount <= 0.0F) {
            return 0.0F;
        }
        float taken = Math.min(amount, found.getVis());
        if (simulate || taken <= 0.0F) {
            return Math.max(0.0F, taken);
        }
        found.setVis(found.getVis() - taken);
        markChunkDirty(level, pos);
        return taken;
    }

    public static float withdrawFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        AuraData found = chunkAt(level, pos);
        if (found == null || amount <= 0.0F) {
            return 0.0F;
        }
        float taken = Math.min(amount, found.getFlux());
        if (simulate || taken <= 0.0F) {
            return Math.max(0.0F, taken);
        }
        found.setFlux(found.getFlux() - taken);
        markChunkDirty(level, pos);
        return taken;
    }

    public static void creditVis(Level level, BlockPos pos, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        AuraData found = chunkAt(level, pos);
        if (found != null) {
            found.addVis(amount);
            markChunkDirty(level, pos);
        }
    }

    public static void creditFlux(Level level, BlockPos pos, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        AuraData found = chunkAt(level, pos);
        if (found != null) {
            found.addFlux(amount);
            markChunkDirty(level, pos);
        }
    }

    public static void taint(Level level, BlockPos pos, float amount, boolean showEffect) {
        creditFlux(level, pos, amount);
        if (showEffect && amount > 0.0F && level instanceof ServerLevel server) {
            emitFume(server, pos);
        }
    }

    // --- dirty marking ---

    static void markChunkDirty(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            markChunkDirty(server, chunkOf(pos));
        }
    }

    static void markChunkDirty(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk != null) {
            chunk.setUnsaved(true);
        }
    }

    private static @Nullable LevelChunk loadedChunk(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().getChunkNow(pos.x, pos.z);
    }

    private static ChunkPos chunkOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> CHUNK_SHIFT, pos.getZ() >> CHUNK_SHIFT);
    }

    private static void emitFume(ServerLevel level, BlockPos pos) {
        level.sendParticles(TTParticles.TAINT.get(), pos.getX() + BLOCK_CENTER, pos.getY() + BLOCK_CENTER, pos.getZ() + BLOCK_CENTER, 4, 0.25, 0.25, 0.25, 0.01);
    }

    private static final class DimensionState {
        private final Set<ChunkPos> loaded = ConcurrentHashMap.newKeySet();
    }
}
