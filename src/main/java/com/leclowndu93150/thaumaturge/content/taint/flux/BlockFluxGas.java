package com.leclowndu93150.thaumaturge.content.taint.flux;

import com.leclowndu93150.thaumaturge.content.taint.TaintSeedEntity;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public final class BlockFluxGas extends Block implements PhysicalFluxBlock, LiquidBlockContainer {
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, PhysicalFlux.MAX_QUANTA);
    private static final int TICK_DELAY = 10;

    public BlockFluxGas(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AMOUNT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT);
    }

    @Override public int fluxAmount(BlockState state) { return state.getValue(AMOUNT); }
    @Override public BlockState withFluxAmount(int amount) {
        return defaultBlockState().setValue(AMOUNT, Mth.clamp(amount, 1, PhysicalFlux.MAX_QUANTA));
    }
    @Override public void scheduleFluxTick(ServerLevel level, BlockPos pos) { level.scheduleTick(pos, this, TICK_DELAY); }
    @Override public float auraFloorPerQuantum() { return 0.25F; }
    @Override public float taintWeightPerQuantum() { return 0.35F; }
    @Override public int outbreakCost() { return 2; }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) { return neighbor.is(this); }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return fluxAmount(state) <= 2; }
    @Override public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { return false; }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) { return false; }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level instanceof ServerLevel server) {
            PhysicalFluxIndex.observe(server, pos);
            scheduleFluxTick(server, pos);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, TICK_DELAY);
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FluxFlow.spread(level, pos, state, this, Direction.UP, random);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && !living.isInvertedHealAndHarm()
                && !(living instanceof TaintSeedEntity) && !living.hasEffect(TTMobEffects.VIS_EXHAUST.get())
                && !living.hasEffect(MobEffects.CONFUSION) && living.getRandom().nextInt(10) == 0) {
            int amount = fluxAmount(state);
            var effect = living.getRandom().nextBoolean()
                    ? new MobEffectInstance(TTMobEffects.VIS_EXHAUST.get(), 1200, (amount - 1) / 3, false, true, false)
                    : new MobEffectInstance(MobEffects.CONFUSION, (3 + amount) * 20);
            living.addEffect(effect);
            PhysicalFlux.reduce(server, pos, 1);
        }
    }
}
