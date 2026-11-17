package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshCache;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshPart;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.JarNodeBlock;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeStabilizer;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeTransducer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NodeDeviceRenderer implements BlockEntityRenderer<BlockEntity> {
    private static final ResourceLocation MESH = TTIds.rl("models/mesh/node_stabilizer.ttmesh");
    private static final Skin STABILIZER = new Skin("node_stabilizer");
    private static final Skin TRANSDUCER = new Skin("node_converter");
    private static final RenderType BUBBLE = TTFlatRenderTypes.entityAdditiveWorld(TTIds.rl("textures/misc/node_bubble.png"));
    private static final int WHITE = 0xFFFFFFFF;

    public NodeDeviceRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() == null)
            return;
        float time = entity.getLevel().getGameTime() % 24000 + partialTick;
        if (entity instanceof NodeStabilizer.Device stabilizer) {
            float extension = stabilizer.extension(partialTick);
            float strength = extension / NodeStabilizer.Device.MAX_EXTENSION;
            int tint = stabilizer.advanced() ? 0xFF3333 : 0xFFFFFF;
            drawDevice(pose, buffers, STABILIZER, light, time, extension / 100, strength, tint, false);
            if (extension > 0)
                drawBubble(pose, buffers, strength, time, tint, overlay);
        } else if (entity instanceof NodeTransducer.BlockEntity transducer) {
            float extension = transducer.extension(partialTick) / 137;
            var below = entity.getLevel().getBlockEntity(entity.getBlockPos().below());
            int tint = below instanceof BlockEntityNode node && !(below instanceof JarNodeBlock.Entity) ? (node.isEnergized() ? 0xFF004D : 0xFF991A) : 0x80FF80;
            drawDevice(pose, buffers, TRANSDUCER, light, time, extension, extension * 2.5F, tint, true);
        }
    }

    private static void drawDevice(PoseStack pose, MultiBufferSource buffers, Skin skin, int light, float time, float extension, float strength, int tint, boolean ceiling) {
        pose.pushPose();
        pose.translate(0.5, ceiling ? 1 : 0, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(ceiling ? 90 : -90));
        for (var part : TTMeshCache.get(MESH).parts()) {
            if (part.name().equals("lock")) {
                drawPart(pose, buffers, skin, part, light, ceiling ? glow(time, 0, strength, tint) : 0);
            } else if (part.name().equals("piston")) {
                for (int arm = 0; arm < 4; arm++) {
                    pose.pushPose();
                    pose.mulPose(Axis.ZP.rotationDegrees(90 * arm));
                    pose.mulPose(Axis.XP.rotationDegrees(45));
                    pose.translate(0, 0, extension);
                    drawPart(pose, buffers, skin, part, light, glow(time, arm, strength, tint));
                    pose.popPose();
                }
            }
        }
        pose.popPose();
    }

    private static void drawPart(PoseStack pose, MultiBufferSource buffers, Skin skin, TTMeshPart part, int light, int glow) {
        TTMeshCache.renderPart(part, pose.last(), buffers.getBuffer(skin.base()), light, WHITE);
        if (glow != 0)
            TTMeshCache.renderPart(part, pose.last(), buffers.getBuffer(skin.glow()), LightTexture.FULL_BRIGHT, glow);
    }

    private static int glow(float time, int arm, float strength, int tint) {
        float pulse = 0.9F + 0.1F * Mth.sin((time + arm * 5) / 3);
        return ((int) ((50 + 170 * strength * pulse) * 0.8F) << 24) | tint;
    }

    private static void drawBubble(PoseStack pose, MultiBufferSource buffers, float strength, float time, int tint, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        var consumer = buffers.getBuffer(BUBBLE);
        int color = ((int) (255 * strength * (0.5F + 0.1F * Mth.sin(time / 8))) << 24) | tint;
        bubbleVertex(pose.last(), consumer, -0.9F, -0.9F, 0, 1, color, overlay);
        bubbleVertex(pose.last(), consumer, 0.9F, -0.9F, 1, 1, color, overlay);
        bubbleVertex(pose.last(), consumer, 0.9F, 0.9F, 1, 0, color, overlay);
        bubbleVertex(pose.last(), consumer, -0.9F, 0.9F, 0, 0, color, overlay);
        pose.popPose();
    }

    private static void bubbleVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, int u, int v, int color, int overlay) {
        consumer.vertex(pose.pose(), x, y, 0).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).uv(u, v).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    private record Skin(RenderType base, RenderType glow) {
        Skin(String name) {
            this(RenderType.entityCutout(TTIds.rl("textures/block/" + name + ".png")), TTFlatRenderTypes.entityAdditiveWorld(TTIds.rl("textures/block/" + name + "_over.png")));
        }
    }
}
