package com.leclowndu93150.thaumaturge.content.workbench;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class ArcaneCraftingRecipe implements Recipe<CraftingContainer> {
    private final ResourceLocation id;
    private final String group;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int visCost;
    private final AspectList crystals;
    private final String research;

    public ArcaneCraftingRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, int visCost, AspectList crystals, String research) {
        this.id = id;
        this.group = group;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.visCost = visCost;
        this.crystals = crystals;
        this.research = research;
    }

    public int visCost() {
        return visCost;
    }

    public AspectList crystals() {
        return crystals;
    }

    public String research() {
        return research;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private static int totalVis(AspectList cost) {
        int total = 0;
        for (var entry : cost.entries()) {
            total += entry.amount();
        }
        return total;
    }

    public void consumeItems(CraftingContainer container) {
        for (int y = 0; y <= container.getHeight() - height; y++) {
            for (int x = 0; x <= container.getWidth() - width; x++) {
                if (matches(container, x, y, false)) {
                    consumeAt(container, x, y, false);
                    return;
                }
                if (matches(container, x, y, true)) {
                    consumeAt(container, x, y, true);
                    return;
                }
            }
        }
    }

    private void consumeAt(CraftingContainer container, int ox, int oy, boolean mirrored) {
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                Ingredient ingredient = mirrored ? ingredients.get(width - px - 1 + py * width) : ingredients.get(px + py * width);
                if (ingredient.isEmpty()) {
                    continue;
                }
                int cx = ox + px;
                int cy = oy + py;
                ItemStack stack = container.getItem(cx + cy * container.getWidth());
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                }
            }
        }
        container.setChanged();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        for (int y = 0; y <= container.getHeight() - height; y++) {
            for (int x = 0; x <= container.getWidth() - width; x++) {
                if (matches(container, x, y, true) || matches(container, x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(CraftingContainer container, int ox, int oy, boolean mirrored) {
        for (int y = 0; y < container.getHeight(); y++) {
            for (int x = 0; x < container.getWidth(); x++) {
                int px = x - ox;
                int py = y - oy;
                Ingredient ingredient = Ingredient.EMPTY;
                if (px >= 0 && py >= 0 && px < width && py < height) {
                    ingredient = mirrored ? ingredients.get(width - px - 1 + py * width) : ingredients.get(px + py * width);
                }
                if (!ingredient.test(container.getItem(x + y * container.getWidth()))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TTRecipeTypes.ARCANE_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TTRecipeTypes.ARCANE_CRAFTING.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public static class Serializer implements RecipeSerializer<ArcaneCraftingRecipe> {
        @Override
        public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            MapData map = readMap(GsonHelper.getAsJsonObject(json, "key"), GsonHelper.getAsJsonArray(json, "pattern"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int cost = 0;
            if (json.has("vis")) {
                var vis = json.get("vis");
                cost = vis.isJsonArray()
                        ? totalVis(AspectList.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, vis)
                                .getOrThrow(false, message -> { throw new com.google.gson.JsonSyntaxException(message); }))
                        : GsonHelper.getAsInt(json, "vis");
                if (cost < 0) {
                    throw new com.google.gson.JsonSyntaxException("Vis cost cannot be negative");
                }
            }
            AspectList crystals = AspectList.EMPTY;
            if (json.has("crystals")) {
                crystals = AspectList.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("crystals")).result().orElse(AspectList.EMPTY);
            }
            String research = json.has("research") ? GsonHelper.getAsString(json, "research") : null;
            return new ArcaneCraftingRecipe(id, group, map.width, map.height, map.ingredients, result, cost, crystals, research);
        }

        @Override
        public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int i = 0; i < ingredients.size(); i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }
            ItemStack result = buf.readItem();
            int cost = buf.readVarInt();
            AspectList crystals = AspectList.read(buf);
            String research = buf.readBoolean() ? buf.readUtf() : null;
            return new ArcaneCraftingRecipe(id, group, width, height, ingredients, result, cost, crystals, research);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneCraftingRecipe recipe) {
            buf.writeVarInt(recipe.width);
            buf.writeVarInt(recipe.height);
            buf.writeUtf(recipe.group);
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buf);
            }
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.visCost);
            recipe.crystals.write(buf);
            buf.writeBoolean(recipe.research != null);
            if (recipe.research != null) {
                buf.writeUtf(recipe.research);
            }
        }

        private static MapData readMap(com.google.gson.JsonObject key, com.google.gson.JsonArray pattern) {
            java.util.Map<Character, Ingredient> keys = new java.util.HashMap<>();
            for (var entry : key.entrySet()) {
                keys.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
            }
            keys.put(' ', Ingredient.EMPTY);
            String[] rows = new String[pattern.size()];
            for (int i = 0; i < pattern.size(); i++) {
                rows[i] = GsonHelper.convertToString(pattern.get(i), "pattern[" + i + "]");
            }
            int width = rows[0].length();
            int height = rows.length;
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    ingredients.set(x + y * width, keys.getOrDefault(rows[y].charAt(x), Ingredient.EMPTY));
                }
            }
            return new MapData(width, height, ingredients);
        }

        private record MapData(int width, int height, NonNullList<Ingredient> ingredients) {}
    }
}
