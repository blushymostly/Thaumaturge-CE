package com.leclowndu93150.thaumaturge.client.hud;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class WandHudOverlay {
    static final int HEIGHT = 60;
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/hud_wand.png");
    private static final int TEX = 256;
    private static final List<ResourceLocation> DIAL_ORDER = List.of(TTAspects.AER, TTAspects.IGNIS, TTAspects.AQUA,
            TTAspects.TERRA, TTAspects.ORDO, TTAspects.PERDITIO);
    private static final DecimalFormat AMOUNT = new DecimalFormat("0.#");
    private static final int[] previous = new int[DIAL_ORDER.size()];
    private static int previousSlot = Integer.MIN_VALUE;
    private static long snapshotTime;

    private WandHudOverlay() {}

    private static ItemStack held(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !minecraft.mouseHandler.isMouseGrabbed()) return ItemStack.EMPTY;
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        return main.getItem() instanceof WandItem ? main : off.getItem() instanceof WandItem ? off : ItemStack.EMPTY;
    }

    static boolean visible(Minecraft minecraft) {
        return !held(minecraft).isEmpty();
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        var wand = held(mc);
        if (wand.isEmpty()) {
            previousSlot = Integer.MIN_VALUE;
            return;
        }
        int slot = wand == mc.player.getMainHandItem() ? mc.player.getInventory().selected : -1;
        long now = Util.getMillis();
        boolean reset = slot != previousSlot;
        boolean snapshot = reset || now - snapshotTime >= 1000;
        int[] costs = WandItem.focusCosts(wand);
        int capacity = WandCharge.capacity(wand);
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, 0, 0, 32, 32, 0F, 0F, 64, 64, TEX, TEX);
        for (int i = 0; i < DIAL_ORDER.size(); i++) {
            var id = DIAL_ORDER.get(i);
            int stored = WandCharge.stored(wand, id);
            int cost = costs[WandCharge.PRIMALS.indexOf(id)];
            var aspect = TTAspects.get(id);
            int color = aspect == null ? 0xC0FFFF : aspect.color();
            float degrees = 75 + i * 24;
            float radians = degrees * Mth.DEG_TO_RAD;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(16 + 32 * Mth.sin(radians), 16 - 32 * Mth.cos(radians), 0);
            pose.mulPose(Axis.ZP.rotationDegrees(degrees));
            pose.scale(0.5F, 0.5F, 1);
            int fill = 30 * stored / capacity;
            if (fill > 0) {
                graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 0.8F);
                graphics.blit(TEXTURE, -4, 35 - fill, 104, 0, 8, fill, TEX, TEX);
                graphics.setColor(1, 1, 1, 1);
            }
            graphics.blit(TEXTURE, -8, -3, 72, 0, 16, 42, TEX, TEX);
            if (cost > 0) graphics.blit(TEXTURE, -4, -8, 136, 0, 8, 8, TEX, TEX);
            if (!reset && stored != previous[i]) {
                graphics.blit(TEXTURE, -4, cost > 0 ? -16 : -8, stored > previous[i] ? 120 : 128, 0, 8, 8, TEX, TEX);
            }
            if (mc.player.isShiftKeyDown()) {
                pose.mulPose(Axis.ZP.rotationDegrees(-90));
                graphics.drawString(mc.font, AMOUNT.format(stored / 100F), -32, -4, 0xFFFFFFFF);
                if (cost > 0) graphics.drawString(mc.font, AMOUNT.format(cost / 100F), 8, -4, 0xFFFFFFFF);
            }
            pose.popPose();
            if (snapshot) previous[i] = stored;
        }
        if (snapshot) {
            previousSlot = slot;
            snapshotTime = now;
        }
        var focus = WandItem.focus(wand);
        if (!focus.isEmpty()) graphics.renderItem(focus, 8, 8);
        RenderSystem.disableBlend();
    }
}
