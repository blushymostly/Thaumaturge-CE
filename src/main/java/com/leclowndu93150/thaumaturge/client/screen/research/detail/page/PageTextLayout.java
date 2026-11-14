package com.leclowndu93150.thaumaturge.client.screen.research.detail.page;

import com.leclowndu93150.thaumaturge.client.render.research.PageParser;
import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public final class PageTextLayout {
    private static final int PAGE_WIDTH = PageParser.PAGE_WIDTH;
    private static final int PAGE_LEFT_OFFSET = -13;
    private static final int PAGE_SIDE_OFFSET = 151;
    private static final int CONTENT_Y_OFFSET = -10;
    private static final int TITLE_Y_ADVANCE = 28;
    private static final int CONTINUATION_Y_SHIFT = 4;
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_BASELINE_SHIFT = 6;
    private static final int IMAGE_BASELINE_SHIFT = 5;
    private static final int IMAGE_GAP = 2;
    private static final int TITLE_COLOR = 0xFF202020;
    private static final int DIVIDER_INSET_X = 4;
    private static final int DIVIDER_ABOVE_OFFSET = -7;
    private static final int DIVIDER_BELOW_OFFSET = 10;
    private static final int LEFT_SIDE = 0;
    private static final int RIGHT_SIDE = 1;

    private PageTextLayout() {}

    public static List<PageParser.Page> compose(Font font, ResourceLocation entryId, ResearchStage stage, int knowledgeRows, int stageCount, int progressStage, boolean complete) {
        boolean hasResearch = !stage.requiredResearch().isEmpty();
        boolean hasObtain = !stage.obtain().isEmpty();
        boolean hasCraft = !stage.craft().isEmpty();
        boolean hasKnowledge = !stage.requiredKnowledge().isEmpty();
        boolean reserveHistoryGap = stageCount > 1 && progressStage > 0 && !hasResearch && !hasObtain && !hasCraft && !hasKnowledge;
        // CE always passes complete=false: requirement rows stay drawn on finished stages, so first-page
        // text must keep reserving that bottom band (otherwise "Completed"/icons overlap the prose).
        return PageParser.parse(font, entryId, stage.textKey(), List.of(), knowledgeRows, false,
                hasResearch, hasObtain, hasCraft, hasKnowledge, reserveHistoryGap);
    }

    public static int contentTop(DetailFrame frame) {
        return frame.top() + CONTENT_Y_OFFSET;
    }

    public static void drawSpread(GuiGraphics graphics, DetailFrame frame, Component title, List<PageParser.Page> pages, int leftIndex) {
        if (pages.isEmpty()) {
            return;
        }
        int top = contentTop(frame);
        if (leftIndex < pages.size()) {
            drawPage(graphics, frame, title, pages.get(leftIndex), LEFT_SIDE, top, leftIndex == 0);
        }
        if (leftIndex + 1 < pages.size()) {
            drawPage(graphics, frame, title, pages.get(leftIndex + 1), RIGHT_SIDE, top, false);
        }
    }

    private static void drawPage(GuiGraphics graphics, DetailFrame frame, Component title, PageParser.Page page, int side, int top, boolean withTitle) {
        int cursorY = top;
        if (withTitle) {
            drawDivider(graphics, frame.left() + DIVIDER_INSET_X, cursorY + DIVIDER_ABOVE_OFFSET);
            drawTitle(graphics, frame, title, cursorY);
            drawDivider(graphics, frame.left() + DIVIDER_INSET_X, cursorY + DIVIDER_BELOW_OFFSET);
            cursorY += TITLE_Y_ADVANCE;
        } else {
            cursorY += CONTINUATION_Y_SHIFT;
        }
        int columnX = frame.left() + PAGE_LEFT_OFFSET + side * PAGE_SIDE_OFFSET;
        for (PageParser.PageElement element : page.elements()) {
            if (element instanceof PageParser.PageElement.Text text) {
                cursorY += drawTextLine(graphics, frame.font(), text, page.paragraphGap(), columnX, cursorY);
            } else if (element instanceof PageParser.PageElement.Image image) {
                cursorY += drawImage(graphics, image.image(), columnX, cursorY);
            }
        }
    }

    private static int drawTextLine(GuiGraphics graphics, Font font, PageParser.PageElement.Text text, int paragraphGap, int columnX, int lineY) {
        FormattedCharSequence sequence = sink -> StringDecomposer.iterateFormatted(text.content(), text.style(), sink);
        graphics.drawString(font, sequence, columnX, lineY - TEXT_BASELINE_SHIFT, BookSprites.INK_COLOR, false);
        return LINE_HEIGHT + (text.paragraphBreak() ? paragraphGap : 0);
    }

    private static int drawImage(GuiGraphics graphics, PageParser.PageImage image, int columnX, int imageY) {
        int centering = (PAGE_WIDTH - image.renderedWidth()) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(columnX + centering, imageY - IMAGE_BASELINE_SHIFT, 0);
        graphics.pose().scale(image.scale, image.scale, 1.0F);
        graphics.blit(image.texture, 0, 0, image.u, image.v, image.w, image.h, TTScreenTextures.TEX_SIZE, TTScreenTextures.TEX_SIZE);
        graphics.pose().popPose();
        return image.renderedHeight() + IMAGE_GAP;
    }

    private static void drawTitle(GuiGraphics graphics, DetailFrame frame, Component title, int titleY) {
        Font font = frame.font();
        int titleWidth = font.width(title);
        int columnCenter = frame.left() + PAGE_LEFT_OFFSET + PAGE_WIDTH / 2;
        if (titleWidth <= PAGE_WIDTH) {
            graphics.drawString(font, title, columnCenter - titleWidth / 2, titleY, TITLE_COLOR, false);
            return;
        }
        float fitScale = (float) PAGE_WIDTH / titleWidth;
        graphics.pose().pushPose();
        graphics.pose().translate(frame.left() + PAGE_LEFT_OFFSET + PAGE_WIDTH / 2.0F - titleWidth / 2.0F * fitScale, titleY + fitScale, 0);
        graphics.pose().scale(fitScale, fitScale, 1.0F);
        graphics.drawString(font, title, 0, 0, TITLE_COLOR, false);
        graphics.pose().popPose();
    }

    private static void drawDivider(GuiGraphics graphics, int x, int y) {
        BookBlit.sprite(graphics, x, y, BookSprites.DIVIDER_U, BookSprites.DIVIDER_V, BookSprites.DIVIDER_WIDTH, BookSprites.DIVIDER_THIN_HEIGHT);
    }
}
