package com.leclowndu93150.thaumaturge.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Forge 1.20.1 base for CE sheet particles: delay, moon wind, drift and per-frame UVs from a {@link ParticleSheet}. */
public abstract class TTParticle extends SingleQuadParticle {
    private static final int MOON_PHASE_COUNT = 8;
    private static final double MOON_PHASE_STEP_RADIANS = Math.PI / 4.0;
    private static final float WIND_STRENGTH = 0.1F;
    private static final float HEADING_JITTER_RADIANS = 0.17F;

    protected final ParticleSheet sheet;
    protected int frame;
    private int delay;
    private double moonPushX;
    private double moonPushZ;

    protected TTParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, ParticleSheet sheet) {
        super(level, x, y, z, vx, vy, vz);
        this.sheet = sheet;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.quadSize = 0.1F;
        this.setSize(0.1F, 0.1F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.delay > 0) {
            this.delay--;
            return;
        }
        this.xd += this.moonPushX;
        this.zd += this.moonPushZ;
        super.tick();
        if (!this.removed) {
            update();
        }
    }

    protected abstract void update();

    protected float progress() {
        return Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
    }

    protected void setDelay(int delay) {
        this.delay = Math.max(0, delay);
        if (this.delay > 0) {
            this.alpha = 0.0F;
        }
    }

    protected void setMoonWind(double scale) {
        int phase = Math.floorMod(this.level.getMoonPhase(), MOON_PHASE_COUNT);
        float heading = (float) (phase * MOON_PHASE_STEP_RADIANS) + this.random.nextFloat() * HEADING_JITTER_RADIANS;
        this.moonPushX = Mth.cos(heading) * WIND_STRENGTH * scale;
        this.moonPushZ = Mth.sin(heading) * WIND_STRENGTH * scale;
    }

    protected void drift(float strengthX, float strengthY, float strengthZ) {
        if (strengthX != 0.0F) {
            this.xd += this.random.nextGaussian() * strengthX;
        }
        if (strengthY != 0.0F) {
            this.yd += this.random.nextGaussian() * strengthY;
        }
        if (strengthZ != 0.0F) {
            this.zd += this.random.nextGaussian() * strengthZ;
        }
    }

    protected void frame(int index) {
        this.frame = Mth.clamp(index, 0, this.sheet.frames() - 1);
    }

    protected void setColor(int color) {
        this.rCol = (color >> 16 & 0xFF) / 255.0F;
        this.gCol = (color >> 8 & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
    }

    @Override
    protected float getU0() {
        return this.sheet.u0(this.frame);
    }

    @Override
    protected float getU1() {
        return this.sheet.u1(this.frame);
    }

    @Override
    protected float getV0() {
        return 0.0F;
    }

    @Override
    protected float getV1() {
        return 1.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.sheet.renderType();
    }

    /** Draws the quad with an explicit orientation instead of the camera's (vanilla {@code SingleQuadParticle.render} body). */
    protected void renderQuad(VertexConsumer buffer, Camera camera, Quaternionf rotation, float offsetX, float offsetY, float offsetZ, float partialTick) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x()) + offsetX;
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y()) + offsetY;
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z()) + offsetZ;
        Vector3f[] corners = {new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float size = this.getQuadSize(partialTick);
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.mul(size);
            corner.add(x, y, z);
        }
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int light = getLightColor(partialTick);
        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }
}
