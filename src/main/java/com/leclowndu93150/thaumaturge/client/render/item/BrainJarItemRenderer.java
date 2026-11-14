package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.client.render.blockentity.BrainJarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

public final class BrainJarItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ModelPart brain;
    private ModelPart brine;

    public BrainJarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var client = Minecraft.getInstance();
        if (brain == null) {
            brain = client.getEntityModels().bakeLayer(BrainJarRenderer.LAYER);
            brine = client.getEntityModels().bakeLayer(BrainJarRenderer.BRINE_LAYER);
        }
        var shell = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        // Draw the opaque brain and translucent brine before the enclosing glass.
        BrainJarRenderer.renderContents(brain, brine, pose, buffers, light, overlay, 90, 0.78F);
        client.getBlockRenderer().renderSingleBlock(shell, pose, buffers, light, overlay, ModelData.EMPTY,
                RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
    }
}
