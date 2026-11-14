package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestal;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionInlay;
import net.minecraft.core.particles.DustParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class InfusionPedestalRenderer implements BlockEntityRenderer<InfusionPedestal> {
    public InfusionPedestalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(InfusionPedestal pedestal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = pedestal.getLevel();
        var pos = pedestal.getBlockPos();
        if (level.getGameTime() % 8 == 0 && level.random.nextInt(8) == 0 && InfusionInlay.isPowered(level, pos)) {
            level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + 0.5, pos.getY() + 0.18, pos.getZ() + 0.5, 0, 0, 0);
        }
        if (pedestal.item().isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1.2, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees((pedestal.getLevel().getGameTime() % 180 + partial) * 2));
        pose.scale(0.65F, 0.65F, 0.65F);
        Minecraft.getInstance().getItemRenderer().renderStatic(pedestal.item(), ItemDisplayContext.GROUND, light, overlay,
                pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
