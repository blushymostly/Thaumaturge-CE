package com.leclowndu93150.thaumaturge.content.research;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Player research / scan knowledge — Forge capability payload. */
public final class PlayerKnowledge {
    private final Set<ResourceLocation> completed = new HashSet<>();
    private final Set<ResourceLocation> scanned = new HashSet<>();
    private final Map<ResourceLocation, Integer> stages = new HashMap<>();
    private final Map<KnowledgeType, Map<ResourceLocation, Integer>> knowledgePoints = new HashMap<>();
    private final Map<ResourceLocation, Integer> aspectPool = new HashMap<>();
    private final Set<ResourceLocation> discoveredAspects = new HashSet<>();
    private int completedNotes;
    private boolean dirty;

    /** Aspect pool (research table palette). Primals are always usable; compounds appear once discovered. */
    public int aspectAmount(ResourceLocation aspect) {
        return aspectPool.getOrDefault(aspect, 0);
    }

    public void addAspect(ResourceLocation aspect, int delta) {
        int next = Math.max(0, aspectAmount(aspect) + delta);
        aspectPool.put(aspect, next);
        dirty = true;
    }

    public void setAspect(ResourceLocation aspect, int amount) {
        aspectPool.put(aspect, Math.max(0, amount));
        dirty = true;
    }

    public boolean discoverAspect(ResourceLocation aspect) {
        boolean changed = discoveredAspects.add(aspect);
        dirty |= changed;
        return changed;
    }

    public boolean isAspectDiscovered(ResourceLocation aspect) {
        return discoveredAspects.contains(aspect);
    }

    /** Ids the player has discovered or holds in the pool (excludes always-known primals unless pooled). */
    public Set<ResourceLocation> poolAspectIds() {
        Set<ResourceLocation> ids = new HashSet<>(aspectPool.keySet());
        ids.addAll(discoveredAspects);
        return ids;
    }

    public boolean aspectPoolEmpty() {
        return aspectPool.isEmpty() && discoveredAspects.isEmpty();
    }

    public int completedNotes() {
        return completedNotes;
    }

    public void incrementCompletedNotes() {
        completedNotes++;
        dirty = true;
    }

    public Set<ResourceLocation> completed() {
        return Collections.unmodifiableSet(completed);
    }

    public Set<ResourceLocation> scanned() {
        return Collections.unmodifiableSet(scanned);
    }

    public boolean isComplete(ResourceLocation id) {
        return completed.contains(id);
    }

    public boolean isKnown(ResourceLocation id) {
        return completed.contains(id) || stages.containsKey(id);
    }

    public boolean isScanned(ResourceLocation id) {
        return scanned.contains(id);
    }

    public int stage(ResourceLocation id) {
        return stages.getOrDefault(id, 0);
    }

    /** UI badge flags — stub until server tracks notifications. */
    public boolean hasResearchFlag(ResourceLocation id, ResearchFlag flag) {
        return false;
    }

    public void clearResearchFlag(ResourceLocation id, ResearchFlag flag) {}

    public int knowledge(KnowledgeType type, ResourceLocation category) {
        return knowledgePoints.getOrDefault(type, Map.of()).getOrDefault(category, 0);
    }

    public boolean addKnowledge(KnowledgeType type, ResourceLocation category, int amount) {
        if (amount <= 0) {
            return false;
        }
        knowledgePoints.computeIfAbsent(type, ignored -> new HashMap<>()).merge(category, amount, Integer::sum);
        dirty = true;
        return true;
    }

    public boolean startResearch(ResourceLocation id) {
        if (isKnown(id)) {
            return false;
        }
        stages.put(id, 0);
        dirty = true;
        return true;
    }

    public boolean markResearchComplete(ResourceLocation id, int finalStage) {
        boolean changed = completed.add(id);
        stages.put(id, Math.max(finalStage, stages.getOrDefault(id, 0)));
        dirty = true;
        return changed;
    }

    /** Marks a lightweight completion flag (crafted keys, etc.). */
    public boolean complete(ResourceLocation id) {
        return markResearchComplete(id, Math.max(stages.getOrDefault(id, 0), 1));
    }

    public void forgetResearch(ResourceLocation id) {
        boolean removed = completed.remove(id);
        dirty |= stages.remove(id) != null || removed;
    }

    public boolean scan(ResourceLocation id) {
        boolean changed = scanned.add(id);
        dirty |= changed;
        return changed;
    }

    public void setStage(ResourceLocation id, int stage) {
        stages.put(id, stage);
        dirty = true;
    }

