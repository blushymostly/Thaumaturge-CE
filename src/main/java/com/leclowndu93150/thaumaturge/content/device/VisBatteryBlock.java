package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class VisBatteryBlock extends Block {
    public static final int MAX_CHARGE = 10;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, MAX_CHARGE);

    public VisBatteryBlock() {
        super(Properties.copy(Blocks.STONE).strength(0.5F).randomTicks().lightLevel(state -> state.getValue(CHARGE)));
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (!level.isClientSide && !previous.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(CHARGE);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        float vis = AuraHelper.getVis(level, pos);
        float base = AuraHelper.getAuraBase(level, pos);
        boolean powered = level.hasNeighborSignal(pos);
        int delay = 20 + random.nextInt(20);
        int next = charge;
        if (charge > 0 && (powered || vis < base * 0.75F)) {
            AuraHelper.addVis(level, pos, 1);
            next--;
            if (powered) delay = 5;
        } else if (!powered && charge < MAX_CHARGE && vis > Math.max(1, base * 0.9F)
                && AuraHelper.drainVis(level, pos, 1, true) >= 1) {
            AuraHelper.drainVis(level, pos, 1, false);
            next++;
            delay = 100 + random.nextInt(100);
        }
        if (next != charge) level.setBlock(pos, state.setValue(CHARGE, next), UPDATE_ALL);
        level.scheduleTick(pos, this, delay);
    }
}
