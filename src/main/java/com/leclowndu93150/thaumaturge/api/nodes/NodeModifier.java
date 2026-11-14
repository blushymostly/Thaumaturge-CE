package com.leclowndu93150.thaumaturge.api.nodes;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/** Vigor of an aura node; absent modifier means an average node (Forge port of CE {@code NodeModifier}). */
public enum NodeModifier implements StringRepresentable {
    BRIGHT("bright"),
    PALE("pale"),
    FADING("fading");

    public static final Codec<NodeModifier> CODEC = StringRepresentable.fromEnum(NodeModifier::values);

    private final String name;

    NodeModifier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Nullable
    public static NodeModifier byName(String name) {
        for (NodeModifier modifier : values()) {
            if (modifier.name.equals(name)) {
                return modifier;
            }
        }
        return null;
    }
}
