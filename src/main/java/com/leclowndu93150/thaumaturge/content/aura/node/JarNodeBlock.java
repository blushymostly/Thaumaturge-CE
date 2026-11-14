package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class JarNodeBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 12, 13), box(5, 12, 5, 11, 14, 11));

    public JarNodeBlock() {
        super(Properties.copy(Blocks.GLASS).noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.JAR_NODE.get(),
                (world, pos, block, jar) -> jar.serverTick(world, pos));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(player.getItemInHand(hand).getItem() instanceof WandItem) || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof Entity jar) || jar.capacity().isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) release(server, jar);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void release(ServerLevel level, Entity jar) {
        BlockPos pos = jar.getBlockPos();
        if (jar.capacity().isEmpty() || level.getBlockEntity(pos) != jar) return;
        NodeData data = jar.nodeData();
        BlockState shell = jar.getBlockState();
        if (level.setBlock(pos, TTBlocks.NODE.get().defaultBlockState(), Block.UPDATE_ALL)
                && level.getBlockEntity(pos) instanceof BlockEntityNode node) {
            node.applyNodeData(data);
            level.levelEvent(2001, pos, Block.getId(shell));
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        NodeData data = TTItemData.getNodeData(stack);
        if (!level.isClientSide && data != null && level.getBlockEntity(pos) instanceof Entity jar) jar.applyNodeData(data);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!params.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) return List.of();
        return List.of(withNode(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return withNode(level.getBlockEntity(pos));
    }

    private ItemStack withNode(BlockEntity entity) {
        ItemStack stack = new ItemStack(this);
        if (entity instanceof Entity jar && !jar.capacity().isEmpty()) TTItemData.setNodeData(stack, jar.nodeData());
        return stack;
    }

    public static final class Entity extends BlockEntityNode {
        public Entity(BlockPos pos, BlockState state) {
            super(TTBlockEntities.JAR_NODE.get(), pos, state);
        }

    }
}
