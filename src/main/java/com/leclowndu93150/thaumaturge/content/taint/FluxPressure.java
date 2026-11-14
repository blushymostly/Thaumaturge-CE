package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class FluxPressure {
    private static final Map<ServerLevel, List<Rain>> RAIN = new WeakHashMap<>();

    private FluxPressure() {}

    public static void roll(ServerLevel level, ChunkPos chunk, float flux, float scaledBase) {
        if (ThaumaturgeCommonConfig.WUSS_MODE.get() || flux <= 0.75F * scaledBase) return;
        var random = level.random;
        if (random.nextFloat() < flux / 5000.0F) {
            FluxRift.trySpawn(level, chunk.getBlockAt(random.nextInt(16), 0, random.nextInt(16)));
        } else if (ThaumaturgeCommonConfig.FLUX_PRESSURE_EVENTS.get()
                && random.nextFloat() < flux / (100.0F * Math.max(1.0F, scaledBase))) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    chunk.getBlockAt(random.nextInt(16), 0, random.nextInt(16)));
            if (!level.canSeeSky(pos) || EtherealBloom.protects(level, pos)) return;
            if (random.nextInt(6) == 0) {
                if (flux >= 40 && rain(level, pos.above(20))) AuraHelper.drainFlux(level, pos, 40, false);
            } else if (flux >= 25 && lightning(level, pos)) {
                AuraHelper.drainFlux(level, pos, 25, false);
            }
        }
    }

    static boolean lightning(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || EtherealBloom.protects(level, pos)) return false;
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.setVisualOnly(true);
        bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (!level.addFreshEntity(bolt)) return false;
        var random = level.random;
        for (int i = 0; i < 4; i++) {
            BlockPos at = pos.offset(random.nextIntBetweenInclusive(-2, 2), 0, random.nextIntBetweenInclusive(-2, 2));
            if (level.hasChunkAt(at)) {
                at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at);
                if (!EtherealBloom.protects(level, at)) PhysicalFlux.placeGoo(level, at, 1);
            }
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3),
                target -> target.isAlive() && target.distanceToSqr(bolt) <= 9.0 && !(target instanceof TaintSeedEntity))) {
            target.hurt(level.damageSources().magic(), 3);
            target.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT.get(), 200));
        }
        return true;
    }

    private static boolean rain(ServerLevel level, BlockPos center) {
        if (level.isOutsideBuildHeight(center)) return false;
        var active = RAIN.computeIfAbsent(level, unused -> new ArrayList<>());
        if (active.stream().anyMatch(rain -> rain.center().distSqr(center) <= 32 * 32)) return false;
        active.add(new Rain(center, level.getGameTime() + 20L * (30 + level.random.nextInt(10))));
        return true;
    }

    public static void tick(ServerLevel level) {
        var active = RAIN.get(level);
        if (active == null) return;
        if (ThaumaturgeCommonConfig.WUSS_MODE.get() || !ThaumaturgeCommonConfig.FLUX_PRESSURE_EVENTS.get()) {
            RAIN.remove(level);
            return;
        }
        long now = level.getGameTime();
        active.removeIf(rain -> now >= rain.end() || !level.hasChunkAt(rain.center()));
        if (active.isEmpty()) {
            RAIN.remove(level);
            return;
        }
        if (now % 5 != 0) return;
        for (Rain rain : active) {
            BlockPos center = rain.center();
            level.sendParticles(ParticleTypes.WITCH, center.getX() + 0.5, center.getY() + 0.25, center.getZ() + 0.5, 6, 12, 2, 12, 0.02);
            if (now % 20 != 0) continue;
            BlockPos column = center.offset(level.random.nextIntBetweenInclusive(-16, 16), 0, level.random.nextIntBetweenInclusive(-16, 16));
            if (!level.hasChunkAt(column)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            if (!EtherealBloom.protects(level, pos) && AuraHelper.getFlux(level, pos) >= 1.0F
                    && PhysicalFlux.placeGoo(level, pos, PhysicalFlux.MAX_QUANTA)) {
                AuraHelper.drainFlux(level, pos, 1.0F, false);
            }
        }
    }

    private record Rain(BlockPos center, long end) {}
}
