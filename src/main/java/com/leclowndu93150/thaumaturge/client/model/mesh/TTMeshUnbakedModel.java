package com.leclowndu93150.thaumaturge.client.model.mesh;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

public final class TTMeshUnbakedModel {
    private TTMeshUnbakedModel() {}

    public static final class Loader implements IGeometryLoader<TTMeshGeometry> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public TTMeshGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            ResourceLocation model = new ResourceLocation(GsonHelper.getAsString(json, "model"));
            boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", false);
            boolean cornerSpace = GsonHelper.getAsBoolean(json, "corner_space", false);
            boolean worldLight = GsonHelper.getAsBoolean(json, "world_light", false);
            String nodeDevice = GsonHelper.getAsString(json, "node_device", "");
            return new TTMeshGeometry(model, flipV, cornerSpace, worldLight, nodeDevice);
        }
    }
}
