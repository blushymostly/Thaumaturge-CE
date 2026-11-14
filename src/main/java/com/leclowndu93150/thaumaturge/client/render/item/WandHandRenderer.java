package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class WandHandRenderer {
    private static @Nullable Vector3f projectedTip;
    private static @Nullable ClientLevel tipLevel;
    private static InteractionHand tipHand;
    private static long tipTime;

    private WandHandRenderer() {}

    public static @Nullable Vec3 tip(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (projectedTip == null || minecraft.level != tipLevel || hand != tipHand || Util.getMillis() - tipTime > 250) return null;
        var camera = minecraft.gameRenderer.getMainCamera();
        // Forge draws the hand after the world with a separate field of view.
        Vector3f offset = new Matrix4f(RenderSystem.getProjectionMatrix()).invert()
                .transformProject(new Vector3f(projectedTip));
        RenderSystem.getInverseViewRotationMatrix().transform(offset);
        return camera.getPosition().add(offset.x, offset.y, offset.z);
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        var stack = event.getItemStack();
        if (player == null || !(stack.getItem() instanceof WandItem)
                || !(IClientItemExtensions.of(stack).getCustomRenderer() instanceof WandRenderer wand)) return;
        event.setCanceled(true);
        var arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swing = event.getSwingProgress();
        float arc = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float vertical = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI);
        var pose = event.getPoseStack();
        pose.pushPose();
        boolean using = player.isUsingItem() && player.getUsedItemHand() == event.getHand();
        if (!using) pose.translate(-side * arc * 0.4F, vertical * 0.2F, -Mth.sin(swing * Mth.PI) * 0.2F);
        pose.translate(side * 0.56F, -0.52F - event.getEquipProgress() * 0.6F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45 - Mth.sin(swing * swing * Mth.PI) * 20)));
        pose.mulPose(Axis.ZP.rotationDegrees(-side * arc * 20));
        pose.mulPose(Axis.XP.rotationDegrees(-arc * 80));
        pose.scale(0.4F, 0.4F, 0.4F);
        float useTicks = player.getTicksUsingItem() + event.getPartialTick();
        if (using) {
            pose.mulPose(Axis.ZP.rotationDegrees(-18 * side));
            pose.mulPose(Axis.YP.rotationDegrees(-12 * side));
            pose.mulPose(Axis.XP.rotationDegrees(-8));
            pose.translate(-0.9F * side, 0.2F, 0);
            float ticks = Math.max(0, useTicks - 1);
            float draw = ticks / 20F;
            draw = Math.min(1, (draw * draw + draw * 2) / 3F);
            if (draw > 0.1F) pose.translate(0, Mth.sin((ticks - 0.1F) * 1.3F) * 0.01F * (draw - 0.1F), 0);
            pose.translate(0, 0, draw * 0.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(-335 * side));
            pose.mulPose(Axis.YP.rotationDegrees(-50 * side));
            pose.translate(0, 0.5F, 0);
            pose.scale(1, 1, 1 + draw * 0.2F);
            pose.translate(0, -0.5F, 0);
            pose.mulPose(Axis.YP.rotationDegrees(50 * side));
            pose.mulPose(Axis.ZP.rotationDegrees(335 * side));
        }
        pose.translate(0, 1, 0);
        pose.scale(1, 1.1F, 1);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (using) {
            pose.translate(0, 1, 0);
            pose.mulPose(Axis.XP.rotationDegrees(10 - 20 * Math.min(useTicks, 3) + 10 * Mth.sin(useTicks / 15)));
            pose.mulPose(Axis.ZP.rotationDegrees(side * (10 + 10 * Mth.sin(useTicks / 10))));
            pose.translate(0, -1, 0);
        }
        if (using) {
            Vector3f tip = pose.last().pose().transformPosition(new Vector3f(0, WandRenderer.tipY(stack), 0));
            projectedTip = RenderSystem.getProjectionMatrix().transformProject(tip);
            tipLevel = Minecraft.getInstance().level;
            tipHand = event.getHand();
            tipTime = Util.getMillis();
        }
        wand.renderParts(stack, pose, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
