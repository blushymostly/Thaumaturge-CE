package com.leclowndu93150.thaumaturge.content.research;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record ResearchEntry(
        ResourceLocation category,
        String nameKey,
        Set<ResearchParent> parents,
        Set<ResourceLocation> siblings,
        int column,
        int row,
        List<ResearchStage> stages,
        Set<String> meta,
        List<String> icons,
        AspectList noteAspects,
        int complexity) {
    public static final Codec<ResearchEntry> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("category").forGetter(ResearchEntry::category),
                    Codec.STRING.fieldOf("name").forGetter(ResearchEntry::nameKey),
                    ResearchParent.CODEC.listOf().optionalFieldOf("parents", List.of()).forGetter(e -> List.copyOf(e.parents())),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("siblings", List.of()).forGetter(e -> List.copyOf(e.siblings())),
                    Codec.INT.fieldOf("column").forGetter(ResearchEntry::column),
                    Codec.INT.fieldOf("row").forGetter(ResearchEntry::row),
                    ResearchStage.CODEC.listOf().fieldOf("stages").forGetter(ResearchEntry::stages),
                    Codec.STRING.listOf().optionalFieldOf("meta", List.of()).forGetter(e -> List.copyOf(e.meta())),
                    Codec.STRING.listOf().optionalFieldOf("icons", List.of()).forGetter(ResearchEntry::icons),
                    AspectList.CODEC.optionalFieldOf("note_aspects", AspectList.EMPTY).forGetter(ResearchEntry::noteAspects),
                    Codec.intRange(1, 3).optionalFieldOf("complexity", 1).forGetter(ResearchEntry::complexity))
            .apply(instance, ResearchEntry::create));

    private static ResearchEntry create(
            ResourceLocation category,
            String nameKey,
            List<ResearchParent> parents,
            List<ResourceLocation> siblings,
            int column,
            int row,
            List<ResearchStage> stages,
            List<String> meta,
            List<String> icons,
            AspectList noteAspects,
            int complexity) {
        return new ResearchEntry(category, nameKey, new LinkedHashSet<>(parents), new LinkedHashSet<>(siblings), column, row,
                List.copyOf(stages), new LinkedHashSet<>(meta), List.copyOf(icons), noteAspects, complexity);
    }

    public boolean hasMeta(String flag) {
        return meta.contains(flag);
    }
}
