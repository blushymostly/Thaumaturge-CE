package com.leclowndu93150.thaumaturge.content.essentia;

import com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockEntityBellows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class BellowsHelper {
    private BellowsHelper() {}

    public static boolean blowsToward(Level level, BlockPos source, Direction fromTarget) {
        return level.hasChunkAt(source) && level.getBlockEntity(source) instanceof BlockEntityBellows bellows
                && bellows.bellowsEnabled() && bellows.bellowsFacing() == fromTarget.getOpposite();
    }
}
