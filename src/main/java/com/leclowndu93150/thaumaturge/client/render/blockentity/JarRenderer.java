package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 1.20.1 port of CE26 {@code JarRenderer}: renders the essentia surface inside the jar.
 * Labels, brace/lid prisms and brine overlay are deferred (those sub-systems are not ported yet).
 */
public final class JarRenderer implements BlockEntityRenderer<EssentiaJarBlockEntity> {
    public static final ResourceLocation ANIMATED_GLOW_LOCATION = TTIds.rl("block/animatedglow");
    public static final float FLUID_MIN = 0.25F;
    public static final float FLUID_MAX = 0.75F;
    public static final float FLUID_BASE_Y = 0.0625F;
    public static final float FLUID_MAX_HEIGHT = 0.625F;

    private static final float SIDE_V_BOTTOM = 0.9375F;
    private static final int OPAQUE = 0xFF000000;
    private static final float[][] FLUID_RING_TOP = {{FLUID_MIN, FLUID_MIN}, {FLUID_MIN, FLUID_MAX}, {FLUID_MAX, FLUID_MAX}, {FLUID_MAX, FLUID_MIN}};
    private static final float[][] FLUID_RING_BOTTOM = {{FLUID_MIN, FLUID_MIN}, {FLUID_MAX, FLUID_MIN}, {FLUID_MAX, FLUID_MAX}, {FLUID_MIN, FLUID_MAX}};
    /** startX, startZ, endX, endZ, nx, nz */
    private static final float[][] FLUID_WALLS = {
            {FLUID_MIN, FLUID_MIN, FLUID_MIN, FLUID_MAX, -1.0F, 0.0F},
            {FLUID_MAX, FLUID_MAX, FLUID_MAX, FLUID_MIN, 1.0F, 0.0F},
            {FLUID_MAX, FLUID_MIN, FLUID_MIN, FLUID_MIN, 0.0F, -1.0F},
            {FLUID_MIN, FLUID_MAX, FLUID_MAX, FLUID_MAX, 0.0F, 1.0F}};

    public JarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(EssentiaJarBlockEntity jar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderTank(jar.tank(), poseStack, buffer, packedLight, packedOverlay);
    }

    /** Shared with {@code JarItemBewlr}. */
    public static void renderTank(EssentiaTank tank, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        ResourceLocation held = tank.heldAspect();
        int stored = tank.stored();
        if (held == null || stored <= 0) {
            return;
        }
        submitFluid(stored, tank.getCapacity(), OPAQUE | aspectColor(held), light, overlay, poseStack, buffer);
    }

    public static int aspectColor(@Nullable ResourceLocation aspectId) {
        IAspect aspect = aspectId == null ? null : TTAspects.get(aspectId);
        return aspect == null ? 0xFFFFFF : aspect.color() & 0xFFFFFF;
    }

    public static void submitFluid(float amount, int capacity, int colour, int light, int overlay, PoseStack poseStack, MultiBufferSource buffers) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ANIMATED_GLOW_LOCATION);
        VertexConsumer wrapped = sprite.wrap(buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)));
        float fill = Math.min(1.0F, amount / Math.max(1, capacity));
        float height = FLUID_BASE_Y + FLUID_MAX_HEIGHT * fill;
        PoseStack.Pose pose = poseStack.last();
        horizontalFace(wrapped, pose, FLUID_RING_TOP, height, 1.0F, colour, light, overlay);
        horizontalFace(wrapped, pose, FLUID_RING_BOTTOM, FLUID_BASE_Y, -1.0F, colour, light, overlay);
        for (float[] wall : FLUID_WALLS) {
            sideQuad(wrapped, pose, wall, height, colour, light, overlay);
        }
    }

    private static void horizontalFace(VertexConsumer buffer, PoseStack.Pose pose, float[][] ring, float y, float ny, int colour, int light, int overlay) {
        for (float[] xz : ring) {
            addVertex(buffer, pose, xz[0], y, xz[1], xz[0], xz[1], colour, light, overlay, 0.0F, ny, 0.0F);
        }
    }

    private static void sideQuad(VertexConsumer buffer, PoseStack.Pose pose, float[] wall, float height, int colour, int light, int overlay) {
        float vLow = SIDE_V_BOTTOM - height;
        float[] xs = {wall[0], wall[2], wall[2], wall[0]};
        float[] zs = {wall[1], wall[3], wall[3], wall[1]};
        float[] ys = {FLUID_BASE_Y, FLUID_BASE_Y, height, height};
        float[] us = {FLUID_MIN, FLUID_MAX, FLUID_MAX, FLUID_MIN};
        float[] vs = {vLow, vLow, SIDE_V_BOTTOM, SIDE_V_BOTTOM};
        for (int i = 0; i < 4; i++) {
            addVertex(buffer, pose, xs[i], ys[i], zs[i], us[i], vs[i], colour, light, overlay, wall[4], 0.0F, wall[5]);
        }
    }

    private static void addVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int colour, int light, int overlay,
            float nx, float ny, float nz) {
        buffer.vertex(pose.pose(), x, y, z)
                .color((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, (colour >>> 24) & 0xFF)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
