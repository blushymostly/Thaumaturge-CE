package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelay;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class VisRelayRenderer implements BlockEntityRenderer<VisRelayBlockEntity> {
    private static final RenderType BEAM = TTFlatRenderTypes.entityAdditiveWorld(TTIds.rl("textures/misc/beam1.png"));
    private static final double HALF_WIDTH = 0.105;

    public VisRelayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(VisRelayBlockEntity relay) {
        return true;
    }

    @Override
    public void render(VisRelayBlockEntity relay, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = relay.getLevel();
        var parent = relay.parent();
        if (level == null || parent == null || !level.hasChunkAt(parent)) return;
        var parentEntity = level.getBlockEntity(parent);
        if (!(parentEntity instanceof VisRelayBlockEntity upstream && upstream.parent() != null)
                && !(parentEntity instanceof BlockEntityNode node && node.isEnergized())) return;
        Vec3 origin = crystal(relay.getBlockPos(), relay.getBlockState());
        Vec3 target = crystal(parent, parentEntity.getBlockState()).subtract(origin);
        if (target.lengthSqr() < 1.0e-7) return;

        long time = level.getGameTime();
        float elapsed = Math.max(0, time - relay.pulseStart() + partialTick);
        float glow = Mth.clamp(0.8F - (elapsed - 10) * 0.025F, 0.3F, 0.8F);
        float alpha = glow * (GogglesAccess.wearsRevealingGear(Minecraft.getInstance().player) ? 1 : 0.1F);
        float wash = elapsed * 0.025F;
        int color = relay.pulseColor();
        float red = Math.min(1, (color >> 16 & 255) / 255F + wash);
        float green = Math.min(1, (color >> 8 & 255) / 255F + wash);
        float blue = Math.min(1, (color & 255) / 255F + wash);
        float scroll = -0.2F * (Math.floorMod(time, 100) + partialTick);
        Vec3 axis = target.normalize();
        Vec3 first = axis.cross(Math.abs(axis.y) >= 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 second = axis.cross(first).normalize();
        Vec3 offset = origin.subtract(Vec3.atLowerCornerOf(relay.getBlockPos()));
        var consumer = buffers.getBuffer(BEAM);

        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        ribbon(consumer, pose.last(), target, first.scale(HALF_WIDTH), scroll, red, green, blue, alpha, overlay);
        ribbon(consumer, pose.last(), target, second.scale(HALF_WIDTH), scroll + 1 / 3F, red, green, blue, alpha, overlay);
        pose.popPose();
    }

    private static Vec3 crystal(BlockPos pos, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (!(state.getBlock() instanceof VisRelay)) return center;
        var facing = state.getValue(VisRelay.FACING);
        return center.add(facing.getStepX() * 0.05, facing.getStepY() * 0.05, facing.getStepZ() * 0.05);
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 target, Vec3 side,
            float scroll, float red, float green, float blue, float alpha, int overlay) {
        float near = scroll + (float) target.length();
        vertex(consumer, pose, side.reverse(), 1, near, red, green, blue, alpha, overlay);
        vertex(consumer, pose, side, 0, near, red, green, blue, alpha, overlay);
        vertex(consumer, pose, target.add(side), 0, scroll, red, green, blue, alpha, overlay);
        vertex(consumer, pose, target.subtract(side), 1, scroll, red, green, blue, alpha, overlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, float u, float v,
            float red, float green, float blue, float alpha, int overlay) {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z).color(red, green, blue, alpha)
                .uv(u, v).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
    }
}
