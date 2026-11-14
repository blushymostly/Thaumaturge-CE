package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionPillarBlock;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModelChecks {
    private ClientModelChecks() {}

    @SubscribeEvent
    public static void checkModels(ModelEvent.BakingCompleted event) {
        Map<ResourceLocation, BakedModel> baked = event.getModels();
        BakedModel missing = event.getModelManager().getMissingModel();
        List<String> failures = new ArrayList<>();
        for (var item : TTItems.ITEMS.getEntries()) {
            checkModel(baked, missing, new ModelResourceLocation(item.getId(), "inventory"), failures);
        }
        int states = 0;
        for (var block : TTBlocks.BLOCKS.getEntries()) {
            for (var state : block.get().getStateDefinition().getPossibleStates()) {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    checkModel(baked, missing, BlockModelShaper.stateToModelLocation(state), failures);
                    states++;
                }
            }
        }
        checkGogglesTexture(failures);
        checkSpecificModels(baked, failures);
        if (!failures.isEmpty()) {
            throw new IllegalStateException("Thaumaturge model checks failed: " + String.join(", ", failures));
        }
        Thaumaturge.LOGGER.info("Model checks passed for {} items, {} block states and worn goggles",
                TTItems.ITEMS.getEntries().size(), states);
    }

    private static void checkSpecificModels(Map<ResourceLocation, BakedModel> baked, List<String> failures) {
        var random = RandomSource.create(0);
        var shield = baked.get(new ModelResourceLocation(TTItems.THAUMIUM_SHIELD.getId(), "inventory"));
        var vanillaShield = baked.get(new ModelResourceLocation(ResourceLocation.tryParse("minecraft:shield"), "inventory"));
        if (shield == null || !shield.isCustomRenderer() || vanillaShield == null) {
            failures.add("thaumium shield must use the entity renderer and vanilla poses");
        } else {
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
                var pose = shield.getTransforms().getTransform(context);
                var vanillaPose = vanillaShield.getTransforms().getTransform(context);
                if (!pose.rotation.equals(vanillaPose.rotation) || !pose.translation.equals(vanillaPose.translation)
                        || !pose.scale.equals(vanillaPose.scale)) {
                    failures.add("thaumium shield differs from the vanilla pose in " + context);
                }
            }
        }
        var node = baked.get(new ModelResourceLocation(TTItems.NODE.getId(), "inventory"));
        if (node != null && node.isGui3d()) {
            failures.add("node inventory model renders a block instead of its icon");
        }
        var brainJar = baked.get(new ModelResourceLocation(TTIds.rl("jar_brain"), "inventory"));
        if (brainJar == null || !brainJar.isCustomRenderer()) {
            failures.add("brain jar inventory model does not render its 3D contents");
        }
        var chargerBlock = TTBulkContent.BLOCKS.get("arcane_workbench_charger").get();
        var chargerState = chargerBlock.defaultBlockState();
        var charger = baked.get(BlockModelShaper.stateToModelLocation(chargerState));
        if (charger == null || (!charger.getRenderTypes(chargerState, random, ModelData.EMPTY).contains(RenderType.cutout())
                || charger.getRenderTypes(chargerState, random, ModelData.EMPTY).contains(RenderType.translucent()))
                || charger.getQuads(chargerState, null, random).stream().anyMatch(quad -> !quad.isShade())) {
            failures.add("workbench charger must use one cutout layer and shaded mesh faces");
        }
        var jarState = TTBlocks.JAR_NORMAL.get().defaultBlockState();
        var jar = baked.get(BlockModelShaper.stateToModelLocation(jarState));
        if (jar != null && !jar.getRenderTypes(jarState, random, ModelData.EMPTY).contains(RenderType.translucent())) {
            failures.add("jar glass is not in the translucent render layer");
        }
        var lampState = TTBlocks.LAMP_ARCANE.get().defaultBlockState();
        var lit = baked.get(BlockModelShaper.stateToModelLocation(lampState));
        var unlit = baked.get(BlockModelShaper.stateToModelLocation(lampState.setValue(BlockStateProperties.ENABLED, false)));
        var inventory = baked.get(new ModelResourceLocation(TTItems.LAMP_ARCANE.getId(), "inventory"));
        if (lit == unlit || lit != null && lit.getQuads(lampState, null, random).size() <= 6
                || inventory != null && inventory.getQuads(null, null, random).size() <= 6) {
            failures.add("arcane lamp is missing distinct lit/unlit or inventory geometry");
        }
        if (lit != null && !lit.getRenderTypes(lampState, random, ModelData.EMPTY).contains(RenderType.cutout())) {
            failures.add("arcane lamp does not use the cutout layer for its animated texture");
        }
        var table = baked.get(new ModelResourceLocation(TTItems.DECONSTRUCTION_TABLE.getId(), "inventory"));
        if (table == null || !table.isCustomRenderer()
                || TTBlocks.DECONSTRUCTION_TABLE.get().defaultBlockState().getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
            failures.add("deconstruction table is missing its shared world and inventory model");
        }
        var relayState = TTBlocks.REDSTONE_RELAY.get().defaultBlockState();
        var relay = baked.get(BlockModelShaper.stateToModelLocation(relayState));
        var relayPowered = baked.get(BlockModelShaper.stateToModelLocation(relayState.setValue(BlockStateProperties.POWERED, true)));
        if (relay == relayPowered || relay == null || relay.getQuads(relayState, null, random).size() <= 6) {
            failures.add("redstone relay has no separate powered model or torch geometry");
        }
        var dioptra = baked.get(new ModelResourceLocation(TTItems.DIOPTRA.getId(), "inventory"));
        if (dioptra == null || dioptra.getQuads(null, null, random).size() <= 6) {
            failures.add("dioptra inventory renders a cube");
        }
        var infernal = com.leclowndu93150.thaumaturge.registry.TTBulkContent.BLOCKS.get("infernal_furnace").get();
        for (var state : infernal.getStateDefinition().getPossibleStates()) {
            var furnace = baked.get(BlockModelShaper.stateToModelLocation(state));
            if (furnace == null || furnace.getQuads(state, null, random).size() <= 20
                    || !furnace.getRenderTypes(state, random, ModelData.EMPTY).contains(RenderType.cutout())) {
                failures.add("infernal furnace model lost its shell or transparent flame in " + state);
            }
        }
        var furnaceBlock = com.leclowndu93150.thaumaturge.registry.TTBulkContent.BLOCKS.get("advanced_alchemical_furnace").get();
        for (var state : furnaceBlock.getStateDefinition().getPossibleStates()) {
            var furnace = baked.get(BlockModelShaper.stateToModelLocation(state));
            if (furnace == null || furnace.getQuads(state, null, random).size() < 100) {
                failures.add("advanced furnace is missing the full CE mesh in " + state);
            }
        }
        if (TTBlocks.INFUSION_MATRIX.get().defaultBlockState().getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
            failures.add("infusion matrix has no animated model");
        }
        var wand = baked.get(new ModelResourceLocation(TTItems.WAND.getId(), "inventory"));
        if (wand == null || !wand.isCustomRenderer()) {
            failures.add("wand inventory model does not use the CE part renderer");
        }
        for (String material : List.of("arcane", "ancient", "eldritch")) {
            var pillar = TTBulkContent.BLOCKS.get("pillar_" + material).get();
            for (BlockPos corner : List.of(new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1),
                    new BlockPos(-1, 0, 1), new BlockPos(1, 0, 1))) {
                var facing = InfusionPillarBlock.faceMatrix(pillar.defaultBlockState(), corner);
                var placedModel = baked.get(BlockModelShaper.stateToModelLocation(facing));
                if (placedModel != null && !pillarTipPointsIn(placedModel, facing, corner)) {
                    failures.add(material + " pillar mesh leans away from the matrix at " + corner);
                }
            }
            var models = pillar.getStateDefinition().getPossibleStates().stream()
                    .map(state -> baked.get(BlockModelShaper.stateToModelLocation(state))).distinct().count();
            if (models != 4) failures.add(material + " pillar has no four directional models");
            var state = pillar.defaultBlockState();
            var model = baked.get(BlockModelShaper.stateToModelLocation(state));
            if (model == null || !model.getRenderTypes(state, random, ModelData.EMPTY).asList().equals(List.of(RenderType.cutout()))) {
                failures.add(material + " pillar must render only on the cutout layer");
            } else {
                checkPillarUvs(material, model, state, failures);
            }
        }
        var chest = baked.get(new ModelResourceLocation(TTItems.HUNGRY_CHEST.getId(), "inventory"));
        var chestTexture = Minecraft.getInstance().getTextureAtlas(Sheets.CHEST_SHEET).apply(TTIds.rl("entity/chest/hungry"));
        if (chest == null || !chest.isCustomRenderer() || !chestTexture.contents().name().equals(TTIds.rl("entity/chest/hungry"))) {
            failures.add("hungry chest renderer or chest atlas sprite is missing");
        }
    }

    private static boolean pillarTipPointsIn(BakedModel model, net.minecraft.world.level.block.state.BlockState state, BlockPos corner) {
        float tipX = 0, tipZ = 0;
        int count = 0;
        for (var quad : model.getQuads(state, null, RandomSource.create(0))) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int i = 0; i < vertices.length; i += stride) {
                if (Float.intBitsToFloat(vertices[i + 1]) <= 1.9F) continue;
                tipX += Float.intBitsToFloat(vertices[i]) - 0.5F;
                tipZ += Float.intBitsToFloat(vertices[i + 2]) - 0.5F;
                count++;
            }
        }
        return count > 0 && tipX * corner.getX() < 0 && tipZ * corner.getZ() < 0;
    }

    private static void checkPillarUvs(String material, BakedModel model, net.minecraft.world.level.block.state.BlockState state,
            List<String> failures) {
        var quads = model.getQuads(state, null, RandomSource.create(0));
        if (quads.isEmpty()) {
            failures.add(material + " pillar has no geometry");
            return;
        }
        var sprite = quads.get(0).getSprite();
        var path = TTIds.rl("textures/" + sprite.contents().name().getPath() + ".png");
        try (var stream = Minecraft.getInstance().getResourceManager().open(path); var texture = NativeImage.read(stream)) {
            for (var quad : quads) {
                var vertices = quad.getVertices();
                int stride = vertices.length / 4;
                float u = 0, v = 0;
                for (int i = 0; i < 4; i++) {
                    u += Float.intBitsToFloat(vertices[i * stride + 4]) / 4;
                    v += Float.intBitsToFloat(vertices[i * stride + 5]) / 4;
                }
                int x = (int) ((u - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * texture.getWidth());
                int y = (int) ((v - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * texture.getHeight());
                if ((texture.getPixelRGBA(x, y) >>> 24) == 0) {
                    failures.add(material + " pillar quad samples an invisible texture region");
                    break;
                }
            }
        } catch (IOException e) {
            failures.add(material + " pillar texture could not be decoded");
        }
    }

    private static void checkModel(Map<ResourceLocation, BakedModel> baked, BakedModel missing, ResourceLocation id, List<String> failures) {
        if (baked.getOrDefault(id, missing) == missing) {
            failures.add(id.toString());
        }
    }

    private static void checkGogglesTexture(List<String> failures) {
        var goggles = TTItems.GOGGLES_REVEALING.get();
        String path = goggles.getArmorTexture(new ItemStack(goggles), null, EquipmentSlot.HEAD, null);
        if (path == null || !path.startsWith(TTIds.MODID + ":")) {
            failures.add("goggles use a fallback armor texture");
            return;
        }
        var texture = ResourceLocation.tryParse(path);
        if (texture == null) {
            failures.add(path);
            return;
        }
        try (var stream = Minecraft.getInstance().getResourceManager().open(texture); var image = NativeImage.read(stream)) {
            if (image.getWidth() != 2 * image.getHeight()) {
                failures.add(path + " has incompatible humanoid armor dimensions");
            }
        } catch (IOException e) {
            failures.add(path + " could not be decoded");
        }
    }
}
