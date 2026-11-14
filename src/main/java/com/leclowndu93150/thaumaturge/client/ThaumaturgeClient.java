package com.leclowndu93150.thaumaturge.client;

import com.leclowndu93150.thaumaturge.client.render.FluxRiftRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.hud.AuraHudOverlay;
import com.leclowndu93150.thaumaturge.client.hud.KnowledgeGainOverlay;
import com.leclowndu93150.thaumaturge.client.render.blockentity.CrucibleRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.BellowsRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.RechargePedestalRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.DioptraRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.DeconstructionTableRenderer;
import com.leclowndu93150.thaumaturge.client.model.entity.DeconTableModel;
import com.leclowndu93150.thaumaturge.client.render.blockentity.HungryChestRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.ResearchTableRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.WorkbenchWandRenderer;
import com.leclowndu93150.thaumaturge.client.particle.BlockRunesParticle;
import com.leclowndu93150.thaumaturge.client.particle.SparkleParticle;
import com.leclowndu93150.thaumaturge.content.golem.ThaumicGolemRenderer;
import com.leclowndu93150.thaumaturge.content.item.VisCrystalItem;
import com.leclowndu93150.thaumaturge.content.misc.nitor.BlockNitor;
import com.leclowndu93150.thaumaturge.content.taint.TaintSeedRenderer;
import com.leclowndu93150.thaumaturge.content.research.ResearchReloadListener;
import com.leclowndu93150.thaumaturge.content.world.crystal.BlockCrystal;
import com.leclowndu93150.thaumaturge.client.screen.research.DeconstructionTableScreen;
import com.leclowndu93150.thaumaturge.client.screen.research.ResearchTableScreen;
import com.leclowndu93150.thaumaturge.client.screen.workbench.ArcaneWorkbenchScreen;
import com.leclowndu93150.thaumaturge.client.render.blockentity.JarRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.BrainJarRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.VisRelayRenderer;
import com.leclowndu93150.thaumaturge.client.render.blockentity.NodeRenderer;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTMenus;
import com.leclowndu93150.thaumaturge.registry.TTParticles;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ThaumaturgeClient {
    private ThaumaturgeClient() {}

    @SubscribeEvent
    public static void onClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResearchReloadListener());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientAuraCache.clear();
        event.enqueueWork(() -> {
            MenuScreens.register(TTMenus.ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new);
            MenuScreens.register(TTMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            MenuScreens.register(TTMenus.DECONSTRUCTION_TABLE.get(), DeconstructionTableScreen::new);
            net.minecraft.client.renderer.item.ItemProperties.register(TTItems.THAUMIUM_SHIELD.get(), TTIds.rl("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            MenuScreens.register(TTMenus.SMELTER.get(), com.leclowndu93150.thaumaturge.client.screen.SmelterScreen::new);
            net.minecraft.client.renderer.item.ItemProperties.register(TTItems.PHIAL.get(), TTIds.rl("filled"),
                    (stack, level, entity, seed) -> com.leclowndu93150.thaumaturge.registry.TTItemData.getAspects(stack).isEmpty() ? 0.0F : 1.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(TTItems.RESEARCH_NOTE.get(), TTIds.rl("complete"),
                    (stack, level, entity, seed) -> {
                        var data = com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes.dataOf(stack);
                        return data != null && data.complete() ? 1.0F : 0.0F;
                    });
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TTEntities.GOLEM.get(), ThaumicGolemRenderer::new);
        event.registerEntityRenderer(TTEntities.TAINT_SEED.get(), TaintSeedRenderer::new);
        event.registerEntityRenderer(TTEntities.FLUX_RIFT.get(), FluxRiftRenderer::new);
        event.registerEntityRenderer(TTEntities.CAUSALITY_COLLAPSER.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.ESSENTIA_JAR.get(), JarRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.BRAIN_JAR.get(), BrainJarRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.VIS_RELAY.get(), VisRelayRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.CRUCIBLE.get(), CrucibleRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.BELLOWS.get(), BellowsRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.RECHARGE_PEDESTAL.get(), RechargePedestalRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.ARCANE_WORKBENCH.get(), WorkbenchWandRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.RESEARCH_TABLE.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.NODE.get(), NodeRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.JAR_NODE.get(), NodeRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.HUNGRY_CHEST.get(), HungryChestRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.DIOPTRA.get(), DioptraRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.INFUSION_MATRIX.get(),
                com.leclowndu93150.thaumaturge.client.render.blockentity.InfusionMatrixRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.INFUSION_PEDESTAL.get(),
                com.leclowndu93150.thaumaturge.client.render.blockentity.InfusionPedestalRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.DECONSTRUCTION_TABLE.get(), DeconstructionTableRenderer::new);
    }

    @SubscribeEvent
    public static void registerBellowsModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        BellowsRenderer.registerModels(event);
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BrainJarRenderer.LAYER, BrainJarRenderer::createLayer);
        event.registerLayerDefinition(BrainJarRenderer.BRINE_LAYER, BrainJarRenderer::createBrineLayer);
        event.registerLayerDefinition(DeconstructionTableRenderer.LAYER, DeconTableModel::createLayer);
        event.registerLayerDefinition(com.leclowndu93150.thaumaturge.client.render.blockentity.InfusionMatrixRenderer.LAYER,
                com.leclowndu93150.thaumaturge.client.model.entity.MatrixCubeModel::createLayer);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TTParticles.WISP.get(), TTSimpleParticle.Provider::new);
        event.registerSpriteSet(TTParticles.BORE.get(), TTSimpleParticle.Provider::new);
        event.registerSpriteSet(TTParticles.TAINT.get(), TTSimpleParticle.Provider::new);
        event.registerSpriteSet(TTParticles.ESSENTIA.get(), TTSimpleParticle.Provider::new);
        event.registerSpecial(TTParticles.SPARKLE.get(), new SparkleParticle.Provider());
        event.registerSpecial(TTParticles.BLOCK_RUNES.get(), new BlockRunesParticle.Provider());
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "knowledge_gain",
                (gui, graphics, partial, width, height) -> KnowledgeGainOverlay.render(graphics, partial, width, height));
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "wand_dial",
                (gui, graphics, partial, width, height) -> com.leclowndu93150.thaumaturge.client.hud.WandHudOverlay.render(graphics));
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "aura_gauge",
                (gui, graphics, partialTick, width, height) -> AuraHudOverlay.render(graphics, partialTick));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(VisCrystalItem::tintColor, TTItems.ESSENTIA_CRYSTAL.get());
        event.register(com.leclowndu93150.thaumaturge.content.item.PhialItem::tintColor, TTItems.PHIAL.get());
        event.register((stack, tint) -> {
            if (tint != 1) {
                return 0xFFFFFF;
            }
            var data = com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes.dataOf(stack);
            return data == null ? 0x999999 : (data.color() & 0xFFFFFF);
        }, TTItems.RESEARCH_NOTE.get());
        event.register((stack, tint) -> tint == 0 ? 0xFFFF7E : 0xFFFFFF, TTItems.CRYSTAL_AER.get());
        event.register((stack, tint) -> tint == 0 ? 0xFF5A01 : 0xFFFFFF, TTItems.CRYSTAL_IGNIS.get());
        event.register((stack, tint) -> tint == 0 ? 0x3CD4FC : 0xFFFFFF, TTItems.CRYSTAL_AQUA.get());
        event.register((stack, tint) -> tint == 0 ? 0x56C000 : 0xFFFFFF, TTItems.CRYSTAL_TERRA.get());
        event.register((stack, tint) -> tint == 0 ? 0xD5D4EC : 0xFFFFFF, TTItems.CRYSTAL_ORDO.get());
        event.register((stack, tint) -> tint == 0 ? 0x404040 : 0xFFFFFF, TTItems.CRYSTAL_PERDITIO.get());
        event.register((stack, tint) -> tint == 0 ? 0x800080 : 0xFFFFFF, TTItems.CRYSTAL_VITIUM.get());
        TTItems.NITORS.forEach((dye, item) -> event.register((stack, tint) -> dye.getFireworkColor(), item.get()));
        for (DyeColor dye : DyeColor.values()) {
            Block candle = bulkBlock("candle_" + dye.getName());
            if (candle != null) {
                event.register((stack, tint) -> tint == 0 ? dye.getMapColor().col : 0xFFFFFF, candle);
            }
        }
        Block greatwoodLeaves = bulkBlock("leaves_greatwood");
        if (greatwoodLeaves != null) {
            event.register((stack, tint) -> FoliageColor.getDefaultColor(), greatwoodLeaves);
        }
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DyeColor dye : DyeColor.values()) {
            Block candle = bulkBlock("candle_" + dye.getName());
            if (candle != null) {
                event.register((state, level, pos, tint) -> tint == 0 ? dye.getMapColor().col : 0xFFFFFF, candle);
            }
        }
        event.register((BlockState state, net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, int tint) -> {
            if (state.getBlock() instanceof BlockCrystal crystal) {
                IAspect aspect = TTAspects.get(crystal.aspect());
                return aspect != null ? aspect.color() : 0xFFFFFF;
            }
            return 0xFFFFFF;
        }, TTBlocks.CRYSTAL_AER.get(), TTBlocks.CRYSTAL_IGNIS.get(), TTBlocks.CRYSTAL_AQUA.get(), TTBlocks.CRYSTAL_TERRA.get(),
                TTBlocks.CRYSTAL_ORDO.get(), TTBlocks.CRYSTAL_PERDITIO.get(), TTBlocks.CRYSTAL_VITIUM.get());
        Block[] nitors = TTBlocks.NITORS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
        event.register((state, level, pos, tint) -> state.getBlock() instanceof BlockNitor nitor ? nitor.dyeColor() : 0xFFFFFF, nitors);
        Block greatwoodLeaves = bulkBlock("leaves_greatwood");
        if (greatwoodLeaves != null) {
            event.register((state, level, pos, tint) -> {
                if (level == null || pos == null) {
                    return FoliageColor.getDefaultColor();
                }
                return BiomeColors.getAverageFoliageColor(level, pos);
            }, greatwoodLeaves);
        }
    }

    private static Block bulkBlock(String id) {
        RegistryObject<Block> entry = TTBulkContent.BLOCKS.get(id);
        return entry == null ? null : entry.get();
    }
}
