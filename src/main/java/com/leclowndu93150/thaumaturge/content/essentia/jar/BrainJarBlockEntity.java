package com.leclowndu93150.thaumaturge.content.essentia.jar;

import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.mixin.world.entity.ExperienceOrbAccessor;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BrainJarBlockEntity extends AbstractSyncedBlockEntity {
    public static final int CAPACITY = 2000;
    private static final String EXPERIENCE = "XP";
    private int experience;
    private int eatDelay;
    private long nextSigh = -1;

    static void clientTick(Level level, BlockPos pos, BlockState state, BrainJarBlockEntity jar) {
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, true) == null) return;
        long time = level.getGameTime();
        if (jar.nextSigh < 0) jar.nextSigh = time + 30;
        if (time < jar.nextSigh) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TTSounds.BRAIN.get(),
                SoundSource.AMBIENT, 0.15F, 0.8F + level.random.nextFloat() * 0.4F, false);
        jar.nextSigh = time + 100 + level.random.nextInt(500);
    }

    public BrainJarBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.BRAIN_JAR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BrainJarBlockEntity jar) {
        if (jar.eatDelay > 0) {
            jar.eatDelay--;
            return;
        }
        if (jar.experience == CAPACITY) {
            if (level instanceof ServerLevel server && level.getGameTime() % 20 == 0) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0);
            }
            return;
        }
        jar.collect(level);
    }

    private void collect(Level level) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        ExperienceOrb nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        AABB collectionBox = new AABB(worldPosition).inflate(0.1);
        int previous = experience;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(worldPosition).inflate(8))) {
            if (collectionBox.intersects(orb.getBoundingBox())) {
                var stack = (ExperienceOrbAccessor) orb;
                int value = orb.getValue();
                if (value <= 0) continue;
                int collected = Math.min(stack.thaumaturge$getCount(), (CAPACITY - experience) / value);
                if (collected == 0) continue;
                int remaining = stack.thaumaturge$getCount() - collected;
                experience += collected * value;
                if (remaining == 0) orb.discard();
                else stack.thaumaturge$setCount(remaining);
                level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.1F, 1);
            } else {
                double distance = center.distanceTo(orb.position());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = orb;
                }
            }
        }
        if (experience != previous) setChangedAndSync();
        if (nearest != null && nearestDistance > 0 && experience < CAPACITY) {
            double falloff = 1 - nearestDistance / 25;
            Vec3 pull = center.subtract(nearest.position()).scale(falloff * falloff / nearestDistance);
            nearest.setDeltaMovement(nearest.getDeltaMovement().add(pull.x * 0.3, pull.y * 0.5, pull.z * 0.3));
        }
    }

    public int experience() {
        return experience;
    }

    void release(ServerLevel level) {
        eatDelay = 40;
        int released = level.random.nextInt(Math.min(experience, 63) + 1);
        if (released > 0) {
            experience -= released;
            ExperienceOrb.award(level, Vec3.atCenterOf(worldPosition), released);
            setChangedAndSync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(EXPERIENCE, experience);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        experience = Mth.clamp(tag.getInt(EXPERIENCE), 0, CAPACITY);
    }
}
