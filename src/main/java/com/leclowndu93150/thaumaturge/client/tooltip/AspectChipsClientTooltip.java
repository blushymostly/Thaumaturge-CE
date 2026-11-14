package com.leclowndu93150.thaumaturge.client.tooltip;

import com.leclowndu93150.thaumaturge.api.aspect.AspectChipsTooltip;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class AspectChipsClientTooltip implements ClientTooltipComponent {
    private static final int CHIP_STRIDE = 18;
    private static final int ROW_HEIGHT = 18;

    private final AspectList aspects;

    public AspectChipsClientTooltip(AspectChipsTooltip carrier) {
        this.aspects = carrier.aspects();
    }

    @Override
    public int getHeight() {
        return aspects.isEmpty() ? 0 : ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        return aspects.size() * CHIP_STRIDE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int chipX = x;
        for (AspectInstance entry : aspects.sortedByAmount()) {
            IAspect aspect = TTAspects.get(entry.aspectId());
            if (aspect != null) {
                AspectTagRenderer.render(graphics, font, chipX, y, aspect, entry.amount());
            } else {
                AspectTagRenderer.renderUnknown(graphics, chipX, y, null);
            }
            chipX += CHIP_STRIDE;
        }
    }
}
