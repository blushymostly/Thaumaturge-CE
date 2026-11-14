package com.leclowndu93150.thaumaturge.content.taint.flux;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.leclowndu93150.thaumaturge.registry.TTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockFluxGoo extends LiquidBlock implements PhysicalFluxBlock {
    public BlockFluxGoo(Properties properties) {
        super(TTFluids.GOO_SOURCE, properties);
    }

    @Override public int fluxAmount(BlockState state) { return state.getFluidState().getAmount(); }

    @Override
    public BlockState withFluxAmount(int amount) {
        return defaultBlockState().setValue(LEVEL, PhysicalFlux.MAX_QUANTA - Mth.clamp(amount, 1, PhysicalFlux.MAX_QUANTA));
    }

    @Override
    public void scheduleFluxTick(ServerLevel level, BlockPos pos) {
        var fluid = level.getFluidState(pos).getType();
        level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
    }

    @Override public float auraFloorPerQuantum() { return 0.5F; }
    @Override public float taintWeightPerQuantum() { return 1.0F; }
    @Override public int outbreakCost() { return 0; }
    @Override public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int amount = fluxAmount(state);
        if (entity.blockPosition().equals(pos)) {
            double drag = 1.0 - 0.5 * amount / PhysicalFlux.MAX_QUANTA;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(drag, 1, drag));
        }
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            var current = living.getEffect(TTMobEffects.VIS_EXHAUST.get());
            int amplifier = (amount - 1) / 3;
            if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 580) {
                living.addEffect(new MobEffectInstance(TTMobEffects.VIS_EXHAUST.get(), 600, amplifier, true, true, false));
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel server) PhysicalFluxIndex.observe(server, pos);
    }
}
