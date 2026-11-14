package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Snapshot of a node's identity: type, optional modifier, held aspects and capacity. */
public record NodeData(NodeType type, Optional<NodeModifier> modifier, AspectList aspects, AspectList aspectsBase) {
    public static final Codec<NodeData> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(NodeType.CODEC.fieldOf("type").forGetter(NodeData::type),
                    NodeModifier.CODEC.optionalFieldOf("modifier").forGetter(NodeData::modifier),
                    AspectList.CODEC.fieldOf("aspects").forGetter(NodeData::aspects),
                    AspectList.CODEC.fieldOf("aspects_base").forGetter(NodeData::aspectsBase))
            .apply(instance, NodeData::new));

    public CompoundTag toTag() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result()
                .filter(tag -> tag instanceof CompoundTag)
                .map(tag -> (CompoundTag) tag)
                .orElseGet(CompoundTag::new);
    }

    @Nullable
    public static NodeData fromTag(@Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        return CODEC.parse(NbtOps.INSTANCE, (Tag) tag).result().orElse(null);
    }
}
