package com.leclowndu93150.thaumaturge.content.research.decon;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class BlockDeconstructionTable extends Block implements EntityBlock {
    private static final ResourceLocation DECONSTRUCTOR_RESEARCH = TTIds.rl("deconstructor");

    private static final VoxelShape SHAPE = Shapes.or(box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0), box(0.0, 0.0, 0.0, 3.0, 13.0, 3.0), box(13.0, 0.0, 0.0, 16.0, 13.0, 3.0),
            box(0.0, 0.0, 13.0, 3.0, 13.0, 16.0), box(13.0, 0.0, 13.0, 16.0, 13.0, 16.0), box(3.0, 8.0, 1.0, 13.0, 13.0, 2.0), box(3.0, 8.0, 14.0, 13.0, 13.0, 15.0),
            box(1.0, 8.0, 3.0, 2.0, 13.0, 13.0), box(14.0, 8.0, 3.0, 15.0, 13.0, 13.0), box(3.0, 1.0, 1.0, 13.0, 2.0, 2.0), box(3.0, 1.0, 14.0, 13.0, 2.0, 15.0), box(1.0, 1.0, 3.0, 2.0, 2.0, 13.0),
            box(14.0, 1.0, 3.0, 15.0, 2.0, 13.0));

    public BlockDeconstructionTable(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityDeconstructionTable(pos, state);
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!TTAttachments.knowledge(serverPlayer).isComplete(DECONSTRUCTOR_RESEARCH)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.thaumaturge.device.not_understood").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof BlockEntityDeconstructionTable table) {
                NetworkHooks.openScreen(serverPlayer, table, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TTBlockEntities.DECONSTRUCTION_TABLE.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, be) -> BlockEntityDeconstructionTable.serverTick(tickLevel, tickPos, tickState, (BlockEntityDeconstructionTable) be);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlockEntityDeconstructionTable table) {
            table.spillContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
