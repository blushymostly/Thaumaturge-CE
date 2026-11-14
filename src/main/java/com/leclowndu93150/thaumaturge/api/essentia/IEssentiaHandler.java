package com.leclowndu93150.thaumaturge.api.essentia;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 1.20.1 essentia handler. The suction / sidedness defaults mirror CE26 {@code IEssentiaTransport}
 * (suction gradient between jars and tubes) without the Holder-based aspect handling.
 */
public interface IEssentiaHandler {
    AspectList getEssentia();

    int getCapacity();

    int insert(ResourceLocation aspectId, int amount, boolean simulate);

    int extract(ResourceLocation aspectId, int amount, boolean simulate);

    @Nullable
    ResourceLocation getFilter();

    /** Whether a tube may attach on {@code side} (null = any). */
    default boolean isConnectable(@Nullable Direction side) {
        return true;
    }

    default boolean canInputFrom(@Nullable Direction side) {
        return true;
    }

    default boolean canOutputTo(@Nullable Direction side) {
        return true;
    }

    /** Suction strength this device exerts on {@code side}; 0 = none. */
    default int getSuction(@Nullable Direction side) {
        return 0;
    }

    /** Aspect this device wants, or null for any. */
    @Nullable
    default ResourceLocation getSuctionAspect(@Nullable Direction side) {
        return getFilter();
    }

    /** Smallest suction an outside puller needs to draw from this device. */
    default int getMinimumSuction() {
        return 0;
    }
}
