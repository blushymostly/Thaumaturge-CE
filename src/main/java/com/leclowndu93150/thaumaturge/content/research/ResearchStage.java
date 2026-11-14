package com.leclowndu93150.thaumaturge.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record ResearchStage(
        String textKey,
        List<ResourceLocation> recipes,
        List<ResearchRequirement> obtain,
        List<ResearchRequirement> craft,
        List<KnowledgeReward> requiredKnowledge,
        List<ResourceLocation> requiredResearch,
        int warp) {
    public static final Codec<ResearchStage> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(Codec.STRING.fieldOf("text").forGetter(ResearchStage::textKey),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(ResearchStage::recipes),
                    ResearchRequirement.CODEC.listOf().optionalFieldOf("obtain", List.of()).forGetter(ResearchStage::obtain),
                    ResearchRequirement.CODEC.listOf().optionalFieldOf("craft", List.of()).forGetter(ResearchStage::craft),
                    KnowledgeReward.CODEC.listOf().optionalFieldOf("required_knowledge", List.of()).forGetter(ResearchStage::requiredKnowledge),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("required_research", List.of()).forGetter(ResearchStage::requiredResearch),
                    Codec.INT.optionalFieldOf("warp", 0).forGetter(ResearchStage::warp))
            .apply(instance, ResearchStage::new));
}
