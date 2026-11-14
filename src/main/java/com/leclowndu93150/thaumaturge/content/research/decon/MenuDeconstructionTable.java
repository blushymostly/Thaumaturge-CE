package com.leclowndu93150.thaumaturge.content.research.decon;

import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class MenuDeconstructionTable extends AbstractContainerMenu {
    public static final int INPUT_X = 63;
    public static final int INPUT_Y = 15;
    private static final int PLAYER_GRID_X = 8;
    private static final int PLAYER_GRID_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int TABLE_SLOTS = 1;

    private final @Nullable BlockEntityDeconstructionTable blockEntity;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final DataSlot breakTime = DataSlot.standalone();

    public static MenuDeconstructionTable fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity found = inventory.player.level().getBlockEntity(pos);
        return new MenuDeconstructionTable(containerId, inventory, found instanceof BlockEntityDeconstructionTable table ? table : null, pos);
    }

    public MenuDeconstructionTable(int containerId, Inventory inventory, BlockEntityDeconstructionTable blockEntity) {
        this(containerId, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private MenuDeconstructionTable(int containerId, Inventory inventory, @Nullable BlockEntityDeconstructionTable blockEntity, BlockPos pos) {
        super(TTMenus.DECONSTRUCTION_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.access = blockEntity == null || blockEntity.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), pos);
        Container container = blockEntity == null ? new SimpleContainer(TABLE_SLOTS) : blockEntity.items();
        addSlot(new Slot(container, BlockEntityDeconstructionTable.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(BlockEntityDeconstructionTable.SLOT_INPUT, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, PLAYER_GRID_X + col * 18, PLAYER_GRID_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_GRID_X + col * 18, HOTBAR_Y));
        }
        breakTime.set(BlockEntityDeconstructionTable.BREAK_TIME_TICKS);
        addDataSlot(breakTime);
    }

    public static @Nullable BlockEntityDeconstructionTable target(@Nullable ServerPlayer player, BlockPos pos) {
        if (player == null || !(player.containerMenu instanceof MenuDeconstructionTable menu) || !menu.pos.equals(pos) || !menu.stillValid(player)) {
            return null;
        }
        return menu.blockEntity;
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            breakTime.set(blockEntity.breakTime());
        }
        super.broadcastChanges();
    }

    public int breakTime() {
        return breakTime.get();
    }

    public @Nullable BlockEntityDeconstructionTable blockEntity() {
        return blockEntity;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TTBlocks.DECONSTRUCTION_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = slot.getItem();
        ItemStack original = moved.copy();
        if (index < TABLE_SLOTS) {
            if (!moveItemStackTo(moved, TABLE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(moved, 0, TABLE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (moved.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
