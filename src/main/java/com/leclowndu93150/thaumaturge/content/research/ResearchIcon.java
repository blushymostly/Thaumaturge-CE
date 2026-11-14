package com.leclowndu93150.thaumaturge.content.research;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Icon on a research map node: item id, texture path ending in .png, or focus:spell_part. */
public record ResearchIcon(ResourceLocation id, Kind kind) {
    public static final Codec<ResearchIcon> CODEC = Codec.STRING.xmap(ResearchIcon::parse, ResearchIcon::serialize);

    public enum Kind {
        ITEM,
        TEXTURE,
        FOCUS
    }

    public static ResearchIcon ofTexture(ResourceLocation id) {
        return new ResearchIcon(id, Kind.TEXTURE);
    }

    public static ResearchIcon ofItem(ResourceLocation id) {
        return new ResearchIcon(id, Kind.ITEM);
    }

    public static ResearchIcon ofFocus(ResourceLocation id) {
        return new ResearchIcon(id, Kind.FOCUS);
    }

    public boolean texture() {
        return kind == Kind.TEXTURE;
    }

    public static ResearchIcon parse(String value) {
        if (value.startsWith("focus:")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring("focus:".length()));
            return ofFocus(id != null ? id : ResourceLocation.tryParse("minecraft:air"));
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return ofItem(ResourceLocation.tryParse("minecraft:air"));
        }
        return new ResearchIcon(id, id.getPath().endsWith(".png") ? Kind.TEXTURE : Kind.ITEM);
    }

    @Nullable
    public static ResearchIcon tryParse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parse(value);
    }

    private static String serialize(ResearchIcon icon) {
        return icon.kind == Kind.FOCUS ? "focus:" + icon.id : icon.id.toString();
    }
}
