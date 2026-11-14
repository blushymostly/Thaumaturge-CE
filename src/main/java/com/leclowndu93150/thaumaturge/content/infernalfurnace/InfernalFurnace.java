package com.leclowndu93150.thaumaturge.content.infernalfurnace;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.content.essentia.BellowsHelper;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public final class InfernalFurnace extends BlockEntity {
    private static final TagKey<Item> RARE_HIGH = TagKey.create(Registries.ITEM, TTIds.rl("infernal/rare_high"));
    private static final TagKey<Item> RARE_NORMAL = TagKey.create(Registries.ITEM, TTIds.rl("infernal/rare_normal"));
    private static final TagKey<Item> RARE_LOW = TagKey.create(Registries.ITEM, TTIds.rl("infernal/rare_low"));
    private final ItemStackHandler input = new ItemStackHandler(32) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int time;
    private int boost;

    public InfernalFurnace(BlockPos pos, BlockState state) {
        super(TTBlockEntities.INFERNAL_FURNACE.get(), pos, state);
    }

    public ItemStack feed(ServerLevel level, ItemStack stack) {
        if (!assembled(level) || stack.isEmpty()) return stack;
        if (recipe(level, stack) == null) {
            level.levelEvent(1501, worldPosition, 0);
            return ItemStack.EMPTY;
        }
        return ItemHandlerHelper.insertItemStacked(input, stack, false);
    }

    public void tick(ServerLevel level) {
        if (time > 1) {
            time--;
            setChanged();
            return;
        }
        for (int slot = 0; slot < input.getSlots(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (!assembled(level)) return;
            var recipe = recipe(level, stack);
            if (recipe == null) {
                input.extractItem(slot, 1, false);
                continue;
            }
            int bellows = bellows(level);
            if (time == 0) {
                if (boost == 0 && AuraHelper.drainVis(level, worldPosition, 20, true) >= 20) {
                    boost = (int) AuraHelper.drainVis(level, worldPosition, 20, false);
                }
                time = Math.max(10, (boost > 0 ? 80 : 140) - (21 - bellows) * bellows) + 1;
                setChanged();
                return;
            }
            var result = recipe.assemble(new SimpleContainer(stack.copyWithCount(1)), level.registryAccess());
            input.extractItem(slot, 1, false);
            deliver(level, result);
            for (int i = 0; i <= bellows; i++) bonus(level, stack);
            if (level.random.nextInt(20) == 0) AuraHelper.polluteAura(level, worldPosition, 1, true);
            float experience = recipe.getExperience() * result.getCount();
            int whole = (int) experience;
            if (level.random.nextFloat() < experience - whole) whole++;
            if (whole > 0) ExperienceOrb.award(level, Vec3.atCenterOf(worldPosition.relative(outlet())), whole);
            if (boost > 0) boost--;
            time = 0;
            setChanged();
            return;
        }
        if (time != 0) {
            time = 0;
            setChanged();
        }
    }

    private boolean assembled(ServerLevel level) {
        if (!level.hasChunksAt(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, 0, 1))) return false;
        var brick = TTBulkContent.BLOCKS.get("placeholder_nether_bricks").get();
        var obsidian = TTBulkContent.BLOCKS.get("placeholder_obsidian").get();
        BlockPos outlet = worldPosition.relative(outlet());
        for (BlockPos at : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            int dx = at.getX() - worldPosition.getX();
            int dy = at.getY() - worldPosition.getY();
            int dz = at.getZ() - worldPosition.getZ();
            if (dx == 0 && dz == 0 && dy >= 0 || at.equals(outlet)) continue;
            if (!level.getBlockState(at).is(dx != 0 && dz != 0 ? brick : obsidian)) return false;
        }
        return true;
    }

    private Direction outlet() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    private int bellows(ServerLevel level) {
        int count = 0;
        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == outlet()) continue;
            if (BellowsHelper.blowsToward(level, worldPosition.relative(side, 2), side)) count++;
        }
        return count;
    }

    private static SmeltingRecipe recipe(ServerLevel level, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level).orElse(null);
    }

    private void deliver(ServerLevel level, ItemStack stack) {
        Direction side = outlet();
        BlockPos opening = worldPosition.relative(side);
        BlockPos target = level.isEmptyBlock(opening) ? opening.relative(side) : opening;
        BlockEntity block = level.getBlockEntity(target);
        if (block != null) {
            var handler = block.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).resolve().orElse(null);
            if (handler != null) stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            else if (block instanceof Container container) stack = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(
                    null, container, stack, side.getOpposite());
        }
        if (!stack.isEmpty()) {
            Vec3 point = Vec3.atCenterOf(opening);
            ItemEntity entity = new ItemEntity(level, point.x, point.y - 0.3, point.z, stack,
                    side.getStepX() * 0.3, 0, side.getStepZ() * 0.3);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    private void bonus(ServerLevel level, ItemStack stack) {
        float rareChance = stack.is(RARE_HIGH) ? 0.025F : stack.is(RARE_NORMAL) ? 0.02F : stack.is(RARE_LOW) ? 0.01F : 0;
        if (rareChance > 0 && level.random.nextFloat() < rareChance) {
            deliver(level, new ItemStack(TTBulkItems.ITEMS.get("rare_earth").get()));
        }
        if (level.random.nextFloat() >= 0.33F) return;
        String meat = stack.is(Items.BEEF) ? "chunk_beef" : stack.is(Items.CHICKEN) ? "chunk_chicken"
                : stack.is(Items.PORKCHOP) ? "chunk_pork" : stack.is(Items.MUTTON) ? "chunk_mutton"
                : stack.is(Items.RABBIT) ? "chunk_rabbit" : stack.is(net.minecraft.tags.ItemTags.FISHES) ? "chunk_fish" : null;
        if (meat != null) deliver(level, new ItemStack(com.leclowndu93150.thaumaturge.registry.TTBulkItems.ITEMS.get(meat).get()));
        else if (stack.is(Tags.Items.ORES_IRON) || stack.is(Tags.Items.RAW_MATERIALS_IRON)) deliver(level, new ItemStack(Items.IRON_NUGGET, stack.is(Tags.Items.ORES_IRON) ? 2 : 1));
        else if (stack.is(Tags.Items.ORES_GOLD) || stack.is(Tags.Items.RAW_MATERIALS_GOLD)) deliver(level, new ItemStack(Items.GOLD_NUGGET, stack.is(Tags.Items.ORES_GOLD) ? 2 : 1));
        else if (stack.is(Tags.Items.ORES_QUARTZ)) deliver(level, new ItemStack(TTItems.NUGGET_QUARTZ.get(), 2));
        else if (stack.is(TTItems.RAW_CINNABAR.get())) deliver(level, new ItemStack(TTItems.NUGGET_QUICKSILVER.get()));
        else if (stack.is(TTItems.ORE_CINNABAR.get()) || stack.is(TTItems.DEEPSLATE_ORE_CINNABAR.get())) {
            deliver(level, new ItemStack(TTItems.NUGGET_QUICKSILVER.get(), 2));
        }
    }

    void spill() {
        if (level == null) return;
        for (int slot = 0; slot < input.getSlots(); slot++) {
            Block.popResource(level, worldPosition, input.getStackInSlot(slot).copy());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Input", input.serializeNBT());
        tag.putInt("Time", time);
        tag.putInt("Boost", boost);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Input")) input.deserializeNBT(tag.getCompound("Input"));
        time = Math.max(0, tag.getInt("Time"));
        boost = Math.max(0, tag.getInt("Boost"));
    }
}
