package com.leclowndu93150.thaumaturge.registry;

import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraft.world.level.block.SoundType;

/** Wave 0–1 block sound types (CE {@code TTSoundTypes}). */
public final class TTSoundTypes {
    public static final SoundType CRYSTAL = new ForgeSoundType(0.5F, 1.0F,
            TTSounds.CRYSTAL, TTSounds.CRYSTAL, TTSounds.CRYSTAL, TTSounds.CRYSTAL, TTSounds.CRYSTAL);

    public static final SoundType JAR = new ForgeSoundType(0.5F, 1.0F,
            TTSounds.JAR, TTSounds.JAR, TTSounds.JAR, TTSounds.JAR, TTSounds.JAR);

    private TTSoundTypes() {}
}
