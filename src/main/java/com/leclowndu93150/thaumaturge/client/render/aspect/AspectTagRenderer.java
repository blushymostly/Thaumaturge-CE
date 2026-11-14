package com.leclowndu93150.thaumaturge.client.render.aspect;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Draws a 16x16 aspect tag (tinted glyph, optional amount label) with {@link GuiGraphics}. */
public final class AspectTagRenderer {
    public static final int TAG_SIZE = 16;
    public static final int TEXTURE_SIZE = 32;

    private static final int MONOCHROME_RGB = 0x1A1A1A;
    private static final float MONOCHROME_ALPHA_FACTOR = 0.8F;
    private static final float SMALL_TEXT_SCALE = 0.5F;
    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int AMOUNT_X = 8;
    private static final int AMOUNT_Y = 10;
    private static final int UNKNOWN_GREY = 0xFF888888;
    private static final ResourceLocation UNKNOWN_TEXTURE = TTIds.rl("textures/aspects/_unknown.png");

    private AspectTagRenderer() {}

    public static ResourceLocation textureOf(IAspect aspect) {
        return TTIds.rl("textures/aspects/" + aspect.tag() + ".png");
    }

    public static int colorOf(IAspect aspect, float alpha, boolean monochrome) {
        int rgb = monochrome ? MONOCHROME_RGB : aspect.color() & 0xFFFFFF;
        int a = Mth.clamp(Math.round((monochrome ? alpha * MONOCHROME_ALPHA_FACTOR : alpha) * 255.0F), 0, 255);
        return a << 24 | rgb;
    }

    public static void render(GuiGraphics graphics, int x, int y, IAspect aspect) {
        render(graphics, x, y, aspect, 1.0F, false);
    }

    public static void render(GuiGraphics graphics, int x, int y, IAspect aspect, float alpha, boolean monochrome) {
        BookBlit.iconTile(graphics, textureOf(aspect), x, y, colorOf(aspect, alpha, monochrome));
    }

    public static void render(GuiGraphics graphics, Font font, int x, int y, IAspect aspect, float amount) {
        render(graphics, font, x, y, aspect, amount, 1.0F, false);
    }

    public static void render(GuiGraphics graphics, Font font, int x, int y, IAspect aspect, float amount, float alpha, boolean monochrome) {
        render(graphics, x, y, aspect, alpha, monochrome);
        if (amount > 0.0F) {
            // Flush the tinted blit so the amount string cannot be covered by the same GUI batch.
            graphics.flush();
            drawAmount(graphics, font, x, y, amount);
        }
    }

    public static void renderUnknown(GuiGraphics graphics, int x, int y, @Nullable IAspect aspect) {
        int color = aspect == null ? UNKNOWN_GREY : BookBlit.opaque(aspect.color());
        BookBlit.iconTile(graphics, UNKNOWN_TEXTURE, x, y, color);
    }

    private static void drawAmount(GuiGraphics graphics, Font font, int x, int y, float amount) {
        String label = amount == (int) amount ? Integer.toString((int) amount) : String.format(Locale.ROOT, "%.1f", amount);
        graphics.pose().pushPose();
        // Above the aspect blit so GUI/chip amounts are not covered by the glyph.
        graphics.pose().translate(x + AMOUNT_X, y + AMOUNT_Y, 200.0F);
        graphics.pose().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
        graphics.drawString(font, label, 1, 1, OUTLINE_COLOR, false);
        graphics.drawString(font, label, 0, 0, TEXT_COLOR, false);
        graphics.pose().popPose();
    }
}
