package com.leclowndu93150.thaumaturge.client.model;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshUnbakedModel;
import com.leclowndu93150.thaumaturge.client.render.crystal.CrystalUnbakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TTModelsHandlers {
    private TTModelsHandlers() {}

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("mesh", TTMeshUnbakedModel.Loader.INSTANCE);
        event.register("crystal", CrystalUnbakedModel.Loader.INSTANCE);
    }
}
