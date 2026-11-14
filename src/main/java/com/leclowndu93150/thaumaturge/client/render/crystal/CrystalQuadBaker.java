package com.leclowndu93150.thaumaturge.client.render.crystal;

import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshPart;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshQuadBaker;
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

public final class CrystalQuadBaker {
    private static final int TINT_INDEX = 0;

    private CrystalQuadBaker() {}

    public static void bakePart(TTMeshPart part, TextureAtlasSprite sprite, Matrix4f transform, List<BakedQuad> output) {
        Matrix3f normalTransform = transform.normal(new Matrix3f());
        for (int quad = 0; quad < part.quadCount(); quad++) {
            output.add(bakeQuad(part, quad, sprite, transform, normalTransform));
        }
    }

    private static BakedQuad bakeQuad(TTMeshPart part, int quad, TextureAtlasSprite sprite, Matrix4f transform, Matrix3f normalTransform) {
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
            vs[i] = Mth.lerp(v, sprite.getV0(), sprite.getV1());
        }
        Direction facing = TTMeshQuadBaker.computeFaceDirection(positions);
        AtomicReference<BakedQuad> result = new AtomicReference<>();
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer(result::set);
        builder.setSprite(sprite);
        builder.setDirection(facing);
        builder.setTintIndex(TINT_INDEX);
        builder.setShade(true);
        builder.setHasAmbientOcclusion(true);
        for (int i = 0; i < 4; i++) {
            builder.vertex(positions[i].x, positions[i].y, positions[i].z);
            builder.color(255, 255, 255, 255);
            builder.uv(us[i], vs[i]);
            builder.uv2(LightTexture.FULL_BRIGHT);
            builder.normal(normals[i].x, normals[i].y, normals[i].z);
            builder.endVertex();
        }
        return result.get();
    }
}
