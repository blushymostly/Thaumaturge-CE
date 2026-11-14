package com.leclowndu93150.thaumaturge.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TTTooltipRenderer {
    /** Above GuiGraphics item blit depth (~150) so map icons do not cover hover text. */
    private static final float TOOLTIP_Z = 400.0F;

    private TTTooltipRenderer() {}

    public static void render(GuiGraphics graphics, Font font, List<TooltipLine> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        int height = 0;
        for (TooltipLine line : lines) {
            width = Math.max(width, (int) (font.width(line.text()) * line.scale().scale()));
            height += (int) (font.lineHeight * line.scale().scale()) + 1;
        }
        int x = mouseX + 12;
        int y = mouseY - 12;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, TOOLTIP_Z);
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 1, 0xF0100010);
        int cursor = y;
        for (TooltipLine line : lines) {
            float scale = line.scale().scale();
            graphics.pose().pushPose();
            graphics.pose().translate(x, cursor, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, line.text(), 0, 0, 0xFFFFFF, false);
            graphics.pose().popPose();
            cursor += (int) (font.lineHeight * scale) + 1;
        }
        graphics.pose().popPose();
    }

    public static void renderComponents(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        render(graphics, font, lines.stream().map(TooltipLine::new).toList(), mouseX, mouseY);
    }
}
