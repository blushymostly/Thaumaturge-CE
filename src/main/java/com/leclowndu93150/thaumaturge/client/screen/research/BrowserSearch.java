package com.leclowndu93150.thaumaturge.client.screen.research;

import com.leclowndu93150.thaumaturge.client.render.research.EntryIconRenderer;
import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.client.screen.research.BrowserModel.CategoryRef;
import com.leclowndu93150.thaumaturge.client.screen.research.BrowserModel.EntryNode;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchRequirement;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class BrowserSearch {
    private static final int TEX = TTScreenTextures.TEX_SIZE;
    private static final int FIRST_ROW_Y = 33;
    private static final int ROW_PITCH = 10;
    private static final int TEXT_X = 32;
    private static final int ICON_X = 22;
    private static final int ICON_SIZE = 8;
    private static final int HOVER_LEFT = 22;
    private static final int HOVER_RIGHT_OFFSET = 18;
    private static final int HOVER_HEIGHT = 8;
    private static final int RESERVED_LINES = 1;
    private static final int MORE_X = 22;
    private static final int MORE_Y_GAP = 2;
    private static final int MORE_COLOR = 0xFFAAAAAA;
    private static final int RECIPE_ICON_U = 224;
    private static final int RECIPE_ICON_V = 48;
    private static final int CATEGORY_ICON_TINT = 0xCCA8A8A8;

    private final BrowserModel model;
    private final List<Result> results = new ArrayList<>();

    BrowserSearch(BrowserModel model) {
        this.model = model;
    }

    void clear() {
        results.clear();
    }

    Result get(int index) {
        return results.get(index);
    }

    void rebuild(String query) {
        results.clear();
        PlayerKnowledge knowledge = model.knowledge();
        String needle = query.toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return;
        }
        for (CategoryRef category : model.shownBuiltIn()) {
            addCategory(category, needle);
        }
        for (CategoryRef category : model.shownAddOn()) {
            addCategory(category, needle);
        }
        for (EntryNode node : model.allEntries()) {
            if (!knowledge.isKnown(node.id())) {
                continue;
            }
            add(Kind.ENTRY, Component.translatable(node.value().nameKey()).getString(), node.id(), needle);
            // CE RECIPE_STAGE_LOOKAHEAD: search current and following craft requirements.
            List<ResearchStage> stages = node.value().stages();
            int current = Math.max(0, knowledge.stage(node.id()));
            for (int index = current; index < stages.size(); index++) {
                for (ResearchRequirement requirement : stages.get(index).craft()) {
                    for (int i = 0; i < Math.min(8, requirement.candidateIds().size()); i++) {
                        ItemStack lead = requirement.stackAt(i);
                        if (!lead.isEmpty()) {
                            add(Kind.RECIPE, lead.getHoverName().getString(), node.id(), needle);
                        }
                    }
                }
            }
        }
        results.sort(Comparator.comparing(Result::text));
    }

    int rowAt(double mx, double my, int viewWidth, int viewHeight) {
        if (mx <= HOVER_LEFT || mx >= viewWidth + HOVER_RIGHT_OFFSET || my < FIRST_ROW_Y) {
            return -1;
        }
        int row = (int) ((my - FIRST_ROW_Y) / ROW_PITCH);
        boolean inside = my < FIRST_ROW_Y + ROW_PITCH * row + HOVER_HEIGHT;
        return inside && row < visibleRows(viewHeight) && row < results.size() ? row : -1;
    }

    void draw(GuiGraphics graphics, Font font, int mouseX, int mouseY, int viewWidth, int viewHeight, long tick) {
        int rows = visibleRows(viewHeight);
        int hoveredRow = rowAt(mouseX, mouseY, viewWidth, viewHeight);
        for (int i = 0; i < results.size() && i < rows; i++) {
            Result result = results.get(i);
            int y = FIRST_ROW_Y + ROW_PITCH * i;
            graphics.drawString(font, result.text(), TEXT_X, y, i == hoveredRow ? result.kind().hoverColor() : result.kind().color(), false);
            drawIcon(graphics, result, y, tick);
        }
        if (results.size() > rows) {
            graphics.drawString(font, Component.translatable("gui.thaumaturge.thaumonomicon.search_more"), MORE_X, FIRST_ROW_Y + ROW_PITCH * rows + MORE_Y_GAP, MORE_COLOR, false);
        }
    }

    private static int visibleRows(int viewHeight) {
        return Math.max(0, (viewHeight - FIRST_ROW_Y) / ROW_PITCH - RESERVED_LINES);
    }

    private void addCategory(CategoryRef category, String needle) {
        String label = Component.translatable("research.thaumaturge.category." + category.id().getPath()).getString();
        add(Kind.CATEGORY, label, category.id(), needle);
    }

    private void add(Kind kind, String text, ResourceLocation id, String needle) {
        if (text.toLowerCase(Locale.ROOT).contains(needle)) {
            results.add(new Result(kind, text, id));
        }
    }

    private void drawIcon(GuiGraphics graphics, Result result, int y, long tick) {
        switch (result.kind()) {
            case RECIPE -> graphics.blit(TTScreenTextures.RESEARCH_BROWSER, ICON_X, y, RECIPE_ICON_U, RECIPE_ICON_V, ICON_SIZE, ICON_SIZE, TEX, TEX);
            case ENTRY -> {
                EntryNode node = model.entry(result.id());
                if (node != null) {
                    float scale = (float) ICON_SIZE / EntryIconRenderer.ICON_REFERENCE_SIZE;
                    graphics.pose().pushPose();
                    graphics.pose().translate(ICON_X, y, 0);
                    graphics.pose().scale(scale, scale, 1.0F);
                    EntryIconRenderer.drawResearchIcon(graphics, 0, 0, EntryIconRenderer.resolveIcon(node.value(), tick), false);
                    graphics.pose().popPose();
                }
            }
            case CATEGORY -> {
                CategoryRef category = model.category(result.id());
                if (category != null) {
                    EntryIconRenderer.drawStaticTexture(graphics, category.data().icon(), ICON_X, y, ICON_SIZE);
                }
            }
        }
    }

    enum Kind {
        CATEGORY(0xFFDDAAAA, 0xFFFFCCCC),
        ENTRY(0xFFDDDDDD, 0xFFFFFFFF),
        RECIPE(0xFFAAAADD, 0xFFCCCCFF);

        private final int color;
        private final int hoverColor;

        Kind(int color, int hoverColor) {
            this.color = color;
            this.hoverColor = hoverColor;
        }

        int color() {
            return color;
        }

        int hoverColor() {
            return hoverColor;
        }
    }

    record Result(Kind kind, String text, ResourceLocation id) {}
}
