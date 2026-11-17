package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = TTIds.MODID)
public final class BlockEntityVisGenerator extends BlockEntity implements IEnergyStorage {
    private static final int CAPACITY = 1000;
    private static final int OUTPUT_PER_TICK = 20;
    private static final String ENERGY_KEY = "energy";

    private int energy;
    private LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> this);

    public BlockEntityVisGenerator(BlockPos pos, BlockState state) {
        super(TTBlockEntities.VIS_GENERATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityVisGenerator generator) {
        if (!state.getValue(BlockStateProperties.ENABLED) || level.hasNeighborSignal(pos))
            return;
        if (generator.energy == 0) {
            generator.energy = (int) (AuraHelper.drainVis(level, pos, 1, false) * CAPACITY);
            if (generator.energy > 0)
                generator.setChanged();
        }
        if (generator.energy == 0)
            return;
        Direction face = state.getValue(BlockStateProperties.FACING);
        BlockPos output = pos.relative(face);
        if (!level.hasChunkAt(output))
            return;
        BlockEntity target = level.getBlockEntity(output);
        if (target == null)
            return;
        target.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).ifPresent(receiver -> {
            int limit = Math.min(generator.energy, OUTPUT_PER_TICK);
            int accepted = Mth.clamp(receiver.receiveEnergy(limit, false), 0, limit);
            if (accepted > 0) {
                generator.energy -= accepted;
                generator.setChanged();
            }
        });
    }

    @SubscribeEvent
    public static void restoreLegacyGenerators(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide())
            return;
        // Older builds saved the generator as a block without a block entity.
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.getBlock() instanceof BlockVisGenerator))
                continue;
            int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!(section.getBlockState(x, y, z).getBlock() instanceof BlockVisGenerator))
                            continue;
                        var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                        if (chunk.getBlockEntity(pos) == null) {
                            chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
                            chunk.setUnsaved(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> type, @Nullable Direction side) {
        if (type == ForgeCapabilities.ENERGY && side == getBlockState().getValue(BlockStateProperties.FACING)) {
            return capability.cast();
        }
        return super.getCapability(type, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capability = LazyOptional.of(() -> this);
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return CAPACITY;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY_KEY, energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = Mth.clamp(tag.getInt(ENERGY_KEY), 0, CAPACITY);
    }
}
