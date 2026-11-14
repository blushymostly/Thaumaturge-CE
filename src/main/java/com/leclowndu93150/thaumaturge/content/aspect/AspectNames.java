package com.leclowndu93150.thaumaturge.content.aspect;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Translation helpers for aspect ids (lang keys {@code aspect.<ns>.<path>}). */
public final class AspectNames {
    private AspectNames() {}

    public static MutableComponent name(ResourceLocation aspect) {
        return Component.translatable(Util.makeDescriptionId("aspect", aspect));
    }

    /** Short study hint ({@code aspect.<id>.help}), used by discovery-error messages. */
    public static MutableComponent help(ResourceLocation aspect) {
        return Component.translatable(Util.makeDescriptionId("aspect", aspect) + ".help");
    }

    /** "A + B" composition line for a compound aspect. */
    public static MutableComponent composition(ResourceLocation aspect) {
        IAspect value = TTAspects.get(aspect);
        List<ResourceLocation> parts = value == null ? List.of() : value.componentIds();
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(" + ");
            }
            out.append(name(parts.get(i)));
        }
        return out;
    }
}
