package com.leclowndu93150.thaumaturge.compat.jei;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.compat.jei.category.ArcaneWorkbenchCategory;
import com.leclowndu93150.thaumaturge.compat.jei.category.AspectCompositionCategory;
import com.leclowndu93150.thaumaturge.compat.jei.category.AspectFromStacksCategory;
import com.leclowndu93150.thaumaturge.compat.jei.category.CrucibleCategory;
import com.leclowndu93150.thaumaturge.compat.jei.category.DustTriggerCategory;
import com.leclowndu93150.thaumaturge.compat.jei.category.InfusionCategory;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTMenus;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

@JeiPlugin
public class ThaumaturgeJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return TTIds.rl("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        ThaumonomiconGuiHandler.bind(jeiRuntime.getIngredientManager());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helpers = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ArcaneWorkbenchCategory(helpers),
                new CrucibleCategory(helpers),
                new DustTriggerCategory(helpers),
                new InfusionCategory(helpers),
                new AspectCompositionCategory(helpers),
                new AspectFromStacksCategory(helpers));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        RecipeManager recipes = minecraft.level.getRecipeManager();
        registration.addRecipes(ArcaneWorkbenchCategory.RECIPE_TYPE, new ArrayList<>(recipes.getAllRecipesFor(TTRecipeTypes.ARCANE_CRAFTING.get())));
        registration.addRecipes(CrucibleCategory.RECIPE_TYPE, new ArrayList<>(recipes.getAllRecipesFor(TTRecipeTypes.CRUCIBLE.get())));
        List<DustTrigger> dust = new ArrayList<>(recipes.getAllRecipesFor(TTRecipeTypes.DUST_TRIGGER.get()));
        registration.addRecipes(DustTriggerCategory.RECIPE_TYPE, dust);
        registration.addRecipes(InfusionCategory.RECIPE_TYPE, new ArrayList<>(recipes.getAllRecipesFor(TTRecipeTypes.INFUSION.get())));
        registration.addRecipes(AspectCompositionCategory.RECIPE_TYPE, AspectCompositionCategory.all());
        registration.addRecipes(AspectFromStacksCategory.RECIPE_TYPE, AspectFromStacksCategory.all(registration.getIngredientManager()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(TTItems.ARCANE_WORKBENCH.get()), ArcaneWorkbenchCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.CRUCIBLE.get()), CrucibleCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.INFUSION_MATRIX.get()), InfusionCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.SALIS_MUNDUS.get()), DustTriggerCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.THAUMOMETER.get()), AspectCompositionCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.THAUMONOMICON.get()), AspectFromStacksCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TTItems.THAUMOMETER.get()), AspectFromStacksCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // craft grid starts at menu slot 1 (0=result); 9 craft + 6 crystal = 15; player inv follows after wand
        registration.addRecipeTransferHandler(ArcaneWorkbenchMenu.class, TTMenus.ARCANE_WORKBENCH.get(),
                ArcaneWorkbenchCategory.RECIPE_TYPE, 1, 15, 17, 36);
        registration.addRecipeTransferHandler(ArcaneWorkbenchMenu.class, TTMenus.ARCANE_WORKBENCH.get(),
                RecipeTypes.CRAFTING, 1, 9, 17, 36);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new ThaumonomiconGuiHandler());
    }
}
