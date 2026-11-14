package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EtherealBloom extends BlockEntity {
    public static final int RADIUS = 8;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;

    public EtherealBloom(BlockPos pos, BlockState state) {
        super(TTBlockEntities.ETHEREAL_BLOOM.get(), pos, state);
    }

    public static boolean protects(ServerLevel level, BlockPos pos) {
        for (int x = (pos.getX() - RADIUS) >> 4; x <= (pos.getX() + RADIUS) >> 4; x++) {
            for (int z = (pos.getZ() - RADIUS) >> 4; z <= (pos.getZ() + RADIUS) >> 4; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof EtherealBloom && !entity.isRemoved()
                            && entity.getBlockPos().distSqr(pos) <= RADIUS_SQUARED) return true;
                }
            }
        }
        return false;
    }

    void tick() {
        if (!(level instanceof ServerLevel server) || Math.floorMod(server.getGameTime() + worldPosition.asLong(), 20) != 0) return;
        var random = server.random;
        for (int sample = 0; sample < 4; sample++) {
            int x = random.nextIntBetweenInclusive(-RADIUS, RADIUS);
            int z = random.nextIntBetweenInclusive(-RADIUS, RADIUS);
            BlockPos column = worldPosition.offset(x, 0, z);
            if (x * x + z * z > RADIUS_SQUARED || !server.hasChunkAt(column)) continue;
            for (int y = 4; y >= -4; y--) {
                BlockPos target = column.offset(0, y, 0);
                if (target.distSqr(worldPosition) <= RADIUS_SQUARED && clean(server, target)) break;
            }
        }
    }

    private static boolean clean(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(TTBulkContent.BLOCKS.get("taint_soil").get())) return level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        if (state.is(TTBulkContent.BLOCKS.get("taint_rock").get())) return level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        return false;
    }
}
