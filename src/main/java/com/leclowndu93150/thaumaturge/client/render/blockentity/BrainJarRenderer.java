package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.essentia.jar.BrainJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class BrainJarRenderer implements BlockEntityRenderer<BrainJarBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TTIds.rl("jar_brain"), "main");
    private static final ResourceLocation BRAIN = TTIds.rl("textures/entity/brain2.png");
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 64;
    public static final ModelLayerLocation BRINE_LAYER = new ModelLayerLocation(TTIds.rl("jar_brain"), "brine");
    private static final ResourceLocation BRINE = TTIds.rl("textures/entity/jarbrine.png");
    private final ModelPart brain;
    private final ModelPart brine;

    public BrainJarRenderer(BlockEntityRendererProvider.Context context) {
        brain = context.bakeLayer(LAYER);
        brine = context.bakeLayer(BRINE_LAYER);
    }

    @Override
    public void render(BrainJarBlockEntity jar, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        float time = jar.getLevel().getGameTime() + partial;
        renderContents(brain, brine, pose, buffer, light, overlay, time * 0.575F,
                0.81F - (Mth.sin(time / 14) + 1) * 0.03F);
        if (jar.experience() > 0) {
            JarRenderer.submitFluid(jar.experience(), BrainJarBlockEntity.CAPACITY, 0xFF8CFF4A, LightTexture.FULL_BRIGHT, overlay, pose, buffer);
        }
    }

    public static void renderContents(ModelPart brain, ModelPart brine, PoseStack pose, MultiBufferSource buffer,
            int light, int overlay, float yaw, float brainHeight) {
        pose.pushPose();
        pose.translate(0.5, brainHeight, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(0.4F, 0.4F, 0.4F);
        brain.render(pose, buffer.getBuffer(RenderType.entityCutout(BRAIN)), light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5, 0.01, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        brine.render(pose, buffer.getBuffer(RenderType.entityTranslucent(BRINE)), light, overlay);
        pose.popPose();
    }

    public static LayerDefinition createBrineLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("brine", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(-4, -11, -4, 8, 10, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition brain = root.addOrReplaceChild("brain", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 0.0F));
        brain.addOrReplaceChild("left",
                CubeListBuilder.create().texOffs(1, 1).addBox(-4.5F, -4.5F, -7.0F, 4.5F, 7.0F, 14.0F).texOffs(39, 1)
                        .addBox(-4.0F, -6.0F, -5.5F, 3.5F, 1.5F, 11.0F).texOffs(69, 1)
                        .addBox(-6.5F, -2.0F, -3.5F, 2.0F, 5.0F, 8.0F).texOffs(90, 1)
                        .addBox(-4.0F, -3.0F, -8.0F, 3.5F, 4.5F, 1.0F),
                PartPose.offset(-0.5F, 0.0F, 0.0F));
        brain.addOrReplaceChild("right",
                CubeListBuilder.create().texOffs(1, 23).addBox(0.0F, -4.5F, -7.0F, 4.5F, 7.0F, 14.0F).texOffs(39, 23)
                        .addBox(0.5F, -6.0F, -5.5F, 3.5F, 1.5F, 11.0F).texOffs(69, 23)
                        .addBox(4.5F, -2.0F, -3.5F, 2.0F, 5.0F, 8.0F).texOffs(90, 23)
                        .addBox(0.5F, -3.0F, -8.0F, 3.5F, 4.5F, 1.0F),
                PartPose.offset(0.5F, 0.0F, 0.0F));
        brain.addOrReplaceChild("hindbrain", CubeListBuilder.create().texOffs(100, 23).addBox(-4.0F, -1.0F, -2.0F, 8.0F, 1.5F, 5.5F).texOffs(1, 45).addBox(-3.0F, 0.5F, -1.5F, 6.0F, 1.0F, 4.5F), PartPose.offset(0.0F, 4.0F, 3.0F));
        PartDefinition stem = brain.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(23, 45).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 1.5F, -0.3839724F, 0.0F, 0.0F));
        stem.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(32, 45).addBox(-0.5F, -0.25F, -0.5F, 1.0F, 1.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, -0.2094395F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

}
