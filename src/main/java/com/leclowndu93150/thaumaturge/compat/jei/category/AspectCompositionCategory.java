package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Shows how a compound aspect is made from two components. */
public final class AspectCompositionCategory implements IRecipeCategory<AspectCompositionCategory.Composition> {
    public static final RecipeType<Composition> RECIPE_TYPE = RecipeType.create(TTIds.MODID, "aspect_composition", Composition.class);

    private static final int WIDTH = 100;
    private static final int HEIGHT = 40;

    private final IDrawable background;
    private final IDrawable icon;

    public AspectCompositionCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(TTItems.THAUMOMETER.get()));
    }

    public static List<Composition> all() {
        List<Composition> list = new ArrayList<>();
        TTAspects.all().forEach((id, aspect) -> {
            if (aspect.componentIds().size() == 2) {
                list.add(new Composition(id, aspect));
            }
        });
        return list;
    }

    @Override
    public RecipeType<Composition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumaturge.category.aspect_composition");
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
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Composition recipe, IFocusGroup focuses) {}

    @Override
    public void draw(Composition recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        IAspect left = TTAspects.get(recipe.aspect().componentIds().get(0));
        IAspect right = TTAspects.get(recipe.aspect().componentIds().get(1));
        if (left != null) {
            AspectTagRenderer.render(graphics, 16, 12, left);
        }
        if (right != null) {
            AspectTagRenderer.render(graphics, 42, 12, right);
        }
        AspectTagRenderer.render(graphics, 74, 12, recipe.aspect());
        graphics.drawString(font, "+", 34, 16, 0x404040, false);
        graphics.drawString(font, "=", 62, 16, 0x404040, false);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(Composition recipe) {
        return TTIds.rl("aspect_composition/" + recipe.id().getNamespace() + "/" + recipe.id().getPath());
    }

    public record Composition(ResourceLocation id, IAspect aspect) {}
}
