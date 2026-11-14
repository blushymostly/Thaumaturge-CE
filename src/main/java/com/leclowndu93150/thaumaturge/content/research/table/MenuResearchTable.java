package com.leclowndu93150.thaumaturge.content.research.table;

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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class MenuResearchTable extends AbstractContainerMenu {
    public static final int SCRIBE_TOOLS_X = 14;
    public static final int SCRIBE_TOOLS_Y = 10;
    public static final int NOTE_X = 70;
    public static final int NOTE_Y = 10;
    public static final int PLAYER_GRID_X = 48;
    public static final int PLAYER_GRID_Y = 175;
    public static final int HOTBAR_Y = 233;
    public static final int TABLE_SLOT_COUNT = BlockEntityResearchTable.SLOT_COUNT;

    private final @Nullable BlockEntityResearchTable table;
    private final BlockPos origin;
    private final ContainerLevelAccess access;

    public static MenuResearchTable fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity found = inventory.player.level().getBlockEntity(pos);
        return new MenuResearchTable(containerId, inventory, found instanceof BlockEntityResearchTable researchTable ? researchTable : null, pos);
    }

    public MenuResearchTable(int containerId, Inventory inventory, BlockEntityResearchTable blockEntity) {
        this(containerId, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private MenuResearchTable(int containerId, Inventory inventory, @Nullable BlockEntityResearchTable blockEntity, BlockPos origin) {
        super(TTMenus.RESEARCH_TABLE.get(), containerId);
        this.table = blockEntity;
        this.origin = origin;
        this.access = blockEntity == null || blockEntity.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), origin);
        Container container = blockEntity == null ? new SimpleContainer(TABLE_SLOT_COUNT) : blockEntity.items();
        addSlot(new TableSlot(container, BlockEntityResearchTable.SLOT_SCRIBE_TOOLS, SCRIBE_TOOLS_X, SCRIBE_TOOLS_Y));
        addSlot(new TableSlot(container, BlockEntityResearchTable.SLOT_NOTE, NOTE_X, NOTE_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, PLAYER_GRID_X + col * 18, PLAYER_GRID_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_GRID_X + col * 18, HOTBAR_Y));
        }
    }

    /** Server-side lookup used by table packets: the player must have this exact table's menu open. */
    public static @Nullable BlockEntityResearchTable target(@Nullable ServerPlayer player, BlockPos pos) {
        if (player == null || !(player.containerMenu instanceof MenuResearchTable menu) || !menu.origin.equals(pos) || !menu.stillValid(player)) {
            return null;
        }
        return menu.table;
    }

    public BlockPos pos() {
        return origin;
    }

    public @Nullable BlockEntityResearchTable blockEntity() {
        return table;
    }

    public BlockPos tablePos() {
        return origin;
    }

    public boolean hasUsableScribeTools() {
        ItemStack tools = slots.get(BlockEntityResearchTable.SLOT_SCRIBE_TOOLS).getItem();
        return !tools.isEmpty() && tools.isDamageableItem() && tools.getDamageValue() < tools.getMaxDamage();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TTBlocks.RESEARCH_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = slot.getItem();
        ItemStack original = moved.copy();
        if (index < TABLE_SLOT_COUNT) {
            if (!moveItemStackTo(moved, TABLE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(moved, 0, TABLE_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (moved.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private static final class TableSlot extends Slot {
        TableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
