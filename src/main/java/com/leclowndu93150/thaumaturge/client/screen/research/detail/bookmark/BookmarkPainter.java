package com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark;

import com.leclowndu93150.thaumaturge.client.render.research.ResearchRecipeResolver;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BookmarkPainter {
    private static final int SIDE_BODY_U_ASPECTS = 76;
    private static final int SIDE_BODY_U_KNOWLEDGE = 44;
    private static final int SIDE_BODY_OFFSET = 0;
    private static final int SIDE_TIP_OFFSET = 20;
    private static final int SIDE_V = 232;
    private static final int SIDE_BODY_WIDTH = 24;
    private static final int SIDE_IDLE_INSET = 3;
    private static final int SIDE_TIP_U = 100;
    private static final int SIDE_TIP_WIDTH = 4;

    private static final int RECIPE_BODY_U = 120;
    private static final int RECIPE_V = 232;
    private static final int RECIPE_WIDTH = 28;
    private static final int RECIPE_HEIGHT = 16;
    private static final int RECIPE_TIP_U = 116;
    private static final int RECIPE_TIP_WIDTH = 4;
    private static final int RECIPE_ICON_OFFSET = 7;
    private static final int RECIPE_IDLE_LEAN = 3;

    private BookmarkPainter() {}

    public static void paint(GuiGraphics graphics, DetailFrame frame, List<BookmarkSlot> slots, Level level, int mouseX, int mouseY) {
        paint(graphics, frame, slots, level, BookmarkJitter.forEntry(net.minecraft.resources.ResourceLocation.tryParse("thaumaturge:none"), 0), mouseX, mouseY);
    }

    public static void paint(GuiGraphics graphics, DetailFrame frame, List<BookmarkSlot> slots, Level level, BookmarkJitter jitter, int mouseX, int mouseY) {
        int ordinal = 0;
        for (BookmarkSlot slot : slots) {
            switch (slot.kind()) {
                case ASPECTS -> paintSide(graphics, frame, slot, SIDE_BODY_U_ASPECTS, "gui.thaumaturge.thaumonomicon.aspects", jitter.shift(ordinal++), mouseX, mouseY);
                case KNOWLEDGE -> paintSide(graphics, frame, slot, SIDE_BODY_U_KNOWLEDGE, "gui.thaumaturge.thaumonomicon.knowledge", jitter.shift(ordinal++), mouseX, mouseY);
                case RECIPE -> paintRecipe(graphics, frame, slot, level, jitter.lean(ordinal++), mouseX, mouseY);
            }
        }
    }

    private static void paintSide(GuiGraphics graphics, DetailFrame frame, BookmarkSlot slot, int bodyU, String tooltipKey, int jitterShift, int mouseX, int mouseY) {
        var area = slot.hoverArea();
        boolean hovered = area.contains(mouseX, mouseY);
        int inset = hovered ? 0 : SIDE_IDLE_INSET;
        int y = area.y() + jitterShift;
        BookBlit.sprite(graphics, area.x() + SIDE_BODY_OFFSET + inset, y, bodyU, SIDE_V, SIDE_BODY_WIDTH - inset, BookmarkLayout.SIDE_HEIGHT);
        BookBlit.sprite(graphics, area.x() + SIDE_TIP_OFFSET, y, SIDE_TIP_U, SIDE_V, SIDE_TIP_WIDTH, BookmarkLayout.SIDE_HEIGHT);
        if (hovered) {
            graphics.renderTooltip(frame.font(), Component.translatable(tooltipKey), mouseX, mouseY);
        }
    }

    private static void paintRecipe(GuiGraphics graphics, DetailFrame frame, BookmarkSlot slot, Level level, int jitterLean, int mouseX, int mouseY) {
        var area = slot.hoverArea();
        boolean hovered = area.contains(mouseX, mouseY);
        int lean = (hovered ? 0 : RECIPE_IDLE_LEAN) + jitterLean;
        BookBlit.sprite(graphics, area.x() + lean, area.y(), RECIPE_BODY_U, RECIPE_V, RECIPE_WIDTH - lean, RECIPE_HEIGHT);
        BookBlit.sprite(graphics, area.x() + lean, area.y(), RECIPE_TIP_U, RECIPE_V, RECIPE_TIP_WIDTH, RECIPE_HEIGHT);
        ItemStack stack = recipeIcon(level, slot.recipeId());
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, area.x() + RECIPE_ICON_OFFSET - lean, area.y());
        }
        if (hovered && slot.recipeId() != null) {
            if (!stack.isEmpty()) {
                graphics.renderTooltip(frame.font(), stack, mouseX, mouseY);
            } else {
                graphics.renderTooltip(frame.font(), Component.literal(slot.recipeId().toString()), mouseX, mouseY);
            }
        }
    }

    private static ItemStack recipeIcon(Level level, net.minecraft.resources.ResourceLocation recipeId) {
        return ResearchRecipeResolver.icon(level, recipeId);
    }
}
