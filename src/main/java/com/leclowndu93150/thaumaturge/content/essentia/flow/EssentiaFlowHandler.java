package com.leclowndu93150.thaumaturge.content.essentia.flow;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Forge adaptation of CE26 {@code EssentiaFlowHandler}: tubes derive suction from the strongest neighbour
 * (minus one per hop) and pull one unit per cycle from any neighbour with weaker suction.
 */
public final class EssentiaFlowHandler {
    private static final int HOP_LOSS = 1;
    private static final int TRANSFER_AMOUNT = 1;

    private EssentiaFlowHandler() {}

    public static void recalculateSuction(Level level, BlockPos pos, TubeBlockEntity tube) {
        tube.setSuction(null, 0);
        ResourceLocation held = tube.cellAmount() > 0 ? tube.cellAspect() : null;
        int strongest = 0;
        ResourceLocation strongestType = null;
        for (Direction direction : Direction.values()) {
            if (!tube.isConnectable(direction)) {
                continue;
            }
            Direction touching = direction.getOpposite();
            IEssentiaHandler neighbour = EssentiaAccess.handler(level, pos.relative(direction), touching);
            if (neighbour == null || !neighbour.isConnectable(touching)) {
                continue;
            }
            int offered = neighbour.getSuction(touching);
            if (offered <= 0 || offered <= strongest + HOP_LOSS) {
                continue;
            }
            ResourceLocation offeredType = neighbour.getSuctionAspect(touching);
            if (offeredType != null && held != null && !offeredType.equals(held)) {
                continue;
            }
            strongest = offered - HOP_LOSS;
            strongestType = offeredType;
        }
        if (strongest > 0) {
            tube.setSuction(strongestType, strongest);
        }
    }

    public static void equalizeWithNeighbours(Level level, BlockPos pos, TubeBlockEntity tube) {
        int suction = tube.getSuction(null);
        if (suction <= 0 || tube.cellAmount() > 0) {
            return;
        }
        ResourceLocation wanted = tube.getSuctionAspect(null);
        for (Direction direction : Direction.values()) {
            if (!tube.isConnectable(direction)) {
                continue;
            }
            Direction touching = direction.getOpposite();
            IEssentiaHandler neighbour = EssentiaAccess.handler(level, pos.relative(direction), touching);
            if (neighbour == null || !neighbour.isConnectable(touching) || !neighbour.canOutputTo(touching)
                    || neighbour.getSuction(touching) >= suction || suction < neighbour.getMinimumSuction()) {
                continue;
            }
            @Nullable ResourceLocation aspect = EssentiaAccess.firstAvailable(neighbour, wanted);
            if (aspect == null) {
                continue;
            }
            if (EssentiaAccess.transfer(neighbour, touching, tube, direction, aspect, TRANSFER_AMOUNT, false) >= TRANSFER_AMOUNT) {
                return;
            }
        }
    }
}
