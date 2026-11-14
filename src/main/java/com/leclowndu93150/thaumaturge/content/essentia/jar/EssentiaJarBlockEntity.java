package com.leclowndu93150.thaumaturge.content.essentia.jar;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EssentiaJarBlockEntity extends BlockEntity {
    public static final int CAPACITY = 250;
    private static final int FILL_INTERVAL = 5;

    private final EssentiaTank tank = new EssentiaTank(CAPACITY).suckWhenNotFull(true).onChange(this::onTankChanged);
    private LazyOptional<EssentiaTank> tankOpt = LazyOptional.of(() -> tank);
    private int fillTicks;

    public EssentiaJarBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.ESSENTIA_JAR.get(), pos, state);
    }

    public EssentiaTank tank() {
        return tank;
    }

    private void onTankChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    /** CE26 {@code AbstractBlockEntityJar.serverTick}: pull from the block above every few ticks. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EssentiaJarBlockEntity jar) {
        if (++jar.fillTicks % FILL_INTERVAL == 0 && jar.tank.stored() < jar.tank.getCapacity()) {
            jar.pullFromAbove(level, pos);
        }
    }

    private void pullFromAbove(Level level, BlockPos pos) {
        IEssentiaHandler source = EssentiaAccess.handler(level, pos.above(), Direction.DOWN);
        if (source == null || !source.canOutputTo(Direction.DOWN)) {
            return;
        }
        int suction = tank.getSuction(Direction.UP);
        if (suction <= 0 || source.getSuction(Direction.DOWN) >= suction || suction < source.getMinimumSuction()) {
            return;
        }
        ResourceLocation aspect = EssentiaAccess.firstAvailable(source, tank.getSuctionAspect(Direction.UP));
        if (aspect != null) {
            EssentiaAccess.transfer(source, Direction.DOWN, tank, Direction.UP, aspect, 1, false);
        }
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
        CompoundTag tag = super.getUpdateTag();
        tag.put("Tank", tank.save());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            tank.load(tag.getCompound("Tank"));
        }
    }

    /** CE jars only connect to tubes through the top face. */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.ESSENTIA) {
            return side == null || side == Direction.UP ? tankOpt.cast() : LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankOpt.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        tankOpt = LazyOptional.of(() -> tank);
    }
}
