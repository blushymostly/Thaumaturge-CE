package com.leclowndu93150.thaumaturge.client.hud;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.client.ClientAuraCache;
import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.content.aura.AuraData;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundRequestAuraChunkPacket;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

/** Left-side vis / flux gauge while holding a thaumometer or wearing goggles (CE AuraHudOverlay).
 * Overlay is registered once from {@link com.leclowndu93150.thaumaturge.client.ThaumaturgeClient}.
 */
public final class AuraHudOverlay {
    private static final ResourceLocation HUD = TTIds.rl("textures/gui/hud.png");
    private static final int TEX = TTScreenTextures.TEX_SIZE;
    private static final float AURA_FULL = 525.0F;
    private static final long REFRESH_TICKS = 20L;

    private static final int FRAME_X = 3;
    private static final int FRAME_Y = 1;
    private static final int FRAME_U = 72;
    private static final int FRAME_V = 48;
    private static final int FRAME_W = 16;
    private static final int FRAME_H = 80;

    private static final int TUBE_TOP = 10;
    private static final int TUBE_HEIGHT = 64;
    private static final int TUBE_BOTTOM = 74;
    private static final int COLUMN_X = 7;
    private static final int COLUMN_W = 8;
    private static final int FILL_U = 88;
    private static final int FILL_V = 56;
    private static final int VIS_RIPPLE_U = 96;
    private static final int FLUX_RIPPLE_U = 104;
    private static final float VIS_RIPPLE_BASE_V = 56.0F;
    private static final float FLUX_RIPPLE_BASE_V = 120.0F;
    private static final float FLUX_CLOCK_DIVISOR = 3.0F;
    private static final int VIS_FILL_TINT = 0xFFB266E5;
    private static final int VIS_RIPPLE_TINT = 0x80FFFFFF;
    private static final int FLUX_FILL_TINT = 0xFF3F194C;
    private static final int FLUX_RIPPLE_TINT = 0x80B266FF;

    private static final int POINTER_X = 4;
    private static final int POINTER_U = 117;
    private static final int POINTER_V = 61;
    private static final int POINTER_W = 14;
    private static final int POINTER_H = 5;
    private static final int POINTER_TOP = 8;

    private static final int TEXT_X = 18;
    private static final float TEXT_SCALE = 0.5F;
    private static final int FLUX_TEXT_RISE = 4;
    private static final int VIS_TEXT_COLOR = 0xFFEEAAFF;
    private static final int FLUX_TEXT_COLOR = 0xFFAA11BB;

    private static final NumberFormat AMOUNT_FORMAT = createAmountFormat();

    private static long requestedChunk = Long.MIN_VALUE;
    private static long requestedAt = Long.MIN_VALUE;
    private static AuraData latest;

    private AuraHudOverlay() {}

    private static boolean visible(LocalPlayer player) {
        return GogglesAccess.wearsRevealingGear(player)
                || player.getMainHandItem().is(TTItems.THAUMOMETER.get())
                || player.getOffhandItem().is(TTItems.THAUMOMETER.get());
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !visible(player)) {
            return;
        }
        ChunkPos chunk = player.chunkPosition();
        AuraData snapshot = ClientAuraCache.get(chunk);
        long now = player.level().getGameTime();
        long chunkKey = ChunkPos.asLong(chunk.x, chunk.z);
        if (snapshot == null && chunkKey != requestedChunk || now - requestedAt >= REFRESH_TICKS || now < requestedAt) {
            requestedChunk = chunkKey;
            requestedAt = now;
            TTNetwork.CHANNEL.sendToServer(new ServerboundRequestAuraChunkPacket(chunk));
        }
        if (snapshot != null) {
            latest = snapshot;
        } else {
            snapshot = latest;
        }
        if (snapshot == null) {
            return;
        }
        graphics.pose().pushPose();
        if (WandHudOverlay.visible(mc)) graphics.pose().translate(0, WandHudOverlay.HEIGHT, 0);
        float clockA = player.tickCount + partialTick;
        float clockB = player.tickCount / FLUX_CLOCK_DIVISOR + partialTick;
        float base = fraction(snapshot.getBase());
        float vis = fraction(snapshot.getVis());
        float flux = fraction(snapshot.getFlux());
        float combined = flux + vis;
        if (combined > 1.0F) {
            float squeeze = 1.0F / combined;
            base *= squeeze;
            vis *= squeeze;
            flux *= squeeze;
        }
        int visHeight = Math.round(vis * TUBE_HEIGHT);
        int visTop = TUBE_BOTTOM - visHeight;
        float fluxTopExact = TUBE_TOP + (1.0F - flux - vis) * TUBE_HEIGHT;
        int fluxTop = Math.round(fluxTopExact);
        int fluxHeight = Math.round(flux * TUBE_HEIGHT);
        if (vis > 0.0F && visHeight > 0) {
            drawColumn(graphics, visTop, visHeight, VIS_FILL_TINT, VIS_RIPPLE_U, VIS_RIPPLE_BASE_V + clockA % TUBE_HEIGHT, VIS_RIPPLE_TINT);
        }
        if (flux > 0.0F && fluxHeight > 0) {
            drawColumn(graphics, fluxTop, fluxHeight, FLUX_FILL_TINT, FLUX_RIPPLE_U, FLUX_RIPPLE_BASE_V - clockB % TUBE_HEIGHT, FLUX_RIPPLE_TINT);
        }
        graphics.blit(HUD, FRAME_X, FRAME_Y, FRAME_U, FRAME_V, FRAME_W, FRAME_H, TEX, TEX);
        if (player.isShiftKeyDown()) {
            if (vis > 0.0F) {
                drawAmount(graphics, mc, snapshot.getVis(), TUBE_TOP + (1.0F - vis) * TUBE_HEIGHT, VIS_TEXT_COLOR);
            }
            if (flux > 0.0F) {
                drawAmount(graphics, mc, snapshot.getFlux(), fluxTopExact - FLUX_TEXT_RISE, FLUX_TEXT_COLOR);
            }
        }
        int pointerTop = Math.round(POINTER_TOP + (1.0F - base) * TUBE_HEIGHT);
        graphics.blit(HUD, POINTER_X, pointerTop, POINTER_U, POINTER_V, POINTER_W, POINTER_H, TEX, TEX);
        graphics.pose().popPose();
    }

    private static void drawColumn(GuiGraphics graphics, int top, int height, int fillTint, int rippleU, float rippleV, int rippleTint) {
        tint(graphics, fillTint);
        graphics.blit(HUD, COLUMN_X, top, COLUMN_W, height, FILL_U, FILL_V, COLUMN_W, TUBE_HEIGHT, TEX, TEX);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        tint(graphics, rippleTint);
        graphics.blit(HUD, COLUMN_X, top, COLUMN_W, height, rippleU, rippleV, COLUMN_W, height, TEX, TEX);
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void tint(GuiGraphics graphics, int argb) {
        graphics.setColor((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, (argb >>> 24) / 255.0F);
    }

    private static void drawAmount(GuiGraphics graphics, Minecraft mc, float amount, float y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(TEXT_X, y, 0.0F);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(mc.font, AMOUNT_FORMAT.format(amount), 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static float fraction(float value) {
        return Mth.clamp(value / AURA_FULL, 0.0F, 1.0F);
    }

    private static NumberFormat createAmountFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setMaximumIntegerDigits(7);
        format.setMaximumFractionDigits(1);
        format.setGroupingUsed(false);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
