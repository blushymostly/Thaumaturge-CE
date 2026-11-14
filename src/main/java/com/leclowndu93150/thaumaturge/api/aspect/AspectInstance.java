package com.leclowndu93150.thaumaturge.api.aspect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AspectInstance(ResourceLocation aspectId, int amount) {
    public static final Codec<AspectInstance> CODEC = RecordCodecBuilder.create(builder -> builder
            .group(ResourceLocation.CODEC.fieldOf("aspect").forGetter(AspectInstance::aspectId),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("amount", 1).forGetter(AspectInstance::amount))
            .apply(builder, AspectInstance::new));

    public AspectInstance {
        if (amount < 1) {
            throw new IllegalArgumentException("AspectInstance amount must be positive, was " + amount);
        }
    }

    public AspectInstance withAmount(int newAmount) {
        return new AspectInstance(aspectId, newAmount);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(aspectId);
        buf.writeVarInt(amount);
    }

    public static AspectInstance read(FriendlyByteBuf buf) {
        return new AspectInstance(buf.readResourceLocation(), buf.readVarInt());
    }

    @Override
    public String toString() {
        return aspectId.getPath() + " x" + amount;
    }
}
