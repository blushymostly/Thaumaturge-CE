package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.compat.jei.drawables.AlphaDrawable;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerSimpleRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerTagRecipe;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class DustTriggerCategory implements IRecipeCategory<DustTrigger> {
    public static final RecipeType<DustTrigger> RECIPE_TYPE = RecipeType.create(TTIds.MODID, "dust_trigger", DustTrigger.class);

    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_researchbook_overlay.png");
    private static final int WIDTH = 144;
    private static final int HEIGHT = 54;
    private static final int DUST_SLOT_X = 6;
    private static final int DUST_SLOT_Y = 18;
    private static final int TARGET_SLOT_X = 56;
    private static final int TARGET_SLOT_Y = 18;
    private static final int RESULT_SLOT_X = 118;
    private static final int RESULT_SLOT_Y = 18;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable resultIcon;

    public DustTriggerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(TTItems.SALIS_MUNDUS.get()));
        this.resultIcon = new AlphaDrawable(TEXTURE, 41, 7, 30, 30);
    }

    @Override
    public RecipeType<DustTrigger> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumaturge.category.dust_trigger");
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
    public void setRecipe(IRecipeLayoutBuilder builder, DustTrigger recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, DUST_SLOT_X + 1, DUST_SLOT_Y + 1)
                .addItemStack(new ItemStack(TTItems.SALIS_MUNDUS.get()));
        if (recipe instanceof com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe structure) {
            structure.cells().stream().filter(cell -> cell.offset().equals(net.minecraft.core.BlockPos.ZERO)).findFirst()
                    .ifPresent(cell -> builder.addSlot(RecipeIngredientRole.INPUT, TARGET_SLOT_X + 1, TARGET_SLOT_Y + 1)
                            .addItemStack(new ItemStack(cell.source())));
        } else if (recipe instanceof DustTriggerSimpleRecipe simple) {
            builder.addSlot(RecipeIngredientRole.INPUT, TARGET_SLOT_X + 1, TARGET_SLOT_Y + 1)
                    .addItemStack(new ItemStack(simple.target().asItem()));
        } else if (recipe instanceof DustTriggerTagRecipe tag) {
            List<ItemStack> blocks = new ArrayList<>();
            BuiltInRegistries.BLOCK.getTagOrEmpty(tag.targetTag()).forEach(holder -> {
                ItemStack stack = new ItemStack(holder.value().asItem());
                if (!stack.isEmpty()) {
                    blocks.add(stack);
                }
            });
            if (!blocks.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, TARGET_SLOT_X + 1, TARGET_SLOT_Y + 1).addItemStacks(blocks);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_SLOT_X + 1, RESULT_SLOT_Y + 1)
                .addItemStack(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
    }

    @Override
    public void draw(DustTrigger recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        resultIcon.draw(graphics, RESULT_SLOT_X - 6, RESULT_SLOT_Y - 6);
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, "+", (((DUST_SLOT_X + 16) + TARGET_SLOT_X) / 2 - 5) / 2, 20 / 2, 0xFF404040, false);
        graphics.drawString(font, "=", (((TARGET_SLOT_X + 16) + RESULT_SLOT_X) / 2 - 5) / 2, 20 / 2, 0xFF404040, false);
        graphics.pose().popPose();
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(DustTrigger recipe) {
        return recipe.getId();
    }
}
