package com.leclowndu93150.thaumaturge.content.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class StarterFocusItem extends Item {
    public StarterFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String spell = StarterFocus.spell(stack);
        tooltip.add(Component.translatable("tooltip.thaumaturge.focus." + (spell.isEmpty() ? "blank" : spell)));
    }
}
