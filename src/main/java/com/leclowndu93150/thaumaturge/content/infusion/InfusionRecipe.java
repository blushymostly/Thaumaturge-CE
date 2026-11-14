package com.leclowndu93150.thaumaturge.content.infusion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class InfusionRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient central;
    private final List<Ingredient> components;
    private final AspectList aspects;
    private final ItemStack result;
    private final int instability;
    private final String research;

    public InfusionRecipe(ResourceLocation id, Ingredient central, List<Ingredient> components, AspectList aspects, ItemStack result, int instability, String research) {
        this.id = id;
        this.central = central;
        this.components = List.copyOf(components);
        this.aspects = aspects;
        this.result = result;
        this.instability = instability;
        this.research = research;
    }

    public boolean isUnlocked(net.minecraft.world.entity.player.Player player) {
        return com.leclowndu93150.thaumaturge.api.recipe.ResearchGate.isOpen(
                com.leclowndu93150.thaumaturge.registry.TTAttachments.knowledge(player), research);
    }

    public Ingredient central() {
        return central;
    }

    public List<Ingredient> components() {
        return components;
    }

    public AspectList aspects() {
        return aspects;
    }

    public int instability() {
        return instability;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return central.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
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
        return TTRecipeTypes.INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TTRecipeTypes.INFUSION.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(central);
        list.addAll(components);
        return list;
    }

    public static class Serializer implements RecipeSerializer<InfusionRecipe> {
        @Override
        public InfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient central = Ingredient.fromJson(json.get("central"));
            List<Ingredient> components = new ArrayList<>();
            JsonArray array = GsonHelper.getAsJsonArray(json, "components");
            for (int i = 0; i < array.size(); i++) {
                components.add(Ingredient.fromJson(array.get(i)));
            }
            AspectList aspects = AspectList.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("aspects")).result().orElse(AspectList.EMPTY);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int instability = GsonHelper.getAsInt(json, "instability", 0);
            return new InfusionRecipe(id, central, components, aspects, result, instability, GsonHelper.getAsString(json, "research", null));
        }

        @Override
        public InfusionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient central = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            List<Ingredient> components = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                components.add(Ingredient.fromNetwork(buf));
            }
            AspectList aspects = AspectList.read(buf);
            ItemStack result = buf.readItem();
            int instability = buf.readVarInt();
            return new InfusionRecipe(id, central, components, aspects, result, instability, buf.readBoolean() ? buf.readUtf() : null);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusionRecipe recipe) {
            recipe.central.toNetwork(buf);
            buf.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) {
                component.toNetwork(buf);
            }
            recipe.aspects.write(buf);
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.instability);
            buf.writeBoolean(recipe.research != null);
            if (recipe.research != null) buf.writeUtf(recipe.research);
        }
    }
}
