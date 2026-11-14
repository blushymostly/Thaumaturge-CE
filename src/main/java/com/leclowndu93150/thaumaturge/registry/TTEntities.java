package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.content.taint.CausalityCollapser;
import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.golem.ThaumicGolem;
import com.leclowndu93150.thaumaturge.content.taint.TaintSeedEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TTIds.MODID);

    public static final RegistryObject<EntityType<ThaumicGolem>> GOLEM = ENTITIES.register("golem",
            () -> EntityType.Builder.of(ThaumicGolem::new, MobCategory.CREATURE).sized(0.6F, 1.0F).clientTrackingRange(8).build("golem"));
    public static final RegistryObject<EntityType<TaintSeedEntity>> TAINT_SEED = ENTITIES.register("taint_seed",
            () -> EntityType.Builder.of(TaintSeedEntity::new, MobCategory.MONSTER).sized(0.8F, 0.8F).clientTrackingRange(8).build("taint_seed"));

    public static final RegistryObject<EntityType<FluxRift>> FLUX_RIFT = ENTITIES.register("flux_rift",
            () -> EntityType.Builder.of(FluxRift::new, MobCategory.MISC).sized(1.0F, 1.0F)
                    .fireImmune().clientTrackingRange(10).build("flux_rift"));
    public static final RegistryObject<EntityType<CausalityCollapser>> CAUSALITY_COLLAPSER = ENTITIES.register("causality_collapser",
            () -> EntityType.Builder.<CausalityCollapser>of(CausalityCollapser::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("causality_collapser"));

    private TTEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
