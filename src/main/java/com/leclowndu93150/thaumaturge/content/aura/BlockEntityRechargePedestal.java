package com.leclowndu93150.thaumaturge.content.aura;

import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayNetwork;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class BlockEntityRechargePedestal extends BlockEntity {
    private static final int RECHARGE_INTERVAL = 5;
    private static final int NODE_RANGE = 8;
    private static final String ITEM_KEY = "Item";

    private ItemStack item = ItemStack.EMPTY;
    private int tickCounter;

    public BlockEntityRechargePedestal(BlockPos pos, BlockState state) {
        super(TTBlockEntities.RECHARGE_PEDESTAL.get(), pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack stack) {
        item = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        setChanged();
        sync();
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof WandItem;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityRechargePedestal pedestal) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        pedestal.tickCounter++;
        if (pedestal.tickCounter % RECHARGE_INTERVAL != 0) {
            return;
        }
        ItemStack stack = pedestal.item;
        if (!(stack.getItem() instanceof WandItem)) {
            return;
        }
        if (WandCharge.PRIMALS.stream().noneMatch(aspect -> WandCharge.room(stack, aspect) > 0)) return;
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-NODE_RANGE, -NODE_RANGE, -NODE_RANGE),
                pos.offset(NODE_RANGE, NODE_RANGE, NODE_RANGE))) {
            if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof BlockEntityNode node
                    && WandItem.drawFromNode(stack, node)) {
                pedestal.setChanged();
                pedestal.sync();
                return;
            }
        }
        var source = VisRelayNetwork.nearest(server, pos);
        if (source == null) return;
        for (var aspect : WandCharge.PRIMALS) {
            int requested = Math.min(WandCharge.room(stack, aspect), WandCharge.CENTIVIS_PER_VIS);
            int taken = source.drain(server, aspect, requested);
            if (taken > 0) {
                WandCharge.add(stack, aspect, taken);
                pedestal.setChanged();
                pedestal.sync();
                return;
            }
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!item.isEmpty()) {
            tag.put(ITEM_KEY, item.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        item = tag.contains(ITEM_KEY) ? ItemStack.of(tag.getCompound(ITEM_KEY)) : ItemStack.EMPTY;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 1, 0);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
