package com.leclowndu93150.thaumaturge.client.render.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerSimpleRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Thaumonomicon recipe page laid out like CE {@code GridRecipePanel} / {@code PanelContext}:
 * 2× scaled overlay sprites + unscaled item icons on a 32px grid.
 * Overlay PNG is 512² but UV space is logical 256 (CE {@code TEX_SIZE}) so hi-res texels map correctly.
 */
public final class ResearchRecipePanel {
    public static final int PANEL = 104;

    private static final ResourceLocation OVERLAY = TTIds.rl("textures/gui/gui_researchbook_overlay.png");
    /** Logical atlas size — must stay 256; physical PNG is 512² (2×). */
    private static final int ATLAS = 256;
    private static final int PANEL_SCALE = 2;
    private static final int CENTER = PANEL / 2;

    private static final int GRID = 3;
    private static final int SLOT = 16;
    private static final int SLOT_ORIGIN = -40;
    private static final int SLOT_STRIDE = 32;
    private static final int OUTPUT_X = -8;
    private static final int OUTPUT_Y = -84;
    private static final int FRAME_U = 20;
    private static final int FRAME_V = 3;
    private static final int FRAME_X = -8;
    private static final int FRAME_Y = -46;
    private static final int BG_V = 15;
    private static final int BG_H = 52;
    private static final int BG_OFFSET = -26;
    private static final int WORKBENCH_U = 60;
    private static final int WORKBENCH_W = 51;
    private static final int ARCANE_U = 112;
    private static final int ARCANE_W = 52;
    private static final int LABEL_Y = -104;
    private static final int VIS_Y = 90;
    private static final int CRYSTAL_Y = 59;
    private static final int CRYSTAL_BASE_X = 4;
    private static final int CRYSTAL_SPACING = 10;
    private static final int CRYSTAL_STRIDE = 20;
    private static final int TEXT_COLOR = 0xFF504E50;

    private ResearchRecipePanel() {}

    public static int width() {
        return PANEL;
    }

    public static int height() {
        return PANEL;
    }

    public static void render(GuiGraphics graphics, Font font, Level level, int left, int top, Recipe<?> recipe, long gameTime, float rotation, float tilt, int layer) {
        int cx = left + CENTER;
        int cy = top + CENTER;
        if (recipe instanceof ArcaneCraftingRecipe arcane) {
            renderArcane(graphics, font, level, cx, cy, arcane, gameTime);
            return;
        }
        if (recipe instanceof CrucibleRecipe crucible) {
            renderCrucible(graphics, font, level, cx, cy, crucible, gameTime);
            return;
        }
        if (recipe instanceof InfusionRecipe infusion) {
            drawLabel(graphics, font, cx, cy, "jei.thaumaturge.category.infusion");
            drawOutputFrame(graphics, cx, cy);
            drawOutput(graphics, level, cx, cy, recipe);
            for (Hit hit : infusionHits(cx, cy, infusion, gameTime)) {
                graphics.renderItem(hit.stack(), hit.x(), hit.y());
            }
            int index = 0;
            for (AspectInstance entry : infusion.aspects().entries()) {
                IAspect aspect = TTAspects.get(entry.aspectId());
                if (aspect != null) {
                    AspectTagRenderer.render(graphics, font, cx - 43 + 18 * (index % 5), cy + 66 + 18 * (index / 5), aspect, entry.amount());
                }
                index++;
            }
            return;
        }
        if (recipe instanceof DustTrigger dust) {
            renderDust(graphics, font, level, cx, cy, dust, rotation, tilt, layer);
            return;
        }
        renderCrafting(graphics, font, level, cx, cy, recipe, gameTime);
    }

    public static @Nullable ItemStack hover(Level level, int left, int top, Recipe<?> recipe, double mouseX, double mouseY, long gameTime) {
        int cx = left + CENTER;
        int cy = top + CENTER;
        for (Hit hit : hits(level, cx, cy, recipe, gameTime)) {
            if (hit.contains(mouseX, mouseY)) {
                return hit.stack();
            }
        }
        return null;
    }

