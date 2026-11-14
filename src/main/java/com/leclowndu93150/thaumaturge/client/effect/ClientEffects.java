package com.leclowndu93150.thaumaturge.client.effect;

import com.leclowndu93150.thaumaturge.content.particle.BlockRunesParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Forge port of CE ClientEffects (scan sparkles and rune glyphs). */
public final class ClientEffects {
    private static final float CHANNEL_MAX = 255.0F;

    private static final double RUNE_CENTRE = 0.5;
    private static final float RUNE_CHANNEL_MIN = 0.3F;
    private static final float RUNE_CHANNEL_SPREAD = 0.7F;
    private static final float RUNE_GRAVITY = 0.03F;
    private static final float ENTITY_RUNE_TICKS_PER_HEIGHT = 15.0F;
    private static final int BLOCK_RUNE_TICKS = 15;
    private static final double BLOCK_RUNE_Y_OFFSET = 0.25;
    private static final float EYE_HEIGHT_DIVISOR = 2.0F;

    private static final float SCAN_SCALE_BASE = 0.4F;
    private static final float SCAN_SCALE_DEVIATION = 0.1F;
    private static final int SCAN_DELAY_RANGE = 10;
    private static final float SCAN_DECAY = 1.0F;
    private static final float SCAN_GRAVITY = 0.0F;
    private static final int SCAN_BASE_AGE = 4;
    private static final double SURFACE_POINTS_PER_AREA = 4.0;

    private ClientEffects() {}

    public static void scanSparkles(ClientLevel level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            return;
        }
        scanSparkles(level, shape.bounds().move(pos));
    }

    public static void scanSparkles(Entity entity) {
        scanSparkles(entity.level(), entity.getBoundingBox());
    }

    public static void scanSparkles(Level level, AABB box) {
        RandomSource random = level.getRandom();
        double areaX = box.getYsize() * box.getZsize();
        double areaY = box.getXsize() * box.getZsize();
        double areaZ = box.getXsize() * box.getYsize();
        int points = Mth.ceil(2.0 * (areaX + areaY + areaZ) * SURFACE_POINTS_PER_AREA);
        for (int left = points; left > 0; left--) {
            Vec3 spot = sampleSurface(box, areaX, areaY, areaZ, random);
            float red = channel(random, 16, 32);
            float green = channel(random, 132, 165);
            float blue = channel(random, 223, 239);
            float jitter = (float) random.nextGaussian() * SCAN_SCALE_DEVIATION;
            int delay = random.nextInt(SCAN_DELAY_RANGE);
            sparkle(spot.x, spot.y, spot.z, 0.0, 0.0, 0.0, red, green, blue, SCAN_SCALE_BASE + jitter, delay, SCAN_DECAY, SCAN_GRAVITY, SCAN_BASE_AGE);
        }
    }

    public static void runeGlyph(ClientLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        runeGlyph(pos.getX(), pos.getY() + BLOCK_RUNE_Y_OFFSET, pos.getZ(), runeChannel(random), 0.0F, runeChannel(random), BLOCK_RUNE_TICKS, RUNE_GRAVITY);
    }

    public static void runeGlyph(Entity entity) {
        RandomSource random = entity.level().getRandom();
        double x = entity.getX() - RUNE_CENTRE;
        double y = entity.getY() + entity.getEyeHeight() / EYE_HEIGHT_DIVISOR;
        double z = entity.getZ() - RUNE_CENTRE;
        int lifetime = (int) (entity.getBoundingBox().getYsize() * ENTITY_RUNE_TICKS_PER_HEIGHT);
        runeGlyph(x, y, z, runeChannel(random), 0.0F, runeChannel(random), lifetime, RUNE_GRAVITY);
    }

    public static void runeGlyph(double x, double y, double z, float r, float g, float b, int duration, float gravity) {
        submit(new BlockRunesParticleOptions(r, g, b, duration, gravity, false), RUNE_CENTRE + x, RUNE_CENTRE + y, RUNE_CENTRE + z, 0.0, 0.0, 0.0);
    }

    public static void sparkle(double x, double y, double z, double vx, double vy, double vz, float r, float g, float b, float scale, int delay, float decay, float gravity, int baseAge) {
        submit(new SparkleParticleOptions(opaque(r, g, b), scale, delay, decay, gravity, baseAge, true), x, y, z, vx, vy, vz);
    }

    public static void nitorCore(Level level, double x, double y, double z, int color) {
        float r = ((color >> 16) & 0xFF) / CHANNEL_MAX;
        float g = ((color >> 8) & 0xFF) / CHANNEL_MAX;
        float b = (color & 0xFF) / CHANNEL_MAX;
        sparkle(x, y, z, 0.0, 0.0, 0.0, r, g, b, 0.55F, 0, 0.85F, 0.0F, 8);
    }

    public static void nitorFlames(Level level, double x, double y, double z, double vx, double vy, double vz, int color) {
        float r = ((color >> 16) & 0xFF) / CHANNEL_MAX;
        float g = ((color >> 8) & 0xFF) / CHANNEL_MAX;
        float b = (color & 0xFF) / CHANNEL_MAX;
        sparkle(x, y, z, vx, vy, vz, r, g, b, 0.28F + level.getRandom().nextFloat() * 0.12F, 0, 1.0F, -0.02F, 6);
    }

    private static Vec3 sampleSurface(AABB box, double areaX, double areaY, double areaZ, RandomSource random) {
        double pick = random.nextDouble() * (areaX + areaY + areaZ);
        boolean high = random.nextBoolean();
        double u = random.nextDouble();
        double v = random.nextDouble();
        if (pick < areaX) {
            return new Vec3(high ? box.maxX : box.minX, Mth.lerp(u, box.minY, box.maxY), Mth.lerp(v, box.minZ, box.maxZ));
        }
        if (pick < areaX + areaY) {
            return new Vec3(Mth.lerp(u, box.minX, box.maxX), high ? box.maxY : box.minY, Mth.lerp(v, box.minZ, box.maxZ));
        }
        return new Vec3(Mth.lerp(u, box.minX, box.maxX), Mth.lerp(v, box.minY, box.maxY), high ? box.maxZ : box.minZ);
    }

    private static void submit(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        client.particleEngine.createParticle(options, x, y, z, vx, vy, vz);
    }

    private static float runeChannel(RandomSource random) {
        return RUNE_CHANNEL_MIN + random.nextFloat() * RUNE_CHANNEL_SPREAD;
    }

    private static float channel(RandomSource random, int min, int max) {
        return (min + random.nextInt(max - min + 1)) / CHANNEL_MAX;
    }

    private static int opaque(float r, float g, float b) {
        return 0xFF000000 | (Mth.clamp((int) (r * CHANNEL_MAX), 0, 255) << 16) | (Mth.clamp((int) (g * CHANNEL_MAX), 0, 255) << 8) | Mth.clamp((int) (b * CHANNEL_MAX), 0, 255);
    }
}
