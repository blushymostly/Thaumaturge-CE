package com.leclowndu93150.thaumaturge.api.recipe;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A transformation Salis Mundus performs when used on a block, either a single-block conversion or a
 * multiblock construction.
 *
 * <p>Implementations are recipes of the recipe type {@code thaumaturge:dust_trigger}. All methods run on the
 * logical server thread (except {@code matches}, which the client may call to decide whether to swing).
 */
public interface DustTrigger extends Recipe<DustTriggerInput> {
    /** Research gate that must be satisfied before the trigger fires. */
    Optional<DustGate> researchGate();

    default boolean doesPassGate(Player player) {
        return researchGate().map(gate -> gate.passes(player)).orElse(true);
    }

    /** Special recipes are never offered by the recipe book. */
    @Override
    default boolean isSpecial() {
        return true;
    }

    /** Block positions that receive the sparkle effect when the trigger fires. */
    default List<BlockPos> sparkle(Level level, Player player, BlockPos clicked, DustTriggerPlacement placement) {
        return List.of(clicked);
    }

    /** Placement at which a multiblock trigger matches around the clicked block, or {@code null}. */
    @Nullable
    default DustTriggerPlacement findPlacement(DustTriggerInput input) {
        return null;
    }

    /** {@code true} when the trigger performs every world change itself through {@link #execute}. */
    default boolean isMultiblock() {
        return false;
    }

    /** Applies or schedules the world changes of this trigger. */
    void execute(DustTriggerInput input, Player player, @Nullable DustTriggerPlacement placement, Direction face);
}
