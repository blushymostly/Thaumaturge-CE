package com.leclowndu93150.thaumaturge.client.screen.research.detail.draw;

import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class BookBlit {
    private static final float SWELL_BASE = 1.0F;
    private static final float HALF = 2.0F;
    private static final int TEX = TTScreenTextures.TEX_SIZE;
    private static final int PAPER_SIZE = 256;
    private static final int ICON_TILE = 16;

    private BookBlit() {}

    public static void sprite(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TTScreenTextures.RESEARCH_BOOK, x, y, u, v, width, height, TEX, TEX);
    }

    public static void paper(GuiGraphics graphics, int x, int y) {
        graphics.blit(TTScreenTextures.PAPER, x, y, 0, 0, PAPER_SIZE, PAPER_SIZE, PAPER_SIZE, PAPER_SIZE);
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int alphaTint(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return a << 24 | 0xFFFFFF;
    }

    public static void iconTile(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        // Aspect PNGs use real alpha; Forge blit without blend paints transparent texels opaque black ("jpg cutout").
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a <= 0 ? 1.0F : a);
        graphics.blit(texture, x, y, ICON_TILE, ICON_TILE, 0.0F, 0.0F, BookSprites.ICON_TEXTURE_SIZE, BookSprites.ICON_TEXTURE_SIZE, BookSprites.ICON_TEXTURE_SIZE,
                BookSprites.ICON_TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void checkmark(GuiGraphics graphics, int x, int y) {
        sprite(graphics, x + BookSprites.CHECKMARK_OFFSET_X, y, BookSprites.CHECKMARK_U, BookSprites.CHECKMARK_V, BookSprites.CHECKMARK_SIZE, BookSprites.CHECKMARK_SIZE);
    }

    public static void tintedSprite(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a <= 0 ? 1.0F : a);
        sprite(graphics, x, y, u, v, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void swellingSprite(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float swell) {
        float centerX = x + width / HALF;
        float centerY = y + height / HALF;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(SWELL_BASE + swell, SWELL_BASE + swell, 1.0F);
        graphics.pose().translate(-width / HALF, -height / HALF, 0);
        sprite(graphics, 0, 0, u, v, width, height);
        graphics.pose().popPose();
    }
}
