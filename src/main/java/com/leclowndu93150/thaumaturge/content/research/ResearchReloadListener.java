package com.leclowndu93150.thaumaturge.content.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ResearchReloadListener extends SimplePreparableReloadListener<ResearchReloadListener.Prepared> {
    private static final String ENTRY_PREFIX = "thaumaturge/research_entry/";
    private static final String CATEGORY_PREFIX = "thaumaturge/research_category/";

    record Prepared(Map<ResourceLocation, ResearchEntry> entries, Map<ResourceLocation, ResearchCategory> categories) {}

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResearchEntry> entries = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> file : manager.listResources("thaumaturge/research_entry", path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation id = datapackId(file.getKey(), ENTRY_PREFIX);
            parseJson(file.getValue()).flatMap(json -> ResearchEntry.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(msg ->
                    Thaumaturge.LOGGER.warn("Failed to parse research entry {}: {}", id, msg))).ifPresent(entry -> entries.put(id, entry));
        }

        Map<ResourceLocation, ResearchCategory> categories = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> file : manager.listResources("thaumaturge/research_category", path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation id = datapackId(file.getKey(), CATEGORY_PREFIX);
            parseJson(file.getValue()).flatMap(json -> ResearchCategory.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(msg ->
                    Thaumaturge.LOGGER.warn("Failed to parse research category {}: {}", id, msg))).ifPresent(category -> categories.put(id, category));
        }
        return new Prepared(entries, categories);
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        ResearchData.replace(prepared.entries(), prepared.categories());
        Thaumaturge.LOGGER.info("Loaded {} research entries and {} categories", prepared.entries().size(), prepared.categories().size());
    }

    private static java.util.Optional<JsonElement> parseJson(Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return java.util.Optional.of(JsonParser.parseReader(reader));
        } catch (Exception ex) {
            Thaumaturge.LOGGER.warn("Failed reading research datapack resource", ex);
            return java.util.Optional.empty();
        }
    }

    private static ResourceLocation datapackId(ResourceLocation file, String folderPrefix) {
        String path = file.getPath();
        if (path.startsWith(folderPrefix) && path.endsWith(".json")) {
            String relative = path.substring(folderPrefix.length(), path.length() - ".json".length());
            return new ResourceLocation(file.getNamespace(), relative);
        }
        String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
        return TTIds.rl(name);
    }
}
