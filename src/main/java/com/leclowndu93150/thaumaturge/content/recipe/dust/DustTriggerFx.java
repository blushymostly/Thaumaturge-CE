package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerPlacement;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Server-driven sparkle effects for Salis Mundus (CE SparkleParticleOptions). */
public final class DustTriggerFx {
    private static final double HAND_HEIGHT = 0.25;
    private static final double HAND_SIDE_OFFSET = 0.3;
    private static final double HAND_LOOK_OFFSET = 0.3;
    private static final double HALF_TURN_DEGREES = 180.0;

    public static final double SPARKLE_TICKS_PER_BLOCK = 16.0;

    private static final int BURST_COUNT = 50;
    private static final int BURST_FLOATY_COUNT = 16;
    private static final double BURST_DIRECTION_DIVISOR = 6.0;
    private static final double BURST_VELOCITY_DEVIATION = 0.05;
    private static final double BURST_FLOATY_LIFT = 0.05;
    private static final double BURST_GROUNDED_LIFT = 0.15;
    private static final float BURST_FLOATY_DECAY_MIN = 0.3F;
    private static final float BURST_FLOATY_DECAY_SPAN = 0.5F;
    private static final float BURST_GROUNDED_DECAY = 0.85F;
    private static final float BURST_FLOATY_GRAVITY = 0.2F;
    private static final float BURST_GROUNDED_GRAVITY = 0.5F;
    private static final float BURST_SCALE = 0.5F;

    private static final int START_DELAY_BOUND = 5;
    private static final int SPARKLE_BASE_AGE = 16;
    private static final int COLOR_OPAQUE_RED = 0xFFFF0000;
    private static final int GREEN_MIN = 189;
    private static final int GREEN_BOUND = 67;
    private static final int BLUE_MIN = 64;
    private static final int BLUE_BOUND = 192;
    private static final int GREEN_SHIFT = 8;

    private static final AABB UNIT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final double SHELL_BOX_GROWTH = 0.1;
    private static final double SHELL_DENSITY = 20.0;
    private static final double SHELL_EDGE_COUNT = 3.0;
    private static final int SHELL_FACE_MULTIPLIER = 2;
    private static final double SHELL_CENTER = 0.5;
    private static final double SHELL_FACE_OFFSET = 0.51;
    private static final double SHELL_SPREAD = 0.6;
    private static final double SHELL_RISE = 0.0025;
    private static final float SHELL_SCALE = 0.4F;
    private static final double SHELL_SCALE_DEVIATION = 0.1;
    private static final float SHELL_DECAY = 1.0F;
    private static final float SHELL_GRAVITY = 0.01F;

    private DustTriggerFx() {}

    public static Vec3 posToHand(ServerPlayer player, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        double turns = player.getYRot() + (arm == HumanoidArm.RIGHT ? 0.0 : HALF_TURN_DEGREES);
        double radians = turns / HALF_TURN_DEGREES * Math.PI;
        AABB body = player.getBoundingBox();
        Vec3 anchor = new Vec3(player.getX(), (body.minY + body.maxY) / 2.0 + HAND_HEIGHT, player.getZ());
        Vec3 sideStep = new Vec3(-Math.cos(radians) * HAND_SIDE_OFFSET, 0.0, -Math.sin(radians) * HAND_SIDE_OFFSET);
        return anchor.add(sideStep).add(player.getLookAngle().scale(HAND_LOOK_OFFSET));
    }

    public static void emitUseBurst(ServerLevel level, ServerPlayer player, InteractionHand hand, BlockPos clicked) {
        Vec3 origin = posToHand(player, hand);
        Vec3 pull = Vec3.atCenterOf(clicked).subtract(origin);
        RandomSource random = level.getRandom();
        for (int index = 0; index < BURST_COUNT; index++) {
            spawnBurstSparkle(level, random, origin, pull, index < BURST_FLOATY_COUNT);
        }
    }

    private static void spawnBurstSparkle(ServerLevel level, RandomSource random, Vec3 origin, Vec3 pull, boolean floaty) {
        double lift = floaty ? BURST_FLOATY_LIFT : BURST_GROUNDED_LIFT;
        double driftX = pull.x / BURST_DIRECTION_DIVISOR + random.nextGaussian() * BURST_VELOCITY_DEVIATION;
        double driftY = pull.y / BURST_DIRECTION_DIVISOR + random.nextGaussian() * BURST_VELOCITY_DEVIATION + lift;
        double driftZ = pull.z / BURST_DIRECTION_DIVISOR + random.nextGaussian() * BURST_VELOCITY_DEVIATION;
        float decay = BURST_GROUNDED_DECAY;
        float gravity = BURST_GROUNDED_GRAVITY;
        if (floaty) {
            decay = BURST_FLOATY_DECAY_MIN + random.nextFloat() * BURST_FLOATY_DECAY_SPAN;
            gravity = BURST_FLOATY_GRAVITY;
        }
        int delay = random.nextInt(START_DELAY_BOUND);
        level.sendParticles(new SparkleParticleOptions(randomColor(random), BURST_SCALE, delay, decay, gravity, SPARKLE_BASE_AGE, true), origin.x, origin.y, origin.z, 0, driftX, driftY, driftZ, 1.0);
    }

