package com.leclowndu93150.thaumaturge.content.infernalfurnace;

import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class InfernalFurnaceBlock extends BaseEntityBlock {
    private static final VoxelShape BASIN = box(0, 0, 0, 16, 8, 16);

    public InfernalFurnaceBlock() {
        super(Properties.of().strength(5).noOcclusion().lightLevel(state -> 15).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASIN;
    }

    @Override
    public InfernalFurnace newBlockEntity(BlockPos pos, BlockState state) {
        return new InfernalFurnace(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.INFERNAL_FURNACE.get(),
                (world, pos, block, furnace) -> furnace.tick((ServerLevel) world));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server)) return;
        if (entity instanceof ItemEntity item && level.getBlockEntity(pos) instanceof InfernalFurnace furnace) {
            var remaining = furnace.feed(server, item.getItem());
            if (remaining.isEmpty()) item.discard();
            else if (remaining.getCount() != item.getItem().getCount()) item.setItem(remaining);
        } else if (entity instanceof LivingEntity living && !living.fireImmune() && living.tickCount % 10 == 0) {
            living.hurt(level.damageSources().lava(), 3);
            living.setSecondsOnFire(10);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            if (level.getBlockEntity(pos) instanceof InfernalFurnace furnace) furnace.spill();
            Block obsidian = TTBulkContent.BLOCKS.get("placeholder_obsidian").get();
            Block bricks = TTBulkContent.BLOCKS.get("placeholder_nether_bricks").get();
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (!level.hasChunkAt(at)) continue;
                var shell = level.getBlockState(at);
                if (shell.is(obsidian)) level.setBlock(at, Blocks.OBSIDIAN.defaultBlockState(), UPDATE_ALL);
                else if (shell.is(bricks)) level.setBlock(at, Blocks.NETHER_BRICKS.defaultBlockState(), UPDATE_ALL);
            }
            BlockPos outlet = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
            if (level.isEmptyBlock(outlet)) {
                level.setBlock(outlet, Block.updateFromNeighbourShapes(Blocks.IRON_BARS.defaultBlockState(), level, outlet), UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
