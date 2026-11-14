package com.leclowndu93150.thaumaturge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractTTScreen extends Screen {
    private final @Nullable ResourceLocation background;
    private final int backgroundTextureWidth;
    private final int backgroundTextureHeight;

    protected AbstractTTScreen(Component title) {
        this(title, null, 256, 256);
    }

    protected AbstractTTScreen(Component title, @Nullable ResourceLocation background) {
        this(title, background, 256, 256);
    }

    protected AbstractTTScreen(Component title, @Nullable ResourceLocation background, int backgroundTextureWidth, int backgroundTextureHeight) {
        super(title);
        this.background = background;
        this.backgroundTextureWidth = backgroundTextureWidth;
        this.backgroundTextureHeight = backgroundTextureHeight;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        if (background != null) {
            graphics.blit(background, 0, 0, 0, 0, width, height, backgroundTextureWidth, backgroundTextureHeight);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
