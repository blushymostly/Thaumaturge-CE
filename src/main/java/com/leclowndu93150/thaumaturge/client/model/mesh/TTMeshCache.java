package com.leclowndu93150.thaumaturge.client.model.mesh;

import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Cached runtime mesh load + entity-quad upload (CE GolemMeshes stand-in for BER props). */
public final class TTMeshCache {
    private static final Map<ResourceLocation, TTMesh> CACHE = new ConcurrentHashMap<>();

    private TTMeshCache() {}

    public static void clear() {
        CACHE.clear();
    }

    public static TTMesh get(ResourceLocation meshLocation) {
        return CACHE.computeIfAbsent(meshLocation, location -> {
            try {
                return TTMeshLoader.load(Minecraft.getInstance().getResourceManager(), location);
            } catch (IOException e) {
                Thaumaturge.LOGGER.error("Failed to load mesh {}", location, e);
                return TTMesh.EMPTY;
            }
        });
    }

    public static void renderPart(TTMeshPart part, PoseStack.Pose pose, VertexConsumer buffer, int light, int argb) {
        float[] positions = part.positions();
        float[] uvs = part.uvs();
        float[] normals = part.normals();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float a = ((argb >> 24) & 255) / 255.0F;
        float r = ((argb >> 16) & 255) / 255.0F;
        float g = ((argb >> 8) & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;
        if (a <= 0.0F) {
            a = 1.0F;
        }
        int vertexCount = part.quadCount() * 4;
        Vector3f normal = new Vector3f();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            float u = uvs == null ? 0.0F : uvs[vertex * 2];
            float v = uvs == null ? 0.0F : 1.0F - uvs[vertex * 2 + 1];
            normal.set(normals[vertex * 3], normals[vertex * 3 + 1], normals[vertex * 3 + 2]);
            normal.mul(normalMatrix);
            buffer.vertex(matrix, positions[vertex * 3], positions[vertex * 3 + 1], positions[vertex * 3 + 2])
                    .color(r, g, b, a)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normal.x(), normal.y(), normal.z())
                    .endVertex();
        }
    }
}
