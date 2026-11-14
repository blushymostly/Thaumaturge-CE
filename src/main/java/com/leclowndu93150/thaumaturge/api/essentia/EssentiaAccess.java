package com.leclowndu93150.thaumaturge.api.essentia;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Forge port of CE26 {@code EssentiaAccess}: capability lookups plus a simulation-aware single transfer. */
public final class EssentiaAccess {
    private EssentiaAccess() {}

    /** Essentia handler on the given face of the block at {@code pos}; never loads chunks. */
    @Nullable
    public static IEssentiaHandler handler(Level level, BlockPos pos, @Nullable Direction face) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        return handler((BlockGetter) level, pos, face);
    }

    @Nullable
    public static IEssentiaHandler handler(BlockGetter getter, BlockPos pos, @Nullable Direction face) {
        BlockEntity be = getter.getBlockEntity(pos);
        return be == null ? null : be.getCapability(TTCapabilities.ESSENTIA, face).resolve().orElse(null);
    }

    /** Essentia a client or server block entity currently holds, or {@link AspectList#EMPTY}. */
    public static AspectList contentsOf(BlockGetter getter, BlockPos pos) {
        IEssentiaHandler handler = handler(getter, pos, null);
        return handler == null ? AspectList.EMPTY : handler.getEssentia();
    }

    /** First aspect held by a handler that is acceptable to {@code wanted} (null = any), or null. */
    @Nullable
    public static ResourceLocation firstAvailable(IEssentiaHandler handler, @Nullable ResourceLocation wanted) {
        for (AspectInstance entry : handler.getEssentia().entries()) {
            if (wanted == null || wanted.equals(entry.aspectId())) {
                return entry.aspectId();
            }
        }
        return null;
    }

    /**
     * Moves up to {@code max} of {@code aspect} from {@code src} to {@code dst}, honouring sidedness.
     *
     * @return amount moved (or that would move when {@code simulate})
     */
    public static int transfer(IEssentiaHandler src, @Nullable Direction srcFace, IEssentiaHandler dst, @Nullable Direction dstFace,
            ResourceLocation aspect, int max, boolean simulate) {
        if (max <= 0 || !src.canOutputTo(srcFace) || !dst.canInputFrom(dstFace)) {
            return 0;
        }
        int available = src.extract(aspect, max, true);
        if (available <= 0) {
            return 0;
        }
        int acceptable = dst.insert(aspect, available, true);
        if (acceptable <= 0) {
            return 0;
        }
        if (simulate) {
            return acceptable;
        }
        int taken = src.extract(aspect, acceptable, false);
        int accepted = dst.insert(aspect, taken, false);
        if (accepted < taken) {
            src.insert(aspect, taken - accepted, false);
        }
        return accepted;
    }
}
