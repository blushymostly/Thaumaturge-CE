package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.registry.TTMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FocalManipulatorMenu extends AbstractContainerMenu {
    private static final int PROGRESS = 0;
    private static final int STATUS = 1;

    private final Player player;
    private final BlockPos pos;
    private final ContainerData data = new SimpleContainerData(2);
    private final @Nullable FocalManipulatorBlockEntity serverTable;

    public static FocalManipulatorMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        return new FocalManipulatorMenu(id, inventory, buffer.readBlockPos(), null);
    }

    FocalManipulatorMenu(int id, Inventory inventory, FocalManipulatorBlockEntity table) {
        this(id, inventory, table.getBlockPos(), table);
    }

    private FocalManipulatorMenu(int id, Inventory inventory, BlockPos pos, @Nullable FocalManipulatorBlockEntity table) {
        super(TTMenus.FOCAL_MANIPULATOR.get(), id);
        this.pos = pos;
        this.serverTable = table;
        this.player = inventory.player;
        Container focus = table == null ? new SimpleContainer(1) : table.focus();
        addSlot(new Slot(focus, 0, 91, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !busy() && StarterFocus.isFocus(stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return !busy();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int cell = 0; cell < 27; cell++) {
            addSlot(new Slot(inventory, 9 + cell, 13 + cell / 9 * 18, 69 + cell % 9 * 18));
        }
        for (int cell = 0; cell < 9; cell++) {
            addSlot(new Slot(inventory, cell, 13 + cell % 3 * 18, 12 + cell / 3 * 18));
        }
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (serverTable != null) {
            data.set(PROGRESS, serverTable.progress());
            data.set(STATUS, serverTable.status(player).ordinal());
        }
        super.broadcastChanges();
    }

    public BlockPos pos() {
        return pos;
    }

    public boolean busy() {
        return serverTable == null ? status() == Status.WRITING : serverTable.busy();
    }

    public int progressPercent() {
        return data.get(PROGRESS);
    }

    public Status status() {
        return Status.values()[data.get(STATUS)];
    }

    public int budget() {
        return StarterFocus.budget(getSlot(0).getItem());
    }

    public int focusVersion() {
        var table = table();
        return table == null ? 0 : table.focusVersion();
    }

    public FocusSpell draft() {
        var table = table();
        return table == null ? FocusSpell.empty() : table.draft();
    }

    public String draftName() {
        var table = table();
        return table == null ? "" : table.draftName();
    }

    private @Nullable FocalManipulatorBlockEntity table() {
        return player.level().getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table ? table : null;
    }

    public boolean submitDraft(Player player, int focusVersion, FocusSpell spell, String name, boolean inscribe) {
        if (serverTable == null || !stillValid(player) || busy() || budget() == 0 || focusVersion != serverTable.focusVersion() || !spell.researched(player))
            return false;
        serverTable.setDraft(spell, name);
        boolean accepted = !inscribe || serverTable.startInscribing(player);
        broadcastChanges();
        return accepted;
    }

    @Override
    public boolean stillValid(Player player) {
        return serverTable == null || serverTable.getLevel() == player.level() && table() == serverTable && !serverTable.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 && FocalManipulatorBlockEntity.canOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size())
            return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player))
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    public enum Status {
        NO_FOCUS, INCOMPLETE, RESEARCH, TIER, LEVELS, CRYSTALS, READY, WRITING
    }
}
