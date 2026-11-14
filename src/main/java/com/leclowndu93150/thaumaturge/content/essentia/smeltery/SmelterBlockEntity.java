package com.leclowndu93150.thaumaturge.content.essentia.smeltery;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.content.essentia.BellowsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

public final class SmelterBlockEntity extends BaseContainerBlockEntity {
    static final int INPUT = 0;
    static final int FUEL = 1;
    static final int CAPACITY = 256;
    private static final int EXPORT_INTERVAL = 15;

    private static final float VITIUM_RETENTION = 0.66F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private AspectList stock = AspectList.EMPTY;
    private int fuel;
    private int fuelCapacity;
    private int progress;
    private int target;
    private int exportTicks;
    private boolean boosted;

    public SmelterBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.SMELTER.get(), pos, state);
    }

    public static boolean validInput(ItemStack stack) {
        return !stack.isEmpty() && !AspectIndex.of(stack).isEmpty();
    }

    public static int burnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, null);
    }

    public AspectList stock() {
        return stock;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity smelter) {
        smelter.tickServer(level, pos, state);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean dirty = fuel > 0 || progress > 0;
        if (fuel > 0) {
            fuel--;
        }
        ItemStack input = items.get(INPUT);
        AspectList aspects = input.isEmpty() ? AspectList.EMPTY : AspectIndex.of(input);
        int points = total(aspects);
        target = points * 2;
        if (points > 0 && points <= CAPACITY - total(stock)) {
            int bellows = 0;
            Direction front = state.getValue(SmelterBlock.FACING);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (side != front && BellowsHelper.blowsToward(level, pos.relative(side), side)) bellows++;
            }
            target = Math.max(1, target * (8 - bellows) / 8);
            if (fuel == 0) {
                ignite();
            }
            if (fuel > 0) {
                dirty = true;
                if (++progress >= target) {
                    smelt(level, pos, input, aspects, points);
                    progress = 0;
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        int interval = isTier("smelter_thaumium") ? 10 : EXPORT_INTERVAL;
        if (++exportTicks >= (boosted ? interval * 4 / 5 : interval)) {
            exportTicks = 0;
            for (AspectInstance aspect : stock.sortedByAmount()) {
                if (AlembicBlockEntity.feedColumn(level, pos, aspect.aspectId())) {
                    stock = stock.remove(aspect.aspectId(), 1);
                    dirty = true;
                    break;
                }
            }
        }

        boolean lit = fuel > 0;
        if (state.getValue(SmelterBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(SmelterBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (dirty) {
            setChanged();
        }
    }

    private void ignite() {
        ItemStack stack = items.get(FUEL);
        int duration = burnTime(stack);
        if (duration <= 0) {
            return;
        }
        boosted = stack.is(TTItems.ALUMENTUM.get());
        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(FUEL, remainder);
        }
        fuel = fuelCapacity = duration;
    }

    private boolean isTier(String id) {
        return getBlockState().is(com.leclowndu93150.thaumaturge.registry.TTBulkContent.BLOCKS.get(id).get());
    }

    private void smelt(Level level, BlockPos pos, ItemStack input, AspectList aspects, int points) {
        int kept = 0;
        float yield = isTier("smelter_void") ? 0.95F : isTier("smelter_thaumium") ? 0.9F : 0.8F;
        for (AspectInstance aspect : aspects.entries()) {
            float chance = aspect.aspectId().equals(TTAspects.VITIUM) ? yield * VITIUM_RETENTION : yield;
            int amount = 0;
            for (int i = 0; i < aspect.amount(); i++) {
                if (level.random.nextFloat() < chance) {
                    amount++;
                }
            }
            if (amount > 0) {
                stock = stock.add(aspect.withAmount(amount));
                kept += amount;
            }
        }
        input.shrink(1);
        AuraHelper.polluteAura(level, pos, points - kept, true);
    }

    void spill() {
        if (level != null && !level.isClientSide) {
            AuraHelper.polluteAura(level, worldPosition, total(stock), true);
        }
    }

    private static int total(AspectList aspects) {
        return aspects.entries().stream().mapToInt(AspectInstance::amount).sum();
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, count);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == INPUT && !ItemStack.isSameItemSameTags(items.get(slot), stack)) {
            progress = 0;
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
        progress = 0;
        setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SmelterMenu(id, inventory, this, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> fuel;
                    case 1 -> fuelCapacity;
                    case 2 -> progress;
                    case 3 -> target;
                    case 4 -> total(stock);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return SmelterMenu.DATA_COUNT;
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        AspectList.CODEC.encodeStart(NbtOps.INSTANCE, stock).result().ifPresent(value -> tag.put("Aspects", value));
        tag.putInt("BurnTime", fuel);
        tag.putInt("CurrentItemBurnTime", fuelCapacity);
        tag.putInt("CookTime", progress);
        tag.putBoolean("SpeedBoost", boosted);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        stock = tag.contains("Aspects")
                ? AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get("Aspects")).result().orElse(AspectList.EMPTY)
                : AspectList.EMPTY;
        fuel = tag.getInt("BurnTime");
        fuelCapacity = tag.getInt("CurrentItemBurnTime");
        progress = tag.getInt("CookTime");
        boosted = tag.getBoolean("SpeedBoost");
    }
}
