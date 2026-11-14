package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Forge BER for aura nodes — CE {@code NodeRenderer} subset with pulsed aspect layers,
 * animated atlas frames and distinct core styles.
 * Drain lines / jarred-node paths remain out until those systems land on Forge.
 */
public final class NodeRenderer implements BlockEntityRenderer<BlockEntityNode> {
    private static final ResourceLocation AURA_TEXTURE = TTIds.rl("textures/misc/auranodes.png");
    private static final int ATLAS_CELLS = 32;
    private static final int FALLBACK_ROW = 7;
    private static final float FALLBACK_ALPHA = 0.0066F;
    private static final float FALLBACK_HALF = 0.5F;
    private static final int ASPECT_ROW = 0;
    private static final float THAUMOMETER_VIEW = 48.0F;
    private static final float GOGGLES_VIEW = 64.0F;
    private static final float BRIGHT_FACTOR = 1.5F;
    private static final float PALE_FACTOR = 0.66F;
    private static final float FADING_AMPLITUDE = 0.25F;
    private static final float FADING_BASE = 0.33F;
    private static final float FADING_PERIOD = 3.0F;
    private static final float FRAME_RATE = 1.25F;
    private static final float CLOCK_RATE = 10.0F;
    private static final float DISPLAY_CAP = 50.0F;
    private static final float ASPECT_BASE_HALF = 0.2F;
    private static final float ASPECT_PULSE_AMPLITUDE = 0.25F;
    private static final float ASPECT_PULSE_BASE = 0.5F;
    private static final float PULSE_PERIOD = 14.0F;
    private static final float ROTATION_PERIOD_BASE = 5000.0F;
    private static final float ROTATION_PERIOD_STEP = 500.0F;
    private static final float CORE_BASE_HALF = 0.1F;
    private static final float CORE_AMOUNT_DIVISOR = 150.0F;
    private static final Map<NodeType, CoreStyle> CORE_STYLES = coreStyles();

    public NodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(BlockEntityNode node) {
        return true;
    }

    @Override
    public void render(BlockEntityNode node, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float viewRange = viewRange(player);
        Vec3 center = Vec3.atCenterOf(node.getBlockPos());
        double distance = player.position().distanceTo(center);
        if (distance > (viewRange > 0 ? viewRange : GOGGLES_VIEW)) {
            return;
        }
        float ticks = player.tickCount + partialTick;
        float alpha = viewRange <= 0 ? 0 : Math.min((float) ((viewRange - distance) / viewRange)
                * modifierFactor(node.trait(), ticks), 1.0F);
        float clock = ticks * CLOCK_RATE;
        int column = Math.floorMod((int) Math.floor(ticks * FRAME_RATE + node.getBlockPos().getX()), ATLAS_CELLS);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        List<AspectInstance> aspects = node.getAspects().entries();
        int count = aspects.size();
        if (viewRange <= 0 || count == 0) {
            drawCell(poseStack, buffer, FALLBACK_ROW, column, FALLBACK_HALF, 0xFFFFFF, FALLBACK_ALPHA, false);
            poseStack.popPose();
            return;
        }
        float layerAlpha = alpha / Math.max(1.0F, count / 2.0F);
        float displaySum = 0.0F;
        float lastAngle = 0.0F;
        for (int index = 0; index < count; index++) {
            AspectInstance entry = aspects.get(index);
            IAspect aspect = TTAspects.get(entry.aspectId());
            int rgb = aspect == null ? 0xFFFFFF : aspect.color() & 0xFFFFFF;
            float display = Math.min(entry.amount(), DISPLAY_CAP);
            float divisor = PULSE_PERIOD - index;
            float pulse = divisor == 0.0F ? 0.0F : Mth.sin(ticks / divisor);
            float half = (ASPECT_BASE_HALF + (ASPECT_PULSE_AMPLITUDE * pulse + ASPECT_PULSE_BASE) * display / DISPLAY_CAP);
            float period = ROTATION_PERIOD_BASE + ROTATION_PERIOD_STEP * index;
            lastAngle = (clock % period) / period * Mth.TWO_PI;
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotation(lastAngle));
            boolean contrast = entry.aspectId().equals(TTAspects.PERDITIO) || entry.aspectId().equals(TTAspects.VACUOS);
            drawCell(poseStack, buffer, ASPECT_ROW, column, half, rgb, contrast ? layerAlpha * 1.5F : layerAlpha, contrast);
            poseStack.popPose();
            displaySum += display;
        }

