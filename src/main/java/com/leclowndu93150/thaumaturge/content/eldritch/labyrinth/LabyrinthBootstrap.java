package com.leclowndu93150.thaumaturge.content.eldritch.labyrinth;

import com.leclowndu93150.thaumaturge.Thaumaturge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Labyrinth / outer lands entry — datapack + Lithostitched biomes remain in resources;
 * chunk-generator port grows here from CE26.
 */
public final class LabyrinthBootstrap {
    private LabyrinthBootstrap() {}

    public static void init(IEventBus modBus) {
        Thaumaturge.LOGGER.debug("Labyrinth bootstrap ready (Lithostitched datapack retained)");
    }
}
