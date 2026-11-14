package com.leclowndu93150.thaumaturge.client.screen.research.detail.insert;

import com.leclowndu93150.thaumaturge.client.render.research.ResearchRecipePanel;
import com.leclowndu93150.thaumaturge.client.render.research.StructurePreview;
import com.leclowndu93150.thaumaturge.client.render.research.ResearchRecipeResolver;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import java.util.List;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RecipeInsertRenderer {
    private static final int PAPER_CENTER = BookSprites.PAPER_LAYOUT_SPAN / 2;

    private RecipeInsertRenderer() {}

    public static void render(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, int mouseX, int mouseY) {
        ResourceLocation recipeId = model.selectedRecipe();
        Level level = frame.level();
        if (recipeId == null || level == null) {
            return;
        }
        BookBlit.paper(graphics, frame.paperLeft(), frame.paperTop());
        List<Recipe<?>> variants = ResearchRecipeResolver.variants(level, recipeId);
        if (variants.isEmpty()) {
            graphics.drawCenteredString(frame.font(), recipeId.toString(), frame.paperLeft() + PAPER_CENTER, frame.paperTop() + PAPER_CENTER, 0xAA0000);
            return;
        }
        int variant = Mth.clamp(model.recipeVariant(), 0, variants.size() - 1);
        model.setRecipeVariant(variant);
        Recipe<?> recipe = variants.get(variant);
        int panelLeft = frame.paperLeft() + PAPER_CENTER - ResearchRecipePanel.width() / 2;
        int panelTop = frame.paperTop() + PAPER_CENTER - ResearchRecipePanel.height() / 2;
        long time = frame.gameTime();
        ResearchRecipePanel.render(graphics, frame.font(), level, panelLeft, panelTop, recipe, time, model.structureRotation(), model.structureTilt(), model.structureLayer());
        ItemStack hovered = ResearchRecipePanel.hover(level, panelLeft, panelTop, recipe, mouseX, mouseY, time);
        if (hovered != null && !hovered.isEmpty()) {
            graphics.renderTooltip(frame.font(), hovered, mouseX, mouseY);
        }
        if (variants.size() > 1) {
            graphics.drawCenteredString(frame.font(), (variant + 1) + " / " + variants.size(), frame.paperLeft() + PAPER_CENTER, frame.paperTop() + 232, 0x404040);
        }
    }

    public static @Nullable com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen.HoveredItem hoveredItem(
            DetailFrame frame, EntryDetailModel model, double mouseX, double mouseY) {
        ResourceLocation recipeId = model.selectedRecipe();
        Level level = frame.level();
        if (recipeId == null || level == null) {
            return null;
        }
        List<Recipe<?>> variants = ResearchRecipeResolver.variants(level, recipeId);
        if (variants.isEmpty()) {
            return null;
        }
        int variant = Mth.clamp(model.recipeVariant(), 0, variants.size() - 1);
        Recipe<?> recipe = variants.get(variant);
        int panelLeft = frame.paperLeft() + PAPER_CENTER - ResearchRecipePanel.width() / 2;
        int panelTop = frame.paperTop() + PAPER_CENTER - ResearchRecipePanel.height() / 2;
        ItemStack hovered = ResearchRecipePanel.hover(level, panelLeft, panelTop, recipe, mouseX, mouseY, frame.gameTime());
        if (hovered == null || hovered.isEmpty()) {
            return null;
        }
        return new com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen.HoveredItem(hovered, (int) mouseX - 8, (int) mouseY - 8);
    }

    private static @Nullable DustStructureRecipe structure(DetailFrame frame, EntryDetailModel model) {
        if (model.insert() != EntryDetailModel.Insert.RECIPE || model.selectedRecipe() == null || frame.level() == null) return null;
        var variants = ResearchRecipeResolver.variants(frame.level(), model.selectedRecipe());
        int variant = model.recipeVariant();
        return variant < variants.size() && variants.get(variant) instanceof DustStructureRecipe structure ? structure : null;
    }

    public static boolean startPreviewDrag(DetailFrame frame, EntryDetailModel model, double x, double y) {
        if (structure(frame, model) == null || !previewArea(frame).contains(x, y)) return false;
        model.beginStructureRotation();
        return true;
    }

    public static boolean clickPreviewLayer(DetailFrame frame, EntryDetailModel model, double x, double y) {
        var structure = structure(frame, model);
        if (structure == null) return false;
        int count = StructurePreview.layers(structure);
        if (count <= 1) return false;
        var label = StructurePreview.layerLabel(model.structureLayer(), count);
        int cx = frame.paperLeft() + PAPER_CENTER, cy = frame.paperTop() + PAPER_CENTER;
        for (int step : new int[] {-1, 1}) {
            if (StructurePreview.layerButton(frame.font(), cx, cy, label, step).contains(x, y)) {
                model.changeStructureLayer(step, count);
                return true;
            }
        }
        return false;
    }

    public static boolean scrollPreview(DetailFrame frame, EntryDetailModel model, double x, double y, double delta) {
        var structure = structure(frame, model);
        if (structure == null || delta == 0 || !previewArea(frame).contains(x, y)) return false;
        model.changeStructureLayer(delta > 0 ? 1 : -1, StructurePreview.layers(structure));
        return true;
    }

    private static Rect previewArea(DetailFrame frame) {
        return StructurePreview.area(frame.paperLeft() + PAPER_CENTER, frame.paperTop() + PAPER_CENTER);
    }

    public static boolean clickPager(EntryDetailModel model, DetailFrame frame, double mouseX, double mouseY) {
        ResourceLocation recipeId = model.selectedRecipe();
        Level level = frame.level();
        if (recipeId == null || level == null) {
            return false;
        }
        List<Recipe<?>> variants = ResearchRecipeResolver.variants(level, recipeId);
        if (variants.size() <= 1) {
            return false;
        }
        int paperLeft = frame.paperLeft();
        int paperTop = frame.paperTop();
        if (mouseX < paperLeft + 60 && model.recipeVariant() > 0) {
            model.setRecipeVariant(model.recipeVariant() - 1);
            return true;
        }
        if (mouseX > paperLeft + 196 && model.recipeVariant() < variants.size() - 1) {
            model.setRecipeVariant(model.recipeVariant() + 1);
            return true;
        }
        return false;
    }
}
