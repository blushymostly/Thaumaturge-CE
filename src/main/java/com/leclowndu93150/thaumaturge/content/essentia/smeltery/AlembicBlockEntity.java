package com.leclowndu93150.thaumaturge.content.essentia.smeltery;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public final class AlembicBlockEntity extends BlockEntity {
    private static final int CAPACITY = 128;
    private final EssentiaTank tank = new EssentiaTank(CAPACITY).onChange(this::contentsChanged);
    private LazyOptional<IEssentiaHandler> output;

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.ALEMBIC.get(), pos, state);
        output = newOutput();
    }

    private LazyOptional<IEssentiaHandler> newOutput() {
        return LazyOptional.of(() -> new IEssentiaHandler() {
            @Override
            public AspectList getEssentia() {
                return tank.getEssentia();
            }

            @Override
            public int getCapacity() {
                return CAPACITY;
            }

            @Override
            public @Nullable ResourceLocation getFilter() {
                return tank.getFilter();
            }

            @Override
            public boolean canInputFrom(@Nullable Direction side) {
                return false;
            }

            @Override
            public int extract(ResourceLocation aspect, int amount, boolean simulate) {
                return tank.extract(aspect, amount, simulate);
            }

            @Override
            public int insert(ResourceLocation aspect, int amount, boolean simulate) {
                return 0;
            }
        });
    }

    static boolean feedColumn(Level level, BlockPos base, ResourceLocation aspect) {
        AlembicBlockEntity firstEmpty = null;
        BlockPos.MutableBlockPos cursor = base.mutable().move(Direction.UP);
        while (level.hasChunkAt(cursor) && level.getBlockEntity(cursor) instanceof AlembicBlockEntity alembic) {
            if (alembic.tank.insert(aspect, 1, true) > 0) {
                if (alembic.tank.stored() > 0) {
                    return alembic.tank.insert(aspect, 1, false) == 1;
                }
                if (firstEmpty == null) {
                    firstEmpty = alembic;
                }
            }
            cursor.move(Direction.UP);
        }
        return firstEmpty != null && firstEmpty.tank.insert(aspect, 1, false) == 1;
    }

    int comparatorSignal() {
        int amount = tank.stored();
        return amount == 0 ? 0 : Math.min(15, 1 + 14 * amount / CAPACITY);
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    boolean pourOut() {
        if (level == null || tank.stored() == 0) {
            return false;
        }
        tank.pourOut(level, worldPosition);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.save());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.load(tag.getCompound("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.ESSENTIA) {
            return side == Direction.DOWN ? LazyOptional.empty() : output.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        output.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        output = newOutput();
    }
}
