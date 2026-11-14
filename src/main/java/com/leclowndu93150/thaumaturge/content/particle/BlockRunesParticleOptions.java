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

public record BlockRunesParticleOptions(float r, float g, float b, int duration, float gravity, boolean variant) implements ParticleOptions {

    public static final Codec<BlockRunesParticleOptions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("r").forGetter(BlockRunesParticleOptions::r),
            Codec.FLOAT.fieldOf("g").forGetter(BlockRunesParticleOptions::g),
            Codec.FLOAT.fieldOf("b").forGetter(BlockRunesParticleOptions::b),
            Codec.INT.fieldOf("duration").forGetter(BlockRunesParticleOptions::duration),
            Codec.FLOAT.fieldOf("gravity").forGetter(BlockRunesParticleOptions::gravity),
            Codec.BOOL.fieldOf("variant").forGetter(BlockRunesParticleOptions::variant)).apply(inst, BlockRunesParticleOptions::new));

    public static final ParticleOptions.Deserializer<BlockRunesParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BlockRunesParticleOptions fromCommand(ParticleType<BlockRunesParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            reader.expect(' ');
            int duration = reader.readInt();
            reader.expect(' ');
            float gravity = reader.readFloat();
            reader.expect(' ');
            boolean variant = reader.readBoolean();
            return new BlockRunesParticleOptions(r, g, b, duration, gravity, variant);
        }

        @Override
        public BlockRunesParticleOptions fromNetwork(ParticleType<BlockRunesParticleOptions> type, FriendlyByteBuf buf) {
            return new BlockRunesParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readBoolean());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return TTParticles.BLOCK_RUNES.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeVarInt(duration);
        buf.writeFloat(gravity);
        buf.writeBoolean(variant);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %d %.2f %b", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), r, g, b, duration, gravity, variant);
    }
}
