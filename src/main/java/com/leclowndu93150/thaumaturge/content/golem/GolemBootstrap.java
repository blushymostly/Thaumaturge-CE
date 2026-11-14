package com.leclowndu93150.thaumaturge.content.golem;

import com.leclowndu93150.thaumaturge.registry.TTEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class GolemBootstrap {
    private GolemBootstrap() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(GolemBootstrap::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TTEntities.GOLEM.get(), ThaumicGolem.createAttributes().build());
    }
}
