package com.leclowndu93150.thaumaturge.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that assists research when it stands directly on top of a research table. The table
 * checks the block above each of its two halves whenever a player writes an aspect onto a note or
 * combines two aspects, and the chances of both blocks are added together.
 */
public interface IResearchTableAid {
    /**
     * The chance, between {@code 0} and {@code 1}, that an aspect written or combined at the table
     * below is not consumed.
     */
    float aspectSaveChance(Level level, BlockPos pos, BlockState state);
}
