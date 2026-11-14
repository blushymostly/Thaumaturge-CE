package com.leclowndu93150.thaumaturge.client.render.aspect;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

/** Billboard aspect tags in world / on the thaumometer lens (Forge port of CE AspectTagWorldRenderer). */
public final class AspectTagWorldRenderer {
    public static final float DEFAULT_SCALE = 0.0625F;
    public static final ResourceLocation UNKNOWN_TEXTURE = TTIds.rl("textures/aspects/_unknown.png");

    private static final float HALF = 0.5F;
    private static final int ROW_SIZE = 5;
    private static final float SPACING_FACTOR = 4.0F;
    private static final float ROW_HEIGHT_FACTOR = 1.05F;
    private static final float FACE_REACH_FACTOR = 2.0F;
    private static final float UNKNOWN_ALPHA = 0.75F;
    private static final float TEXT_PIXEL = 0.04F;
    private static final float AMOUNT_RIGHT = 0.52F;
    private static final float AMOUNT_TOP = 0.24F;
    private static final float DARK_LIFT = 0.004F;
    private static final float WHITE_LIFT = 0.008F;
    private static final float LENS_TAG_SIZE = 16.0F;
    private static final float LENS_TAG_HALF = 8.0F;
    private static final float LENS_AMOUNT_SCALE = 0.5F;
    private static final float LENS_AMOUNT_GAP = 1.0F;
    /** CE {@code AMOUNT_FRONT_Z}: in front of the tag quad before GRID_SCALE. */
    private static final float LENS_AMOUNT_Z = 0.01F;
    private static final int AMOUNT_COLOR = 0xFFFFFFFF;
    private static final int AMOUNT_SHADOW_COLOR = 0xFF111111;

    private AspectTagWorldRenderer() {}

