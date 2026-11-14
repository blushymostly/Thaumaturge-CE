package com.leclowndu93150.thaumaturge.client.screen.research.detail.knowledge;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.research.EntryIconRenderer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeType;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchData;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class KnowledgeGridRenderer {
    private static final int TYPE_ICON_SIZE = 16;
    private static final int BADGE_OFFSET = 4;
    private static final int BADGE_SIZE = 10;
    private static final float BADGE_ALPHA = 0.75F;
    private static final int GRID_LEFT_OFFSET = -10;
    private static final int GRID_TOP_OFFSET = -18;
    private static final int AMOUNT_X_OFFSET = 16;
    private static final int AMOUNT_Y_OFFSET = 8;
    private static final int CLOSING_DIVIDER_INSET = 4;
    private static final int CLOSING_DIVIDER_OFFSET = 12;
    private static final int INSERT_GRID_LEFT = 60;
    private static final int INSERT_GRID_TOP = 75;
    private static final int IN_PAGE_GRID_TOP = 210 - 16;

    private KnowledgeGridRenderer() {}

    public static ResourceLocation typeIcon(KnowledgeType type) {
        return TTIds.rl("textures/research/knowledge_" + type.name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static Component label(KnowledgeType type, ResourceLocation categoryId) {
        String typeName = type.name().substring(0, 1) + type.name().substring(1).toLowerCase(Locale.ROOT);
        return Component.literal(typeName + " - ").append(Component.translatable(categoryId.toLanguageKey("research_category")));
    }

    public static int activeTypeCount(@Nullable Player player) {
        if (player == null) {
            return 0;
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        List<ResourceLocation> categories = ResearchData.categories();
        int count = 0;
        for (KnowledgeType type : KnowledgeType.values()) {
            for (ResourceLocation categoryId : categories) {
                if (knowledge.knowledge(type, categoryId) > 0) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    public static boolean hasAnyKnowledge(@Nullable Player player) {
        return activeTypeCount(player) > 0;
    }

    public static void drawInPage(GuiGraphics graphics, DetailFrame frame, int mouseX, int mouseY) {
        draw(graphics, frame, frame.left(), frame.top() + IN_PAGE_GRID_TOP, mouseX, mouseY, KnowledgeGridLayout.IN_PAGE);
    }

    public static void drawInsert(GuiGraphics graphics, DetailFrame frame, int mouseX, int mouseY) {
        BookBlit.paper(graphics, frame.paperLeft(), frame.paperTop());
        draw(graphics, frame, frame.paperLeft() + INSERT_GRID_LEFT, frame.top() + INSERT_GRID_TOP, mouseX, mouseY, KnowledgeGridLayout.INSERT);
        if (!hasAnyKnowledge(frame.player())) {
            Component hint = Component.translatable("gui.thaumaturge.thaumonomicon.knowledge_none");
            graphics.drawString(frame.font(), hint, (frame.screenWidth() - frame.font().width(hint)) / 2, frame.top() + INSERT_GRID_TOP, BookSprites.INK_COLOR, false);
        }
    }

    private static void draw(GuiGraphics graphics, DetailFrame frame, int originX, int originY, int mouseX, int mouseY, KnowledgeGridLayout layout) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        List<ResourceLocation> categories = ResearchData.categories();
        if (categories.isEmpty()) {
            return;
        }
        int columnStride = layout.columnStride(categories.size());
        int firstRowY = originY + GRID_TOP_OFFSET;
        int rowIndex = 0;
        boolean anyDrawn = false;
        for (KnowledgeType type : KnowledgeType.values()) {
            int columnIndex = 0;
            boolean rowHasCells = false;
            for (ResourceLocation categoryId : categories) {
                int amount = knowledge.knowledge(type, categoryId);
                if (amount <= 0) {
                    continue;
                }
                anyDrawn = true;
                int cellX = originX + GRID_LEFT_OFFSET + columnStride * columnIndex;
                int cellY = firstRowY - rowIndex * layout.rowStride();
                drawCell(graphics, frame.font(), cellX, cellY, type, ResearchData.category(categoryId), amount);
                if (Rect.inside(cellX, cellY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE, mouseX, mouseY)) {
                    graphics.renderTooltip(frame.font(), label(type, categoryId), mouseX, mouseY);
                }
                columnIndex++;
                rowHasCells = true;
            }
            if (rowHasCells) {
                rowIndex++;
            }
        }
        if (layout == KnowledgeGridLayout.IN_PAGE && anyDrawn) {
            BookBlit.sprite(graphics, originX + CLOSING_DIVIDER_INSET, firstRowY - rowIndex * layout.rowStride() + CLOSING_DIVIDER_OFFSET, BookSprites.DIVIDER_U, BookSprites.DIVIDER_V,
                    BookSprites.DIVIDER_WIDTH, BookSprites.DIVIDER_TALL_HEIGHT);
        }
    }

    /** Draws one knowledge cell: type icon, category badge and a right-aligned amount. Public for requirement rows. */
    public static void drawCell(GuiGraphics graphics, Font font, int cellX, int cellY, KnowledgeType type, @Nullable ResearchCategory category, int amount) {
        graphics.blit(typeIcon(type), cellX, cellY, 0, 0, TYPE_ICON_SIZE, TYPE_ICON_SIZE, TYPE_ICON_SIZE, TYPE_ICON_SIZE);
        if (category != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BADGE_ALPHA);
            EntryIconRenderer.drawStaticTexture(graphics, category.icon(), cellX + BADGE_OFFSET, cellY + BADGE_OFFSET, BADGE_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        String amountText = Integer.toString(amount);
        graphics.drawString(font, amountText, cellX + AMOUNT_X_OFFSET - font.width(amountText), cellY + AMOUNT_Y_OFFSET, BookSprites.WHITE, true);
    }
}
