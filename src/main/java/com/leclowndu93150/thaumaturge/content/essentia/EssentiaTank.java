package com.leclowndu93150.thaumaturge.content.essentia;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EssentiaTank implements IEssentiaHandler {
    /** CE26 jar suction values. */
    public static final int FILTERED_SUCTION = 64;
    public static final int UNFILTERED_SUCTION = 32;

    private final int capacity;
    private AspectList contents = AspectList.EMPTY;
    @Nullable
    private ResourceLocation filter;
    private boolean acceptsInput = true;
    private boolean suckWhenNotFull;
    @Nullable
    private Runnable listener;

    public EssentiaTank(int capacity) {
        this.capacity = capacity;
    }

    /** Called whenever stored contents or filter change. */
    public EssentiaTank onChange(@Nullable Runnable listener) {
        this.listener = listener;
        return this;
    }

    /** Output-only tanks (e.g. the crucible) refuse tube insertion. */
    public EssentiaTank acceptsInput(boolean acceptsInput) {
        this.acceptsInput = acceptsInput;
        return this;
    }

    /** Jar behaviour: exert suction (32, or 64 when filtered) while there is room. */
    public EssentiaTank suckWhenNotFull(boolean suck) {
        this.suckWhenNotFull = suck;
        return this;
    }

    private void changed() {
        if (listener != null) {
            listener.run();
        }
    }

    @Override
    public AspectList getEssentia() {
        return contents;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public int stored() {
        int total = 0;
        for (AspectInstance entry : contents.entries()) {
            total += entry.amount();
        }
        return total;
    }

    /** First (usually only) aspect held, or null. */
    @Nullable
    public ResourceLocation heldAspect() {
        return contents.isEmpty() ? null : contents.entries().get(0).aspectId();
    }

    @Override
    public int insert(ResourceLocation aspectId, int amount, boolean simulate) {
        if (amount <= 0 || !acceptsInput) {
            return 0;
        }
        if (filter != null && !filter.equals(aspectId)) {
            return 0;
        }
        if (!contents.isEmpty()) {
            // single-aspect tanks by default
            boolean hasOther = contents.entries().stream().anyMatch(e -> !e.aspectId().equals(aspectId));
            if (hasOther) {
                return 0;
            }
        }
        int space = Math.max(0, capacity - stored());
        int accepted = Math.min(space, amount);
        if (!simulate && accepted > 0) {
            contents = contents.add(new AspectInstance(aspectId, accepted));
            changed();
        }
        return accepted;
    }

    @Override
    public int extract(ResourceLocation aspectId, int amount, boolean simulate) {
        int available = contents.amountOf(aspectId);
        int taken = Math.min(available, Math.max(0, amount));
        if (!simulate && taken > 0) {
            contents = contents.remove(aspectId, taken);
            changed();
        }
        return taken;
    }

    @Override
    @Nullable
    public ResourceLocation getFilter() {
        return filter;
    }

    @Override
    public int getSuction(@Nullable Direction side) {
        if (!suckWhenNotFull || stored() >= capacity) {
            return 0;
        }
        return filter != null ? FILTERED_SUCTION : UNFILTERED_SUCTION;
    }

    @Override
    @Nullable
    public ResourceLocation getSuctionAspect(@Nullable Direction side) {
        return filter != null ? filter : heldAspect();
    }

    @Override
    public int getMinimumSuction() {
        return suckWhenNotFull ? (filter != null ? FILTERED_SUCTION : UNFILTERED_SUCTION) : 0;
    }

    @Override
    public boolean canInputFrom(@Nullable Direction side) {
        return acceptsInput;
    }

    public void setFilter(@Nullable ResourceLocation filter) {
        this.filter = filter;
        changed();
    }

    public void setContents(AspectList list) {
        this.contents = list == null ? AspectList.EMPTY : list;
        changed();
    }

    public void pourOut(Level level, BlockPos pos) {
        int amount = stored();
        if (!level.isClientSide && amount > 0) {
            setContents(AspectList.EMPTY);
            level.playSound(null, pos, TTSounds.JAR.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F, 1.0F);
            AuraHelper.polluteAura(level, pos, amount, true);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Capacity", capacity);
        AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, contents).result().ifPresent(t -> tag.put("Contents", t));
        if (filter != null) {
            tag.putString("Filter", filter.toString());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        contents = tag.contains("Contents")
                ? AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Contents")).result().orElse(AspectList.EMPTY)
                : AspectList.EMPTY;
        filter = tag.contains("Filter") ? ResourceLocation.tryParse(tag.getString("Filter")) : null;
    }
}
