package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TaintSeedRenderer extends MobRenderer<TaintSeedEntity, HumanoidModel<TaintSeedEntity>> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/entity/taint_seed.png");

    public TaintSeedRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(TaintSeedEntity entity) {
        return TEXTURE;
    }
}
