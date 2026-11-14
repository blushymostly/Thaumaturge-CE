package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerSimpleRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerTagRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, TTIds.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TTIds.MODID);

    public static final RegistryObject<RecipeType<ArcaneCraftingRecipe>> ARCANE_CRAFTING = TYPES.register("arcane_crafting", () -> RecipeType.simple(TTIds.rl("arcane_crafting")));
    public static final RegistryObject<RecipeType<CrucibleRecipe>> CRUCIBLE = TYPES.register("crucible", () -> RecipeType.simple(TTIds.rl("crucible")));
    public static final RegistryObject<RecipeType<InfusionRecipe>> INFUSION = TYPES.register("infusion", () -> RecipeType.simple(TTIds.rl("infusion")));
    public static final RegistryObject<RecipeType<DustTrigger>> DUST_TRIGGER = TYPES.register("dust_trigger", () -> RecipeType.simple(TTIds.rl("dust_trigger")));

    public static final RegistryObject<RecipeSerializer<ArcaneCraftingRecipe>> ARCANE_CRAFTING_SERIALIZER = SERIALIZERS.register("arcane_crafting", ArcaneCraftingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER = SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<InfusionRecipe>> INFUSION_SERIALIZER = SERIALIZERS.register("infusion", InfusionRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<DustTriggerSimpleRecipe>> DUST_TRIGGER_SIMPLE_SERIALIZER =
            SERIALIZERS.register("dust_trigger_simple", DustTriggerSimpleRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<DustTriggerTagRecipe>> DUST_TRIGGER_TAG_SERIALIZER =
            SERIALIZERS.register("dust_trigger_tag", DustTriggerTagRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe>> DUST_STRUCTURE_SERIALIZER =
            SERIALIZERS.register("dust_structure", com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe.Serializer::new);

    private TTRecipeTypes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
