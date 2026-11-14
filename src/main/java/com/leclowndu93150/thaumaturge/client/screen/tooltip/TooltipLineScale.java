package com.leclowndu93150.thaumaturge.client.screen.tooltip;

public enum TooltipLineScale {
    NORMAL(1.0F),
    SMALL(0.75F);

    private final float scale;

    TooltipLineScale(float scale) {
        this.scale = scale;
    }

    public float scale() {
        return scale;
    }
}
