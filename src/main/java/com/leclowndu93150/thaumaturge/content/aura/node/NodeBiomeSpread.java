package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

public final class NodeBiomeSpread {
    private static final int INTERVAL = 50;
    private static final int DARK_RANGE = 16;
    private static final int PURE_RANGE = 8;
    private static final int QUARTS_PER_CHUNK = 4;
    private static final TagKey<Block> SILVERWOOD_LOGS = TagKey.create(Registries.BLOCK, TTIds.rl("silverwood_logs"));
    private static final TagKey<Biome> TAINTED = TagKey.create(Registries.BIOME, TTIds.rl("is_tainted"));

    private NodeBiomeSpread() {}

    static void tick(ServerLevel level, NodeType type, BlockPos origin, int ticks) {
        int range = range(type);
        if (range == 0 || ticks % INTERVAL != 0 || !level.dimension().equals(Level.OVERWORLD))
            return;
        var random = level.getRandom();
        BlockPos sample = origin.offset(random.nextInt(range) - random.nextInt(range), 0, random.nextInt(range) - random.nextInt(range));
        spreadAt(level, type, origin, sample);
    }

    static boolean spreadAt(ServerLevel level, NodeType type, BlockPos origin, BlockPos sample) {
        int range = range(type);
        if (range == 0 || !level.dimension().equals(Level.OVERWORLD))
            return false;
        int x = QuartPos.fromBlock(sample.getX());
        int z = QuartPos.fromBlock(sample.getZ());
        double dx = QuartPos.toBlock(x) + 2 - (origin.getX() + 0.5);
        double dz = QuartPos.toBlock(z) + 2 - (origin.getZ() + 0.5);
        if (dx * dx + dz * dz > range * range)
            return false;
        LevelChunk chunk = level.getChunkSource().getChunkNow(QuartPos.toSection(x), QuartPos.toSection(z));
        if (chunk == null)
            return false;

        ResourceKey<Biome> target = paintedBiome(type);
        if (type == NodeType.DARK && level.getBiome(sample).is(target))
            return false;
        if (type == NodeType.PURE && !chunk.getNoiseBiome(x, QuartPos.fromBlock(sample.getY()), z).is(TAINTED) && !hasSilverwood(level, origin))
            return false;

        for (int y = QuartPos.fromBlock(chunk.getMinBuildHeight()); y < QuartPos.fromBlock(chunk.getMaxBuildHeight()); y++) {
            if (chunk.getNoiseBiome(x, y, z).is(target))
                return false;
        }
        Holder<Biome> replacement = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(target);
        boolean changed = rewriteChunk(level, chunk, (qx, qy, qz, current) -> qx == x && qz == z ? replacement : current);
        if (changed)
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        return changed;
    }

    public static void onNodeBlockRemoved(Level level, BlockPos pos, BlockState previous, BlockState replacement) {
        if (previous.is(replacement.getBlock()) || !(level instanceof ServerLevel server) || level.restoringBlockSnapshots)
            return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNode node) || range(node.kind()) == 0)
            return;
        NodeType type = node.kind();
        if (level.captureBlockSnapshots) {
            // A cancelled Forge placement can restore the node after onRemove returns.
            BlockPos origin = pos.immutable();
            var executor = server.getServer();
            executor.tell(new TickTask(executor.getTickCount(), () -> {
                var chunk = server.getChunkSource().getChunkNow(origin.getX() >> 4, origin.getZ() >> 4);
                if (chunk != null && !chunk.getBlockState(origin).is(previous.getBlock()))
                    revert(server, origin, type);
            }));
        } else {
            revert(server, pos, type);
        }
    }

    static void revert(ServerLevel level, BlockPos origin, NodeType type) {
        int range = range(type);
        if (range == 0 || !level.dimension().equals(Level.OVERWORLD))
            return;
        int reach = range - 1;
        int minX = QuartPos.fromBlock(origin.getX() - reach);
        int maxX = QuartPos.fromBlock(origin.getX() + reach);
        int minZ = QuartPos.fromBlock(origin.getZ() - reach);
        int maxZ = QuartPos.fromBlock(origin.getZ() + reach);
        ResourceKey<Biome> painted = paintedBiome(type);
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        List<ChunkAccess> changed = new ArrayList<>();
        for (int cx = QuartPos.toSection(minX); cx <= QuartPos.toSection(maxX); cx++) {
            for (int cz = QuartPos.toSection(minZ); cz <= QuartPos.toSection(maxZ); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                if (rewriteChunk(level, chunk, (x, y, z, current) -> x >= minX && x <= maxX && z >= minZ && z <= maxZ && current.is(painted) ? source.getNoiseBiome(x, y, z, sampler) : current)) {
                    changed.add(chunk);
                }
            }
        }
        if (!changed.isEmpty())
            level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
    }

    private static boolean rewriteChunk(ServerLevel level, LevelChunk chunk, BiomeEdit edit) {
        int minX = QuartPos.fromSection(chunk.getPos().x);
        int minZ = QuartPos.fromSection(chunk.getPos().z);
        int minY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxY = QuartPos.fromBlock(chunk.getMaxBuildHeight());
        var values = new ArrayList<Holder<Biome>>((maxY - minY) * QUARTS_PER_CHUNK * QUARTS_PER_CHUNK);
        boolean changed = false;
        for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < minZ + QUARTS_PER_CHUNK; z++) {
                for (int x = minX; x < minX + QUARTS_PER_CHUNK; x++) {
                    var current = chunk.getNoiseBiome(x, y, z);
                    var updated = edit.replace(x, y, z, current);
                    values.add(updated);
                    changed |= !current.equals(updated);
                }
            }
        }
        if (!changed)
            return false;
        // The chunk rebuilds its palettes in place, so resolve every sample from the prepared values.
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> values.get(((y - minY) * QUARTS_PER_CHUNK + z - minZ) * QUARTS_PER_CHUNK + x - minX), level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        return true;
    }

    private static boolean hasSilverwood(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockState(pos).is(SILVERWOOD_LOGS))
                return true;
        }
        return false;
    }

    private static int range(NodeType type) {
        return switch (type) {
            case DARK -> DARK_RANGE;
            case PURE -> PURE_RANGE;
            default -> 0;
        };
    }

    private static ResourceKey<Biome> paintedBiome(NodeType type) {
        return type == NodeType.DARK ? TTBiomeKeys.EERIE : TTBiomeKeys.MAGICAL_FOREST;
    }

    @FunctionalInterface
    private interface BiomeEdit {
        Holder<Biome> replace(int x, int y, int z, Holder<Biome> current);
    }
}
