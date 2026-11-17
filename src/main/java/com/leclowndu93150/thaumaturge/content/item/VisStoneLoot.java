package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID)
public final class VisStoneLoot {
    private static final Set<ResourceLocation> TREASURE = Set.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.JUNGLE_TEMPLE, BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.ABANDONED_MINESHAFT, BuiltInLootTables.STRONGHOLD_CORRIDOR, BuiltInLootTables.STRONGHOLD_CROSSING, BuiltInLootTables.STRONGHOLD_LIBRARY);

    private VisStoneLoot() {}

    @SubscribeEvent
    public static void addVisStone(LootTableLoadEvent event) {
        if (!TREASURE.contains(event.getName()))
            return;
        event.getTable().addPool(LootPool.lootPool().name("thaumaturge_vis_stone").setRolls(ConstantValue.exactly(1)).add(EmptyLootItem.emptyItem().setWeight(54))
                .add(LootItem.lootTableItem(TTBulkItems.ITEMS.get("amulet_vis").get())).build());
    }
}
