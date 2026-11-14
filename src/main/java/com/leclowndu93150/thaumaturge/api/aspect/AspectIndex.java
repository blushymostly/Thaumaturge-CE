package com.leclowndu93150.thaumaturge.api.aspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import java.util.Comparator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Item to {@link AspectList} lookup backed by {@code data/thaumaturge/data_maps/item/base_aspects.json}.
 *
 * <p>Forge 1.20.1 has no data maps, so the bundled file is read straight from the mod jar. Direct item
 * entries win; otherwise the first matching {@code #tag} entry (file order) is used. Results are cached
 * per item and must be dropped with {@link #invalidate()} whenever tags change.
 */
public final class AspectIndex {
    private static final String BASE_ASPECTS = "/data/thaumaturge/data_maps/item/base_aspects.json";

    private static final Map<Item, AspectList> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, AspectList> BY_ITEM = new HashMap<>();
    private static final List<Map.Entry<TagKey<Item>, AspectList>> BY_TAG = new ArrayList<>();
    private static boolean loaded;
    private static volatile Map<Item, AspectList> serverDerived = Map.of();
    private static volatile Map<Item, AspectList> clientDerived = Map.of();

    private AspectIndex() {}

    public static AspectList of(ItemStack stack) {
        if (stack.isEmpty()) {
            return AspectList.EMPTY;
        }
        AspectInstance crystal = TTItemData.getCrystalAspect(stack);
        if (crystal != null) {
            return AspectList.EMPTY.add(crystal);
        }
        ensureLoaded();
        AspectList base = CACHE.computeIfAbsent(stack.getItem(), AspectIndex::resolve);
        if (!base.isEmpty()) {
            return base;
        }
        var derived = EffectiveSide.get().isServer() ? serverDerived : clientDerived;
        return derived.getOrDefault(stack.getItem(), AspectList.EMPTY);
    }

    public static void invalidate() {
        CACHE.clear();
    }

    public static void rebuildDerived(RecipeManager recipes, RegistryAccess registries, LogicalSide side) {
        ensureLoaded();
        Map<Item, List<Candidate>> candidates = new HashMap<>();
        for (Recipe<?> recipe : recipes.getRecipes().stream().sorted(Comparator.comparing(Recipe::getId)).toList()) {
            if (!(recipe instanceof CraftingRecipe) && !(recipe instanceof ArcaneCraftingRecipe)) {
                continue;
            }
            try {
                ItemStack result = recipe.getResultItem(registries);
                if (result.isEmpty() || !resolve(result.getItem()).isEmpty()) {
                    continue;
                }
                List<Item> inputs = new ArrayList<>();
                for (var ingredient : recipe.getIngredients()) {
                    ItemStack[] options = ingredient.getItems();
                    if (options.length > 0) {
                        inputs.add(options[0].getItem());
                    }
                }
                if (!inputs.isEmpty()) {
                    int vis = recipe instanceof ArcaneCraftingRecipe arcane ? arcane.visCost() : 0;
                    candidates.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>())
                            .add(new Candidate(inputs, result.getCount(), vis));
                }
            } catch (RuntimeException error) {
                Thaumaturge.LOGGER.debug("Skipping aspect derivation for {}", recipe.getId(), error);
            }
        }

        Map<Item, AspectList> known = new HashMap<>();
        // Resolve one recipe depth at a time. Cyclic recipes without a known source never enter the index.
        while (!candidates.isEmpty()) {
            Map<Item, AspectList> next = new HashMap<>();
            candidates.forEach((item, options) -> options.stream().map(candidate -> candidate.derive(known))
                    .filter(aspects -> !aspects.isEmpty()).min(Comparator.comparingInt(AspectIndex::totalAmount))
                    .ifPresent(aspects -> next.put(item, aspects)));
            if (next.isEmpty()) {
                break;
            }
            next.keySet().forEach(candidates::remove);
            known.putAll(next);
        }
        if (side.isServer()) {
            serverDerived = Map.copyOf(known);
        } else {
            clientDerived = Map.copyOf(known);
        }
        Thaumaturge.LOGGER.info("Derived aspects for {} additional items on {}", known.size(), side);
    }

    private static int totalAmount(AspectList aspects) {
        return aspects.entries().stream().mapToInt(AspectInstance::amount).sum();
    }

    private record Candidate(List<Item> inputs, int outputCount, int visCost) {
        AspectList derive(Map<Item, AspectList> known) {
            Map<ResourceLocation, Integer> totals = new java.util.LinkedHashMap<>();
            for (Item item : inputs) {
                AspectList list = CACHE.computeIfAbsent(item, AspectIndex::resolve);
                if (list.isEmpty()) {
                    list = known.getOrDefault(item, AspectList.EMPTY);
                }
                for (AspectInstance aspect : list.entries()) {
                    totals.merge(aspect.aspectId(), aspect.amount(), Integer::sum);
                }
            }
            AspectList result = AspectList.EMPTY;
            for (var entry : totals.entrySet()) {
                double share = entry.getValue() * 0.75 / outputCount;
                if (share >= 0.75) {
                    result = result.add(new AspectInstance(entry.getKey(), Math.max(1, (int) share)));
                }
            }
            int magic = visCost > 0 ? (int) (Math.sqrt(1 + visCost / 2) / outputCount) : 0;
            return magic > 0 ? result.add(new AspectInstance(TTAspects.PRAECANTATIO, magic)) : result;
        }
    }

    private static AspectList resolve(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        AspectList direct = id == null ? null : BY_ITEM.get(id);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<TagKey<Item>, AspectList> entry : BY_TAG) {
            if (item.builtInRegistryHolder().is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return AspectList.EMPTY;
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream in = AspectIndex.class.getResourceAsStream(BASE_ASPECTS)) {
            if (in == null) {
                Thaumaturge.LOGGER.warn("Aspect base map {} not found", BASE_ASPECTS);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("values").entrySet()) {
                AspectList list = parseList(entry.getValue().getAsJsonArray());
                if (list.isEmpty()) {
                    continue;
                }
                String key = entry.getKey();
                if (key.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
                    if (tagId != null) {
                        tagId = com.leclowndu93150.thaumaturge.registry.TTCommonTags.remapItemTag(tagId);
                        BY_TAG.add(Map.entry(TagKey.create(Registries.ITEM, tagId), list));
                    }
                } else {
                    ResourceLocation itemId = ResourceLocation.tryParse(key);
                    if (itemId != null) {
                        BY_ITEM.put(itemId, list);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Thaumaturge.LOGGER.error("Failed to read aspect base map {}", BASE_ASPECTS, e);
        }
    }

    private static AspectList parseList(JsonArray array) {
        AspectList result = AspectList.EMPTY;
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            ResourceLocation aspect = ResourceLocation.tryParse(object.get("aspect").getAsString());
            int amount = object.has("amount") ? object.get("amount").getAsInt() : 1;
            if (aspect != null && amount > 0) {
                result = result.add(new AspectInstance(aspect, amount));
            }
        }
        return result;
    }
}
