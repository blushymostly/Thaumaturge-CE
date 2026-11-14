package com.leclowndu93150.thaumaturge.content.research.scan;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ScanKeys {
    private static final String PREFIX = "scanned/";

    private ScanKeys() {}

    public static ResourceLocation item(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return key("item/" + id.getNamespace() + "/" + id.getPath());
    }

    public static ResourceLocation entity(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key("entity/" + id.getNamespace() + "/" + id.getPath());
    }

    public static ResourceLocation block(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return key("block/" + id.getNamespace() + "/" + id.getPath());
    }

    /** Research key recorded when an aspect is first discovered by scanning something that carries it. */
    public static ResourceLocation aspect(ResourceLocation aspect) {
        return key("aspect/" + aspect.getNamespace() + "/" + aspect.getPath());
    }

    public static ResourceLocation celestial(long day, String body) {
        return key("celestial/" + day + "/" + body);
    }

    static boolean isOldCelestial(ResourceLocation id, long day) {
        String path = id.getPath();
        return id.getNamespace().equals(TTIds.MODID) && path.startsWith(PREFIX + "celestial/")
                && !path.startsWith(PREFIX + "celestial/" + day + "/");
    }

    private static ResourceLocation key(String path) {
        return TTIds.rl(PREFIX + path);
    }
}
