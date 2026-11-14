package com.leclowndu93150.thaumaturge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ThaumaturgeCommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue NO_SLEEP;
    public static final ForgeConfigSpec.BooleanValue WUSS_MODE;
    public static final ForgeConfigSpec.BooleanValue PHYSICAL_FLUX_AURA_FLOOR;
    public static final ForgeConfigSpec.BooleanValue PHYSICAL_FLUX_TAINT_OUTBREAKS;
    public static final ForgeConfigSpec.BooleanValue FLUX_PRESSURE_EVENTS;
    public static final ForgeConfigSpec.IntValue TAINT_STARVE_TICKS;
    public static final ForgeConfigSpec.DoubleValue TAINTED_LANDS_STARTING_FLUX;
    public static final ForgeConfigSpec.DoubleValue ENERGIZED_NODE_VIS_PER_POINT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("progression");
        NO_SLEEP = builder
                .comment("If true, grant the Salis Mundus dream journal when picking up a vis crystal without needing to sleep.")
                .define("noSleep", false);
        builder.pop();
        builder.push("world");
        WUSS_MODE = builder.comment("Disable flux rifts, pressure events and taint spread.").define("wussMode", false);
        PHYSICAL_FLUX_AURA_FLOOR = builder.comment("Physical flux sustains a minimum level of aura flux.").define("physicalFluxAuraFloor", true);
        PHYSICAL_FLUX_TAINT_OUTBREAKS = builder.comment("Concentrations of physical flux can spawn taint seeds.").define("physicalFluxTaintOutbreaks", true);
        FLUX_PRESSURE_EVENTS = builder.comment("Allow local flux rain and lightning from high aura flux.").define("fluxPressureEvents", true);
        TAINT_STARVE_TICKS = builder.comment("Ticks of continuous protection before a tainted node is purified.")
                .defineInRange("taintStarveTicks", 12000, 20, 1000000);
        TAINTED_LANDS_STARTING_FLUX = builder
                .comment("Starting flux in Tainted Lands chunks, as a fraction of the chunk's base aura.")
                .defineInRange("taintedLandsStartingFlux", 1.0D, 0.0D, 10.0D);
        builder.pop();
        builder.push("aura");
        ENERGIZED_NODE_VIS_PER_POINT = builder.comment("Raw vis or flux used per point restored to an energized node.")
                .defineInRange("energizedNodeVisPerPoint", 6.0D, 0.0D, 1000.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ThaumaturgeCommonConfig() {}
}
