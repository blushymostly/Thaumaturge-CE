package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class AmuletVisItem extends Item implements ICurioItem {
    private final int interval;

    public AmuletVisItem(Properties properties, int interval) {
        super(properties.stacksTo(1));
        this.interval = interval;
    }

    @Override
    public void curioTick(SlotContext slot, ItemStack stack) {
        if (!(slot.entity() instanceof ServerPlayer player) || player.tickCount % interval != 0)
            return;

        Target neediest = null;
        for (int i = 0; i < 9; i++) {
            neediest = lowestTarget(neediest, player.getInventory().getItem(i));
        }
        neediest = lowestTarget(neediest, player.getOffhandItem());
        if (neediest != null && AuraHelper.drainVis(player.level(), player.blockPosition(), 1, false) > 0) {
            WandCharge.add(neediest.stack(), neediest.primal(), WandCharge.CENTIVIS_PER_VIS);
        }
    }

    private static @Nullable Target lowestTarget(@Nullable Target best, ItemStack stack) {
        if (!(stack.getItem() instanceof WandItem))
            return best;
        int capacity = WandCharge.capacity(stack);
        for (ResourceLocation primal : WandCharge.PRIMALS) {
            int stored = WandCharge.stored(stack, primal);
            if (stored >= capacity)
                continue;
            float fill = (float) stored / capacity;
            if (best == null || fill < best.fill())
                best = new Target(stack, primal, fill);
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumaturge.amulet_vis.text").withStyle(ChatFormatting.AQUA));
    }

    private record Target(ItemStack stack, ResourceLocation primal, float fill) {
    }
}
