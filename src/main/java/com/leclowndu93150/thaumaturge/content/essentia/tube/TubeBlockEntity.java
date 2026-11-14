package com.leclowndu93150.thaumaturge.content.essentia.tube;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.flow.EssentiaFlowHandler;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simplified CE26 {@code BlockEntityTube}: a one-unit buffer that derives suction from neighbours and pulls
 * essentia along the suction gradient. Side toggling, facing, valves and venting are deferred.
 */
public class TubeBlockEntity extends BlockEntity implements IEssentiaHandler {
    public static final int CELL_CAPACITY = 1;
    private static final int SUCTION_REFRESH = 2;
    private static final int EQUALISE = 5;
    private static final int STAGGER_SPAN = 10;

    private LazyOptional<IEssentiaHandler> selfOpt = LazyOptional.of(() -> this);
    @Nullable
    private ResourceLocation cellAspect;
    private int cellAmount;
    @Nullable
    private ResourceLocation suctionAspect;
    private int suction;
    private int ticks;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.TUBE.get(), pos, state);
        this.ticks = Math.floorMod(31 * pos.getX() + 17 * pos.getY() + 13 * pos.getZ(), STAGGER_SPAN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TubeBlockEntity tube) {
        tube.tickServer(level, pos);
    }

    private void tickServer(Level level, BlockPos pos) {
        ticks++;
        if (ticks % SUCTION_REFRESH == 0) {
            EssentiaFlowHandler.recalculateSuction(level, pos, this);
            if (cellAspect != null && cellAmount == 0) {
                cellAspect = null;
                setChanged();
            }
        }
        if (ticks % EQUALISE == 0 && suction > 0) {
            EssentiaFlowHandler.equalizeWithNeighbours(level, pos, this);
        }
    }

    @Nullable
    public ResourceLocation cellAspect() {
        return cellAspect;
    }

    public int cellAmount() {
        return cellAmount;
    }

    public void setSuction(@Nullable ResourceLocation aspect, int strength) {
        this.suction = Math.max(0, strength);
        this.suctionAspect = this.suction > 0 ? aspect : null;
    }

    @Override
    public AspectList getEssentia() {
        return cellAspect == null || cellAmount <= 0 ? AspectList.EMPTY : AspectList.EMPTY.add(new AspectInstance(cellAspect, cellAmount));
    }

    @Override
    public int getCapacity() {
        return CELL_CAPACITY;
    }

    @Override
    public int insert(ResourceLocation aspectId, int amount, boolean simulate) {
        if (amount <= 0 || cellAmount > 0) {
            return 0;
        }
        if (!simulate) {
            cellAspect = aspectId;
            cellAmount = CELL_CAPACITY;
            setChanged();
        }
        return CELL_CAPACITY;
    }

    @Override
    public int extract(ResourceLocation aspectId, int amount, boolean simulate) {
        if (amount <= 0 || cellAmount <= 0 || !aspectId.equals(cellAspect)) {
            return 0;
        }
        if (!simulate) {
            cellAspect = null;
            cellAmount = 0;
            setChanged();
        }
        return CELL_CAPACITY;
    }

    @Nullable
    @Override
    public ResourceLocation getFilter() {
        return null;
    }

    @Override
    public int getSuction(@Nullable Direction side) {
        return suction;
    }

    @Nullable
    @Override
    public ResourceLocation getSuctionAspect(@Nullable Direction side) {
        return suctionAspect;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (cellAspect != null && cellAmount > 0) {
            tag.putString("EssentiaType", cellAspect.toString());
            tag.putInt("EssentiaAmount", cellAmount);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cellAspect = tag.contains("EssentiaType") ? ResourceLocation.tryParse(tag.getString("EssentiaType")) : null;
        cellAmount = cellAspect == null ? 0 : tag.getInt("EssentiaAmount");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.ESSENTIA) {
            return selfOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        selfOpt.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        selfOpt = LazyOptional.of(() -> this);
    }
}
