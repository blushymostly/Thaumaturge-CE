package com.leclowndu93150.thaumaturge.api.aspect;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record Aspect(String tag, int color, List<ResourceLocation> componentIds) implements IAspect {
    public Aspect(String tag, int color) {
        this(tag, color, List.of());
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public List<ResourceLocation> componentIds() {
        return componentIds;
    }
}
