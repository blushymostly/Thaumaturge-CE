package com.leclowndu93150.thaumaturge.content.particle;

import com.leclowndu93150.thaumaturge.registry.TTParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public record BoltParticleOptions(Vec3 target, float width) implements ParticleOptions {
    public static final Codec<BoltParticleOptions> CODEC = RecordCodecBuilder
            .create(instance -> instance.group(Vec3.CODEC.fieldOf("target").forGetter(BoltParticleOptions::target), Codec.floatRange(0.01F, 1).fieldOf("width").forGetter(BoltParticleOptions::width))
                    .apply(instance, BoltParticleOptions::new));

    public static final ParticleOptions.Deserializer<BoltParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BoltParticleOptions fromCommand(ParticleType<BoltParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            double x = reader.readDouble();
            reader.expect(' ');
            double y = reader.readDouble();
            reader.expect(' ');
            double z = reader.readDouble();
            reader.expect(' ');
            return new BoltParticleOptions(new Vec3(x, y, z), reader.readFloat());
        }

        @Override
        public BoltParticleOptions fromNetwork(ParticleType<BoltParticleOptions> type, FriendlyByteBuf buffer) {
            return new BoltParticleOptions(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
        }
    };

    public static void strike(ServerLevel level, Vec3 origin, Vec3 target, float width) {
        level.sendParticles(new BoltParticleOptions(target, width), origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
    }

    @Override
    public ParticleType<?> getType() {
        return TTParticles.BOLT.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(target.x);
        buffer.writeDouble(target.y);
        buffer.writeDouble(target.z);
        buffer.writeFloat(width);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "thaumaturge:bolt %f %f %f %f", target.x, target.y, target.z, width);
    }
}
