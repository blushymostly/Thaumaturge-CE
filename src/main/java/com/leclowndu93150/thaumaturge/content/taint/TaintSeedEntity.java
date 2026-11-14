package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class TaintSeedEntity extends PathfinderMob {
    public TaintSeedEntity(EntityType<? extends TaintSeedEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive() || tickCount % 40 != 0) return;
        if (EtherealBloom.protects(server, blockPosition())) {
            hurt(server.damageSources().starve(), 1.0F);
            return;
        }
        if (ThaumaturgeCommonConfig.WUSS_MODE.get() || !server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        var target = blockPosition().offset(random.nextIntBetweenInclusive(-4, 4), random.nextIntBetweenInclusive(-2, 0),
                random.nextIntBetweenInclusive(-4, 4));
        if (!server.hasChunkAt(target) || EtherealBloom.protects(server, target) || !server.isEmptyBlock(target.above())) return;
        var state = server.getBlockState(target);
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
            server.setBlockAndUpdate(target, TTBulkContent.BLOCKS.get("taint_soil").get().defaultBlockState());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
