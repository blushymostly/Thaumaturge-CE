package com.leclowndu93150.thaumaturge.api.aspect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class AspectList {
    public static final AspectList EMPTY = new AspectList(Collections.emptyList());

    public static final Codec<AspectList> CODEC = AspectInstance.CODEC.listOf().flatXmap(entries -> {
        AspectList result = AspectList.EMPTY;
        for (AspectInstance entry : entries) {
            result = result.add(entry);
        }
        return DataResult.success(result);
    }, list -> DataResult.success(list.entries()));

    private final List<AspectInstance> entries;

    private AspectList(List<AspectInstance> entries) {
        this.entries = List.copyOf(entries);
    }

    public static AspectList ofEntries(List<AspectInstance> entries) {
        AspectList result = EMPTY;
        for (AspectInstance entry : entries) {
            result = result.add(entry);
        }
        return result;
    }

    public List<AspectInstance> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public List<AspectInstance> sortedByAmount() {
        List<AspectInstance> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Integer.compare(b.amount(), a.amount()));
        return sorted;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int amountOf(ResourceLocation aspectId) {
        for (AspectInstance entry : entries) {
            if (entry.aspectId().equals(aspectId)) {
                return entry.amount();
            }
        }
        return 0;
    }

    public AspectList add(AspectInstance instance) {
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>();
        for (AspectInstance entry : entries) {
            merged.put(entry.aspectId(), entry.amount());
        }
        merged.merge(instance.aspectId(), instance.amount(), Integer::sum);
        List<AspectInstance> next = new ArrayList<>(merged.size());
        merged.forEach((id, amount) -> next.add(new AspectInstance(id, amount)));
        return new AspectList(next);
    }

    public AspectList remove(ResourceLocation aspectId, int amount) {
        if (amount <= 0 || isEmpty()) {
            return this;
        }
        List<AspectInstance> next = new ArrayList<>();
        for (AspectInstance entry : entries) {
            if (!entry.aspectId().equals(aspectId)) {
                next.add(entry);
                continue;
            }
            int remain = entry.amount() - amount;
            if (remain > 0) {
                next.add(entry.withAmount(remain));
            }
        }
        return next.isEmpty() ? EMPTY : new AspectList(next);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (AspectInstance entry : entries) {
            entry.write(buf);
        }
    }

    public static AspectList read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<AspectInstance> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(AspectInstance.read(buf));
        }
        return ofEntries(list);
    }
}
