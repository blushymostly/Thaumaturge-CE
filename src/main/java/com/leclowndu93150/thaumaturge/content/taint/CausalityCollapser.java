package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class CausalityCollapser extends ThrowableItemProjectile {
    public CausalityCollapser(EntityType<? extends CausalityCollapser> type, Level level) {
        super(type, level);
    }

    private CausalityCollapser(Level level, Player player) {
        super(TTEntities.CAUSALITY_COLLAPSER.get(), player, level);
    }

    @Override protected Item getDefaultItem() { return TTBulkItems.ITEMS.get("causality_collapser").get(); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) level().addParticle(ParticleTypes.WITCH, getX(), getY(), getZ(), 0, 0, 0);
        else if (tickCount > 200) discard();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel level)) return;
        level.explode(this, getX(), getY(), getZ(), 2, Level.ExplosionInteraction.MOB);
        level.getEntitiesOfClass(FluxRift.class, getBoundingBox().inflate(3)).forEach(FluxRift::beginCollapse);
        discard();
    }

    public static final class ChargeItem extends Item {
        public ChargeItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                var projectile = new CausalityCollapser(level, player);
                projectile.setItem(stack);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 0.8F, 2.0F);
                if (level.addFreshEntity(projectile)) {
                    level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.9F);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
