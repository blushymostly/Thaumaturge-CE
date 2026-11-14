package com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public final class FurnaceNozzle extends BlockEntity implements IEssentiaHandler {
    private LazyOptional<IEssentiaHandler> capability = LazyOptional.of(() -> this);

    public FurnaceNozzle(BlockPos pos, BlockState state) {
        super(TTBlockEntities.FURNACE_NOZZLE.get(), pos, state);
    }

    private @Nullable AdvancedFurnace furnace(@Nullable Direction side) {
        if (level != null) {
            for (Direction inward : Direction.Plane.HORIZONTAL) {
                BlockPos at = worldPosition.relative(inward);
                if (level.hasChunkAt(at) && level.getBlockEntity(at) instanceof AdvancedFurnace furnace) {
                    return side == null || side == inward.getOpposite() ? furnace : null;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isConnectable(@Nullable Direction side) {
        return furnace(side) != null;
    }

    @Override
    public boolean canOutputTo(@Nullable Direction side) {
        return isConnectable(side);
    }

    @Override
    public boolean canInputFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public AspectList getEssentia() {
        var furnace = furnace(null);
        return furnace == null ? AspectList.EMPTY : furnace.stock();
    }

    @Override
    public int getCapacity() {
        return AdvancedFurnace.CAPACITY;
    }

    @Override
    public int insert(ResourceLocation aspectId, int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int extract(ResourceLocation aspectId, int amount, boolean simulate) {
        var furnace = furnace(null);
        return furnace == null ? 0 : furnace.extract(aspectId, amount, simulate);
    }

    @Override
    public @Nullable ResourceLocation getFilter() {
        return null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.ESSENTIA) {
            return isConnectable(side) ? capability.cast() : LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capability = LazyOptional.of(() -> this);
    }
}
