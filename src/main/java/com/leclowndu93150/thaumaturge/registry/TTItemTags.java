package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class TTItemTags {
    public static final TagKey<Item> SCRIBING_TOOLS = TagKey.create(Registries.ITEM, TTIds.rl("scribing_tools"));
    public static final TagKey<Item> RESEARCH_NOTES = TagKey.create(Registries.ITEM, TTIds.rl("research_notes"));

    private TTItemTags() {}
}
