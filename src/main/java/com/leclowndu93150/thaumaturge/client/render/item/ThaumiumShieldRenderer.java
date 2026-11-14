package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Uses the vanilla shield geometry and coordinate system with the Thaumium texture. */
public final class ThaumiumShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/item/thaumium_shield.png");
    private final ShieldModel shield;

    public ThaumiumShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        shield = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        poses.scale(1.0F, -1.0F, -1.0F);
        var vertices = ItemRenderer.getFoilBufferDirect(buffers, shield.renderType(TEXTURE), true, stack.hasFoil());
        shield.handle().render(poses, vertices, light, overlay);
        shield.plate().render(poses, vertices, light, overlay);
        poses.popPose();
    }
}
