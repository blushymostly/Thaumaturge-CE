package com.leclowndu93150.thaumaturge.content.recipe.dust;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block swap scheduled by a dust trigger; persisted per chunk until it lands. */
public record PendingSwap(BlockPos pos, BlockState originalState, @Nullable ItemStack resultStack, @Nullable BlockState resultState, int ticksRemaining) {
    public PendingSwap withTicks(int ticks) {
        return new PendingSwap(this.pos, this.originalState, this.resultStack, this.resultState, ticks);
    }

    public static PendingSwap dropItem(BlockPos pos, BlockState original, ItemStack stack, int delay) {
        return new PendingSwap(pos.immutable(), original, stack.copy(), null, delay);
    }

    public static PendingSwap placeBlock(BlockPos pos, BlockState original, BlockState placed, int delay) {
        return new PendingSwap(pos.immutable(), original, null, placed, delay);
    }

    public static PendingSwap clearOnly(BlockPos pos, BlockState original, int delay) {
        return new PendingSwap(pos.immutable(), original, null, null, delay);
    }

    public @Nullable ItemStack stackResult() {
        return this.resultStack;
    }

    public @Nullable BlockState stateResult() {
        return this.resultState;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", this.pos.asLong());
        tag.put("original_state", NbtUtils.writeBlockState(this.originalState));
        if (this.resultStack != null) {
            tag.put("result_stack", this.resultStack.save(new CompoundTag()));
        }
        if (this.resultState != null) {
            tag.put("result_state", NbtUtils.writeBlockState(this.resultState));
        }
        tag.putInt("ticks_remaining", this.ticksRemaining);
        return tag;
    }

    public static PendingSwap load(CompoundTag tag) {
        BlockPos pos = BlockPos.of(tag.getLong("pos"));
        BlockState original = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("original_state"));
        ItemStack stack = tag.contains("result_stack") ? ItemStack.of(tag.getCompound("result_stack")) : null;
        BlockState state = tag.contains("result_state") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("result_state")) : null;
        return new PendingSwap(pos, original, stack, state, tag.getInt("ticks_remaining"));
    }
}
