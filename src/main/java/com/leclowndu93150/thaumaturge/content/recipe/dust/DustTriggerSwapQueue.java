package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Per-chunk queue of delayed block swaps (stored in the {@code dust_queue} chunk capability), plus the
 * session-wide "active chunks" and "blocked positions" indexes.
 */
public final class DustTriggerSwapQueue {
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> BLOCKED = new ConcurrentHashMap<>();

    private final List<PendingSwap> pending = new ArrayList<>();

    public DustTriggerSwapQueue() {}

    public static void resetSession() {
        ACTIVE.clear();
        BLOCKED.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (PendingSwap entry : this.pending) {
            list.add(entry.save());
        }
        tag.put("pending", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.pending.clear();
        ListTag list = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            this.pending.add(PendingSwap.load(list.getCompound(i)));
        }
    }

    public List<PendingSwap> entries() {
        return this.pending;
    }

    public void add(PendingSwap entry) {
        this.pending.add(entry);
    }

    public boolean isEmpty() {
        return this.pending.isEmpty();
    }

    public Iterator<PendingSwap> iterator() {
        return this.pending.iterator();
    }

    public static void enqueueDrop(ServerLevel level, BlockPos pos, BlockState originalState, ItemStack result, int delayTicks) {
        enqueueInternal(level, PendingSwap.dropItem(pos, originalState, result, delayTicks));
    }

    public static void enqueuePlace(ServerLevel level, BlockPos pos, BlockState originalState, BlockState placed, int delayTicks) {
        enqueueInternal(level, PendingSwap.placeBlock(pos, originalState, placed, delayTicks));
    }

    public static void enqueueClear(ServerLevel level, BlockPos pos, BlockState originalState, int delayTicks) {
        enqueueInternal(level, PendingSwap.clearOnly(pos, originalState, delayTicks));
    }

    private static void enqueueInternal(ServerLevel level, PendingSwap entry) {
        ChunkPos cp = new ChunkPos(entry.pos());
        LevelChunk chunk = level.getChunk(cp.x, cp.z);
        DustTriggerSwapQueue queue = TTAttachments.dustQueue(chunk);
        queue.add(entry);
        chunk.setUnsaved(true);
        ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(cp);
        markBlocked(level, entry.pos());
    }

    public static Set<ChunkPos> activeChunks(ServerLevel level) {
        Set<ChunkPos> set = ACTIVE.get(level.dimension());
        if (set == null) {
            return Set.of();
        }
        return new HashSet<>(set);
    }

    public static void markChunkActive(ServerLevel level, ChunkPos pos) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    public static void markChunkClear(ServerLevel level, ChunkPos pos) {
        Set<ChunkPos> set = ACTIVE.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    public static void markBlocked(ServerLevel level, BlockPos pos) {
        BLOCKED.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
    }

    public static void clearBlocked(ServerLevel level, BlockPos pos) {
        Set<Long> set = BLOCKED.get(level.dimension());
        if (set != null) {
            set.remove(pos.asLong());
        }
    }

    public static boolean isBlocked(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        Set<Long> set = BLOCKED.get(sl.dimension());
        return set != null && set.contains(pos.asLong());
    }
}
