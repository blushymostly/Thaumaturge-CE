package com.leclowndu93150.thaumaturge.api.aspect;

import com.leclowndu93150.thaumaturge.TTIds;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public interface IAspect {
    ResourceKey<Registry<IAspect>> REGISTRY_KEY = ResourceKey.createRegistryKey(TTIds.rl("aspect"));

    String tag();

    int color();

    List<ResourceLocation> componentIds();

    default boolean isPrimal() {
        return componentIds().isEmpty();
    }
}
