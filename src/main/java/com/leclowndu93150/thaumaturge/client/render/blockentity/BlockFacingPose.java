package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/** Forge port of CE BlockFacingPose. */
public final class BlockFacingPose {
    private static final float QUARTER = 90.0F;
    private static final float HALF = 180.0F;
    private static final float THREE_QUARTERS = 270.0F;

    private BlockFacingPose() {}

    public static void northBased(PoseStack poseStack, Direction facing) {
        float xRot = switch (facing) {
            case DOWN -> QUARTER;
            case UP -> THREE_QUARTERS;
            default -> 0.0F;
        };
        float yRot = switch (facing) {
            case EAST -> QUARTER;
            case SOUTH -> HALF;
            case WEST -> THREE_QUARTERS;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
    }
}
