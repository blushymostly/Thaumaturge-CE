package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public final class EverfullUrnBlockEntity extends BlockEntity {
    static final int PORTION = 333;
    private static final int CAPACITY = 1000;
    private static final int PUSH_AMOUNT = 25;
    private static final int PROBE_CELLS = 75;
    private static final int OWN_CELL = 37;
    private final FluidTank tank = new FluidTank(CAPACITY, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> capability = LazyOptional.of(() -> tank);
    private final Set<Integer> receivers = new LinkedHashSet<>();
    private int ticks;
    private int probe = 1;

    public EverfullUrnBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.EVERFULL_URN.get(), pos, state);
    }

    public int water() {
        return tank.getFluidAmount();
    }

    FluidTank tank() {
        return tank;
    }

    static void tick(Level level, BlockPos pos, BlockState state, EverfullUrnBlockEntity urn) {
        if (!(level instanceof ServerLevel server) || ++urn.ticks % 5 != 0) return;
        urn.probe(server);
        urn.distribute(server);
        urn.refill(server);
    }

    private BlockPos position(int cell) {
        return worldPosition.offset(cell % 25 / 5 - 2, cell / 25 - 1, cell % 5 - 2);
    }

    private void probe(ServerLevel level) {
        int cell = probe;
        probe = (probe + 1) % PROBE_CELLS;
        if (probe == OWN_CELL) probe++;
        BlockPos pos = position(cell);
        if (level.hasChunkAt(pos) && !receivers.contains(cell)
                && (isCauldron(level.getBlockState(pos)) || receiver(level, pos) != null)) {
            receivers.add(cell);
            setChanged();
        }
    }

    private void distribute(ServerLevel level) {
        for (var iterator = receivers.iterator(); iterator.hasNext() && water() >= PUSH_AMOUNT;) {
            BlockPos pos = position(iterator.next());
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (isCauldron(state)) {
                int fill = state.is(Blocks.CAULDRON) ? 0 : state.getValue(LayeredCauldronBlock.LEVEL);
                if (water() >= PORTION && fill < LayeredCauldronBlock.MAX_FILL_LEVEL
                        && level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, fill + 1), Block.UPDATE_ALL)) {
                    tank.drain(PORTION, FluidAction.EXECUTE);
                    splash(level, pos);
                }
                continue;
            }
            var handler = receiver(level, pos);
            if (handler == null) {
                iterator.remove();
                setChanged();
            } else if (!FluidUtil.tryFluidTransfer(handler, tank, PUSH_AMOUNT, true).isEmpty()) {
                splash(level, pos);
                return;
            }
        }
    }

    private static boolean isCauldron(BlockState state) {
        return state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON);
    }

    private static @Nullable IFluidHandler receiver(ServerLevel level, BlockPos pos) {
        // Urns refill themselves. Pushing between two urns would continually waste their transfer cycles.
        if (level.getBlockEntity(pos) instanceof EverfullUrnBlockEntity) return null;
        return FluidUtil.getFluidHandler(level, pos, Direction.UP).orElse(null);
    }

    private static void splash(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0);
    }

    private void refill(ServerLevel level) {
        int room = Math.min(100, CAPACITY - water());
        int possible = (int) (CAPACITY * AuraHelper.drainVis(level, worldPosition, room / (float) CAPACITY, true) + 0.0001F);
        if (possible <= 0) return;
        AuraHelper.drainVis(level, worldPosition, possible / (float) CAPACITY, false);
        tank.fill(new FluidStack(Fluids.WATER, possible), FluidAction.EXECUTE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putIntArray("Handlers", receivers.stream().mapToInt(Integer::intValue).toArray());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.setFluid(FluidStack.EMPTY);
        tank.readFromNBT(tag.getCompound("Tank"));
        if (!tank.getFluid().isEmpty() && !tank.isFluidValid(tank.getFluid())) tank.setFluid(FluidStack.EMPTY);
        if (water() > CAPACITY) tank.setFluid(new FluidStack(Fluids.WATER, CAPACITY));
        receivers.clear();
        for (int cell : tag.getIntArray("Handlers")) {
            if (cell >= 0 && cell < PROBE_CELLS && cell != OWN_CELL) receivers.add(cell);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> type, @Nullable Direction side) {
        return type == ForgeCapabilities.FLUID_HANDLER ? capability.cast() : super.getCapability(type, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capability = LazyOptional.of(() -> tank);
    }
}