    private static void renderCrafting(GuiGraphics graphics, Font font, Level level, int cx, int cy, Recipe<?> recipe, long gameTime) {
        blitBg(graphics, cx, cy, WORKBENCH_U, WORKBENCH_W);
        drawLabel(graphics, font, cx, cy, "gui.thaumaturge.recipe_type.workbench");
        drawOutputFrame(graphics, cx, cy);
        drawGrid(graphics, cx, cy, ingredientsOf(recipe), widthOf(recipe), heightOf(recipe), gameTime);
        drawOutput(graphics, level, cx, cy, recipe);
    }

    private static void renderArcane(GuiGraphics graphics, Font font, Level level, int cx, int cy, ArcaneCraftingRecipe recipe, long gameTime) {
        blitBg(graphics, cx, cy, ARCANE_U, ARCANE_W);
        drawLabel(graphics, font, cx, cy, "gui.thaumaturge.recipe_type.arcane");
        drawOutputFrame(graphics, cx, cy);
        drawGrid(graphics, cx, cy, recipe.getIngredients(), recipe.getWidth(), recipe.getHeight(), gameTime);
        drawOutput(graphics, level, cx, cy, recipe);
        String cost = Integer.toString(recipe.visCost());
        graphics.drawString(font, cost, cx - font.width(cost) / 2, cy + VIS_Y, TEXT_COLOR, false);
        List<AspectInstance> crystals = recipe.crystals().entries();
        for (int i = 0; i < crystals.size(); i++) {
            ItemStack crystal = crystalStack(crystals.get(i));
            if (!crystal.isEmpty()) {
                int x = cx + CRYSTAL_BASE_X - CRYSTAL_SPACING * crystals.size() + CRYSTAL_STRIDE * i;
                graphics.renderItem(crystal, x, cy + CRYSTAL_Y);
            }
        }
    }

    private static void renderCrucible(GuiGraphics graphics, Font font, Level level, int cx, int cy, CrucibleRecipe recipe, long gameTime) {
        blitBg(graphics, cx, cy, WORKBENCH_U, WORKBENCH_W);
        drawLabel(graphics, font, cx, cy, "gui.thaumaturge.recipe_type.crucible");
        drawOutputFrame(graphics, cx, cy);
        ItemStack catalyst = pick(recipe.catalyst(), 0, gameTime);
        if (!catalyst.isEmpty()) {
            graphics.renderItem(catalyst, cx + SLOT_ORIGIN + SLOT_STRIDE, cy + SLOT_ORIGIN + SLOT_STRIDE);
        }
        drawOutput(graphics, level, cx, cy, recipe);
        int ax = cx - 40;
        int ay = cy + 60;
        for (AspectInstance entry : recipe.aspects().entries()) {
            IAspect aspect = TTAspects.get(entry.aspectId());
            if (aspect != null) {
                AspectTagRenderer.render(graphics, font, ax, ay, aspect, entry.amount());
                ax += 18;
            }
        }
    }

    private static void renderDust(GuiGraphics graphics, Font font, Level level, int cx, int cy, DustTrigger recipe, float rotation, float tilt, int layer) {
        drawLabel(graphics, font, cx, cy, recipe instanceof DustStructureRecipe
                ? "gui.thaumaturge.recipe_type.construct" : "gui.thaumaturge.recipe_type.dust");
        if (recipe instanceof DustStructureRecipe structure) {
            drawOutputFrame(graphics, cx, cy);
            drawOutput(graphics, level, cx, cy, recipe);
            StructurePreview.render(graphics, cx, cy, structure, rotation, tilt, layer);
            for (Hit hit : structureHits(cx, cy, structure)) {
                graphics.renderItem(hit.stack(), hit.x(), hit.y());
                graphics.renderItemDecorations(font, hit.stack(), hit.x(), hit.y());
            }
            StructurePreview.drawControls(graphics, font, cx, cy, layer, StructurePreview.layers(structure));
            return;
        }
        blitBg(graphics, cx, cy, WORKBENCH_U, WORKBENCH_W);
        drawOutputFrame(graphics, cx, cy);
        ItemStack target = dustTarget(recipe);
        if (!target.isEmpty()) {
            graphics.renderItem(target, cx + SLOT_ORIGIN + SLOT_STRIDE, cy + SLOT_ORIGIN + SLOT_STRIDE);
        }
        graphics.renderItem(new ItemStack(TTItems.SALIS_MUNDUS.get()), cx + SLOT_ORIGIN, cy + SLOT_ORIGIN + SLOT_STRIDE * 2);
        drawOutput(graphics, level, cx, cy, recipe);
    }