    public static void emitTriggerSparkles(ServerLevel level, ServerPlayer player, BlockPos clicked, DustTrigger trigger, Vec3 start, @Nullable DustTriggerPlacement placement) {
        if (placement == null || placement.facing() == null) {
            emitBlockSparkles(level, clicked, start);
            return;
        }
        trigger.sparkle(level, player, clicked, placement).forEach(position -> emitBlockSparkles(level, position, start));
    }

    public static void emitBlockSparkles(ServerLevel level, BlockPos pos, Vec3 start) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        AABB box = shapeBounds(level, pos).inflate(SHELL_BOX_GROWTH);
        int edgeSparkles = Math.max(1, (int) Math.floor(SHELL_DENSITY * (box.getXsize() + box.getYsize() + box.getZsize()) / SHELL_EDGE_COUNT));
        RandomSource random = level.getRandom();
        MutableBlockPos neighbor = new MutableBlockPos();
        for (Direction face : Direction.values()) {
            neighbor.setWithOffset(pos, face);
            if (!isExposed(level, neighbor, face)) {
                continue;
            }
            for (int i = 0; i < edgeSparkles * SHELL_FACE_MULTIPLIER; i++) {
                emitShellSparkle(level, random, pos, box, face, start);
            }
        }
    }

    private static void emitShellSparkle(ServerLevel level, RandomSource random, BlockPos pos, AABB box, Direction face, Vec3 start) {
        double x = pos.getX() + localCoordinate(box, Axis.X, face, random);
        double y = pos.getY() + localCoordinate(box, Axis.Y, face, random);
        double z = pos.getZ() + localCoordinate(box, Axis.Z, face, random);
        double travel = Math.sqrt(start.distanceToSqr(x, y, z));
        int delay = random.nextInt(START_DELAY_BOUND) + (int) Math.floor(SPARKLE_TICKS_PER_BLOCK * travel);
        float scale = (float) (SHELL_SCALE + random.nextGaussian() * SHELL_SCALE_DEVIATION);
        level.sendParticles(new SparkleParticleOptions(randomColor(random), scale, delay, SHELL_DECAY, SHELL_GRAVITY, SPARKLE_BASE_AGE, true), x, y, z, 0, 0.0, SHELL_RISE, 0.0, 1.0);
    }

    /** Poof played when a queued swap lands (CE: {@code Effects.bamf}). */
    public static void emitSwapBamf(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.POOF, x, y, z, 18, 0.35, 0.35, 0.35, 0.03);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 36, 0.45, 0.45, 0.45, 0.55);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.25, 0.25, 0.25, 0.02);
        RandomSource random = level.getRandom();
        float pitch = 0.9F + random.nextFloat() * 0.2F;
        level.playSound(null, pos, TTSounds.POOF.get(), SoundSource.BLOCKS, 0.4F, pitch);
        for (int i = 0; i < 16; i++) {
            level.sendParticles(new SparkleParticleOptions(randomColor(random), BURST_SCALE, 0, BURST_GROUNDED_DECAY, BURST_FLOATY_GRAVITY, SPARKLE_BASE_AGE, true), x, y, z, 0,
                    (random.nextDouble() - 0.5) * 0.35,
                    random.nextDouble() * 0.25,
                    (random.nextDouble() - 0.5) * 0.35,
                    1.0);
        }
    }

    private static AABB shapeBounds(ServerLevel level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return !shape.isEmpty() ? shape.bounds() : UNIT_BOX;
    }

    private static boolean isExposed(ServerLevel level, BlockPos neighborPos, Direction face) {
        if (!level.hasChunkAt(neighborPos)) {
            return false;
        }
        BlockState occupant = level.getBlockState(neighborPos);
        boolean sealed = occupant.isSolidRender(level, neighborPos) && occupant.isFaceSturdy(level, neighborPos, face.getOpposite());
        return !sealed;
    }

    private static double localCoordinate(AABB box, Axis axis, Direction face, RandomSource random) {
        double raw = SHELL_CENTER;
        if (face.getAxis() == axis) {
            raw += SHELL_FACE_OFFSET * face.getAxisDirection().getStep();
        } else {
            raw += random.nextGaussian() * SHELL_SPREAD;
        }
        return Mth.clamp(raw, box.min(axis), box.max(axis));
    }

    private static int randomColor(RandomSource random) {
        int green = GREEN_MIN + random.nextInt(GREEN_BOUND);
        int blue = BLUE_MIN + random.nextInt(BLUE_BOUND);
        return COLOR_OPAQUE_RED | (green << GREEN_SHIFT) | blue;
    }
}
