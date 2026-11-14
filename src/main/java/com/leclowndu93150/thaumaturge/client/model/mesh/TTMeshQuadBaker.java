package com.leclowndu93150.thaumaturge.client.model.mesh;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class TTMeshQuadBaker {
    private TTMeshQuadBaker() {}

    public static void bakePart(TTMeshPart part, TextureAtlasSprite sprite, Matrix4f transform, boolean flipV, boolean worldLight, List<BakedQuad> output) {
        Matrix3f normalTransform = transform.normal(new Matrix3f());
        for (int quad = 0; quad < part.quadCount(); quad++) {
            output.add(bakeQuad(part, quad, sprite, transform, normalTransform, flipV, worldLight));
        }
    }

    private static BakedQuad bakeQuad(TTMeshPart part, int quad, TextureAtlasSprite sprite, Matrix4f transform, Matrix3f normalTransform, boolean flipV, boolean worldLight) {
        Vector3f[] positions = new Vector3f[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        Vector3f[] normals = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            int vertex = quad * 4 + i;
            Vector3f pos = new Vector3f(part.positions()[vertex * 3], part.positions()[vertex * 3 + 1], part.positions()[vertex * 3 + 2]);
            Vector3f normal = new Vector3f(part.normals()[vertex * 3], part.normals()[vertex * 3 + 1], part.normals()[vertex * 3 + 2]);
            normal.mul(normalTransform).normalize();
            normals[i] = normal;
            float u = 0.0F;
            float v = 0.0F;
            if (part.uvs() != null) {
                u = part.uvs()[vertex * 2];
                v = part.uvs()[vertex * 2 + 1];
            }
            pos.mulPosition(transform);
            positions[i] = pos;
            us[i] = Mth.lerp(u, sprite.getU0(), sprite.getU1());
            float vv = flipV ? 1.0F - v : v;
            vs[i] = Mth.lerp(vv, sprite.getV0(), sprite.getV1());
        }
        Direction facing = computeFaceDirection(positions);
        AtomicReference<BakedQuad> result = new AtomicReference<>();
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer(result::set);
        builder.setSprite(sprite);
        builder.setDirection(facing);
        builder.setShade(worldLight);
        builder.setHasAmbientOcclusion(false);
        for (int i = 0; i < 4; i++) {
            builder.vertex(positions[i].x, positions[i].y, positions[i].z);
            builder.color(255, 255, 255, 255);
            builder.uv(us[i], vs[i]);
            builder.uv2(worldLight ? 0 : LightTexture.FULL_BRIGHT);
            builder.normal(normals[i].x, normals[i].y, normals[i].z);
            builder.endVertex();
        }
        return result.get();
    }

    public static Direction computeFaceDirection(Vector3fc[] positions) {
        Vector3f edge1 = new Vector3f(positions[1]).sub(positions[0]);
        Vector3f edge2 = new Vector3f(positions[2]).sub(positions[0]);
        Vector3f normal = new Vector3f();
        edge1.cross(edge2, normal);
        if (normal.lengthSquared() < 1.0E-6F) {
            return Direction.UP;
        }
        normal.normalize();
        return Direction.getNearest(normal.x, normal.y, normal.z);
    }
}
