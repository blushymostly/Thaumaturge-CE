package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

public final class NodeTransducer extends BaseEntityBlock {
    private static final int CHARGE_TARGET = 1000;
    private static final int REVERT_THRESHOLD = 50;
    private static final VoxelShape SHAPE = box(0, 6, 0, 16, 16, 16);

    public NodeTransducer() {
        super(Properties.of().strength(3).noOcclusion());
    }

    static int stabilizerTier(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos))
            return 0;
        return level.getBlockState(pos).getBlock() instanceof NodeStabilizer block ? block.tier() : 0;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TTBlockEntities.NODE_TRANSDUCER.get(), level.isClientSide ? BlockEntity::animate : BlockEntity::tick);
    }

    public static final class BlockEntity extends AbstractSyncedBlockEntity {
        private int charge;
        private int previousDisplay;
        private int display;

        public BlockEntity(BlockPos pos, BlockState state) {
            super(TTBlockEntities.NODE_TRANSDUCER.get(), pos, state);
        }

        private static void animate(Level level, BlockPos pos, BlockState state, BlockEntity device) {
            device.previousDisplay = device.display;
            int target = Math.min(device.charge, REVERT_THRESHOLD);
            device.display += Integer.compare(target, device.display);
        }

        public float extension(float partialTick) {
            return Mth.lerp(partialTick, previousDisplay, display);
        }

        private static void tick(Level level, BlockPos pos, BlockState state, BlockEntity device) {
            if (!(level instanceof ServerLevel server))
                return;
            int previous = device.charge;
            BlockPos nodePos = pos.below();
            if (!(level.getBlockEntity(nodePos) instanceof BlockEntityNode node) || node instanceof JarNodeBlock.Entity || node.isCapturing()) {
                device.charge = 0;
            } else if (stabilizerTier(level, nodePos.below()) == 0) {
                if (node.isEnergized()) {
                    AuraHelper.polluteAura(server, nodePos, 32, true);
                    server.removeBlock(nodePos, false);
                    server.explode(null, nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5, 3, Level.ExplosionInteraction.NONE);
                    for (int attempt = 0; attempt < 50; attempt++) {
                        BlockPos target = nodePos.offset(spillOffset(server), spillOffset(server), spillOffset(server));
                        if (!server.hasChunkAt(target))
                            continue;
                        if (target.getY() < nodePos.getY())
                            PhysicalFlux.placeGoo(server, target, PhysicalFlux.MAX_QUANTA);
                        else
                            PhysicalFlux.placeGas(server, target, PhysicalFlux.MAX_QUANTA);
                    }
                    device.charge = REVERT_THRESHOLD;
                } else if (!level.hasNeighborSignal(pos)) {
                    device.charge = Math.max(0, device.charge - 1);
                }
            } else if (level.hasNeighborSignal(pos)) {
                if (device.charge < CHARGE_TARGET) {
                    device.charge++;
                    if (!node.isEnergized()) {
                        var aspects = node.getAspects().entries();
                        if (!aspects.isEmpty())
                            node.drain(aspects.get(level.random.nextInt(aspects.size())).aspectId(), 1);
                    }
                    if (device.charge == CHARGE_TARGET)
                        node.setEnergized(true);
                }
            } else {
                device.charge = Math.max(0, device.charge - 1);
                if (device.charge <= REVERT_THRESHOLD && node.isEnergized())
                    node.setEnergized(false);
            }
            if (device.charge != previous) {
                device.setChanged();
                int visible = Math.min(REVERT_THRESHOLD, device.charge);
                if (visible != Math.min(REVERT_THRESHOLD, previous) && (visible % 5 == 0 || visible == 1)) {
                    device.syncToClient();
                }
                if (device.charge > REVERT_THRESHOLD && device.charge < CHARGE_TARGET && level.getGameTime() % 10 == 0) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    Vec3 target = Vec3.atCenterOf(nodePos);
                    if (server.random.nextBoolean())
                        BoltParticleOptions.strike(server, center.add(0, 0.3, 0), target, 0.06F);
                    if (stabilizerTier(level, nodePos.below()) != 0 && server.random.nextBoolean()) {
                        BoltParticleOptions.strike(server, center.add(0, -0.4, 0), target, 0.06F);
                    }
                }
            }
        }

        private static int spillOffset(ServerLevel level) {
            return level.random.nextInt(8) - level.random.nextInt(8);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("Count", charge);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            charge = Mth.clamp(tag.getInt("Count"), 0, CHARGE_TARGET);
        }

        @Override
        public void handleUpdateTag(CompoundTag tag) {
            load(tag);
            display = Math.min(REVERT_THRESHOLD, charge);
            previousDisplay = display;
        }
    }
}
