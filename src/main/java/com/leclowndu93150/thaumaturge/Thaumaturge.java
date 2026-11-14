package com.leclowndu93150.thaumaturge;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.compat.curios.ThaumaturgeCuriosCompat;
import com.leclowndu93150.thaumaturge.compat.terrablender.TTTerraBlenderCompat;
import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.leclowndu93150.thaumaturge.content.aura.AuraGenHandler;
import com.leclowndu93150.thaumaturge.content.aura.AuraTickHandler;
import com.leclowndu93150.thaumaturge.content.eldritch.labyrinth.LabyrinthBootstrap;
import com.leclowndu93150.thaumaturge.content.golem.GolemBootstrap;
import com.leclowndu93150.thaumaturge.content.taint.TaintBootstrap;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import com.leclowndu93150.thaumaturge.registry.TTCreativeTabs;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTFeatures;
import com.leclowndu93150.thaumaturge.registry.TTFluids;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTMenus;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTParticles;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import com.leclowndu93150.thaumaturge.registry.TTTreePlacers;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TTIds.MODID)
public final class Thaumaturge {
    public static final Logger LOGGER = LoggerFactory.getLogger(TTIds.MODID);

    public Thaumaturge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ThaumaturgeCommonConfig.SPEC);
        modBus.addListener(this::commonSetup);

        TTAspects.bootstrap();
        TTBulkContent.bootstrap();
        TTBulkItems.bootstrap();

        TTFluids.register(modBus);
        TTMobEffects.register(modBus);
        TTBlocks.register(modBus);
        TTItems.register(modBus);
        TTFeatures.register(modBus);
        com.leclowndu93150.thaumaturge.registry.TTPlacementModifiers.register(modBus);
        TTTreePlacers.register(modBus);
        TTBlockEntities.register(modBus);
        TTEntities.register(modBus);
        TTMenus.register(modBus);
        TTRecipeTypes.register(modBus);
        TTParticles.register(modBus);
        TTSounds.register(modBus);
        TTCreativeTabs.register(modBus);
        TTItemData.bootstrap();
        TTAttachments.register(modBus);
        TTCapabilities.register(modBus);
        TTNetwork.register();

        GolemBootstrap.init(modBus);
        TaintBootstrap.init(modBus);
        LabyrinthBootstrap.init(modBus);

        MinecraftForge.EVENT_BUS.register(TTAttachments.class);
        MinecraftForge.EVENT_BUS.register(TTCapabilities.class);
        MinecraftForge.EVENT_BUS.register(AuraGenHandler.class);
        MinecraftForge.EVENT_BUS.register(AuraTickHandler.class);
        MinecraftForge.EVENT_BUS.register(TaintBootstrap.class);

        if (ModList.get().isLoaded(TTIds.CURIOS)) {
            ThaumaturgeCuriosCompat.init(modBus);
        }

        LOGGER.info("Thaumaturge Forge 1.20.1 port loaded ({} aspects, bulk content online)", TTAspects.all().size());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("terrablender")) {
                TTTerraBlenderCompat.init();
                LOGGER.info("Thaumaturge TerraBlender regions registered (magical forest / tainted lands)");
            } else {
                LOGGER.warn("TerraBlender missing — magical_forest/tainted_lands will not place; trees still use forge biome modifiers");
            }
        });
    }
}
