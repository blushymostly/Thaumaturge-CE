package com.leclowndu93150.thaumaturge.api.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Recipe input for {@link DustTrigger}: the dust stack (slot 0), the level, the clicked position and state.
 *
 * <p>1.20.1 recipes take a {@link net.minecraft.world.Container}, so this stands in for the CE {@code RecipeInput} record.
 */
public final class DustTriggerInput extends SimpleContainer {
    private final Level level;
    private final BlockPos pos;
    private final BlockState clicked;

    public DustTriggerInput(ItemStack dust, Level level, BlockPos pos, BlockState clicked) {
        super(dust);
        this.level = level;
        this.pos = pos.immutable();
        this.clicked = clicked;
    }

    public ItemStack dust() {
        return this.getItem(0);
    }

    public Level level() {
        return this.level;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public BlockState clicked() {
        return this.clicked;
    }
}
