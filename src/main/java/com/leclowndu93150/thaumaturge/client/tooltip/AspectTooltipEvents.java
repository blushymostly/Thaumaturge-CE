package com.leclowndu93150.thaumaturge.client.tooltip;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectChipsTooltip;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.item.VisCrystalItem;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Appends the aspect chip row to item tooltips (always for vis crystals; Shift for other items). */
@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class AspectTooltipEvents {
    private AspectTooltipEvents() {}

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        boolean crystal = stack.getItem() instanceof VisCrystalItem && TTItemData.getCrystalAspect(stack) != null;
        if (!crystal && !Screen.hasShiftDown()) {
            return;
        }
        AspectList aspects = AspectIndex.of(stack);
        if (aspects.isEmpty()) {
            return;
        }
        event.getTooltipElements().add(Either.right(new AspectChipsTooltip(aspects)));
    }

    @SubscribeEvent
    public static void onRecipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            AspectIndex.invalidate();
            AspectIndex.rebuildDerived(event.getRecipeManager(), level.registryAccess(), net.minecraftforge.fml.LogicalSide.CLIENT);
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        AspectIndex.invalidate();
    }
}
