package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.compat.jei.drawables.AlphaDrawable;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.Comparator;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public final class ArcaneWorkbenchCategory implements IRecipeCategory<ArcaneCraftingRecipe> {
    public static final RecipeType<ArcaneCraftingRecipe> RECIPE_TYPE =
            RecipeType.create(TTIds.MODID, "arcane_workbench", ArcaneCraftingRecipe.class);

    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_researchbook_overlay.png");
    private static final int WIDTH = 162;
    private static final int HEIGHT = 138;
    private static final int GRID_ORIGIN_X = 41;
    private static final int GRID_ORIGIN_Y = 47;
    private static final int GRID_SPACING = 32;
    private static final int OUTPUT_X = 73;
    private static final int OUTPUT_Y = 7;
    private static final int CRYSTAL_X = 141;
    private static final int CRYSTAL_Y = 6;
    private static final int CRYSTAL_SPACING = 22;
    private static final int PLATE_X = 65;
    private static final int PLATE_Y = 0;
    private static final int ARROW_X = 12;
    private static final int ARROW_Y = 4;
    private static final int VIS_CENTER_X = 50;
    private static final int VIS_Y = 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable plate;
    private final IDrawable arrow;

    public ArcaneWorkbenchCategory(IGuiHelper guiHelper) {
        // CE: AlphaDrawable(TEXTURE, 225, 31, 102, 102, 36, 0, 30, 30) → size 162×138
        this.background = new AlphaDrawable(TEXTURE, 225, 31, 102, 102, 36, 0, 30, 30);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(TTItems.ARCANE_WORKBENCH.get()));
        this.plate = new AlphaDrawable(TEXTURE, 40, 6, 32, 32);
        this.arrow = new AlphaDrawable(TEXTURE, 135, 152, 23, 23);
    }

    @Override
    public RecipeType<ArcaneCraftingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumaturge.category.arcane_workbench");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ArcaneCraftingRecipe recipe, IFocusGroup focuses) {
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, GRID_ORIGIN_X + x * GRID_SPACING, GRID_ORIGIN_Y + y * GRID_SPACING);
                if (x < width && y < height) {
                    int index = x + y * width;
                    if (index < ingredients.size()) {
                        slot.addIngredients(ingredients.get(index));
                    }
                }
            }
        }
        List<AspectInstance> crystals = recipe.crystals().entries().stream()
                .sorted(Comparator.comparingInt(entry -> ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(entry.aspectId())))
                .toList();
        int crystalIndex = 0;
        for (int i = 0; i < 6; i++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, CRYSTAL_X, CRYSTAL_Y + i * CRYSTAL_SPACING);
            if (crystalIndex < crystals.size()
                    && crystals.get(crystalIndex).aspectId().equals(ArcaneWorkbenchMenu.PRIMAL_ORDER.get(i))) {
                AspectInstance entry = crystals.get(crystalIndex++);
                ItemStack crystal = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), Math.max(1, entry.amount()));
                TTItemData.setCrystalAspect(crystal, entry);
                slot.addItemStack(crystal);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getResultItem(Minecraft.getInstance().level != null
                        ? Minecraft.getInstance().level.registryAccess()
                        : net.minecraft.core.RegistryAccess.EMPTY));
    }

    @Override
    public void draw(ArcaneCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        // Background already drawn by JEI from getBackground(); add decorations only.
        plate.draw(graphics, PLATE_X, PLATE_Y);
        arrow.draw(graphics, ARROW_X, ARROW_Y);
        int vis = recipe.visCost();
        String text = Integer.toString(vis);
        graphics.drawString(Minecraft.getInstance().font, text, VIS_CENTER_X - Minecraft.getInstance().font.width(text) / 2, VIS_Y, 0x4040A0, false);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(ArcaneCraftingRecipe recipe) {
        return recipe.getId();
    }
}
