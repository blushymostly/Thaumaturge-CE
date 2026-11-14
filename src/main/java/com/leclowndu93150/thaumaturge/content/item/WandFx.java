package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class WandFx {
    private WandFx() {}

    static Vec3 tip(Player player, InteractionHand hand) {
        Vec3 forward = player.getLookAngle();
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 side = forward.cross(new Vec3(0, 1, 0)).normalize().scale(arm == HumanoidArm.RIGHT ? 0.22 : -0.22);
        return player.getEyePosition().add(forward.scale(0.65)).add(side).add(0, -0.28, 0);
    }

    static void stream(ServerLevel level, Vec3 source, Vec3 target, int color) {
        double length = source.distanceTo(target);
        int count = Math.max(2, Math.min(6, (int) Math.ceil(length * 3)));
        var velocity = target.subtract(source).normalize().scale(0.13);
        var particle = new SparkleParticleOptions(color, 0.45F, 0, 0.91F, 0, 4, false);
        for (int i = 0; i < count; i++) {
            var point = source.lerp(target, (double) i / count);
            level.sendParticles(particle, point.x, point.y, point.z, 0, velocity.x, velocity.y, velocity.z, 1);
        }
    }
}
