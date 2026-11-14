package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlockEntity;
import com.leclowndu93150.thaumaturge.content.workbench.WorkbenchContainer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the wand lying flat across the arcane workbench (CE WorkbenchWandRenderer). */
public final class WorkbenchWandRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity> {
    private static final float TABLE_CENTER = 0.5F;
    private static final float SURFACE_HEIGHT = 1.02F;
    private static final float GRID_DIAGONAL = (float) (10.0 * Math.sqrt(2.0) / 16.0);
    private static final float ASSUMED_WAND_LENGTH = 1.0F;
    private static final float DIAGONAL_DEGREES = 45.0F;
    private static final float LAY_FLAT_DEGREES = 90.0F;

    public WorkbenchWandRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ArcaneWorkbenchBlockEntity workbench, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack wand = workbench.items().get(WorkbenchContainer.WAND_SLOT);
        if (wand.isEmpty()) {
            return;
        }
        float scale = GRID_DIAGONAL / ASSUMED_WAND_LENGTH;
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(TABLE_CENTER, SURFACE_HEIGHT, TABLE_CENTER);
        poseStack.mulPose(Axis.YP.rotationDegrees(DIAGONAL_DEGREES));
        poseStack.mulPose(Axis.XP.rotationDegrees(LAY_FLAT_DEGREES));
        poseStack.scale(scale, scale, scale);
        items.renderStatic(wand, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, workbench.getLevel(), 0);
        poseStack.popPose();
    }
}
