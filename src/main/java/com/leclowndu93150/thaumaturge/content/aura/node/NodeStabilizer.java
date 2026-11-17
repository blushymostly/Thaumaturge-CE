package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NodeStabilizer extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);
    private final int tier;

    public NodeStabilizer(int tier) {
        super(Properties.of().strength(3).noOcclusion());
        this.tier = tier;
    }

    int tier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Device(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != TTBlockEntities.NODE_STABILIZER.get())
            return null;
        return (world, pos, block, entity) -> ((Device) entity).animate(world, pos);
    }

    public static final class Device extends BlockEntity {
        public static final int MAX_EXTENSION = 37;
        private int previous;
        private int extension;

        public Device(BlockPos pos, BlockState state) {
            super(TTBlockEntities.NODE_STABILIZER.get(), pos, state);
        }

        private void animate(Level level, BlockPos pos) {
            previous = extension;
            boolean active = level.getBlockEntity(pos.above()) instanceof BlockEntityNode node && !(node instanceof JarNodeBlock.Entity) && !level.hasNeighborSignal(pos);
            extension = Mth.clamp(extension + (active ? 1 : -1), 0, MAX_EXTENSION);
        }

        public float extension(float partialTick) {
            return Mth.lerp(partialTick, previous, extension);
        }

        public boolean advanced() {
            return ((NodeStabilizer) getBlockState().getBlock()).tier() == 2;
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox() {
            return new net.minecraft.world.phys.AABB(worldPosition).inflate(1).expandTowards(0, 1, 0);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
