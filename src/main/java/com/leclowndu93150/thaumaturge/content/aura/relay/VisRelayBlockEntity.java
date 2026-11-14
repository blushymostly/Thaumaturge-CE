package com.leclowndu93150.thaumaturge.content.aura.relay;

import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class VisRelayBlockEntity extends AbstractSyncedBlockEntity {
    private static final int RELINK_INTERVAL = 40;

    private @Nullable BlockPos parent;
    private int depth;
    private int pulseColor = 0xFFFFFF;
    private long pulseStart = -1000;

    public VisRelayBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.VIS_RELAY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VisRelayBlockEntity relay) {
        if (level instanceof ServerLevel server && (level.getGameTime() + pos.hashCode()) % RELINK_INTERVAL == 0) {
            VisRelayNetwork.refreshLink(server, relay);
        }
    }

    public @Nullable BlockPos parent() {
        return parent;
    }

    int depth() {
        return depth;
    }

    void setLink(@Nullable BlockPos newParent, int newDepth) {
        if (Objects.equals(parent, newParent) && depth == newDepth) return;
        parent = newParent;
        depth = newDepth;
        setChangedAndSync();
    }

    void pulse(long time, int color) {
        if (time - pulseStart < 5) return;
        pulseStart = time;
        pulseColor = color;
        if (level != null) {
            var state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public int pulseColor() {
        return pulseColor;
    }

    public long pulseStart() {
        return pulseStart;
    }

    @Override
    public AABB getRenderBoundingBox() {
        var own = new AABB(worldPosition);
        return parent == null ? own : own.minmax(new AABB(parent));
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        super.writeClientData(tag);
        tag.putLong("PulseStart", pulseStart);
        tag.putInt("PulseColor", pulseColor);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (parent != null) {
            tag.putLong("Parent", parent.asLong());
            tag.putInt("Depth", depth);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        pulseStart = tag.contains("PulseStart", Tag.TAG_LONG) ? tag.getLong("PulseStart") : -1000;
        pulseColor = tag.contains("PulseColor", Tag.TAG_INT) ? tag.getInt("PulseColor") : 0xFFFFFF;
        int savedDepth = tag.getInt("Depth");
        if (tag.contains("Parent", Tag.TAG_LONG) && savedDepth >= 1 && savedDepth <= VisRelayNetwork.MAX_HOPS) {
            parent = BlockPos.of(tag.getLong("Parent"));
            depth = savedDepth;
        } else {
            parent = null;
            depth = 0;
        }
    }
}
