package com.leclowndu93150.thaumaturge.content.aspect;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class AspectCombinations {
    private AspectCombinations() {}

    /** The compound aspect made of exactly {@code first} + {@code second} (either order), or {@code null}. */
    public static @Nullable ResourceLocation result(ResourceLocation first, ResourceLocation second) {
        for (Map.Entry<ResourceLocation, IAspect> candidate : TTAspects.all().entrySet()) {
            List<ResourceLocation> components = candidate.getValue().componentIds();
            if (components.size() != 2) {
                continue;
            }
            boolean direct = components.get(0).equals(first) && components.get(1).equals(second);
            boolean swapped = components.get(0).equals(second) && components.get(1).equals(first);
            if (direct || swapped) {
                return candidate.getKey();
            }
        }
        return null;
    }
}
