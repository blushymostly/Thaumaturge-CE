package com.leclowndu93150.thaumaturge.client.screen.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchData;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchParent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

final class BrowserModel {
    private final Player player;
    private final List<CategoryRef> builtIn = new ArrayList<>();
    private final List<CategoryRef> addOn = new ArrayList<>();
    private final Map<ResourceLocation, EntryNode> entries = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<EntryNode>> entriesByCategory = new HashMap<>();
    private final Map<ResourceLocation, Boolean> visibilityCache = new HashMap<>();

    BrowserModel(Player player) {
        this.player = player;
        for (ResourceLocation id : ResearchData.categories()) {
            ResearchCategory data = ResearchData.category(id);
            if (data == null) {
                continue;
            }
            CategoryRef category = new CategoryRef(id, data);
            (TTIds.MODID.equals(id.getNamespace()) ? builtIn : addOn).add(category);
        }
        builtIn.sort(Comparator.comparingInt(c -> c.data().index()));
        addOn.sort(Comparator.comparing(c -> c.id().toString()));
        for (ResourceLocation id : allEntryIds()) {
            ResearchEntry entry = ResearchData.entry(id);
            if (entry == null) {
                continue;
            }
            EntryNode node = new EntryNode(id, entry, entry.category());
            entries.put(id, node);
            entriesByCategory.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(node);
        }
    }

    private static List<ResourceLocation> allEntryIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation categoryId : ResearchData.categories()) {
            ids.addAll(ResearchData.entriesInCategory(categoryId));
        }
        return ids;
    }

    Player player() {
        return player;
    }

    PlayerKnowledge knowledge() {
        return ClientKnowledge.get();
    }

    List<CategoryRef> builtIn() {
        return builtIn;
    }

    List<CategoryRef> addOn() {
        return addOn;
    }

    List<EntryNode> allEntries() {
        return List.copyOf(entries.values());
    }

    @Nullable
    EntryNode entry(ResourceLocation id) {
        return entries.get(id);
    }

    List<EntryNode> entriesOf(ResourceLocation categoryId) {
        return entriesByCategory.getOrDefault(categoryId, List.of());
    }

    @Nullable
    CategoryRef category(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        for (CategoryRef category : builtIn) {
            if (category.id().equals(id)) {
                return category;
            }
        }
        for (CategoryRef category : addOn) {
            if (category.id().equals(id)) {
                return category;
            }
        }
        return null;
    }

    boolean shown(CategoryRef category) {
        PlayerKnowledge knowledge = knowledge();
        return category.data().requiredResearch().map(knowledge::isComplete).orElse(true);
    }

    List<CategoryRef> shownBuiltIn() {
        return builtIn.stream().filter(this::shown).toList();
    }

    List<CategoryRef> shownAddOn() {
        return addOn.stream().filter(this::shown).toList();
    }

    void clearVisibilityCache() {
        visibilityCache.clear();
    }

    boolean canUnlock(EntryNode node) {
        PlayerKnowledge knowledge = knowledge();
        if (knowledge.isKnown(node.id())) {
            return true;
        }
        for (ResearchParent parent : node.value().parents()) {
            if (!parent.isSatisfiedBy(knowledge)) {
                return false;
            }
        }
        return true;
    }

    boolean visible(EntryNode node) {
        Boolean cached = visibilityCache.get(node.id());
        if (cached != null) {
            return cached;
        }
        visibilityCache.put(node.id(), Boolean.TRUE);
        boolean result = evaluateVisible(node);
        visibilityCache.put(node.id(), result);
        return result;
    }

    private boolean evaluateVisible(EntryNode node) {
        if (knowledge().isKnown(node.id())) {
            return true;
        }
        ResearchEntry entry = node.value();
        if (entry.hasMeta("hidden") && (entry.parents().isEmpty() || !canUnlock(node))) {
            return false;
        }
        for (ResearchParent parent : entry.parents()) {
            EntryNode parentNode = entries.get(parent.id());
            if (parentNode != null && !visible(parentNode)) {
                return false;
            }
        }
        return true;
    }

    record CategoryRef(ResourceLocation id, ResearchCategory data) {}

    record EntryNode(ResourceLocation id, ResearchEntry value, ResourceLocation categoryId) {}
}
