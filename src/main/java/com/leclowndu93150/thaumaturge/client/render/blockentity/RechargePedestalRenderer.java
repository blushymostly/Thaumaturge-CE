package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.content.aura.BlockEntityRechargePedestal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class RechargePedestalRenderer implements BlockEntityRenderer<BlockEntityRechargePedestal> {
    public RechargePedestalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlockEntityRechargePedestal pedestal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = pedestal.getLevel();
        var wand = pedestal.getItem();
        if (level == null || wand.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1.2, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees((level.getGameTime() % 360L) + partial));
        Minecraft.getInstance().getItemRenderer().renderStatic(wand, ItemDisplayContext.GROUND, light, overlay,
                pose, buffers, level, Long.hashCode(pedestal.getBlockPos().asLong()));
        pose.popPose();
    }
}
