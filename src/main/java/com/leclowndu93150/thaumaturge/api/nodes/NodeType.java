package com.leclowndu93150.thaumaturge.api.nodes;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Behavioral archetype of an aura node (Forge 1.20.1 port of CE {@code NodeType}). */
public enum NodeType implements StringRepresentable {
    NORMAL("normal"),
    UNSTABLE("unstable"),
    DARK("dark"),
    TAINTED("tainted"),
    HUNGRY("hungry"),
    PURE("pure");

    public static final Codec<NodeType> CODEC = StringRepresentable.fromEnum(NodeType::values);

    private final String name;

    NodeType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static NodeType byName(String name) {
        for (NodeType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return NORMAL;
    }
}
