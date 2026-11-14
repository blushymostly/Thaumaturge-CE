package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class InfusionCategory implements IRecipeCategory<InfusionRecipe> {
    public static final RecipeType<InfusionRecipe> RECIPE_TYPE = RecipeType.create(TTIds.MODID, "infusion", InfusionRecipe.class);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 126;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    public InfusionCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(WIDTH, HEIGHT);
        icon = gui.createDrawableItemStack(new ItemStack(TTItems.INFUSION_MATRIX.get()));
        slot = gui.getSlotDrawable();
    }

    @Override
    public RecipeType<InfusionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumaturge.category.infusion");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder layout, InfusionRecipe recipe, IFocusGroup focuses) {
        layout.addSlot(RecipeIngredientRole.INPUT, 55, 44).setBackground(slot, -1, -1).addIngredients(recipe.central());
        int count = recipe.components().size();
        for (int i = 0; i < count; i++) {
            double angle = i * 2 * Math.PI / count - Math.PI / 2;
            int x = 55 + (int) Math.round(40 * Math.cos(angle));
            int y = 44 + (int) Math.round(36 * Math.sin(angle));
            layout.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(slot, -1, -1).addIngredients(recipe.components().get(i));
        }
        var level = Minecraft.getInstance().level;
        layout.addSlot(RecipeIngredientRole.OUTPUT, 132, 44).setBackground(slot, -1, -1)
                .addItemStack(recipe.getResultItem(level == null ? RegistryAccess.EMPTY : level.registryAccess()));
    }

    @Override
    public void draw(InfusionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, Component.translatable("gui.thaumaturge.infusion.recipe_instability", recipe.instability()),
                WIDTH / 2, 93, 0xFF404040);
        int index = 0;
        for (var entry : recipe.aspects().sortedByAmount()) {
            var aspect = TTAspects.get(entry.aspectId());
            if (aspect != null) {
                int x = (WIDTH - recipe.aspects().size() * 19) / 2 + index * 19;
                AspectTagRenderer.render(graphics, font, x, 105, aspect, entry.amount());
            }
            index++;
        }
    }

    @Override
    public ResourceLocation getRegistryName(InfusionRecipe recipe) {
        return recipe.getId();
    }
}
