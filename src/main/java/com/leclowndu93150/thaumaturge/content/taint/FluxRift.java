package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class FluxRift extends Entity {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(FluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STABILITY = SynchedEntityData.defineId(FluxRift.class, EntityDataSerializers.FLOAT);
    private int collapseSize;

    public FluxRift(EntityType<? extends FluxRift> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SIZE, 5);
        entityData.define(SEED, 0);
        entityData.define(STABILITY, 0.0F);
    }

    public int size() { return entityData.get(SIZE); }
    public int seed() { return entityData.get(SEED); }
    public float stability() { return entityData.get(STABILITY); }

    public void nudgeStability() {
        entityData.set(STABILITY, Math.min(100, stability() + 0.125F));
    }

    @Override public boolean isPickable() { return isAlive(); }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(size() / 15.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (collapseSize > 0) {
            collapse(level);
            return;
        }
        if (tickCount % 120 == 0) entityData.set(STABILITY, Math.max(-100.0F, stability() - 0.2F));
        if (tickCount % 600 != 0) return;
        float cost = (float) Math.sqrt(2.0 * size());
        if (size() < 100 && stability() <= 50 && AuraHelper.getFlux(level, blockPosition()) >= cost) {
            AuraHelper.drainFlux(level, blockPosition(), cost, false);
            entityData.set(SIZE, size() + 1);
        }
        if (stability() < 0 && random.nextInt(1000) < -stability() + size()
                && FluxPressure.lightning(level, blockPosition())) {
            entityData.set(STABILITY, Math.min(100.0F, stability() + 10));
        }
    }

    public void beginCollapse() {
        if (collapseSize == 0) collapseSize = size();
    }

    private void collapse(ServerLevel level) {
        if (random.nextBoolean()) AuraHelper.addVis(level, blockPosition(), 1.0F);
        else AuraHelper.polluteAura(level, blockPosition(), 1.0F, false);
        if (random.nextInt(10) == 0) {
            level.explode(this, getX() + random.nextGaussian() * 2, getY() + random.nextGaussian() * 2,
                    getZ() + random.nextGaussian() * 2, random.nextFloat() * 0.5F, Level.ExplosionInteraction.NONE);
        }
        int remaining = size() - 1;
        if (remaining > 1) {
            entityData.set(SIZE, remaining);
            return;
        }
        int strength = (int) Math.sqrt(collapseSize);
        if (random.nextInt(100) < strength) spawnAtLocation(TTItems.PRIMORDIAL_PEARL.get());
        for (int i = 0; i < strength; i++) spawnAtLocation(TTBulkItems.ITEMS.get("void_seed").get());
        discard();
    }

    public static boolean trySpawn(ServerLevel level, BlockPos column) {
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (!level.dimensionType().hasSkyLight()) {
            pos = column.atY(Math.max(10, level.getMinBuildHeight()));
            while (pos.getY() < level.getMaxBuildHeight() - 4 && !level.isEmptyBlock(pos)) pos = pos.above();
        }
        if (pos.getY() >= level.getMaxBuildHeight() - 4 || pos.getY() <= level.getMinBuildHeight()) return false;
        int size = (int) Math.sqrt(AuraHelper.getFlux(level, pos) * 3);
        var area = new AABB(pos).inflate(32);
        if (size <= 5 || !level.getEntitiesOfClass(FluxRift.class, area).isEmpty()) return false;
        FluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        if (rift == null) return false;
        rift.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        rift.entityData.set(SIZE, Math.min(100, size));
        rift.entityData.set(SEED, level.random.nextInt());
        if (!level.addFreshEntity(rift)) return false;
        AuraHelper.drainFlux(level, pos, size, false);
        level.players().stream().filter(player -> area.contains(player.position()))
                .forEach(player -> player.displayClientMessage(Component.translatable("warp.thaumaturge.fluxevent.3"), false));
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RiftSize", size());
        tag.putInt("RiftSeed", seed());
        tag.putFloat("Stability", stability());
        tag.putInt("CollapseSize", collapseSize);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("RiftSize")) entityData.set(SIZE, Mth.clamp(tag.getInt("RiftSize"), 1, 100));
        entityData.set(SEED, tag.getInt("RiftSeed"));
        entityData.set(STABILITY, Mth.clamp(tag.getFloat("Stability"), -100, 100));
        collapseSize = Mth.clamp(tag.getInt("CollapseSize"), 0, 100);
    }
}
