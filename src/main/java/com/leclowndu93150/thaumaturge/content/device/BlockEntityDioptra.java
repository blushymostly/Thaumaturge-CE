package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Samples a 13×13 chunk neighbourhood of vis (enabled) or flux (disabled) into a compact grid. */
public class BlockEntityDioptra extends BlockEntity {
    public static final int GRID_SIZE = 13;
    public static final int GRID_LENGTH = GRID_SIZE * GRID_SIZE;

    private static final int SAMPLE_INTERVAL = 20;
    private static final int CHUNK_OFFSET = GRID_SIZE / 2;
    private static final float AURA_SCALE = 500.0F;
    private static final float GRID_MAX = 64.0F;
    private static final String GRID_KEY = "grid_a";

    private final byte[] grid = new byte[GRID_LENGTH];
    private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
    private int ticks;

    public BlockEntityDioptra(BlockPos pos, BlockState state) {
        super(TTBlockEntities.DIOPTRA.get(), pos, state);
    }

    public byte gridValue(int index) {
        return index >= 0 && index < GRID_LENGTH ? grid[index] : 0;
    }

    public byte[] grid() {
        return grid;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 1, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityDioptra dioptra) {
        if (++dioptra.ticks % SAMPLE_INTERVAL == 0) {
            dioptra.refresh(level, state);
        }
    }

    private void refresh(Level level, BlockState state) {
        boolean showVis = state.getValue(BlockStateProperties.ENABLED);
        int centerX = SectionPos.blockToSectionCoord(worldPosition.getX());
        int centerZ = SectionPos.blockToSectionCoord(worldPosition.getZ());
        boolean changed = false;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                samplePos.set(SectionPos.sectionToBlockCoord(centerX + column - CHUNK_OFFSET), worldPosition.getY(),
                        SectionPos.sectionToBlockCoord(centerZ + row - CHUNK_OFFSET));
                float value = showVis ? AuraHelper.getVis(level, samplePos) : AuraHelper.getFlux(level, samplePos);
                byte sample = toSample(value);
                int index = row * GRID_SIZE + column;
                if (grid[index] != sample) {
                    grid[index] = sample;
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private static byte toSample(float value) {
        if (value <= 0.0F) {
            return 0;
        }
        return (byte) (int) Math.min(GRID_MAX, value / AURA_SCALE * GRID_MAX);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        int[] values = new int[GRID_LENGTH];
        for (int index = 0; index < GRID_LENGTH; index++) {
            values[index] = grid[index] & 0xFF;
        }
        tag.putIntArray(GRID_KEY, values);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(GRID_KEY)) {
            loadGrid(tag.getIntArray(GRID_KEY));
        }
    }

    private void loadGrid(int[] values) {
        if (values.length != GRID_LENGTH) {
            return;
        }
        for (int index = 0; index < GRID_LENGTH; index++) {
            grid[index] = (byte) Math.max(0, Math.min((int) GRID_MAX, values[index]));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
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
            load(tag);
        }
    }
}
