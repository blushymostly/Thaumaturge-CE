package com.leclowndu93150.thaumaturge.client.render;

import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FluxRiftRenderer extends EntityRenderer<FluxRift> {
    private static final int SIDES = 6;
    private static final float STABLE_LIMIT = 50.0F;
    private static final float MAX_INSTABILITY = 1.5F;
    private static final float PHASE_PER_POINT = 10.0F;
    private static final float WOBBLE_AMPLITUDE = 0.1F;
    private static final float WOBBLE_X_DIVISOR = 50.0F;
    private static final float WOBBLE_Y_DIVISOR = 60.0F;
    private static final float WOBBLE_Z_DIVISOR = 70.0F;
    private static final float PULSE_DIVISOR = 8.0F;
    private static final float MIN_DIRECTION_LENGTH = 0.001F;
    private static final float VERTICAL_THRESHOLD = 0.9F;

    public FluxRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FluxRift entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        var points = new ArrayList<Vec3>();
        var widths = new ArrayList<Float>();
        FluxRiftOutline.build(entity.size(), entity.seed(), points, widths);
        if (points.size() >= 3) {
            Tube tube = buildTube(points, widths, entity.stability(), entity.tickCount + partialTicks);
            tube.write(buffers.getBuffer(RenderType.debugQuads()), pose.last().pose(), 1.0F, 0xFF120819);
            tube.write(buffers.getBuffer(RenderType.lightning()), pose.last().pose(), 1.65F, 0x665B216E);
            tube.write(buffers.getBuffer(RenderType.lightning()), pose.last().pose(), 2.2F, 0x304D1680);
        }
        super.render(entity, yaw, partialTicks, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FluxRift entity) {
        return new ResourceLocation("minecraft", "textures/environment/end_sky.png");
    }

    private static Tube buildTube(List<Vec3> points, List<Float> widths, float stability, float timeInTicks) {
        int count = points.size();
        float instability = Mth.clamp(1.0F - stability / STABLE_LIMIT, 0.0F, MAX_INSTABILITY);
        float middle = (count - 1) / 2.0F;
        float[] centers = new float[count * 3];
        float[] radii = new float[count];
        for (int i = 0; i < count; i++) {
            Vec3 point = points.get(i);
            float time = timeInTicks + (i - middle) * PHASE_PER_POINT;
            float amplitude = WOBBLE_AMPLITUDE * instability;
            centers[i * 3] = (float) point.x + Mth.sin(time / WOBBLE_X_DIVISOR) * amplitude;
            centers[i * 3 + 1] = (float) point.y + Mth.sin(time / WOBBLE_Y_DIVISOR) * amplitude;
            centers[i * 3 + 2] = (float) point.z + Mth.sin(time / WOBBLE_Z_DIVISOR) * amplitude;
            radii[i] = widths.get(i) * (1.0F - Mth.sin(time / PULSE_DIVISOR) * amplitude);
        }
        float[] offsets = new float[count * SIDES * 3];
        Vector3f direction = new Vector3f();
        Vector3f reference = new Vector3f();
        Vector3f across = new Vector3f();
        Vector3f along = new Vector3f();
        for (int i = 0; i < count; i++) {
            int previous = Math.max(i - 1, 0);
            int next = Math.min(i + 1, count - 1);
            direction.set(centers[next * 3] - centers[previous * 3], centers[next * 3 + 1] - centers[previous * 3 + 1], centers[next * 3 + 2] - centers[previous * 3 + 2]);
            if (direction.length() < MIN_DIRECTION_LENGTH) {
                direction.set(0.0F, 1.0F, 0.0F);
            }
            direction.normalize();
            reference.set(Math.abs(direction.y) > VERTICAL_THRESHOLD ? 1.0F : 0.0F, Math.abs(direction.y) > VERTICAL_THRESHOLD ? 0.0F : 1.0F, 0.0F);
            reference.cross(direction, across).normalize();
            direction.cross(across, along);
            for (int side = 0; side < SIDES; side++) {
                float angle = Mth.TWO_PI * side / SIDES;
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);
                int base = (i * SIDES + side) * 3;
                offsets[base] = across.x * cos + along.x * sin;
                offsets[base + 1] = across.y * cos + along.y * sin;
                offsets[base + 2] = across.z * cos + along.z * sin;
            }
        }
        return new Tube(count, centers, radii, offsets);
    }

    private record Tube(int count, float[] centers, float[] radii, float[] offsets) {
        void write(VertexConsumer buffer, Matrix4f matrix, float scale, int color) {
            for (int i = 0; i < count - 1; i++) {
                for (int side = 0; side < SIDES; side++) {
                    int nextSide = (side + 1) % SIDES;
                    vertex(buffer, matrix, i, side, scale, color);
                    vertex(buffer, matrix, i, nextSide, scale, color);
                    vertex(buffer, matrix, i + 1, nextSide, scale, color);
                    vertex(buffer, matrix, i + 1, side, scale, color);
                }
            }
        }

        private void vertex(VertexConsumer buffer, Matrix4f matrix, int ring, int side, float scale, int color) {
            int offset = (ring * SIDES + side) * 3;
            float radius = radii[ring] * scale;
            buffer.vertex(matrix, centers[ring * 3] + offsets[offset] * radius, centers[ring * 3 + 1] + offsets[offset + 1] * radius,
                    centers[ring * 3 + 2] + offsets[offset + 2] * radius).color(color).endVertex();
        }
    }
}
