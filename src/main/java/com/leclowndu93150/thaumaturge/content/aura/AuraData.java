package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.api.aura.IAuraChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public final class AuraData implements IAuraChunk {
    private static final float MAX_POOL = 32766.0F;

    private short base;
    private float vis;
    private float flux;
    private boolean initialized;
    private ChunkPos chunkPos = new ChunkPos(0, 0);

    public AuraData() {}

    public AuraData(short base, float vis, float flux, boolean initialized) {
        this.base = base;
        this.vis = clamp(vis);
        this.flux = clamp(flux);
        this.initialized = initialized || base != 0;
    }

    private static float clamp(float value) {
        return Mth.clamp(value, 0.0F, MAX_POOL);
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public short getBase() {
        return base;
    }

    public void setBase(short newBase) {
        initialized = true;
        base = newBase;
    }

    @Override
    public float getVis() {
        return vis;
    }

    public void setVis(float value) {
        vis = clamp(value);
    }

    @Override
    public float getFlux() {
        return flux;
    }

    public void setFlux(float value) {
        flux = clamp(value);
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public void setChunkPos(ChunkPos pos) {
        this.chunkPos = pos;
    }

    public float drainVis(float amount) {
        float taken = Math.min(vis, Math.max(0.0F, amount));
        vis = clamp(vis - taken);
        return taken;
    }

    public float addVis(float amount) {
        float before = vis;
        vis = clamp(vis + Math.max(0.0F, amount));
        return vis - before;
    }

    public float addFlux(float amount) {
        float before = flux;
        flux = clamp(flux + Math.max(0.0F, amount));
        return flux - before;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putShort("base", base);
        tag.putFloat("vis", vis);
        tag.putFloat("flux", flux);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    public void load(CompoundTag tag) {
        base = tag.getShort("base");
        vis = clamp(tag.getFloat("vis"));
        flux = clamp(tag.getFloat("flux"));
        initialized = tag.getBoolean("initialized") || base != 0;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeShort(base);
        buf.writeFloat(vis);
        buf.writeFloat(flux);
        buf.writeBoolean(initialized);
    }

    public static AuraData read(FriendlyByteBuf buf) {
        return new AuraData(buf.readShort(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    public void copyFrom(AuraData other) {
        this.base = other.base;
        this.vis = other.vis;
        this.flux = other.flux;
        this.initialized = other.initialized;
        this.chunkPos = other.chunkPos;
    }
}
