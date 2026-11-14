package com.leclowndu93150.thaumaturge.content.world.tree;

import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Silverwood log that hosts an aura node BE (CE {@code BlockSilverwoodNodeLog}). */
public final class BlockSilverwoodNodeLog extends RotatedPillarBlock implements EntityBlock {
    public BlockSilverwoodNodeLog(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(TTItems.LOG_SILVERWOOD.get());
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityNode node) {
            node.burstAndClear(player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNode(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TTBlockEntities.NODE.get()) {
            return null;
        }
        return level.isClientSide()
                ? null
                : (tickLevel, tickPos, tickState, be) -> ((BlockEntityNode) be).serverTick(tickLevel, tickPos);
    }

    /** After the node is drained / burst, leave a normal silverwood log. */
    public static BlockState depletedState(BlockState state) {
        BlockState log = TTBlocks.LOG_SILVERWOOD.get().defaultBlockState();
        if (state.hasProperty(AXIS) && log.hasProperty(AXIS)) {
            log = log.setValue(AXIS, state.getValue(AXIS));
        }
        return log;
    }
}