    public boolean consumeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Completed", stringList(completed));
        tag.put("Scanned", stringList(scanned));
        CompoundTag stageTag = new CompoundTag();
        stages.forEach((id, stage) -> stageTag.putInt(id.toString(), stage));
        tag.put("Stages", stageTag);
        tag.put("Knowledge", saveKnowledge());
        CompoundTag poolTag = new CompoundTag();
        aspectPool.forEach((id, amount) -> poolTag.putInt(id.toString(), amount));
        tag.put("AspectPool", poolTag);
        tag.put("DiscoveredAspects", stringList(discoveredAspects));
        tag.putInt("CompletedNotes", completedNotes);
        return tag;
    }

    public void load(CompoundTag tag) {
        completed.clear();
        scanned.clear();
        stages.clear();
        knowledgePoints.clear();
        readStrings(tag.getList("Completed", Tag.TAG_STRING), completed);
        readStrings(tag.getList("Scanned", Tag.TAG_STRING), scanned);
        CompoundTag stageTag = tag.getCompound("Stages");
        for (String key : stageTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                stages.put(id, stageTag.getInt(key));
            }
        }
        loadKnowledge(tag.getCompound("Knowledge"));
        aspectPool.clear();
        discoveredAspects.clear();
        CompoundTag poolTag = tag.getCompound("AspectPool");
        for (String key : poolTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                aspectPool.put(id, poolTag.getInt(key));
            }
        }
        readStrings(tag.getList("DiscoveredAspects", Tag.TAG_STRING), discoveredAspects);
        completedNotes = tag.getInt("CompletedNotes");
        dirty = false;
    }

    public void write(FriendlyByteBuf buf) {
        writeSet(buf, completed);
        writeSet(buf, scanned);
        buf.writeVarInt(stages.size());
        stages.forEach((id, stage) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(stage);
        });
        buf.writeVarInt(knowledgePoints.size());
        knowledgePoints.forEach((type, map) -> {
            buf.writeEnum(type);
            buf.writeVarInt(map.size());
            map.forEach((category, amount) -> {
                buf.writeResourceLocation(category);
                buf.writeVarInt(amount);
            });
        });
        buf.writeVarInt(aspectPool.size());
        aspectPool.forEach((id, amount) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(amount);
        });
        writeSet(buf, discoveredAspects);
        buf.writeVarInt(completedNotes);
    }

    public void read(FriendlyByteBuf buf) {
        completed.clear();
        scanned.clear();
        stages.clear();
        knowledgePoints.clear();
        readSet(buf, completed);
        readSet(buf, scanned);
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            stages.put(buf.readResourceLocation(), buf.readVarInt());
        }
        int knowledgeTypes = buf.readVarInt();
        for (int i = 0; i < knowledgeTypes; i++) {
            KnowledgeType type = buf.readEnum(KnowledgeType.class);
            int entries = buf.readVarInt();
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (int j = 0; j < entries; j++) {
                map.put(buf.readResourceLocation(), buf.readVarInt());
            }
            knowledgePoints.put(type, map);
        }
        aspectPool.clear();
        discoveredAspects.clear();
        int poolSize = buf.readVarInt();
        for (int i = 0; i < poolSize; i++) {
            aspectPool.put(buf.readResourceLocation(), buf.readVarInt());
        }
        readSet(buf, discoveredAspects);
        completedNotes = buf.readVarInt();
        dirty = false;
    }

    public void copyFrom(PlayerKnowledge other) {
        completed.clear();
        completed.addAll(other.completed);
        scanned.clear();
        scanned.addAll(other.scanned);
        stages.clear();
        stages.putAll(other.stages);
        knowledgePoints.clear();
        other.knowledgePoints.forEach((type, map) -> knowledgePoints.put(type, new HashMap<>(map)));
        aspectPool.clear();
        aspectPool.putAll(other.aspectPool);
        discoveredAspects.clear();
        discoveredAspects.addAll(other.discoveredAspects);
        completedNotes = other.completedNotes;
        dirty = true;
    }

    private CompoundTag saveKnowledge() {
        CompoundTag tag = new CompoundTag();
        knowledgePoints.forEach((type, map) -> {
            CompoundTag inner = new CompoundTag();
            map.forEach((category, amount) -> inner.putInt(category.toString(), amount));
            tag.put(type.name(), inner);
        });
        return tag;
    }

    private void loadKnowledge(CompoundTag tag) {
        for (String typeName : tag.getAllKeys()) {
            try {
                KnowledgeType type = KnowledgeType.valueOf(typeName);
                CompoundTag inner = tag.getCompound(typeName);
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (String key : inner.getAllKeys()) {
                    ResourceLocation category = ResourceLocation.tryParse(key);
                    if (category != null) {
                        map.put(category, inner.getInt(key));
                    }
                }
                knowledgePoints.put(type, map);
            } catch (IllegalArgumentException ignored) {
                // skip unknown knowledge types
            }
        }
    }

    private static ListTag stringList(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void readStrings(ListTag list, Set<ResourceLocation> into) {
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                into.add(id);
            }
        }
    }

    private static void writeSet(FriendlyByteBuf buf, Set<ResourceLocation> set) {
        buf.writeVarInt(set.size());
        for (ResourceLocation id : set) {
            buf.writeResourceLocation(id);
        }
    }

    private static void readSet(FriendlyByteBuf buf, Set<ResourceLocation> into) {
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            into.add(buf.readResourceLocation());
        }
    }
}
