package com.leclowndu93150.thaumaturge.client.render.research;

import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchIcon;
import com.leclowndu93150.thaumaturge.content.research.ResearchRequirement;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EntryIconRenderer {
    public static final int ICON_REFERENCE_SIZE = 16;

    private static final int COMPLETE_COLOR = 0xFFFFFFFF;
    private static final int UNKNOWN_COLOR = 0xFF4D4D4D;
    private static final float LOCKED_TEXTURE_RGB = 0x33 / 255.0F;
    private static final long TEXTURE_FRAME_MILLIS = 150L;
    private static final long PULSE_PERIOD_MILLIS = 600L;
    private static final float PULSE_BASE = 0.75F;
    private static final float PULSE_AMPLITUDE = 0.25F;
    private static final int FRAME_SIZE = 32;
    private static final int FRAME_OFFSET = 8;
    private static final int FRAME_U_NORMAL = 80;
    private static final int FRAME_U_HEX = 112;
    private static final int FRAME_U_ROUND = 144;
    private static final int FRAME_U_SPIKY = 176;
    private static final int FRAME_V_VISIBLE = 48;
    private static final int FRAME_V_HIDDEN = 80;
    private static final int BADGE_SIZE = 32;
    private static final float BADGE_SCALE = 0.5F;
    private static final int BADGE_OFFSET = 9;
    private static final int BADGE_V = 16;
    private static final int BADGE_RESEARCH_U = 176;
    private static final int BADGE_PAGE_U = 208;
    private static final long ICON_CYCLE_TICKS = 20L;
    private static final int TEX = TTScreenTextures.TEX_SIZE;

    public enum Status {
        UNKNOWN, IN_PROGRESS, COMPLETE
    }

    /** Focus spell icons are stubbed until the focus registry is ported. */
    public record FocusIcon(ResourceLocation elementId) {}

    private EntryIconRenderer() {}

    public static int colorForStatus(Status status) {
        return switch (status) {
            case COMPLETE -> COMPLETE_COLOR;
            case UNKNOWN -> UNKNOWN_COLOR;
            case IN_PROGRESS -> pulseGrey();
        };
    }

    private static int pulseGrey() {
        long elapsed = System.currentTimeMillis() % PULSE_PERIOD_MILLIS;
        float phase = elapsed / (float) PULSE_PERIOD_MILLIS;
        float brightness = PULSE_AMPLITUDE * Mth.sin((float) (2.0 * Math.PI * phase)) + PULSE_BASE;
        int grey = Mth.clamp(Math.round(brightness * 255.0F), 0, 255);
        return 0xFF000000 | grey << 16 | grey << 8 | grey;
    }

    public static void render(GuiGraphics graphics, int x, int y, ResearchEntry entry, Status status, Object icon, boolean newResearch, boolean newPage) {
        int tint = colorForStatus(status);
        int frameV = entry.hasMeta("hidden") ? FRAME_V_HIDDEN : FRAME_V_VISIBLE;
        int frameU = entry.hasMeta("round") ? FRAME_U_ROUND : entry.hasMeta("hex") ? FRAME_U_HEX : FRAME_U_NORMAL;
        blitFrame(graphics, x - FRAME_OFFSET, y - FRAME_OFFSET, frameU, frameV, tint);
        if (entry.hasMeta("spiky")) {
            blitFrame(graphics, x - FRAME_OFFSET, y - FRAME_OFFSET, FRAME_U_SPIKY, frameV, tint);
        }
        drawResearchIcon(graphics, x, y, icon, status == Status.UNKNOWN);
        if (newResearch) {
            blitBadge(graphics, x - BADGE_OFFSET, y - BADGE_OFFSET, BADGE_RESEARCH_U);
        }
        if (newPage) {
            blitBadge(graphics, x - BADGE_OFFSET, y + BADGE_OFFSET, BADGE_PAGE_U);
        }
    }

    public static Object resolveIcon(ResearchEntry entry, long ticks) {
        long cycle = ticks / ICON_CYCLE_TICKS;
        List<String> icons = entry.icons();
        if (!icons.isEmpty()) {
            ResearchIcon icon = ResearchIcon.parse(icons.get((int) Math.floorMod(cycle, icons.size())));
            return switch (icon.kind()) {
                case FOCUS -> new FocusIcon(icon.id());
                case TEXTURE -> icon.id();
                case ITEM -> {
                    Item item = BuiltInRegistries.ITEM.get(icon.id());
                    yield item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
                }
            };
        }
        ResearchRequirement lead = leadingRequirement(entry);
        if (lead == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = lead.stackAt(cycle);
        return stack.isEmpty() || stack.getItem() == Items.AIR ? ItemStack.EMPTY : stack;
    }

    public static void drawResearchIcon(GuiGraphics graphics, int x, int y, Object icon, boolean locked) {
        if (icon instanceof FocusIcon) {
            // Focus registry not ported — leave empty rather than missing texture.
            return;
        }
        if (icon instanceof ResourceLocation texture) {
            drawTextureIcon(graphics, x, y, texture, locked);
            return;
        }
        if (icon instanceof ItemStack stack && !stack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            if (locked) {
                RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
            }
            graphics.renderItem(stack, 0, 0);
            if (locked) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            graphics.pose().popPose();
        }
    }

    private static void drawTextureIcon(GuiGraphics graphics, int x, int y, ResourceLocation texture, boolean locked) {
        TextureSize size = textureSize(texture);
        Region region = Region.animated(size, (int) (Util.getMillis() / TEXTURE_FRAME_MILLIS));
        float rgb = locked ? LOCKED_TEXTURE_RGB : 1.0F;
        RenderSystem.setShaderColor(rgb, rgb, rgb, 1.0F);
        blitRegion(graphics, texture, x, y, ICON_REFERENCE_SIZE, region);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws a whole icon texture (first frame when it is an animation strip) scaled to {@code size} pixels.
     * Used by category tabs, search rows and knowledge rows, whose icons are not necessarily 16x16.
     */
    public static void drawStaticTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        blitRegion(graphics, texture, x, y, size, Region.animated(textureSize(texture), 0));
    }

    private static void blitRegion(GuiGraphics graphics, ResourceLocation texture, int x, int y, int drawSize, Region region) {
        graphics.blit(texture, x, y, drawSize, drawSize, region.u, region.v, region.w, region.h, region.sheetW, region.sheetH);
    }

    private static final Map<ResourceLocation, TextureSize> TEXTURE_SIZES = new HashMap<>();

    private static TextureSize textureSize(ResourceLocation texture) {
        return TEXTURE_SIZES.computeIfAbsent(texture, EntryIconRenderer::readTextureSize);
    }

    private static TextureSize readTextureSize(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                    return new TextureSize(image.getWidth(), image.getHeight());
                }
            }
        } catch (Exception ignored) {
            // fall through to the default reference size
        }
        return new TextureSize(ICON_REFERENCE_SIZE, ICON_REFERENCE_SIZE);
    }

    private record TextureSize(int width, int height) {}

    private record Region(float u, float v, int w, int h, int sheetW, int sheetH) {
        static Region animated(TextureSize size, int frameIndex) {
            int shortSide = Math.min(size.width(), size.height());
            int longSide = Math.max(size.width(), size.height());
            boolean strip = longSide > shortSide && longSide % shortSide == 0;
            if (!strip) {
                return new Region(0.0F, 0.0F, size.width(), size.height(), size.width(), size.height());
            }
            float offset = (float) (frameIndex % (longSide / shortSide)) * shortSide;
            boolean vertical = size.height() > size.width();
            return new Region(vertical ? 0.0F : offset, vertical ? offset : 0.0F, shortSide, shortSide, size.width(), size.height());
        }
    }

    /** CE parity: the node icon falls back to the first obtain requirement, then the first craft requirement. */
    private static ResearchRequirement leadingRequirement(ResearchEntry entry) {
        for (ResearchStage stage : entry.stages()) {
            List<ResearchRequirement> source = stage.obtain().isEmpty() ? stage.craft() : stage.obtain();
            if (!source.isEmpty()) {
                return source.get(0);
            }
        }
        return null;
    }

    private static void blitFrame(GuiGraphics graphics, int x, int y, int u, int v, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, a <= 0 ? 1.0F : a);
        graphics.blit(TTScreenTextures.RESEARCH_BROWSER, x, y, u, v, FRAME_SIZE, FRAME_SIZE, TEX, TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void blitBadge(GuiGraphics graphics, int x, int y, int u) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, 1.0F);
        graphics.blit(TTScreenTextures.RESEARCH_BROWSER, 0, 0, u, BADGE_V, BADGE_SIZE, BADGE_SIZE, TEX, TEX);
        graphics.pose().popPose();
    }
}
