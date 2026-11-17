package com.leclowndu93150.thaumaturge.client.screen;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell.Part;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

final class FocalTextures {
    private static final ResourceLocation CABINET = TTIds.rl("textures/gui/gui_wandtable3.png");
    private static final ResourceLocation STRIP = TTIds.rl("textures/gui/gui_wandtable.png");
    private static final ResourceLocation ROUND = TTIds.rl("textures/foci/_medium.png");
    private static final ResourceLocation DIAMOND = TTIds.rl("textures/foci/_effect.png");
    private static final Map<String, ResourceLocation> SPRITES = new HashMap<>();
    private static final Map<Part, ResourceLocation> GLYPHS = new java.util.EnumMap<>(Part.class);

    private FocalTextures() {}

    static void cabinet(GuiGraphics graphics) {
        graphics.blit(CABINET, 4, 2, 0, 0, 70, 237, 256, 256);
    }

    static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, "frame", x, y, width, height, 294, 19);
    }

    static void well(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, "well", x, y, width, height, 276, 10);
    }

    static void field(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, "field", x, y, width, height, 40, 4);
    }

    static void button(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean pressed) {
        String face = pressed ? "button_pressed" : hovered ? "button_hover" : "button_normal";
        panel(graphics, face, x, y, width, height, 12, 3);
    }

    static void glyph(GuiGraphics graphics, Part part, int x, int y, int size) {
        ResourceLocation location = GLYPHS.computeIfAbsent(part, kind -> TTIds.rl("textures/foci/" + kind.glyph() + ".png"));
        graphics.blit(location, x, y, size, size, 0, 0, 32, 32, 32, 32);
    }

    static void paletteGlyph(GuiGraphics graphics, Part part, int x, int y) {
        partIcon(graphics, part, x, y, 20, 13, 20);
    }

    static void node(GuiGraphics graphics, Part part, int x, int y) {
        partIcon(graphics, part, x, y, 29, 16, 32);
    }

    private static void partIcon(GuiGraphics graphics, Part part, int x, int y, int plate, int icon, int flow) {
        if (part.category().equals("flow")) {
            glyph(graphics, part, x - flow / 2, y - flow / 2, flow);
            return;
        }
        int color = switch (part) {
            case TOUCH -> 0xAD8585;
            case BOLT -> 0xAD9885;
            case FIRE -> 0xFF5A01;
            case FROST -> 0xE1FFFF;
            default -> 0xFFFFFF;
        };
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        graphics.blit(part.isEffect() ? DIAMOND : ROUND, x - plate / 2, y - plate / 2, plate, plate, 0, 0, 32, 32, 32, 32);
        graphics.setColor(1, 1, 1, 1);
        glyph(graphics, part, x - icon / 2, y - icon / 2, icon);
    }

    static void strip(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(STRIP, x, y, u, v, width, height, 256, 256);
    }

    static void sprite(GuiGraphics graphics, String name, int x, int y, int width, int height) {
        sprite(graphics, name, x, y, width, height, width, height);
    }

    static void sprite(GuiGraphics graphics, String name, int x, int y, int width, int height, int sourceW, int sourceH) {
        graphics.blit(location(name), x, y, width, height, 0, 0, sourceW, sourceH, sourceW, sourceH);
    }

    private static ResourceLocation location(String name) {
        return SPRITES.computeIfAbsent(name, item -> TTIds.rl("textures/gui/sprites/focal/" + item + ".png"));
    }

    private static void panel(GuiGraphics graphics, String name, int x, int y, int width, int height, int size, int border) {
        if (width <= 0 || height <= 0)
            return;
        int bx = Math.min(border, width / 2);
        int by = Math.min(border, height / 2);
        int[] widths = {bx, width - bx * 2, bx};
        int[] heights = {by, height - by * 2, by};
        int[] sourceWidths = {bx, size - bx * 2, bx};
        int[] sourceHeights = {by, size - by * 2, by};
        var texture = location(name);
        int targetY = y;
        int sourceY = 0;
        for (int row = 0; row < 3; row++) {
            int targetX = x;
            int sourceX = 0;
            for (int column = 0; column < 3; column++) {
                if (widths[column] > 0 && heights[row] > 0) {
                    graphics.blit(texture, targetX, targetY, widths[column], heights[row], sourceX, sourceY, sourceWidths[column], sourceHeights[row], size, size);
                }
                targetX += widths[column];
                sourceX += sourceWidths[column];
            }
            targetY += heights[row];
            sourceY += sourceHeights[row];
        }
    }
}
