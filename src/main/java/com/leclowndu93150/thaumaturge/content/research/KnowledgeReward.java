package com.leclowndu93150.thaumaturge.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record KnowledgeReward(KnowledgeType type, ResourceLocation category, int amount) {
    public static final Codec<KnowledgeReward> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(KnowledgeType.CODEC.fieldOf("type").forGetter(KnowledgeReward::type),
                    ResourceLocation.CODEC.fieldOf("category").forGetter(KnowledgeReward::category),
                    Codec.INT.fieldOf("amount").forGetter(KnowledgeReward::amount))
            .apply(instance, KnowledgeReward::new));
}
