package com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AdvancedFurnaceBlock extends BaseEntityBlock {
    static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape INTAKE = box(0, 0, 0, 16, 11.2, 16);

    public AdvancedFurnaceBlock() {
        super(Properties.of().strength(4).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FILLED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INTAKE;
    }

    @Override
    public AdvancedFurnace newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedFurnace(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.ADVANCED_FURNACE.get(),
                (world, pos, block, furnace) -> furnace.tick((ServerLevel) world));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!TTAttachments.knowledge(player).isComplete(TTIds.rl("advanced_alchemical_furnace"))
                || !(level.getBlockEntity(pos) instanceof AdvancedFurnace furnace) || !furnace.insert(stack)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) {
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos at = pos.relative(direction, 2);
                if (!level.hasChunkAt(at)) continue;
                var tube = level.getBlockState(at);
                if (tube.getBlock() instanceof com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlock) {
                    level.setBlock(at, com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlock.recomputeConnections(tube, level, at), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item && item.tickCount % 10 == 0
                && level.getBlockEntity(pos) instanceof AdvancedFurnace furnace && furnace.insert(item.getItem())) {
            ItemStack remaining = item.getItem().copy();
            remaining.shrink(1);
            if (remaining.isEmpty()) item.discard();
            else item.setItem(remaining);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof AdvancedFurnace furnace) {
            furnace.spill();
            FurnaceStructure.restore(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
