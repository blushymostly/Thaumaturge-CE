package com.leclowndu93150.thaumaturge.api.recipe;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Where a multiblock dust trigger matched relative to the clicked block.
 *
 * <p>The offsets are added to the clicked position to obtain the multiblock origin. The facing names the
 * horizontal rotation of the structure that matched and is {@code null} only for {@link #origin()}.
 */
public record DustTriggerPlacement(int xOffset, int yOffset, int zOffset, @Nullable Direction facing) {
    private static final int NO_OFFSET = 0;

    public static DustTriggerPlacement origin() {
        return new DustTriggerPlacement(NO_OFFSET, NO_OFFSET, NO_OFFSET, null);
    }
}
