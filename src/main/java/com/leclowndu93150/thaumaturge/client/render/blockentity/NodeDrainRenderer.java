package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.client.render.item.WandTipTracker;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class NodeDrainRenderer {
    private static final RenderType LINE = TTFlatRenderTypes.entityAdditiveWorld(TTIds.rl("textures/misc/wispy.png"));
    private static final float HALF_WIDTH = 0.15F;
    private static final Map<BlockEntityNode, Tint> COLORS = new WeakHashMap<>();

    private NodeDrainRenderer() {}

    static void render(BlockEntityNode node, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        var effect = node.drainEffect();
        var level = node.getLevel();
        if (effect == null || level == null) return;
        Player player = level.getPlayerByUUID(effect.player());
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof WandItem)) return;
        float useTime = player.getTicksUsingItem() + partialTick;
        Vec3 tip = WandTipTracker.tip(player, player.getUsedItemHand());
        if (tip == null) return;
        Vec3 target = tip.subtract(Vec3.atCenterOf(node.getBlockPos()));
        double span = target.length();
        if (span < 0.1) return;

        int segments = Math.max(2, (int) Math.round(span) * 2);
        int visible = (int) (segments * Math.min(useTime / 10.0F, 1));
        float clock = (Math.floorMod(level.getGameTime(), 1_048_576L) + partialTick) * (50F / 30F);
        Tint color = COLORS.computeIfAbsent(node, ignored -> new Tint());
        color.follow(effect.color());

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        VertexConsumer consumer = buffers.getBuffer(LINE);
        Sample from = sample(target, span, 0, clock);
        for (int i = 1; i <= visible; i++) {
            Sample to = sample(target, span, (double) i / segments, clock);
            ribbon(consumer, pose.last(), from, to, new Vec3(HALF_WIDTH, 0, 0), color);
            ribbon(consumer, pose.last(), from, to, new Vec3(0, HALF_WIDTH, 0), color);
            from = to;
        }
        pose.popPose();
    }

    private static Sample sample(Vec3 reach, double span, double t, float time) {
        double taper = 1 - 2 * Math.abs(t - 0.5);
        double sway = 0.5 * taper;
        double phase = span * (1 - t) * 2 - time / 5.0;
        Vec3 position = reach.add(sway * Math.sin(phase / 4), sway * Math.sin(phase / 3), sway * Math.sin(phase / 2)).scale(t);
        float alpha = (float) Math.max(taper, (t - 0.7) / 0.3 * 0.8);
        return new Sample(position, (float) ((1 - t) * span + time * 0.02), alpha);
    }

    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, Sample from, Sample to, Vec3 side, Tint color) {
        vertex(buffer, pose, from, side.reverse(), 1, color);
        vertex(buffer, pose, from, side, 0, color);
        vertex(buffer, pose, to, side, 0, color);
        vertex(buffer, pose, to, side.reverse(), 1, color);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Sample sample, Vec3 edge, float v, Tint color) {
        Vec3 point = sample.position().add(edge);
        buffer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(color.red, color.green, color.blue, sample.alpha()).uv(sample.u(), v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
    }

    private record Sample(Vec3 position, float u, float alpha) {}

    private static final class Tint {
        private float red;
        private float green;
        private float blue;

        void follow(int rgb) {
            red = (4 * red + (rgb >> 16 & 255) / 255F) / 5;
            green = (4 * green + (rgb >> 8 & 255) / 255F) / 5;
            blue = (4 * blue + (rgb & 255) / 255F) / 5;
        }
    }
}
