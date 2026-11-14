package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TTBlockTags {
    public static final TagKey<Block> CRUCIBLE_HEAT_SOURCES = TagKey.create(Registries.BLOCK, TTIds.rl("crucible_heat_sources"));

    public static final TagKey<Block> WORKBENCH_CHARGER_HOSTS = TagKey.create(Registries.BLOCK, TTIds.rl("arcane_workbench_charger_hosts"));

    public static final TagKey<Block> MAGICAL_CAVE_GROUND_REPLACEABLE =
            TagKey.create(Registries.BLOCK, TTIds.rl("magical_cave_ground_replaceable"));

    public static final TagKey<Block> MAGICAL_PLANTS = TagKey.create(Registries.BLOCK, TTIds.rl("magical_plants"));

    private TTBlockTags() {}
}
