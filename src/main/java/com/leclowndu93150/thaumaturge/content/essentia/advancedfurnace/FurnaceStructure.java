package com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace;

import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

final class FurnaceStructure {
    private static final String PREFIX = "advanced_alchemical_furnace_";

    private FurnaceStructure() {}

    static Block block(String id) {
        return TTBulkContent.BLOCKS.get(id).get();
    }

    static boolean loaded(Level level, BlockPos pos) {
        return level.hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1));
    }

    static boolean formed(Level level, BlockPos pos) {
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            if (at.getX() == pos.getX() && at.getZ() == pos.getZ()) {
                continue;
            }
            boolean corner = at.getX() != pos.getX() && at.getZ() != pos.getZ();
            boolean bottom = at.getY() == pos.getY();
            String part = bottom ? (corner ? "advanced_construct_placeholder" : "nozzle")
                    : (corner ? "alembic_placeholder" : "construct_placeholder");
            if (!level.getBlockState(at).is(block(PREFIX + part))) return false;
        }
        return true;
    }

    static void restore(Level level, BlockPos controller) {
        for (BlockPos at : BlockPos.betweenClosed(controller.offset(-1, 0, -1), controller.offset(1, 1, 1))) {
            if (!level.hasChunkAt(at)) continue;
            var state = level.getBlockState(at);
            Block original;
            if (state.is(block(PREFIX + "alembic_placeholder"))) {
                original = TTBlocks.ALEMBIC.get();
            } else if (state.is(block(PREFIX + "advanced_construct_placeholder"))) {
                original = block("advanced_alchemical_construct");
            } else if (state.is(block(PREFIX + "construct_placeholder")) || state.is(block(PREFIX + "nozzle"))) {
                original = block("alchemical_construct");
            } else {
                continue;
            }
            level.setBlock(at, original.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    static void disassemble(Level level, BlockPos part) {
        for (BlockPos at : BlockPos.betweenClosed(part.offset(-1, -1, -1), part.offset(1, 0, 1))) {
            if (level.hasChunkAt(at) && level.getBlockState(at).is(block("advanced_alchemical_furnace"))) {
                level.setBlock(at, TTBlocks.SMELTER_BASIC.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }
}
