package com.leclowndu93150.thaumaturge.client.screen.research.detail.insert;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AspectInsertRenderer {
    public static final int ROWS_PER_PAGE = 5;
    private static final int PAGE_LEFT_OFFSET = 60;
    private static final int PAGE_TOP_OFFSET = 24;
    private static final int ROW_STRIDE = 40;
    private static final int LEFT_PART_X = 60;
    private static final int RIGHT_PART_X = 102;
    private static final int PART_Y_OFFSET = 4;
    private static final int PAGER_LEFT_X = -20;
    private static final int PAGER_RIGHT_X = 144;
    private static final int PAGER_Y = 208;
    private static final int NAME_COLOR = 0xFF505050;

    private AspectInsertRenderer() {}

    public static List<Map.Entry<ResourceLocation, IAspect>> listedAspects() {
        return new ArrayList<>(TTAspects.all().entrySet());
    }

    public static int pageCount() {
        int size = listedAspects().size();
        return size == 0 ? 0 : Mth.ceil(size / (float) ROWS_PER_PAGE);
    }

    public static void render(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, int mouseX, int mouseY) {
        BookBlit.paper(graphics, frame.paperLeft(), frame.paperTop());
        List<Map.Entry<ResourceLocation, IAspect>> listed = listedAspects();
        if (listed.isEmpty()) {
            return;
        }
        int pageIndex = model.aspectsPageIndex();
        int first = Math.min(pageIndex * ROWS_PER_PAGE, listed.size());
        int end = Math.min(first + ROWS_PER_PAGE, listed.size());
        int originX = frame.paperLeft() + PAGE_LEFT_OFFSET;
        int originY = frame.paperTop() + PAGE_TOP_OFFSET;
        for (int row = 0; row < end - first; row++) {
            Map.Entry<ResourceLocation, IAspect> entry = listed.get(first + row);
            drawRow(graphics, frame, entry.getValue(), originX, originY + row * ROW_STRIDE);
        }
        int totalPages = pageCount();
        float swell = Mth.sin(frame.tick() * 0.1F) * 0.05F;
        if (pageIndex > 0) {
            BookBlit.swellingSprite(graphics, originX + PAGER_LEFT_X, originY + PAGER_Y, BookSprites.ARROW_LEFT_U, BookSprites.ARROW_V, BookSprites.ARROW_WIDTH, BookSprites.ARROW_HEIGHT, swell);
        }
        if (pageIndex < totalPages - 1) {
            BookBlit.swellingSprite(graphics, originX + PAGER_RIGHT_X, originY + PAGER_Y, BookSprites.ARROW_RIGHT_U, BookSprites.ARROW_V, BookSprites.ARROW_WIDTH, BookSprites.ARROW_HEIGHT, swell);
        }
    }

    private static void drawRow(GuiGraphics graphics, DetailFrame frame, IAspect aspect, int rowX, int rowY) {
        AspectTagRenderer.render(graphics, rowX + 2, rowY + 2, aspect);
        graphics.drawString(frame.font(), Component.literal(aspect.tag()), rowX + 22, rowY + 6, NAME_COLOR, false);
        List<ResourceLocation> parts = aspect.componentIds();
        if (parts.isEmpty()) {
            graphics.drawString(frame.font(), Component.literal("primal"), rowX + 54, rowY + 20, 0xFF777777, false);
            return;
        }
        IAspect left = TTAspects.get(parts.get(0));
        IAspect right = parts.size() > 1 ? TTAspects.get(parts.get(1)) : null;
        if (left != null) {
            AspectTagRenderer.render(graphics, rowX + LEFT_PART_X, rowY + PART_Y_OFFSET, left);
        }
        graphics.drawString(frame.font(), "+", rowX + 89, rowY + 12, 0xFF999999, false);
        if (right != null) {
            AspectTagRenderer.render(graphics, rowX + RIGHT_PART_X, rowY + PART_Y_OFFSET, right);
        }
    }
}
