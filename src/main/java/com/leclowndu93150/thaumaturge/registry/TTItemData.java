package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 1.20.1 stand-in for CE DataComponents on ItemStack — NBT helpers with Codec backing.
 */
public final class TTItemData {
    public static final String ROOT = "Thaumaturge";
    public static final String ASPECTS = "Aspects";
    public static final String CRYSTAL_ASPECT = "CrystalAspect";
    public static final String ESSENTIA = "Essentia";
    public static final String WAND_VIS = "WandVis";
    public static final String ASPECT_FILTER = "AspectFilter";
    public static final String NODE = "Node";

    private TTItemData() {}

    public static void bootstrap() {
        TTAspects.bootstrap();
    }

    public static CompoundTag root(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(ROOT, Tag.TAG_COMPOUND)) {
            tag.put(ROOT, new CompoundTag());
        }
        return tag.getCompound(ROOT);
    }

    public static AspectList getAspects(ItemStack stack) {
        return read(stack, ASPECTS, AspectList.CODEC).orElse(AspectList.EMPTY);
    }

    public static void setAspects(ItemStack stack, AspectList list) {
        write(stack, ASPECTS, AspectList.CODEC, list);
    }

    @Nullable
    public static AspectInstance getCrystalAspect(ItemStack stack) {
        return read(stack, CRYSTAL_ASPECT, AspectInstance.CODEC).orElse(null);
    }

    public static void setCrystalAspect(ItemStack stack, AspectInstance instance) {
        write(stack, CRYSTAL_ASPECT, AspectInstance.CODEC, instance);
    }

    public static AspectList getEssentia(ItemStack stack) {
        return read(stack, ESSENTIA, AspectList.CODEC).orElse(AspectList.EMPTY);
    }

    public static void setEssentia(ItemStack stack, AspectList list) {
        write(stack, ESSENTIA, AspectList.CODEC, list);
    }

    @Nullable
    public static NodeData getNodeData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = tag.getCompound(ROOT);
        return root.contains(NODE, Tag.TAG_COMPOUND) ? NodeData.fromTag(root.getCompound(NODE)) : null;
    }

    public static void setNodeData(ItemStack stack, NodeData data) {
        root(stack).put(NODE, data.toTag());
    }

    public static float getWandVis(ItemStack stack) {
        CompoundTag root = root(stack);
        return root.contains(WAND_VIS) ? root.getFloat(WAND_VIS) : 0.0F;
    }

    public static void setWandVis(ItemStack stack, float vis) {
        root(stack).putFloat(WAND_VIS, vis);
    }

    @Nullable
    public static ResourceLocation getAspectFilter(ItemStack stack) {
        CompoundTag root = root(stack);
        if (!root.contains(ASPECT_FILTER)) {
            return null;
        }
        return ResourceLocation.tryParse(root.getString(ASPECT_FILTER));
    }

    public static void setAspectFilter(ItemStack stack, @Nullable ResourceLocation id) {
        CompoundTag root = root(stack);
        if (id == null) {
            root.remove(ASPECT_FILTER);
        } else {
            root.putString(ASPECT_FILTER, id.toString());
        }
    }

    private static <T> Optional<T> read(ItemStack stack, String key, Codec<T> codec) {
        CompoundTag root = root(stack);
        if (!root.contains(key)) {
            return Optional.empty();
        }
        return codec.parse(NbtOps.INSTANCE, root.get(key)).result();
    }

    private static <T> void write(ItemStack stack, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(tag -> root(stack).put(key, tag));
    }
}
