package com.leclowndu93150.thaumaturge.content.crucible;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Forge port of CE CrucibleFx using vanilla particles until custom Effects pipeline lands. */
public final class CrucibleFx {
    private static final double FROTH_INSET = 0.2;
    private static final double FROTH_SPAN = 0.6;
    private static final int FROTH_DOWN_PASSES = 2;
    private static final int BUBBLE_ODDS = 6;
    private static final int BUBBLE_CELL_MIN = 5;
    private static final int BUBBLE_CELL_RANGE = 22;
    private static final double BUBBLE_CELLS_PER_BLOCK = 32.0;
    private static final double BUBBLE_CELL_OFFSET = 1.0 / 64.0;
    private static final double BUBBLE_RISE = 0.05;
    private static final double BOIL_INSET = 0.2;
    private static final double BOIL_SPAN = 0.6;
    private static final double BOIL_RISE = 0.1;
    private static final int BOIL_EFFECT_COUNT = 10;
    private static final double BAMF_LIFT = 0.75;

    private CrucibleFx() {}

    public static void froth(ServerLevel level, BlockPos pos, float fluidHeight, boolean overflowing) {
        RandomSource random = level.getRandom();
        double x = pos.getX() + FROTH_INSET + random.nextFloat() * FROTH_SPAN;
        double y = pos.getY() + fluidHeight;
        double z = pos.getZ() + FROTH_INSET + random.nextFloat() * FROTH_SPAN;
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 1, 0.01, 0.02, 0.01, 0.0);
        if (!overflowing) {
            return;
        }
        double top = pos.getY() + 1.0;
        for (int pass = 0; pass < FROTH_DOWN_PASSES; pass++) {
            level.sendParticles(ParticleTypes.DRIPPING_WATER, pos.getX(), top, pos.getZ() + random.nextFloat(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.DRIPPING_WATER, pos.getX() + 1.0, top, pos.getZ() + random.nextFloat(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.DRIPPING_WATER, pos.getX() + random.nextFloat(), top, pos.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.DRIPPING_WATER, pos.getX() + random.nextFloat(), top, pos.getZ() + 1.0, 1, 0, 0, 0, 0);
        }
    }

    public static void bubble(ServerLevel level, BlockPos pos, float fluidHeight, AspectList aspects) {
        RandomSource random = level.getRandom();
        if (aspects.isEmpty() || random.nextInt(BUBBLE_ODDS) != 0) {
            return;
        }
        double cellX = BUBBLE_CELL_MIN + random.nextInt(BUBBLE_CELL_RANGE);
        double cellZ = BUBBLE_CELL_MIN + random.nextInt(BUBBLE_CELL_RANGE);
        Vector3f color = randomColor(aspects, random);
        double x = pos.getX() + cellX / BUBBLE_CELLS_PER_BLOCK + BUBBLE_CELL_OFFSET;
        double y = pos.getY() + BUBBLE_RISE + fluidHeight;
        double z = pos.getZ() + cellZ / BUBBLE_CELLS_PER_BLOCK + BUBBLE_CELL_OFFSET;
        level.sendParticles(new DustParticleOptions(color, 0.6F), x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
        level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
    }

    public static void boilBurst(ServerLevel level, BlockPos pos, float fluidHeight, AspectList aspects, int intensity) {
        RandomSource random = level.getRandom();
        double surface = pos.getY() + BOIL_RISE + fluidHeight;
        int count = Math.max(1, BOIL_EFFECT_COUNT * Math.max(1, intensity));
        for (int effect = 0; effect < count; effect++) {
            Vector3f color = aspects.isEmpty() ? new Vector3f(1.0F, 1.0F, 1.0F) : randomColor(aspects, random);
            double x = pos.getX() + BOIL_INSET + random.nextFloat() * BOIL_SPAN;
            double z = pos.getZ() + BOIL_INSET + random.nextFloat() * BOIL_SPAN;
            level.sendParticles(new DustParticleOptions(color, 0.85F), x, surface, z, 1, 0.02, 0.05, 0.02, 0.0);
            level.sendParticles(ParticleTypes.SPLASH, x, surface, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    public static void craftComplete(ServerLevel level, BlockPos pos) {
        Vec3 at = Vec3.atCenterOf(pos).add(0.0, BAMF_LIFT, 0.0);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 14, 0.2, 0.15, 0.2, 0.02);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 20, 0.2, 0.15, 0.2, 0.02);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.5F, 0.1F, 0.6F), 1.0F), at.x, at.y, at.z, 12, 0.15, 0.1, 0.15, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.01);
        level.sendParticles(TTParticles.WISP.get(), at.x, at.y, at.z, 10, 0.15, 0.1, 0.15, 0.01);
    }

    private static Vector3f randomColor(AspectList aspects, RandomSource random) {
        AspectInstance picked = aspects.entries().get(random.nextInt(aspects.size()));
        IAspect aspect = TTAspects.get(picked.aspectId());
        int rgb = aspect == null ? 0xFFFFFF : aspect.color();
        return new Vector3f(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
    }
}
