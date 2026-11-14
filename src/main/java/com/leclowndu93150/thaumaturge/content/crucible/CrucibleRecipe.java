package com.leclowndu93150.thaumaturge.content.crucible;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CrucibleRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;

    public CrucibleRecipe(ResourceLocation id, Ingredient catalyst, AspectList aspects, ItemStack result) {
        this(id, catalyst, aspects, result, null);
    }

    private CrucibleRecipe(ResourceLocation id, Ingredient catalyst, AspectList aspects, ItemStack result, @Nullable String research) {
        this.id = id;
        this.catalyst = catalyst;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
    }

    public AspectList aspects() {
        return aspects;
    }

    public Ingredient catalyst() {
        return catalyst;
    }

    public boolean researchAllows(@Nullable Player player) {
        return research == null || player != null && ResearchGate.isOpen(TTAttachments.knowledge(player), research);
    }

    public boolean matchesCatalyst(ItemStack stack) {
        return catalyst.test(stack);
    }

    public boolean matchesAspects(AspectList available) {
        if (aspects.isEmpty()) {
            return true;
        }
        for (AspectInstance need : aspects.entries()) {
            if (available.amountOf(need.aspectId()) < need.amount()) {
                return false;
            }
        }
        return true;
    }

    public AspectList removeMatching(AspectList available) {
        AspectList next = available;
        for (AspectInstance need : aspects.entries()) {
            next = next.remove(need.aspectId(), need.amount());
        }
        return next;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return catalyst.test(container.getItem(0));
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
        return TTRecipeTypes.CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TTRecipeTypes.CRUCIBLE.get();
    }

    public static class Serializer implements RecipeSerializer<CrucibleRecipe> {
        @Override
        public CrucibleRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient catalyst = Ingredient.fromJson(json.get("catalyst"));
            AspectList aspects = AspectList.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("aspects")).result().orElse(AspectList.EMPTY);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new CrucibleRecipe(id, catalyst, aspects, result, GsonHelper.getAsString(json, "research", null));
        }

        @Override
        public CrucibleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new CrucibleRecipe(id, Ingredient.fromNetwork(buf), AspectList.read(buf), buf.readItem(),
                    buf.readBoolean() ? buf.readUtf() : null);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CrucibleRecipe recipe) {
            recipe.catalyst.toNetwork(buf);
            recipe.aspects.write(buf);
            buf.writeItem(recipe.result);
            buf.writeBoolean(recipe.research != null);
            if (recipe.research != null) {
                buf.writeUtf(recipe.research);
            }
        }
    }
}
