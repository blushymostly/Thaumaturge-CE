package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.compat.jei.drawables.AlphaDrawable;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CrucibleCategory implements IRecipeCategory<CrucibleRecipe> {
    public static final RecipeType<CrucibleRecipe> RECIPE_TYPE = RecipeType.create(TTIds.MODID, "crucible", CrucibleRecipe.class);

    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_researchbook_overlay.png");
    private static final int BOWL_CENTER = 63;
    private static final int ASPECT_Y = 75;
    private static final int ASPECT_SPACING = 21;
    private static final int ASPECTS_PER_ROW = 4;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public CrucibleCategory(IGuiHelper guiHelper) {
        this.background = new AlphaDrawable(TEXTURE, 0, 5, 112, 132, 0, 0, 7, 9);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(TTItems.CRUCIBLE.get()));
        this.arrow = new AlphaDrawable(TEXTURE, 199, 168, 26, 26);
    }

    @Override
    public RecipeType<CrucibleRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.thaumaturge.recipe_type.crucible");
    }

    @Override
    public int getWidth() {
        return background.getWidth();
    }

    @Override
    public int getHeight() {
        return background.getHeight();
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 12).addIngredients(recipe.catalyst());
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : net.minecraft.core.RegistryAccess.EMPTY);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 54, 8).addItemStack(result);
    }

    @Override
    public void draw(CrucibleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 25, 16);
        var entries = recipe.aspects().sortedByAmount();
        var font = Minecraft.getInstance().font;
        for (int index = 0; index < entries.size(); index++) {
            AspectInstance entry = entries.get(index);
            IAspect aspect = TTAspects.get(entry.aspectId());
            if (aspect != null) {
                int column = index % ASPECTS_PER_ROW;
                int row = index / ASPECTS_PER_ROW;
                int columns = Math.min(ASPECTS_PER_ROW, entries.size() - row * ASPECTS_PER_ROW);
                int left = BOWL_CENTER - 8 - (columns - 1) * ASPECT_SPACING / 2;
                AspectTagRenderer.render(graphics, font, left + column * ASPECT_SPACING,
                        ASPECT_Y + row * ASPECT_SPACING, aspect, entry.amount());
            }
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(CrucibleRecipe recipe) {
        return recipe.getId();
    }
}