    private static void blitBg(GuiGraphics graphics, int cx, int cy, int u, int w) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(PANEL_SCALE, PANEL_SCALE, 1.0F);
        graphics.blit(OVERLAY, BG_OFFSET, BG_OFFSET, (float) u, (float) BG_V, w, BG_H, ATLAS, ATLAS);
        graphics.pose().popPose();
    }

    private static void drawOutputFrame(GuiGraphics graphics, int cx, int cy) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(PANEL_SCALE, PANEL_SCALE, 1.0F);
        graphics.blit(OVERLAY, FRAME_X, FRAME_Y, (float) FRAME_U, (float) FRAME_V, SLOT, SLOT, ATLAS, ATLAS);
        graphics.pose().popPose();
    }

    private static void drawGrid(GuiGraphics graphics, int cx, int cy, NonNullList<Ingredient> ingredients, int width, int height, long gameTime) {
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                if (row >= height || col >= width) {
                    continue;
                }
                int index = col + row * width;
                if (index >= ingredients.size()) {
                    continue;
                }
                ItemStack stack = pick(ingredients.get(index), index, gameTime);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, cx + SLOT_ORIGIN + col * SLOT_STRIDE, cy + SLOT_ORIGIN + row * SLOT_STRIDE);
                }
            }
        }
    }

    private static void drawOutput(GuiGraphics graphics, Level level, int cx, int cy, Recipe<?> recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!result.isEmpty()) {
            int x = cx + OUTPUT_X;
            int y = cy + OUTPUT_Y;
            graphics.renderItem(result, x, y);
            graphics.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, result, x, y);
        }
    }

    private static void drawLabel(GuiGraphics graphics, Font font, int cx, int cy, String key) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, cx - font.width(label) / 2, cy + LABEL_Y, TEXT_COLOR, false);
    }

    private static List<Hit> hits(Level level, int cx, int cy, Recipe<?> recipe, long gameTime) {
        List<Hit> list = new ArrayList<>();
        if (recipe instanceof ArcaneCraftingRecipe arcane) {
            collectGrid(list, cx, cy, arcane.getIngredients(), arcane.getWidth(), arcane.getHeight(), gameTime);
        } else if (recipe instanceof CrucibleRecipe crucible) {
            ItemStack catalyst = pick(crucible.catalyst(), 0, gameTime);
            if (!catalyst.isEmpty()) {
                list.add(new Hit(catalyst, cx + SLOT_ORIGIN + SLOT_STRIDE, cy + SLOT_ORIGIN + SLOT_STRIDE));
            }
        } else if (recipe instanceof InfusionRecipe infusion) {
            list.addAll(infusionHits(cx, cy, infusion, gameTime));
        } else if (recipe instanceof DustStructureRecipe structure) {
            list.addAll(structureHits(cx, cy, structure));
        } else if (recipe instanceof DustTrigger dust) {
            ItemStack target = dustTarget(dust);
            if (!target.isEmpty()) {
                list.add(new Hit(target, cx + SLOT_ORIGIN + SLOT_STRIDE, cy + SLOT_ORIGIN + SLOT_STRIDE));
            }
            list.add(new Hit(new ItemStack(TTItems.SALIS_MUNDUS.get()), cx + SLOT_ORIGIN, cy + SLOT_ORIGIN + SLOT_STRIDE * 2));
        } else {
            collectGrid(list, cx, cy, ingredientsOf(recipe), widthOf(recipe), heightOf(recipe), gameTime);
        }
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!result.isEmpty()) {
            list.add(new Hit(result, cx + OUTPUT_X, cy + OUTPUT_Y));
        }
        return list;
    }

    private static List<Hit> structureHits(int cx, int cy, DustStructureRecipe recipe) {
        var counts = new java.util.LinkedHashMap<net.minecraft.world.item.Item, Integer>();
        for (var cell : recipe.cells()) {
            var item = cell.source() == net.minecraft.world.level.block.Blocks.LAVA
                    ? net.minecraft.world.item.Items.LAVA_BUCKET : cell.source().asItem();
            if (item != net.minecraft.world.item.Items.AIR) counts.merge(item, 1, Integer::sum);
        }
        List<Hit> hits = new ArrayList<>();
        counts.entrySet().stream().sorted(java.util.Map.Entry.<net.minecraft.world.item.Item, Integer>comparingByValue().reversed())
                .forEach(entry -> hits.add(new Hit(new ItemStack(entry.getKey(), entry.getValue()), cx - 85 + 17 * hits.size(), cy + 90)));
        return hits;
    }

    private static List<Hit> infusionHits(int cx, int cy, InfusionRecipe recipe, long gameTime) {
        List<Hit> hits = new ArrayList<>();
        hits.add(new Hit(pick(recipe.central(), 0, gameTime), cx - 8, cy - 8));
        for (int i = 0; i < recipe.components().size(); i++) {
            double angle = i * 2 * Math.PI / recipe.components().size() - Math.PI / 2;
            hits.add(new Hit(pick(recipe.components().get(i), i, gameTime), cx - 8 + (int) (Math.cos(angle) * 40),
                    cy - 8 + (int) (Math.sin(angle) * 40)));
        }
        return hits;
    }

    private static void collectGrid(List<Hit> list, int cx, int cy, NonNullList<Ingredient> ingredients, int width, int height, long gameTime) {
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                if (row >= height || col >= width) {
                    continue;
                }
                int index = col + row * width;
                if (index >= ingredients.size()) {
                    continue;
                }
                ItemStack stack = pick(ingredients.get(index), index, gameTime);
                if (!stack.isEmpty()) {
                    list.add(new Hit(stack, cx + SLOT_ORIGIN + col * SLOT_STRIDE, cy + SLOT_ORIGIN + row * SLOT_STRIDE));
                }
            }
        }
    }

    private static NonNullList<Ingredient> ingredientsOf(Recipe<?> recipe) {
        return recipe.getIngredients();
    }

    private static int widthOf(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth();
        }
        if (recipe instanceof ArcaneCraftingRecipe arcane) {
            return arcane.getWidth();
        }
        return GRID;
    }

    private static int heightOf(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getHeight();
        }
        if (recipe instanceof ArcaneCraftingRecipe arcane) {
            return arcane.getHeight();
        }
        return GRID;
    }

    private static ItemStack pick(Ingredient ingredient, int salt, long gameTime) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        int index = (int) ((gameTime / 20 + salt) % stacks.length);
        return stacks[Math.floorMod(index, stacks.length)].copy();
    }

    private static ItemStack crystalStack(AspectInstance entry) {
        ItemStack stack = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), Math.max(1, entry.amount()));
        TTItemData.setCrystalAspect(stack, entry);
        return stack;
    }

    private static ItemStack dustTarget(DustTrigger recipe) {
        if (recipe instanceof DustTriggerSimpleRecipe simple) {
            Block block = simple.target();
            return new ItemStack(block.asItem());
        }
        return ItemStack.EMPTY;
    }

    private record Hit(ItemStack stack, int x, int y) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        }
    }
}
