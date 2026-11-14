package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeFeature;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeFeatureConfig;
import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalClusterConfig;
import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalClusterFeature;
import com.leclowndu93150.thaumaturge.content.world.objects.MagicalCaveBushFeature;
import com.leclowndu93150.thaumaturge.content.world.objects.MagicalCavePondFeature;
import com.leclowndu93150.thaumaturge.content.world.objects.ObsidianTotemFeature;
import com.leclowndu93150.thaumaturge.content.world.plant.MagicForestFloraConfig;
import com.leclowndu93150.thaumaturge.content.world.plant.MagicForestFloraFeature;
import com.leclowndu93150.thaumaturge.content.world.objects.HilltopStonesFeature;
import com.leclowndu93150.thaumaturge.content.world.objects.TaintedLandsSurfaceFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, TTIds.MODID);

    public static final RegistryObject<CrystalClusterFeature> CRYSTAL_CLUSTER = FEATURES.register("crystal_cluster",
            () -> new CrystalClusterFeature(CrystalClusterConfig.CODEC));
    public static final RegistryObject<NodeFeature> NODE = FEATURES.register("node",
            () -> new NodeFeature(NodeFeatureConfig.CODEC));

    public static final RegistryObject<MagicalCaveBushFeature> MAGICAL_CAVE_BUSH = FEATURES.register("magical_cave_bush",
            () -> new MagicalCaveBushFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<MagicalCavePondFeature> MAGICAL_CAVE_POND = FEATURES.register("magical_cave_pond",
            () -> new MagicalCavePondFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<ObsidianTotemFeature> OBSIDIAN_TOTEM = FEATURES.register("obsidian_totem",
            () -> new ObsidianTotemFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<MagicForestFloraFeature> MAGIC_FOREST_FLORA = FEATURES.register("magic_forest_flora",
            () -> new MagicForestFloraFeature(MagicForestFloraConfig.CODEC));

    public static final RegistryObject<HilltopStonesFeature> HILLTOP_STONES = FEATURES.register("hilltop_stones",
            () -> new HilltopStonesFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<TaintedLandsSurfaceFeature> TAINTED_LANDS_SURFACE = FEATURES.register("taint_biome",
            () -> new TaintedLandsSurfaceFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    private TTFeatures() {}

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
