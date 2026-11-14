package com.leclowndu93150.thaumaturge.content.infusion;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class InfusionStabilizer extends BaseEntityBlock {
    private static final int MAX_ENERGY = 15;
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 5, 16), box(2, 5, 2, 14, 8, 14),
            box(4, 8, 4, 12, 11, 12), box(3, 11, 3, 13, 13, 13), box(4, 13, 4, 12, 16, 12));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public InfusionStabilizer() {
        super(Properties.of().strength(3));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntity device ? device.energy() : 0;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.INFUSION_STABILIZER.get(), BlockEntity::tick);
    }

    public static final class BlockEntity extends AbstractSyncedBlockEntity {
        private int energy;
        private long nextStabilization;

        public BlockEntity(BlockPos pos, BlockState state) {
            super(TTBlockEntities.INFUSION_STABILIZER.get(), pos, state);
        }

        public int energy() {
            return energy;
        }

        boolean mitigate(int cost) {
            if (cost > energy) return false;
            changeEnergy(-cost);
            return true;
        }

        private static void tick(Level level, BlockPos pos, BlockState state, BlockEntity device) {
            if (!(level instanceof ServerLevel server)) return;
            long time = server.getGameTime();
            if (time % 20 == 0 && device.energy < MAX_ENERGY) {
                AuraHelper.polluteAura(server, pos, 0.25F, true);
                device.changeEnergy(1);
            }
            if (time % 5 != 0 || time < device.nextStabilization || device.energy == 0) return;
            for (FluxRift rift : level.getEntitiesOfClass(FluxRift.class, new AABB(pos).inflate(8))) {
                if (rift.stability() > 50) continue;
                rift.nudgeStability();
                device.changeEnergy(-1);
                device.nextStabilization = Math.max(time, device.nextStabilization) + 5;
                if (device.energy == 0) break;
            }
        }

        private void changeEnergy(int amount) {
            energy += amount;
            setChangedAndSync();
            if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("energy", energy);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            energy = Mth.clamp(tag.getInt("energy"), 0, MAX_ENERGY);
        }
    }
}
