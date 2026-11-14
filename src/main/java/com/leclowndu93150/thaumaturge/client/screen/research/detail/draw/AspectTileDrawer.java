package com.leclowndu93150.thaumaturge.client.screen.research.detail.draw;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AspectTileDrawer {
    public static final ResourceLocation UNKNOWN_TILE = TTIds.rl("textures/aspects/_unknown.png");
    public static final ResourceLocation BACKDROP_TILE = TTIds.rl("textures/aspects/_back.png");

    private AspectTileDrawer() {}

    public static void known(GuiGraphics graphics, int x, int y, IAspect aspect) {
        AspectTagRenderer.render(graphics, x, y, aspect);
    }

    public static void unrevealed(GuiGraphics graphics, int x, int y) {
        BookBlit.iconTile(graphics, UNKNOWN_TILE, x, y, 0xFFCCCCCC);
    }
}
