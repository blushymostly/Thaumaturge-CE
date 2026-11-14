package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.taint.EtherealBloom;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

final class NodeTypeEffects {
    private NodeTypeEffects() {}

    static boolean tick(BlockEntityNode node, ServerLevel level, BlockPos pos, int tick) {
        switch (node.kind()) {
            case PURE:
                float removed = AuraHelper.drainFlux(level, pos, 0.25F, false);
                return removed > 0 && tick % 200 == 0 && level.random.nextFloat() < 0.025F && node.erode(level, pos);
            case DARK:
                if (level.random.nextBoolean()) {
                    spawnGuard(level, pos);
                }
                break;
            case UNSTABLE:
                if (tick % 100 == 0 && level.random.nextBoolean()) shedPrimal(node, level, pos);
                break;
            case HUNGRY:
                eatBlock(level, pos);
                break;
            case TAINTED:
                spreadTaint(level, pos);
                break;
            default:
                break;
        }
        return false;
    }

    private static void shedPrimal(BlockEntityNode node, ServerLevel level, BlockPos pos) {
        var primals = node.getAspects().entries().stream().filter(entry -> {
            var aspect = com.leclowndu93150.thaumaturge.api.aspect.TTAspects.get(entry.aspectId());
            return aspect != null && aspect.isPrimal();
        }).toList();
        if (!primals.isEmpty()) {
            var chosen = primals.get(level.random.nextInt(primals.size())).aspectId();
            node.drain(chosen, 1);
            Block.popResource(level, pos, com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory.of(chosen, 1));
        }
    }

    static void attract(BlockEntityNode node, ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos).inflate(8),
                candidate -> candidate instanceof ItemEntity || candidate instanceof net.minecraft.world.entity.LivingEntity
                        && !(candidate instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            Vec3 delta = center.subtract(entity.position());
            if (delta.lengthSqr() > 64) {
                continue;
            }
            if (delta.lengthSqr() < 0.35) {
                if (level.getGameTime() % 10 == 0) {
                    if (entity instanceof ItemEntity item) {
                        var stack = item.getItem().copy();
                        node.feed(com.leclowndu93150.thaumaturge.api.aspect.AspectIndex.of(stack), level.random);
                        stack.shrink(1);
                        if (stack.isEmpty()) item.discard();
                        else item.setItem(stack);
                    } else if (entity.hurt(level.damageSources().magic(), 2) && entity instanceof net.minecraft.world.entity.LivingEntity living
                            && living.isDeadOrDying()) {
                        node.feed(com.leclowndu93150.thaumaturge.content.aspect.EntityAspects.of(living), level.random);
                    }
                }
            } else {
                Vec3 pull = delta.normalize().scale(0.055);
                entity.push(pull.x, pull.y, pull.z);
                entity.hurtMarked = true;
            }
        }
    }

    private static void eatBlock(ServerLevel level, BlockPos origin) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos pos = origin.offset(level.random.nextInt(17) - 8, level.random.nextInt(17) - 8, level.random.nextInt(17) - 8);
        if (pos.equals(origin) || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) {
            return;
        }
        var state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (!state.isAir() && state.getFluidState().isEmpty() && hardness >= 0 && hardness <= 5) {
            level.destroyBlock(pos, true);
        }
    }

    private static void spawnGuard(ServerLevel level, BlockPos pos) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 24, false) == null
                || level.getEntitiesOfClass(Zombie.class, new AABB(pos).inflate(10, 6, 10)).size() >= 4) {
            return;
        }
        BlockPos at = pos.offset(level.random.nextInt(11) - 5, level.random.nextInt(3) - 1, level.random.nextInt(11) - 5);
        if (!level.hasChunkAt(at) || !level.getBlockState(at.below()).isSolidRender(level, at.below())) {
            return;
        }
        Zombie guard = EntityType.ZOMBIE.create(level);
        if (guard != null) {
            guard.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            guard.getPersistentData().putBoolean("ThaumaturgeNodeGuard", true);
            if (guard.checkSpawnRules(level, MobSpawnType.SPAWNER) && guard.checkSpawnObstruction(level)) {
                ForgeEventFactory.onFinalizeSpawn(guard, level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null, null);
                level.addFreshEntity(guard);
            }
        }
    }

    private static void spreadTaint(ServerLevel level, BlockPos origin) {
        if (ThaumaturgeCommonConfig.WUSS_MODE.get() || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || AuraHelper.getFlux(level, origin) < 1) {
            return;
        }
        BlockPos at = origin.offset(level.random.nextInt(9) - 4, level.random.nextInt(9) - 4, level.random.nextInt(9) - 4);
        if (!level.hasChunkAt(at) || EtherealBloom.protects(level, at) || !level.getBlockState(at).is(BlockTags.DIRT)) {
            return;
        }
        var soil = TTBulkContent.BLOCKS.get("taint_soil");
        if (soil != null && level.setBlock(at, soil.get().defaultBlockState(), Block.UPDATE_ALL)) {
            AuraHelper.drainFlux(level, origin, 1, false);
        }
    }
}
