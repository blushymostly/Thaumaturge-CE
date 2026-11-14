package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.particle.BlockRunesParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TTIds.MODID);

    public static final RegistryObject<SimpleParticleType> WISP = PARTICLES.register("wisp", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BORE = PARTICLES.register("bore", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TAINT = PARTICLES.register("taint", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ESSENTIA = PARTICLES.register("essentia", () -> new SimpleParticleType(false));

    public static final RegistryObject<TTParticleType<SparkleParticleOptions>> SPARKLE = PARTICLES.register("sparkle",
            () -> new TTParticleType<>(SparkleParticleOptions.DESERIALIZER, SparkleParticleOptions.CODEC));
    public static final RegistryObject<TTParticleType<BlockRunesParticleOptions>> BLOCK_RUNES = PARTICLES.register("block_runes",
            () -> new TTParticleType<>(BlockRunesParticleOptions.DESERIALIZER, BlockRunesParticleOptions.CODEC));

    private TTParticles() {}

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
