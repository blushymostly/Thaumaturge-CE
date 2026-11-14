package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import org.jetbrains.annotations.Nullable;

/** Subset of CE {@code NodeRules}: refill cadence and modifier degradation. */
final class NodeRules {
    private static final int REFILL_AVERAGE = 600;
    private static final int REFILL_BRIGHT = 400;
    private static final int REFILL_PALE = 900;
    private static final int REFILL_FADING = 0;

    private NodeRules() {}

    /** Ticks between single-point refills; {@code 0} means the node never refills. */
    static int baseRefillInterval(@Nullable NodeModifier modifier) {
        if (modifier == null) {
            return REFILL_AVERAGE;
        }
        return switch (modifier) {
            case BRIGHT -> REFILL_BRIGHT;
            case PALE -> REFILL_PALE;
            case FADING -> REFILL_FADING;
        };
    }

    @Nullable
    static NodeModifier degrade(@Nullable NodeModifier modifier) {
        if (modifier == null) {
            return NodeModifier.PALE;
        }
        return switch (modifier) {
            case BRIGHT -> null;
            case PALE, FADING -> NodeModifier.FADING;
        };
    }
}
