package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class WandRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WOOD = texture("wand_rod_wood");
    private static final ResourceLocation GREATWOOD = texture("wand_rod_greatwood");
    private static final ResourceLocation IRON = texture("wand_cap_iron");
    private static final ResourceLocation FOCUS = texture("wand");
    private final ModelPart rod;
    private final ModelPart cap;
    private final ModelPart focus;

    public WandRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 8).addBox(-1, 1, -1, 2, 18, 2), PartPose.ZERO);
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
        var parts = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        rod = parts.getChild("rod");
        cap = parts.getChild("cap");
        focus = parts.getChild("focus");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        renderParts(stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    public void renderParts(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        rod.render(pose, ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutout(WandItem.isGreatwood(stack) ? GREATWOOD : WOOD), false, stack.hasFoil()), light, overlay);

        pose.pushPose();
        pose.scale(1.2F, 1, 1.2F);
        var iron = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(IRON), false, stack.hasFoil());
        cap.render(pose, iron, light, overlay);
        pose.translate(0, 20 / 16F, 0);
        cap.render(pose, iron, light, overlay);
        pose.popPose();

        int tint = WandItem.focusColor(stack);
        if (tint != 0) {
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 0.5F);
            focus.render(pose, buffers.getBuffer(RenderType.entityTranslucent(FOCUS)), LightTexture.FULL_BRIGHT, overlay,
                    (tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 0.8F);
            pose.popPose();
        }
    }

    private static ResourceLocation texture(String name) {
        return TTIds.rl("textures/models/" + name + ".png");
    }
}
