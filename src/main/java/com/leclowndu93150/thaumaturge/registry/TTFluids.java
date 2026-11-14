package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.taint.flux.FluxGooFluid;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, TTIds.MODID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, TTIds.MODID);
    public static final RegistryObject<FluidType> GOO_TYPE = TYPES.register("flux_goo", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.thaumaturge.flux_goo").density(1400).viscosity(2000)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return TTIds.rl("block/flux_goo"); }
                @Override public ResourceLocation getFlowingTexture() { return getStillTexture(); }
            });
        }
    });
    public static final RegistryObject<FlowingFluid> GOO_SOURCE = FLUIDS.register("flux_goo", FluxGooFluid.Source::new);
    public static final RegistryObject<FlowingFluid> GOO_FLOWING = FLUIDS.register("flowing_flux_goo", FluxGooFluid.Flowing::new);
    public static final ForgeFlowingFluid.Properties GOO_PROPERTIES = new ForgeFlowingFluid.Properties(GOO_TYPE, GOO_SOURCE, GOO_FLOWING)
            .block(() -> (LiquidBlock) TTBulkContent.BLOCKS.get("flux_goo").get())
            .bucket(() -> TTBulkItems.ITEMS.get("flux_goo_bucket").get()).tickRate(10);

    private TTFluids() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
