package com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayNetwork;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class AdvancedFurnace extends AbstractSyncedBlockEntity {
    public static final int CAPACITY = 500;
    private static final int POWER_INTERVAL = 5;
    private static final int POWER_REQUEST = 50;

    private AspectList stock = AspectList.EMPTY;
    private ItemStack input = ItemStack.EMPTY;
    private int heat;
    private int aqua;
    private int perditio;
    private int cooldown;

    public AdvancedFurnace(BlockPos pos, BlockState state) {
        super(TTBlockEntities.ADVANCED_FURNACE.get(), pos, state);
    }

    public boolean insert(ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty() || !input.isEmpty()
                || !FurnaceStructure.loaded(level, worldPosition) || !FurnaceStructure.formed(level, worldPosition)) {
            return false;
        }
        int points = total(AspectIndex.of(stack));
        if (points <= 0 || points > CAPACITY / 2) {
            return false;
        }
        input = stack.copyWithCount(1);
        setChangedAndSync();
        return true;
    }

    public AspectList stock() {
        return stock;
    }

    int extract(ResourceLocation aspect, int amount, boolean simulate) {
        if (level == null || !FurnaceStructure.loaded(level, worldPosition) || !FurnaceStructure.formed(level, worldPosition)) {
            return 0;
        }
        int taken = Math.min(Math.max(amount, 0), stock.amountOf(aspect));
        if (taken > 0 && !simulate && !level.isClientSide) {
            stock = stock.remove(aspect, taken);
            setChangedAndSync();
        }
        return taken;
    }

    public void tick(ServerLevel level) {
        if (!FurnaceStructure.loaded(level, worldPosition) || !FurnaceStructure.formed(level, worldPosition)) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            setChanged();
        }
        if (level.getGameTime() % POWER_INTERVAL == 0) {
            recharge(level);
        }
        if (cooldown == 0 && !input.isEmpty()) {
            AspectList aspects = AspectIndex.of(input);
            int points = total(aspects);
            if (points > 0 && heat >= points * 2 && aqua >= points && perditio >= points && total(stock) + points <= CAPACITY) {
                heat -= points * 2;
                aqua -= points;
                perditio -= points;
                for (AspectInstance aspect : aspects.entries()) {
                    stock = stock.add(aspect);
                }
                input = ItemStack.EMPTY;
                cooldown = 5 + Math.round(100.0F * (CAPACITY - heat) / CAPACITY);
                setChangedAndSync();
            }
        }
        var state = getBlockState();
        var display = state.setValue(AdvancedFurnaceBlock.LIT, cooldown > 0).setValue(AdvancedFurnaceBlock.FILLED, !stock.isEmpty());
        if (display != state) {
            level.setBlock(worldPosition, display, Block.UPDATE_CLIENTS);
        }
    }

    private void recharge(ServerLevel level) {
        int nextHeat = topUp(level, TTAspects.IGNIS, Math.max(0, heat - 1));
        int nextAqua = topUp(level, TTAspects.AQUA, aqua);
        int nextPerditio = topUp(level, TTAspects.PERDITIO, perditio);
        if (heat != nextHeat || aqua != nextAqua || perditio != nextPerditio) {
            heat = nextHeat;
            aqua = nextAqua;
            perditio = nextPerditio;
            setChangedAndSync();
        }
    }

    private int topUp(ServerLevel level, ResourceLocation primal, int reserve) {
        int request = Math.min(POWER_REQUEST, CAPACITY - reserve);
        if (request <= 0) return reserve;
        int gained = VisRelayNetwork.drainEverySourceNear(level, worldPosition, primal, request);
        gained += VisRelayNetwork.drainNodesNear(level, worldPosition, primal, request - gained);
        return reserve + gained;
    }

    void spill() {
        if (level != null) {
            Block.popResource(level, worldPosition, input);
            AuraHelper.polluteAura(level, worldPosition, total(stock), true);
        }
    }

    private static int total(AspectList aspects) {
        return aspects.entries().stream().mapToInt(AspectInstance::amount).sum();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        AspectList.CODEC.encodeStart(NbtOps.INSTANCE, stock).result().ifPresent(value -> tag.put("Aspects", value));
        tag.put("Input", input.save(new CompoundTag()));
        tag.putInt("Heat", heat);
        tag.putInt("Aqua", aqua);
        tag.putInt("Perditio", perditio);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stock = tag.contains("Aspects") ? AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get("Aspects")).result().orElse(AspectList.EMPTY) : AspectList.EMPTY;
        input = ItemStack.of(tag.getCompound("Input"));
        heat = Math.max(0, Math.min(CAPACITY, tag.getInt("Heat")));
        aqua = Math.max(0, Math.min(CAPACITY, tag.getInt("Aqua")));
        perditio = Math.max(0, Math.min(CAPACITY, tag.getInt("Perditio")));
        cooldown = Math.max(0, tag.getInt("Cooldown"));
    }
}
