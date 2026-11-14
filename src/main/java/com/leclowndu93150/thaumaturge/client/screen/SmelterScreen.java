package com.leclowndu93150.thaumaturge.client.screen;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_smelter.png");

    public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawRising(graphics, 80, 26, 176, 16, 20, menu.fuelHeight(20));
        drawRising(graphics, 106, 13, 216, 9, 46, menu.progressHeight(46));
        drawRising(graphics, 61, 12, 200, 8, 48, menu.stockHeight(48));
        graphics.blit(TEXTURE, leftPos + 60, topPos + 8, 232, 0, 10, 55);
    }

    private void drawRising(GuiGraphics graphics, int x, int y, int textureX, int width, int height, int visible) {
        if (visible > 0) {
            int offset = height - visible;
            graphics.blit(TEXTURE, leftPos + x, topPos + y + offset, textureX, offset, width, visible);
        }
    }
}
