package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.model.entity.MatrixCubeModel;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class InfusionMatrixRenderer implements BlockEntityRenderer<InfusionMatrix> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TTIds.rl("infusion_matrix"), "main");
    private static final ResourceLocation ARCANE = TTIds.rl("textures/block/infuser_normal.png");
    private static final ResourceLocation ANCIENT = TTIds.rl("textures/block/infuser_ancient.png");
    private static final ResourceLocation ELDRITCH = TTIds.rl("textures/block/infuser_eldritch.png");
    private final MatrixCubeModel model;

    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {
        model = new MatrixCubeModel(context.bakeLayer(LAYER));
    }

    @Override
    public void render(InfusionMatrix matrix, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = matrix.getLevel();
        if (level == null) return;
        var pillar = level.getBlockState(matrix.getBlockPos().offset(-1, -2, -1));
        ResourceLocation texture = pillar.is(TTBulkContent.BLOCKS.get("pillar_ancient").get()) ? ANCIENT
                : pillar.is(TTBulkContent.BLOCKS.get("pillar_eldritch").get()) ? ELDRITCH : ARCANE;
        float time = (level.getGameTime() % 7200L) + partial;
        float startup = matrix.startup(partial);
        int craftTicks = matrix.clientCraftTicks();
        float idleBob = Mth.sin(Mth.TWO_PI * time / 40) / 4.5F;
        float craftBob = Mth.sin(Mth.TWO_PI * time / 25) / 3.5F;
        float bob = Mth.lerp(Math.min(craftTicks, 15) / 15F, idleBob, craftBob);
        float drift = 0.01F * startup * (1 + Math.min(craftTicks, 50) / 50F);
        pose.pushPose();
        pose.translate(0.5, 0.5 + (0.3F + bob) * startup, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time % 360 * startup));
        pose.mulPose(Axis.ZP.rotationDegrees(time / 4 % 360 * startup));
        pose.mulPose(Axis.XP.rotationDegrees(45 * startup));
        pose.mulPose(Axis.ZP.rotationDegrees(45 * startup));
        pose.scale(0.8F, 0.8F, 0.8F);
        drawCubes(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, time, drift, 0);
        if (startup > 0) {
            drawCubes(pose, buffers.getBuffer(TTFlatRenderTypes.entityAdditiveWorld(texture)),
                    LightTexture.FULL_BRIGHT, overlay, time, drift, startup);
        }
        pose.popPose();
    }

    private void drawCubes(PoseStack pose, VertexConsumer buffer, int light, int overlay, float time, float drift, float glow) {
        for (int i = 0; i < 8; i++) {
            int x = i & 1, y = (i >> 1) & 1, z = (i >> 2) & 1;
            cube(pose, buffer, light, overlay, offset(x, time, drift, 15), offset(y, time, drift, 14),
                    offset(z, time, drift, 13), x, y, z, 0.5F, glowAlpha(time + 2 * x + 3 * y + 4 * z, glow));
        }
        cube(pose, buffer, light, overlay, 0, 0, 0, 0, 0, 0, 0.75F, glowAlpha(time, glow));
    }

    private static float glowAlpha(float time, float startup) {
        return (0.1F * Mth.sin(time / 4) + 0.2F) * startup;
    }

    private static float offset(int side, float time, float drift, int period) {
        return (side == 0 ? -0.3125F : 0.3125F) + drift * Mth.sin((time + side * 10) / period);
    }

    private void cube(PoseStack pose, VertexConsumer buffer, int light, int overlay, float x, float y, float z,
            int turnX, int turnY, int turnZ, float scale, float alpha) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(90 * turnX));
        pose.mulPose(Axis.YP.rotationDegrees(90 * turnY));
        pose.mulPose(Axis.ZP.rotationDegrees(90 * turnZ));
        pose.scale(scale, scale, scale);
        if (alpha > 0) model.glow.render(pose, buffer, light, overlay, 0.8F, 0.1F, 1, alpha);
        else model.cube.render(pose, buffer, light, overlay);
        pose.popPose();
    }
}
