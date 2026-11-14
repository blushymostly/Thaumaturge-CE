package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.world.objects.MagicalCaveFloorPlacement;
import com.leclowndu93150.thaumaturge.content.world.objects.SurfaceBiomeSearch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TTPlacementModifiers {
    private static final DeferredRegister<PlacementModifierType<?>> TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, TTIds.MODID);

    public static final RegistryObject<PlacementModifierType<MagicalCaveFloorPlacement>> MAGICAL_CAVE_FLOOR =
            TYPES.register("magical_cave_floor", () -> () -> MagicalCaveFloorPlacement.CODEC);

    public static final RegistryObject<PlacementModifierType<SurfaceBiomeSearch>> SURFACE_BIOME_SEARCH =
            TYPES.register("surface_biome_search", () -> () -> SurfaceBiomeSearch.CODEC);

    private TTPlacementModifiers() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
