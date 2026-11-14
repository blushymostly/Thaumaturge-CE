package com.leclowndu93150.thaumaturge.client.render.crystal;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMesh;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshLoader;
import java.io.IOException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

public final class CrystalUnbakedModel {
    private CrystalUnbakedModel() {}

    public static final class Loader implements IGeometryLoader<CrystalGeometry> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public CrystalGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            ResourceLocation model = json.has("model")
                    ? new ResourceLocation(GsonHelper.getAsString(json, "model"))
                    : new ResourceLocation(TTIds.MODID, "models/mesh/crystal.ttmesh");
            return new CrystalGeometry(model);
        }
    }

    public static final class CrystalGeometry implements IUnbakedGeometry<CrystalGeometry> {
        private final ResourceLocation meshLocation;

        public CrystalGeometry(ResourceLocation meshLocation) {
            this.meshLocation = meshLocation;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter,
                ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            TTMesh mesh = loadMesh();
            String slot = context.hasMaterial("crystal") ? "crystal" : "texture";
            if (!context.hasMaterial(slot) && context.hasMaterial("particle")) {
                slot = "particle";
            }
            Material material = context.getMaterial(slot);
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = spriteGetter.apply(material);
            return new CrystalBakedModel(mesh, sprite, context.getTransforms(), overrides, context.useAmbientOcclusion(), context.isGui3d(), context.useBlockLight());
        }

        private TTMesh loadMesh() {
            try {
                return TTMeshLoader.load(Minecraft.getInstance().getResourceManager(), meshLocation);
            } catch (IOException e) {
                throw new RuntimeException("Could not load crystal mesh at " + meshLocation, e);
            }
        }
    }
}
