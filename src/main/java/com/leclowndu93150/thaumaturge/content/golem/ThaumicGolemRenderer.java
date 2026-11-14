package com.leclowndu93150.thaumaturge.content.golem;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ThaumicGolemRenderer extends MobRenderer<ThaumicGolem, HumanoidModel<ThaumicGolem>> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/entity/golem.png");

    public ThaumicGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(ThaumicGolem entity) {
        return TEXTURE;
    }
}
