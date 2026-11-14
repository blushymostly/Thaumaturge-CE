package com.leclowndu93150.thaumaturge.content.warp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

public final class PlayerWarpState {
    private int permanent;
    private int normal;
    private int temporary;

    public int getPermanent() {
        return permanent;
    }

    public int getNormal() {
        return normal;
    }

    public int getTemporary() {
        return temporary;
    }

    public int total() {
        return permanent + normal + temporary;
    }

    public void addPermanent(int amount) {
        permanent = Math.max(0, permanent + amount);
    }

    public void addNormal(int amount) {
        normal = Math.max(0, normal + amount);
    }

    public void addTemporary(int amount) {
        temporary = Math.max(0, temporary + amount);
    }

    public void tickDownTemporary() {
        if (temporary > 0) {
            temporary = Math.max(0, temporary - 1);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("perm", permanent);
        tag.putInt("normal", normal);
        tag.putInt("temp", temporary);
        return tag;
    }

    public void load(CompoundTag tag) {
        permanent = Math.max(0, tag.getInt("perm"));
        normal = Math.max(0, tag.getInt("normal"));
        temporary = Math.max(0, tag.getInt("temp"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(permanent);
        buf.writeVarInt(normal);
        buf.writeVarInt(temporary);
    }

    public void read(FriendlyByteBuf buf) {
        permanent = buf.readVarInt();
        normal = buf.readVarInt();
        temporary = buf.readVarInt();
    }

    public void copyFrom(PlayerWarpState other) {
        this.permanent = other.permanent;
        this.normal = other.normal;
        this.temporary = other.temporary;
    }

    public void clamp() {
        permanent = Mth.clamp(permanent, 0, 1000);
        normal = Mth.clamp(normal, 0, 1000);
        temporary = Mth.clamp(temporary, 0, 1000);
    }
}