        CoreStyle style = CORE_STYLES.getOrDefault(node.kind(), CORE_STYLES.get(NodeType.NORMAL));
        float coreHalf = (CORE_BASE_HALF + (count == 0 ? 0.0F : displaySum / count / CORE_AMOUNT_DIVISOR)) * style.scale();
        float coreAngle = style.spins() ? lastAngle : 0.0F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(coreAngle));
        drawCell(poseStack, buffer, style.row(), column, coreHalf, 0xFFFFFF, alpha, style.translucent());
        poseStack.popPose();


        poseStack.popPose();
    }

    private static float viewRange(LocalPlayer player) {
        if (GogglesAccess.wearsRevealingGear(player)) {
            return GOGGLES_VIEW;
        }
        if (holdsThaumometer(player)) {
            return THAUMOMETER_VIEW;
        }
        return 0.0F;
    }

    private static boolean holdsThaumometer(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ThaumometerItem
                || player.getOffhandItem().getItem() instanceof ThaumometerItem;
    }

    private static float modifierFactor(@Nullable NodeModifier modifier, float time) {
        if (modifier == null) {
            return 1.0F;
        }
        return switch (modifier) {
            case BRIGHT -> BRIGHT_FACTOR;
            case PALE -> PALE_FACTOR;
            case FADING -> FADING_AMPLITUDE * Mth.sin(time / FADING_PERIOD) + FADING_BASE;
        };
    }

    private static void drawCell(PoseStack poseStack, MultiBufferSource buffers, int row, int column, float halfSize, int rgb, float alpha, boolean translucent) {
        float u0 = column / (float) ATLAS_CELLS;
        float u1 = (column + 1) / (float) ATLAS_CELLS;
        float v0 = row / (float) ATLAS_CELLS;
        float v1 = (row + 1) / (float) ATLAS_CELLS;
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int tint = (a << 24) | (rgb & 0xFFFFFF);
        VertexConsumer consumer = buffers.getBuffer(translucent ? TTFlatRenderTypes.entityTranslucentFlatWorld(AURA_TEXTURE)
                : TTFlatRenderTypes.entityAdditiveWorld(AURA_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        consumer.vertex(matrix, -halfSize, -halfSize, 0.0F).color(tint).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, halfSize, -halfSize, 0.0F).color(tint).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, halfSize, halfSize, 0.0F).color(tint).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -halfSize, halfSize, 0.0F).color(tint).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
    }

    private static Map<NodeType, CoreStyle> coreStyles() {
        Map<NodeType, CoreStyle> styles = new EnumMap<>(NodeType.class);
        styles.put(NodeType.NORMAL, new CoreStyle(7, 1.0F, true, false));
        styles.put(NodeType.UNSTABLE, new CoreStyle(6, 1.0F, false, false));
        styles.put(NodeType.DARK, new CoreStyle(2, 1.0F, true, true));
        styles.put(NodeType.TAINTED, new CoreStyle(5, 1.0F, true, true));
        styles.put(NodeType.PURE, new CoreStyle(9, 1.0F, true, false));
        styles.put(NodeType.HUNGRY, new CoreStyle(8, 0.75F, true, false));
        return styles;
    }

    private record CoreStyle(int row, float scale, boolean spins, boolean translucent) {}
}
