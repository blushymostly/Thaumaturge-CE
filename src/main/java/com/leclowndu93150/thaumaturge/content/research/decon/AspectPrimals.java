package com.leclowndu93150.thaumaturge.content.research.decon;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import net.minecraft.resources.ResourceLocation;

public final class AspectPrimals {
    private AspectPrimals() {}

    public static AspectList reduce(AspectList aspects) {
        AspectList result = AspectList.EMPTY;
        for (AspectInstance instance : aspects.entries()) {
            result = reduceInto(result, instance.aspectId(), instance.amount());
        }
        return result;
    }

    private static AspectList reduceInto(AspectList result, ResourceLocation aspectId, int amount) {
        IAspect aspect = TTAspects.get(aspectId);
        if (aspect == null) {
            return result;
        }
        if (aspect.isPrimal()) {
            return result.add(new AspectInstance(aspectId, amount));
        }
        for (ResourceLocation component : aspect.componentIds()) {
            result = reduceInto(result, component, amount);
        }
        return result;
    }

    public static int totalAmount(AspectList aspects) {
        int total = 0;
        for (AspectInstance entry : aspects.entries()) {
            total += entry.amount();
        }
        return total;
    }
}
