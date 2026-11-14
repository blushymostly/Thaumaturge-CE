package com.leclowndu93150.thaumaturge.content.taint.flux;

import com.leclowndu93150.thaumaturge.content.taint.BlockEtherealBloom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PhysicalFluxIndex {
    private static final Map<LevelChunk, Set<BlockPos>> CELLS = new WeakHashMap<>();

    private PhysicalFluxIndex() {}

    static void observe(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) CELLS.computeIfAbsent(chunk, unused -> new HashSet<>()).add(pos.immutable());
    }

    public static void rebuild(LevelChunk chunk) {
        Set<BlockPos> positions = new HashSet<>();
        int x = chunk.getPos().getMinBlockX();
        int z = chunk.getPos().getMinBlockZ();
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> PhysicalFlux.isPhysicalFlux(state) || state.getBlock() instanceof BlockEtherealBloom)) continue;
            int y = chunk.getSectionYFromSectionIndex(index) * 16;
            for (int dx = 0; dx < 16; dx++) {
                for (int dy = 0; dy < 16; dy++) {
                    for (int dz = 0; dz < 16; dz++) {
                        var state = section.getBlockState(dx, dy, dz);
                        if (PhysicalFlux.isPhysicalFlux(state)) positions.add(new BlockPos(x + dx, y + dy, z + dz));
                        if (state.getBlock() instanceof BlockEtherealBloom) {
                            BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                            if (chunk.getBlockEntity(pos) == null) {
                                chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
                                chunk.setUnsaved(true);
                            }
                        }
                    }
                }
            }
        }
        if (positions.isEmpty()) CELLS.remove(chunk);
        else CELLS.put(chunk, positions);
    }

    public static void unload(LevelChunk chunk) {
        CELLS.remove(chunk);
    }

    public static List<BlockPos> positions(LevelChunk chunk) {
        Set<BlockPos> positions = CELLS.get(chunk);
        if (positions == null || positions.isEmpty()) return List.of();
        positions.removeIf(pos -> !PhysicalFlux.isPhysicalFlux(chunk.getBlockState(pos)));
        if (positions.isEmpty()) {
            CELLS.remove(chunk);
            return List.of();
        }
        return new ArrayList<>(positions);
    }
}
