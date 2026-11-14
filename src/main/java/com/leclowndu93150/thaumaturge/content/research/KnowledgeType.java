package com.leclowndu93150.thaumaturge.content.research;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum KnowledgeType {
    OBSERVATION,
    THEORY;

    public static final Codec<KnowledgeType> CODEC = Codec.STRING.xmap(
            value -> KnowledgeType.valueOf(value.toUpperCase(Locale.ROOT)),
            type -> type.name().toLowerCase(Locale.ROOT));
}
