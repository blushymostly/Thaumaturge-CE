package com.leclowndu93150.thaumaturge.content.research;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record ResearchCategory(
        Optional<ResourceLocation> requiredResearch,
        AspectList formula,
        ResourceLocation icon,
        ResourceLocation background,
        Optional<ResourceLocation> overlayBackground,
        int index) {
    public static final Codec<ResearchCategory> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.optionalFieldOf("required_research").forGetter(ResearchCategory::requiredResearch),
                    AspectList.CODEC.optionalFieldOf("formula", AspectList.EMPTY).forGetter(ResearchCategory::formula),
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(ResearchCategory::icon),
                    ResourceLocation.CODEC.fieldOf("background").forGetter(ResearchCategory::background),
                    ResourceLocation.CODEC.optionalFieldOf("overlay_background").forGetter(ResearchCategory::overlayBackground),
                    Codec.INT.optionalFieldOf("index", 0).forGetter(ResearchCategory::index))
            .apply(instance, ResearchCategory::new));
}
