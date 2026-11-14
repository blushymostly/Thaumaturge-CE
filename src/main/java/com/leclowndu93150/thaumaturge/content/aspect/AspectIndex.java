package com.leclowndu93150.thaumaturge.content.aspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Loads {@code data/&lt;ns&gt;/data_maps/item/base_aspects.json} (NeoForge data-map shape) and
 * resolves item/tag keys into aspect lists for tooltips.
 */
public final class AspectIndex extends SimplePreparableReloadListener<Map<String, AspectList>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<ResourceLocation, AspectList> byItem = Map.of();
    private static Map<TagKey<Item>, AspectList> byTag = Map.of();

    public static AspectList of(ItemStack stack) {
        if (stack.isEmpty()) {
            return AspectList.EMPTY;
        }
        Item item = stack.getItem();
        AspectList direct = byItem.get(BuiltInRegistries.ITEM.getKey(item));
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        AspectList fromTags = AspectList.EMPTY;
        for (Map.Entry<TagKey<Item>, AspectList> entry : byTag.entrySet()) {
            if (stack.is(entry.getKey())) {
                for (AspectInstance aspect : entry.getValue().entries()) {
                    fromTags = fromTags.add(aspect);
                }
            }
        }
        return fromTags;
    }

    @Override
    protected Map<String, AspectList> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, AspectList> raw = new HashMap<>();
        for (var loc : manager.listResources("data_maps/item", path -> path.getPath().endsWith("base_aspects.json")).keySet()) {
            manager.getResource(loc).ifPresent(resource -> {
                try (var reader = resource.openAsReader()) {
                    JsonObject root = GsonHelper.parse(reader);
                    if (!root.has("values")) {
                        return;
                    }
                    JsonObject values = GsonHelper.getAsJsonObject(root, "values");
                    for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                        AspectList list = parseList(entry.getValue());
                        if (!list.isEmpty()) {
                            raw.merge(entry.getKey(), list, (a, b) -> {
                                AspectList merged = a;
                                for (AspectInstance inst : b.entries()) {
                                    merged = merged.add(inst);
                                }
                                return merged;
                            });
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Failed to load {}", loc, ex);
                }
            });
        }
        return raw;
    }

    @Override
    protected void apply(Map<String, AspectList> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, AspectList> items = new HashMap<>();
        Map<TagKey<Item>, AspectList> tags = new HashMap<>();
        for (Map.Entry<String, AspectList> entry : prepared.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
                if (tagId != null) {
                    tags.put(TagKey.create(Registries.ITEM, tagId), entry.getValue());
                    // Also alias common (#c:) tags onto forge: when present in datapacks.
                    if ("c".equals(tagId.getNamespace())) {
                        ResourceLocation forgeId = ResourceLocation.tryParse("forge:" + tagId.getPath());
                        if (forgeId != null) {
                            tags.putIfAbsent(TagKey.create(Registries.ITEM, forgeId), entry.getValue());
                        }
                    }
                }
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(key);
                if (itemId != null) {
                    items.put(itemId, entry.getValue());
                }
            }
        }
        byItem = Collections.unmodifiableMap(items);
        byTag = Collections.unmodifiableMap(tags);
        LOGGER.info("Aspect index: {} item entries, {} tag entries", byItem.size(), byTag.size());
    }

    private static AspectList parseList(JsonElement element) {
        if (!element.isJsonArray()) {
            return AspectList.EMPTY;
        }
        JsonArray array = element.getAsJsonArray();
        List<AspectInstance> entries = new ArrayList<>(array.size());
        for (JsonElement child : array) {
            Optional<AspectInstance> parsed = AspectInstance.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, child).result();
            parsed.ifPresent(entries::add);
        }
        return AspectList.ofEntries(entries);
    }
}
