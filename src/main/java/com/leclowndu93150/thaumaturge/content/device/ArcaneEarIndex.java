package com.leclowndu93150.thaumaturge.content.device;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Forge stand-in for CE level EAR_INDEX attachment. */
public final class ArcaneEarIndex {
    private static final Map<Level, Set<BlockPos>> INDEX = Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private ArcaneEarIndex() {}

    public static void add(Level level, BlockPos pos) {
        INDEX.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = INDEX.get(level);
        if (set != null) {
            set.remove(pos);
        }
    }

    public static Set<BlockPos> ears(Level level) {
        Set<BlockPos> set = INDEX.get(level);
        return set == null ? Set.of() : Set.copyOf(set);
    }
}
