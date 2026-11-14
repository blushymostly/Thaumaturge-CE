package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.model.entity.DeconTableModel;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagWorldRenderer;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DeconstructionTableRenderer implements BlockEntityRenderer<BlockEntityDeconstructionTable> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TTIds.rl("deconstruction_table"), "main");
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/entity/decontable.png");
    private final ModelPart model;

    public DeconstructionTableRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(LAYER);
    }

    @Override
    public void render(BlockEntityDeconstructionTable table, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        renderTable(model, pose, buffers, light, overlay);
        float spin = (table.getLevel().getGameTime() % 360L + partialTick);
        ItemStack input = table.items().getItem(BlockEntityDeconstructionTable.SLOT_INPUT);
        if (!input.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.19, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(spin + 180));
            pose.scale(0.5128F, 0.5128F, 0.5128F);
            Minecraft.getInstance().getItemRenderer().renderStatic(input, ItemDisplayContext.FIXED, light, overlay, pose,
                    buffers, table.getLevel(), 0);
            pose.popPose();
        }
        var result = table.resultAspect();
        var aspect = result == null ? null : TTAspects.get(result);
        if (aspect != null) {
            pose.pushPose();
            pose.translate(0.5, 1.081, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-spin));
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.scale(0.384F, 0.384F, 0.384F);
            var texture = com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer.textureOf(aspect);
            int tint = com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer.colorOf(aspect, 0.8F, false);
            AspectTagWorldRenderer.renderQuad(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(texture)), tint, light);
            pose.popPose();
        }
    }

    public static void renderTable(ModelPart model, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
        pose.popPose();

        Minecraft client = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(0.5, 1.02, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(0.4103F, 0.4103F, 0.4103F);
        client.getItemRenderer().renderStatic(new ItemStack(TTItems.THAUMOMETER.get()), ItemDisplayContext.FIXED,
                light, overlay, pose, buffers, client.level, 0);
        pose.popPose();
    }
}
