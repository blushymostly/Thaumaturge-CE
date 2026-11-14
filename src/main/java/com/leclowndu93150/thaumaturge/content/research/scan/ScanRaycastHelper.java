package com.leclowndu93150.thaumaturge.content.research.scan;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

public final class ScanRaycastHelper {
    private static final double DEFAULT_BLOCK_REACH = 5.0;
    private static final double DEFAULT_ENTITY_REACH = 5.0;

    private ScanRaycastHelper() {}

    public static HitResult performRaycast(Player player) {
        return performRaycast(player, ClipContext.Fluid.ANY);
    }

    public static HitResult performRaycast(Player player, ClipContext.Fluid fluid) {
        double blockReach = attributeOr(player, ForgeMod.BLOCK_REACH.get(), DEFAULT_BLOCK_REACH);
        double entityReach = attributeOr(player, ForgeMod.ENTITY_REACH.get(), DEFAULT_ENTITY_REACH);
        return performRaycast(player.level(), player.getEyePosition(1.0F), player.getViewVector(1.0F), blockReach, entityReach, fluid, player);
    }

    private static double attributeOr(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, double fallback) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return fallback;
        }
        return instance.getValue();
    }

    private static HitResult performRaycast(Level level, Vec3 start, Vec3 direction, double blockReach, double entityReach, ClipContext.Fluid fluidFilter, Entity entity) {
        BlockHitResult blockResult = level.clip(new ClipContext(start, start.add(direction.scale(blockReach)), ClipContext.Block.OUTLINE, fluidFilter, entity));
        EntityHitResult entityResult = clipEntity(start, direction, entityReach, entity);

        if (blockResult.getType() != HitResult.Type.MISS && entityResult != null) {
            double blockDistance = blockResult.getLocation().distanceToSqr(start);
            double entityDistance = entityResult.getLocation().distanceToSqr(start);
            return blockDistance < entityDistance ? blockResult : entityResult;
        }
        if (blockResult.getType() != HitResult.Type.MISS) {
            return blockResult;
        }
        return entityResult != null ? entityResult : blockResult;
    }

    private static EntityHitResult clipEntity(Vec3 start, Vec3 direction, double entityReach, Entity entity) {
        AABB box = entity.getBoundingBox().expandTowards(direction.scale(entityReach)).inflate(1.0F, 1.0F, 1.0F);
        return ProjectileUtil.getEntityHitResult(entity, start, start.add(direction.scale(entityReach)), box, e -> !e.isSpectator() && e.isPickable(), entityReach * entityReach);
    }
}
