package com.leclowndu93150.thaumaturge.client;

import com.leclowndu93150.thaumaturge.content.aura.AuraData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

public final class ClientAuraCache {
    private static final Map<Long, AuraData> CACHE = new ConcurrentHashMap<>();

    private ClientAuraCache() {}

    public static void put(ChunkPos pos, AuraData data) {
        CACHE.put(ChunkPos.asLong(pos.x, pos.z), data);
    }

    @Nullable
    public static AuraData get(ChunkPos pos) {
        return CACHE.get(ChunkPos.asLong(pos.x, pos.z));
    }

    public static void clear() {
        CACHE.clear();
    }
}
