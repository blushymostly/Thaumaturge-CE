package com.leclowndu93150.thaumaturge.content.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class ResearchData {
    private static final Map<ResourceLocation, ResearchEntry> ENTRIES = new HashMap<>();
    private static final Map<ResourceLocation, ResearchCategory> CATEGORIES = new HashMap<>();

    private ResearchData() {}

    public static void replace(Map<ResourceLocation, ResearchEntry> entries, Map<ResourceLocation, ResearchCategory> categories) {
        ENTRIES.clear();
        ENTRIES.putAll(entries);
        CATEGORIES.clear();
        CATEGORIES.putAll(categories);
    }

    public static ResearchEntry entry(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static ResearchCategory category(ResourceLocation id) {
        return CATEGORIES.get(id);
    }

    public static List<ResourceLocation> categories() {
        List<ResourceLocation> ids = new ArrayList<>(CATEGORIES.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return Collections.unmodifiableList(ids);
    }

    public static List<ResourceLocation> entriesInCategory(ResourceLocation categoryId) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ResearchEntry> pair : ENTRIES.entrySet()) {
            if (pair.getValue().category().equals(categoryId)) {
                ids.add(pair.getKey());
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::getPath));
        return ids;
    }
}
