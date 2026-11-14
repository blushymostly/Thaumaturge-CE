package com.leclowndu93150.thaumaturge.client.screen.research.detail.page;

import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class NavigationPainter {
    private static final int STAGE_LABEL_CENTER_X = 52;
    private static final int STAGE_LABEL_Y_SHIFT = 2;

    private NavigationPainter() {}

    public static void paint(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, int mouseX, int mouseY) {
        Player player = frame.player();
        float swell = Mth.sin(frame.tick() * 0.1F) * 0.05F;
        int arrowRow = NavigationLayout.arrowRow(frame);
        if (model.leftPage() > 0) {
            drawArrow(graphics, NavigationLayout.pageLeftDrawX(frame), arrowRow, BookSprites.ARROW_LEFT_U, swell);
        }
        if (model.hasNextSpread()) {
            drawArrow(graphics, NavigationLayout.pageRightDrawX(frame), arrowRow, BookSprites.ARROW_RIGHT_U, swell);
        }
        paintBackButton(graphics, frame, swell, mouseX, mouseY);
        if (model.canNavigateStageHistory(player)) {
            paintStageHistory(graphics, frame, model, swell, mouseX, mouseY);
        }
    }

    private static void drawArrow(GuiGraphics graphics, int x, int y, int u, float swell) {
        BookBlit.swellingSprite(graphics, x, y, u, BookSprites.ARROW_V, BookSprites.ARROW_WIDTH, BookSprites.ARROW_HEIGHT, swell);
    }

    private static void paintBackButton(GuiGraphics graphics, DetailFrame frame, float swell, int mouseX, int mouseY) {
        Rect back = NavigationLayout.backButton(frame);
        BookBlit.swellingSprite(graphics, back.x(), back.y(), NavigationLayout.BACK_U, NavigationLayout.BACK_V, back.width(), back.height(), swell);
        if (back.contains(mouseX, mouseY)) {
            graphics.renderTooltip(frame.font(), Component.translatable("gui.thaumaturge.thaumonomicon.return_to_map"), mouseX, mouseY);
        }
    }

    private static void paintStageHistory(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, float swell, int mouseX, int mouseY) {
        Player player = frame.player();
        int displayedStage = model.displayedStage(player);
        int progressStage = model.progressStage(player);
        int drawRow = NavigationLayout.stageDrawRow(frame);
        if (displayedStage > 0) {
            drawArrow(graphics, NavigationLayout.stageLeftDrawX(frame), drawRow, BookSprites.ARROW_LEFT_U, swell);
        }
        if (displayedStage < progressStage) {
            drawArrow(graphics, NavigationLayout.stageRightDrawX(frame), drawRow, BookSprites.ARROW_RIGHT_U, swell);
        }
        Component label = Component.translatable("gui.thaumaturge.thaumonomicon.stage_history", displayedStage + 1, model.stages().size());
        graphics.drawString(frame.font(), label, frame.left() + STAGE_LABEL_CENTER_X - frame.font().width(label) / 2, drawRow + STAGE_LABEL_Y_SHIFT, BookSprites.INK_COLOR, false);
    }
}
