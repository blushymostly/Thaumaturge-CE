package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class WandCharge {
    public static final int CENTIVIS_PER_VIS = 100;
    public static final List<ResourceLocation> PRIMALS = List.of(TTAspects.AER, TTAspects.TERRA, TTAspects.IGNIS, TTAspects.AQUA, TTAspects.ORDO, TTAspects.PERDITIO);
    private static final String CHARGE = "PrimalVis";

    private WandCharge() {}

    public static int capacity(ItemStack wand) {
        return Math.round(WandItem.capacity(wand) * CENTIVIS_PER_VIS);
    }

    public static int stored(ItemStack wand, ResourceLocation aspect) {
        int index = PRIMALS.indexOf(aspect);
        var tag = wand.getTagElement(TTItemData.ROOT);
        if (index < 0 || tag == null)
            return 0;
        if (tag.contains(CHARGE, Tag.TAG_COMPOUND)) {
            return Mth.clamp(tag.getCompound(CHARGE).getInt(aspect.getPath()), 0, capacity(wand));
        }
        // Preserve the total charge from Forge saves that predate the six primal stores.
        int legacy = Mth.clamp(Math.round(tag.getFloat(TTItemData.WAND_VIS) * CENTIVIS_PER_VIS), 0, capacity(wand));
        return legacy / PRIMALS.size() + (index < legacy % PRIMALS.size() ? 1 : 0);
    }

    public static int room(ItemStack wand, ResourceLocation aspect) {
        return capacity(wand) - stored(wand, aspect);
    }

    public static int add(ItemStack wand, ResourceLocation aspect, int wanted) {
        if (!PRIMALS.contains(aspect) || wanted <= 0)
            return 0;
        int amount = Math.min(wanted, room(wand, aspect));
        if (amount > 0) {
            CompoundTag charge = snapshot(wand);
            charge.putInt(aspect.getPath(), charge.getInt(aspect.getPath()) + amount);
            save(wand, charge);
        }
        return amount;
    }

    public static boolean spendExact(ItemStack wand, ResourceLocation aspect, int amount) {
        int balance = stored(wand, aspect);
        if (amount <= 0 || balance < amount || !PRIMALS.contains(aspect))
            return false;
        CompoundTag charge = snapshot(wand);
        charge.putInt(aspect.getPath(), balance - amount);
        save(wand, charge);
        return true;
    }

    public static int[] touchFocusCosts(ResourceLocation effect, int cost) {
        return focusCosts(TTAspects.AVERSIO, effect, cost);
    }

    static int[] focusCosts(ResourceLocation delivery, ResourceLocation effect, int cost) {
        return focusCosts(ItemStack.EMPTY, List.of(delivery, effect), cost);
    }

    static int[] focusCosts(ItemStack wand, List<ResourceLocation> aspects, int cost) {
        int[] weights = new int[PRIMALS.size()];
        for (var aspect : aspects)
            addWeights(weights, aspect);
        int totalWeight = java.util.Arrays.stream(weights).sum();
        int remainder = cost;
        int[] shares = new int[weights.length];
        for (int i = 0; i < weights.length; i++) {
            shares[i] = cost * weights[i] / totalWeight;
            remainder -= shares[i];
        }
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] == 0)
                continue;
            int share = shares[i];
            if (remainder > 0) {
                share++;
                remainder--;
            }
            shares[i] = share * WandParts.costPercent(wand, PRIMALS.get(i)) / 100;
        }
        return shares;
    }

    public static boolean payTouchFocus(ItemStack wand, ResourceLocation effect, int cost, boolean commit) {
        return payFocus(wand, focusCosts(wand, List.of(TTAspects.AVERSIO, effect), cost), commit);
    }

    static boolean payFocus(ItemStack wand, int[] costs, boolean commit) {
        CompoundTag after = snapshot(wand);
        for (int i = 0; i < costs.length; i++) {
            String key = PRIMALS.get(i).getPath();
            int owed = costs[i];
            int available = after.getInt(key);
            if (available < owed)
                return false;
            after.putInt(key, available - owed);
        }
        if (commit)
            save(wand, after);
        return true;
    }

    private static void addWeights(int[] weights, ResourceLocation id) {
        int index = PRIMALS.indexOf(id);
        if (index >= 0) {
            weights[index]++;
        } else {
            var aspect = TTAspects.get(id);
            if (aspect != null) {
                for (var component : aspect.componentIds())
                    addWeights(weights, component);
            }
        }
    }

    private static CompoundTag snapshot(ItemStack wand) {
        CompoundTag charge = new CompoundTag();
        for (var primal : PRIMALS)
            charge.putInt(primal.getPath(), stored(wand, primal));
        return charge;
    }

    private static void save(ItemStack wand, CompoundTag charge) {
        var tag = TTItemData.root(wand);
        tag.put(CHARGE, charge);
        tag.remove(TTItemData.WAND_VIS);
    }
}
