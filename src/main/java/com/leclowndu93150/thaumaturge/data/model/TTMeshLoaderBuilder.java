package com.leclowndu93150.thaumaturge.data.model;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.TTIds;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public final class TTMeshLoaderBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {
    private @Nullable ResourceLocation mesh;
    private boolean flipV;
    private boolean cornerSpace;
    private boolean worldLight;

    public TTMeshLoaderBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(new ResourceLocation(TTIds.MODID, "mesh"), parent, existingFileHelper);
    }

    public TTMeshLoaderBuilder<T> mesh(ResourceLocation mesh) {
        this.mesh = Objects.requireNonNull(mesh, "mesh");
        return this;
    }

    public TTMeshLoaderBuilder<T> flipV(boolean flipV) {
        this.flipV = flipV;
        return this;
    }

    public TTMeshLoaderBuilder<T> cornerSpace(boolean cornerSpace) {
        this.cornerSpace = cornerSpace;
        return this;
    }

    public TTMeshLoaderBuilder<T> worldLight(boolean worldLight) {
        this.worldLight = worldLight;
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        JsonObject result = super.toJson(json);
        result.addProperty("model", Objects.requireNonNull(mesh, "mesh must be set").toString());
        if (flipV) {
            result.addProperty("flip_v", true);
        }
        if (cornerSpace) {
            result.addProperty("corner_space", true);
        }
        if (worldLight) {
            result.addProperty("world_light", true);
        }
        return result;
    }
}
