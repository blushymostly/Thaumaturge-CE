package com.leclowndu93150.thaumaturge.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/** Particle type carrying custom {@link ParticleOptions} (Forge 1.20.1 stand-in for CE's TTParticleType). */
public final class TTParticleType<T extends ParticleOptions> extends ParticleType<T> {
    private final Codec<T> codec;

    public TTParticleType(ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
        super(false, deserializer);
        this.codec = codec;
    }

    @Override
    public Codec<T> codec() {
        return codec;
    }
}
