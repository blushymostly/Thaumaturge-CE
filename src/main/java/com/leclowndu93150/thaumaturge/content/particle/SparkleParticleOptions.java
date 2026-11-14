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
import net.minecraftforge.registries.ForgeRegistries;

public record SparkleParticleOptions(int color, float scale, int delay, float decay, float gravity, int baseAge, boolean flicker) implements ParticleOptions {

    public static final Codec<SparkleParticleOptions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("color").forGetter(SparkleParticleOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(SparkleParticleOptions::scale),
            Codec.INT.fieldOf("delay").forGetter(SparkleParticleOptions::delay),
            Codec.FLOAT.fieldOf("decay").forGetter(SparkleParticleOptions::decay),
            Codec.FLOAT.fieldOf("gravity").forGetter(SparkleParticleOptions::gravity),
            Codec.INT.fieldOf("base_age").forGetter(SparkleParticleOptions::baseAge),
            Codec.BOOL.fieldOf("flicker").forGetter(SparkleParticleOptions::flicker)).apply(inst, SparkleParticleOptions::new));

    public static final ParticleOptions.Deserializer<SparkleParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public SparkleParticleOptions fromCommand(ParticleType<SparkleParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            int delay = reader.readInt();
            reader.expect(' ');
            float decay = reader.readFloat();
            reader.expect(' ');
            float gravity = reader.readFloat();
            reader.expect(' ');
            int baseAge = reader.readInt();
            reader.expect(' ');
            boolean flicker = reader.readBoolean();
            return new SparkleParticleOptions(color, scale, delay, decay, gravity, baseAge, flicker);
        }

        @Override
        public SparkleParticleOptions fromNetwork(ParticleType<SparkleParticleOptions> type, FriendlyByteBuf buf) {
            return new SparkleParticleOptions(buf.readInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return TTParticles.SPARKLE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(scale);
        buf.writeVarInt(delay);
        buf.writeFloat(decay);
        buf.writeFloat(gravity);
        buf.writeVarInt(baseAge);
        buf.writeBoolean(flicker);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.2f %d %.2f %.2f %d %b", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), color, scale, delay, decay, gravity, baseAge, flicker);
    }
}
