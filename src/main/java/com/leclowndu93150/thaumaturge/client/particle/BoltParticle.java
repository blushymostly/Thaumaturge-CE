package com.leclowndu93150.thaumaturge.client.particle;

import com.leclowndu93150.thaumaturge.client.particle.support.BoltLineShape;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BoltParticle extends TTParticle {
    private static final ParticleSheet SHEET = ParticleSheet.sheet("bolt");
    private static final int BEADS_PER_SECTION = 4;
    private final BoltLineShape shape;
    private final int seed;
    private final AABB bounds;

    private BoltParticle(ClientLevel level, double x, double y, double z, BoltParticleOptions options) {
        super(level, x, y, z, 0, 0, 0, SHEET);
        Vec3 target = options.target();
        seed = random.nextInt(1000);
        shape = new BoltLineShape(x, y, z, target.x, target.y, target.z, seed, random.nextInt(8) * Math.PI);
        bounds = new AABB(new Vec3(x, y, z), target).inflate(0.5);
        lifetime = 3;
        quadSize = Mth.clamp(options.width(), 0.01F, 1) / 6;
        frame(seed % 16);
    }

    @Override
    protected void update() {
        frame((age + seed) % 16);
        shape.refreshJitter(age);
    }

    @Override
    public AABB getBoundingBox() {
        return bounds == null ? super.getBoundingBox() : bounds;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        double sway = time / 10.0;
        alpha = Mth.clamp(1 - time / lifetime, 0.1F, 1);
        for (int section = 0; section < shape.sections(); section++) {
            for (int bead = 0; bead < BEADS_PER_SECTION; bead++) {
                double t = (double) bead / BEADS_PER_SECTION;
                renderQuad(buffer, camera, camera.rotation(), (float) (coordinate(section, t, 0, sway) - x), (float) (coordinate(section, t, 1, sway) - y),
                        (float) (coordinate(section, t, 2, sway) - z), partialTick);
            }
        }
    }

    private double coordinate(int section, double t, int axis, double sway) {
        return Mth.lerp(t, shape.coordinate(section, axis, sway), shape.coordinate(section + 1, axis, sway));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static final class Provider implements ParticleProvider<BoltParticleOptions> {
        @Override
        public Particle createParticle(BoltParticleOptions options, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new BoltParticle(level, x, y, z, options);
        }
    }
}
