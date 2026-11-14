package com.leclowndu93150.thaumaturge.compat.jei.drawables;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * CE AlphaDrawable for {@code gui_researchbook_overlay.png}.
 * JEI UV rects are in physical 512-space (hi-res atlas), not logical 256.
 */
public final class AlphaDrawable implements IDrawable {
    private static final int ATLAS = 512;

    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int width;
    private final int height;
    private final int paddingTop;
    private final int paddingBottom;
    private final int paddingLeft;
    private final int paddingRight;

    public AlphaDrawable(ResourceLocation texture, int u, int v, int width, int height) {
        this(texture, u, v, width, height, 0, 0, 0, 0);
    }

    public AlphaDrawable(ResourceLocation texture, int u, int v, int width, int height, int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }

    @Override
    public int getWidth() {
        return width + paddingLeft + paddingRight;
    }

    @Override
    public int getHeight() {
        return height + paddingTop + paddingBottom;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        graphics.blit(texture, xOffset + paddingLeft, yOffset + paddingTop, (float) u, (float) v, width, height, ATLAS, ATLAS);
    }
}
