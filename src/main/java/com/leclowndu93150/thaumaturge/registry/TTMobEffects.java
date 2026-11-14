package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.taint.TaintSeedEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TTIds.MODID);
    public static final RegistryObject<MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x80407F) {});
    public static final RegistryObject<MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFF0080) {
                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    return duration % Math.max(1, 40 >> Math.min(amplifier, 5)) == 0;
                }

                @Override
                public void applyEffectTick(LivingEntity entity, int amplifier) {
                    if (entity.level().isClientSide) return;
                    if (entity instanceof TaintSeedEntity) entity.heal(1);
                    else if (!entity.isInvertedHealAndHarm()) entity.hurt(entity.damageSources().magic(), 1);
                }
            });

    private TTMobEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