    public static void renderTagCloud(PoseStack poseStack, Minecraft minecraft, double x, double y, double z, AspectList aspects,
            @Nullable Direction face, float scale, float alpha, Predicate<ResourceLocation> discovered) {
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double baseX = face == null ? x - HALF : x;
        double baseZ = face == null ? z - HALF : z;
        Vec3 anchor = cloudAnchor(baseX, y, baseZ, face, scale);
        double bearing = Math.atan2(camera.x - (baseX + HALF), camera.z - (baseZ + HALF));
        Quaternionf facing = new Quaternionf().rotationY((float) bearing);
        Vec3 right = new Vec3(Math.cos(bearing), 0.0, -Math.sin(bearing));
        List<AspectInstance> tags = aspects.sortedByAmount();
        for (int index = 0; index < tags.size(); index++) {
            int row = index / ROW_SIZE;
            int columns = Math.min(ROW_SIZE, tags.size() - row * ROW_SIZE);
            double slide = (index % ROW_SIZE - columns / 2.0 + HALF) * SPACING_FACTOR * scale * scale;
            Vec3 at = anchor.add(right.scale(slide)).add(0.0, row * ROW_HEIGHT_FACTOR * scale, 0.0);
            poseStack.pushPose();
            poseStack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            poseStack.mulPose(facing);
            drawTag(poseStack, buffers, minecraft.font, tags.get(index), scale, alpha, discovered);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    public static void renderLensGrid(PoseStack poseStack, MultiBufferSource buffers, Font font, List<AspectInstance> entries,
            Predicate<ResourceLocation> discovered) {
        int row = 0;
        int placed = 0;
        final int gridRows = 5;
        while (row < gridRows && placed < entries.size()) {
            int count = Math.min(gridRows - row, entries.size() - placed);
            float left = -(count * LENS_TAG_SIZE / 2.0F);
            float top = -LENS_TAG_HALF + LENS_TAG_SIZE * row;
            for (int column = 0; column < count; column++) {
                AspectInstance entry = entries.get(placed + column);
                boolean known = discovered.test(entry.aspectId());
                IAspect aspect = TTAspects.get(entry.aspectId());
                ResourceLocation texture = known && aspect != null ? AspectTagRenderer.textureOf(aspect) : UNKNOWN_TEXTURE;
                IAspect tint = aspect != null ? aspect : TTAspects.get(TTAspects.ORDO);
                int color = known && aspect != null ? AspectTagRenderer.colorOf(aspect, 1.0F, false)
                        : AspectTagRenderer.colorOf(tint, UNKNOWN_ALPHA, true);
                poseStack.pushPose();
                poseStack.translate(left + LENS_TAG_SIZE * column + LENS_TAG_HALF, top + LENS_TAG_HALF, 0.0F);
                RenderType type = TTFlatRenderTypes.entityTranslucentFlat(texture);
                poseStack.pushPose();
                poseStack.scale(LENS_TAG_SIZE, -LENS_TAG_SIZE, LENS_TAG_SIZE);
                renderQuad(poseStack.last(), buffers.getBuffer(type), color, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
                // Flush glyph before amount so translucent sort cannot cover the digit.
                flush(buffers, type);
                if (known) {
                    poseStack.pushPose();
                    poseStack.translate(LENS_TAG_HALF, LENS_TAG_HALF, LENS_AMOUNT_Z);
                    poseStack.scale(LENS_AMOUNT_SCALE, LENS_AMOUNT_SCALE, LENS_AMOUNT_SCALE);
                    String label = Integer.toString(entry.amount());
                    font.drawInBatch(label, -font.width(label) - LENS_AMOUNT_GAP, -font.lineHeight, AMOUNT_COLOR, true,
                            poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
            placed += count;
            row++;
        }
    }

    private static void drawTag(PoseStack poseStack, MultiBufferSource buffers, Font font, AspectInstance entry, float scale, float alpha,
            Predicate<ResourceLocation> discovered) {
        boolean known = discovered.test(entry.aspectId());
        IAspect aspect = TTAspects.get(entry.aspectId());
        ResourceLocation texture = known && aspect != null ? AspectTagRenderer.textureOf(aspect) : UNKNOWN_TEXTURE;
        IAspect tint = aspect != null ? aspect : TTAspects.get(TTAspects.ORDO);
        int color = known && aspect != null
                ? AspectTagRenderer.colorOf(aspect, alpha, false)
                : AspectTagRenderer.colorOf(tint, alpha * UNKNOWN_ALPHA, true);
        RenderType type = TTFlatRenderTypes.entityTranslucentFlat(texture);
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderQuad(poseStack.last(), buffers.getBuffer(type), color, LightTexture.FULL_BRIGHT);
        flush(buffers, type);
        if (known) {
            drawAmount(poseStack, buffers, font, Integer.toString(entry.amount()), scale);
        }
        poseStack.popPose();
    }

    private static void drawAmount(PoseStack poseStack, MultiBufferSource buffers, Font font, String text, float scale) {
        float pixel = TEXT_PIXEL * scale;
        float width = font.width(text);
        drawDigits(poseStack, buffers, font, text, width, AMOUNT_RIGHT * scale + pixel, -AMOUNT_TOP * scale - pixel, DARK_LIFT * scale, pixel, AMOUNT_SHADOW_COLOR);
        drawDigits(poseStack, buffers, font, text, width, AMOUNT_RIGHT * scale, -AMOUNT_TOP * scale, WHITE_LIFT * scale, pixel, AMOUNT_COLOR);
    }

    private static void drawDigits(PoseStack poseStack, MultiBufferSource buffers, Font font, String text, float width, float right, float top,
            float lift, float pixel, int color) {
        poseStack.pushPose();
        poseStack.translate(right, top, lift);
        poseStack.scale(pixel, -pixel, pixel);
        font.drawInBatch(text, -width, 0.0F, color, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void flush(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    public static void renderQuad(PoseStack.Pose pose, VertexConsumer buffer, int argb, int packedLight) {
        Matrix4f matrix = pose.pose();
        float a = ((argb >> 24) & 255) / 255.0F;
        float r = ((argb >> 16) & 255) / 255.0F;
        float g = ((argb >> 8) & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;
        if (a <= 0.0F) {
            a = 1.0F;
        }
        vertex(buffer, matrix, pose, -HALF, -HALF, 0.0F, 1.0F, r, g, b, a, packedLight);
        vertex(buffer, matrix, pose, HALF, -HALF, 1.0F, 1.0F, r, g, b, a, packedLight);
        vertex(buffer, matrix, pose, HALF, HALF, 1.0F, 0.0F, r, g, b, a, packedLight);
        vertex(buffer, matrix, pose, -HALF, HALF, 0.0F, 0.0F, r, g, b, a, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, float x, float y, float u, float v,
            float r, float g, float b, float a, int light) {
        buffer.vertex(matrix, x, y, 0.0F).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F).endVertex();
    }

    private static Vec3 cloudAnchor(double baseX, double baseY, double baseZ, @Nullable Direction face, float scale) {
        Vec3 center = new Vec3(baseX + HALF, baseY + HALF, baseZ + HALF);
        if (face == null) {
            return center;
        }
        double reach = Math.max(0.55, FACE_REACH_FACTOR * scale);
        return center.add(face.getStepX() * reach, face.getStepY() * reach, face.getStepZ() * reach);
    }
}
