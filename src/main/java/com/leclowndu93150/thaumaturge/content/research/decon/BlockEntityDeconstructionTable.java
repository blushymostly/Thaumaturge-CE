package com.leclowndu93150.thaumaturge.content.research.decon;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BlockEntityDeconstructionTable extends AbstractSyncedBlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int BREAK_TIME_TICKS = 40;

    private static final int SLOT_COUNT = 1;
    private static final int RESULT_ROLL_RANGE = 80;
    private static final float BREAK_SOUND_VOLUME = 0.3F;
    private static final float PITCH_BASE = 0.9F;
    private static final float PITCH_SPREAD = 0.2F;
    private static final String RESULT_KEY = "result_aspect";
    private static final String BREAK_TIME_KEY = "break_time";
    private static final String TITLE_KEY = "gui.thaumaturge.deconstruction_table.title";

    private final DeconstructionInventory items = new DeconstructionInventory();
    private @Nullable ResourceLocation resultAspect;
    private int breakTime = BREAK_TIME_TICKS;
    private ItemStack validatedStack = ItemStack.EMPTY;
    private boolean validatedHasAspects;

    public BlockEntityDeconstructionTable(BlockPos pos, BlockState state) {
        super(TTBlockEntities.DECONSTRUCTION_TABLE.get(), pos, state);
    }

    public Container items() {
        return items;
    }

    public @Nullable ResourceLocation resultAspect() {
        return resultAspect;
    }

    public int breakTime() {
        return breakTime;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(TITLE_KEY);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuDeconstructionTable(containerId, inventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityDeconstructionTable table) {
        if (!table.canBreak()) {
            if (table.breakTime != BREAK_TIME_TICKS) {
                table.breakTime = BREAK_TIME_TICKS;
                table.setChangedAndSync();
            }
            return;
        }
        if (--table.breakTime <= 0) {
            table.breakTime = BREAK_TIME_TICKS;
            table.breakInput(level, pos);
        }
    }

    public void collect(ServerPlayer player) {
        if (resultAspect == null) {
            return;
        }
        AspectPools.grant(player, resultAspect, 1);
        resultAspect = null;
        setChangedAndSync();
    }

    public void spillContents() {
        if (level == null || level.isClientSide()) {
            return;
        }
        Containers.dropContents(level, worldPosition, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.items);
        resultAspect = tag.contains(RESULT_KEY) ? ResourceLocation.tryParse(tag.getString(RESULT_KEY)) : null;
        breakTime = tag.contains(BREAK_TIME_KEY) ? tag.getInt(BREAK_TIME_KEY) : BREAK_TIME_TICKS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items.items);
        if (resultAspect != null) {
            tag.putString(RESULT_KEY, resultAspect.toString());
        }
        tag.putInt(BREAK_TIME_KEY, breakTime);
    }

    private boolean canBreak() {
        ItemStack stack = items.getItem(SLOT_INPUT);
        if (stack.isEmpty() || resultAspect != null) {
            return false;
        }
        return hasAspects(stack);
    }

    private boolean hasAspects(ItemStack stack) {
        ItemStack probe = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (!ItemStack.isSameItemSameTags(probe, validatedStack)) {
            validatedStack = probe;
            validatedHasAspects = !probe.isEmpty() && !AspectIndex.of(probe).isEmpty();
        }
        return validatedHasAspects;
    }

    private void breakInput(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        ItemStack stack = items.getItem(SLOT_INPUT);
        AspectList primals = AspectPrimals.reduce(AspectIndex.of(stack.copyWithCount(1)));
        int total = AspectPrimals.totalAmount(primals);
        if (!primals.isEmpty() && (total >= RESULT_ROLL_RANGE || random.nextInt(RESULT_ROLL_RANGE) < total)) {
            AspectInstance chosen = primals.entries().get(random.nextInt(primals.size()));
            resultAspect = chosen.aspectId();
        }
        stack.shrink(1);
        items.setChanged();
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, BREAK_SOUND_VOLUME, PITCH_BASE + random.nextFloat() * PITCH_SPREAD);
        setChangedAndSync();
    }

    private final class DeconstructionInventory implements Container {
        private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

        @Override
        public int getContainerSize() {
            return SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return items.get(SLOT_INPUT).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = ContainerHelper.removeItem(items, slot, amount);
            if (!result.isEmpty()) {
                setChanged();
            }
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            setChanged();
        }

        @Override
        public void setChanged() {
            BlockEntityDeconstructionTable.this.setChangedAndSync();
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == BlockEntityDeconstructionTable.this
                    && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return hasAspects(stack);
        }

        @Override
        public void clearContent() {
            items.clear();
        }
    }
}
