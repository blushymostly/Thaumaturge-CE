package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Wave 0–1 (+ CE full catalog ids used by later waves). Definitions live in {@code sounds.json}. */
public final class TTSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TTIds.MODID);

    // Wave 0 early loop
    public static final RegistryObject<SoundEvent> SCAN = register("scan");
    public static final RegistryObject<SoundEvent> CAMERA_TICKS = register("cameraticks");
    public static final RegistryObject<SoundEvent> PAGE = register("page");
    public static final RegistryObject<SoundEvent> PAGETURN = register("pageturn");
    public static final RegistryObject<SoundEvent> CLACK = register("clack");
    public static final RegistryObject<SoundEvent> WRITE = register("write");
    public static final RegistryObject<SoundEvent> KEY = register("key");
    public static final RegistryObject<SoundEvent> ERASE = register("erase");
    public static final RegistryObject<SoundEvent> HHOFF = register("hhoff");
    public static final RegistryObject<SoundEvent> HHON = register("hhon");
    public static final RegistryObject<SoundEvent> LEARN = register("learn");
    public static final RegistryObject<SoundEvent> DUST = register("dust");
    public static final RegistryObject<SoundEvent> CRAFT = register("craft");
    public static final RegistryObject<SoundEvent> CRAFTSTART = register("craftstart");
    public static final RegistryObject<SoundEvent> CRAFTFAIL = register("craftfail");
    public static final RegistryObject<SoundEvent> BUBBLE = register("bubble");
    public static final RegistryObject<SoundEvent> SPILL = register("spill");
    public static final RegistryObject<SoundEvent> POOF = register("poof");
    public static final RegistryObject<SoundEvent> CRYSTAL = register("crystal");

    // Wave 1 essentia
    public static final RegistryObject<SoundEvent> BRAIN = register("brain");
    public static final RegistryObject<SoundEvent> JAR = register("jar");
    public static final RegistryObject<SoundEvent> CREAK = register("creak");
    public static final RegistryObject<SoundEvent> PUMP = register("pump");

    // Kept for later waves / existing call sites
    public static final RegistryObject<SoundEvent> INFUSION = register("infusion");
    public static final RegistryObject<SoundEvent> INFUSER = register("infuser");
    public static final RegistryObject<SoundEvent> WHISPER = register("whisper");
    public static final RegistryObject<SoundEvent> WHISPERS = register("whispers");

    private TTSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = TTIds.rl(name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
