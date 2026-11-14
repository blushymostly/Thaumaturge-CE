package com.leclowndu93150.thaumaturge.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

public record ResearchParent(ResourceLocation id, int stage, boolean inherit) {
    public static final Codec<ResearchParent> CODEC = Codec.STRING.comapFlatMap(ResearchParent::parse, ResearchParent::serialize);

    public boolean isSatisfiedBy(PlayerKnowledge knowledge) {
        if (knowledge.isComplete(id)) {
            return true;
        }
        return stage > 0 && knowledge.stage(id) >= stage;
    }

    private static DataResult<ResearchParent> parse(String value) {
        boolean inherit = value.startsWith("~");
        String body = inherit ? value.substring(1) : value;
        int at = body.lastIndexOf('@');
        int stage = 0;
        if (at >= 0) {
            try {
                stage = Integer.parseInt(body.substring(at + 1));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid parent stage in '" + value + "'");
            }
            body = body.substring(0, at);
        }
        ResourceLocation id = ResourceLocation.tryParse(body);
        if (id == null) {
            return DataResult.error(() -> "Invalid parent identifier '" + value + "'");
        }
        return DataResult.success(new ResearchParent(id, stage, inherit));
    }

    private String serialize() {
        return (inherit ? "~" : "") + id + (stage > 0 ? "@" + stage : "");
    }
}
