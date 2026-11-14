package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockEntityBellows;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

public final class BellowsRenderer implements BlockEntityRenderer<BlockEntityBellows> {
    private static final ResourceLocation BOTTOM = TTIds.rl("block/bellows/bottom_plank");
    private static final ResourceLocation TOP = TTIds.rl("block/bellows/top_plank");
    private static final ResourceLocation BAG = TTIds.rl("block/bellows/bag");

    public BellowsRenderer(BlockEntityRendererProvider.Context context) {}

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BOTTOM);
        event.register(TOP);
        event.register(BAG);
    }

    @Override
    public void render(BlockEntityBellows bellows, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float inflation = bellows.inflation(partial);
        float travel = 0.25F * (1 - inflation);
        var minecraft = Minecraft.getInstance();
        var models = minecraft.getModelManager();
        var renderer = minecraft.getBlockRenderer().getModelRenderer();
        var buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        BlockFacingPose.northBased(pose, bellows.bellowsFacing());
        pose.translate(-0.5, -0.5, -0.5);
        for (ResourceLocation part : List.of(BOTTOM, TOP, BAG)) {
            pose.pushPose();
            if (part.equals(BAG)) {
                pose.translate(0, 0.5, 0);
                pose.scale(1, inflation + 0.1F, 1);
                pose.translate(0, -0.5, 0);
            } else {
                pose.translate(0, part.equals(BOTTOM) ? travel : -travel, 0);
            }
            renderer.renderModel(pose.last(), buffer, null, models.getModel(part),
                    1, 1, 1, light, overlay, ModelData.EMPTY, null);
            pose.popPose();
        }
        pose.popPose();
    }
}
