package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator;
import com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.AdvancedFurnace;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestal;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.content.research.scan.DiscoveryScans;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanBindings;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class InfusionWaveTests {
    private InfusionWaveTests() {}

    @GameTest(template = "essentia_lab")
    public static void discoveryFlagsFromScansAndOldSaves(GameTestHelper helper) {
        var player = player(helper, "tt_scan_flags");
        var knowledge = TTAttachments.knowledge(player);
        ScanBindings.scanItem(player, new ItemStack(Items.IRON_INGOT));
        ScanBindings.scanItem(player, new ItemStack(Items.ENDER_PEARL));
        knowledge.scan(net.minecraft.resources.ResourceLocation.tryParse("minecraft:bat"));
        DiscoveryScans.restoreKnownScans(player);
        for (String flag : List.of("f_matiron", "f_teleport", "f_bat", "f_fly")) {
            helper.assertTrue(knowledge.isComplete(TTIds.rl(flag)), "No discovery flag " + flag);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void infusionPillarsFaceMatrixAndOldAltarsRecover(GameTestHelper helper) {
        var player = player(helper, "tt_pillar_directions");
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("infusion"), 3);
        BlockPos matrixPos = new BlockPos(2, 3, 2);
        build(helper, player, matrixPos, "infusion_altar");
        var offsets = List.of(new BlockPos(-1, -2, -1), new BlockPos(1, -2, -1),
                new BlockPos(-1, -2, 1), new BlockPos(1, -2, 1));
        var directions = List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH);
        var legacyDirections = List.of(Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST);
        Runnable assertFacings = () -> {
            for (int i = 0; i < offsets.size(); i++) {
                var state = helper.getBlockState(matrixPos.offset(offsets.get(i)));
                helper.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == directions.get(i),
                        "Pillar points away from the matrix at " + offsets.get(i));
            }
        };
        assertFacings.run();
        for (int i = 0; i < offsets.size(); i++) {
            helper.setBlock(matrixPos.offset(offsets.get(i)), bulk("pillar_arcane").defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, legacyDirections.get(i)));
        }
        helper.succeedWhen(assertFacings);
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 1000)
    public static void infusionBuildsAndWaitsForEssentia(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_infusion_wave");
        var knowledge = TTAttachments.knowledge(player);
        knowledge.markResearchComplete(TTIds.rl("infusion"), 3);
        knowledge.markResearchComplete(TTIds.rl("jar_brain"), 2);
        BlockPos matrixPos = new BlockPos(2, 3, 2);
        build(helper, player, matrixPos, "infusion_altar");
        var matrix = (InfusionMatrix) helper.getBlockEntity(matrixPos);
        var center = (InfusionPedestal) helper.getBlockEntity(matrixPos.below(2));
        center.setItem(new ItemStack(TTItems.JAR_NORMAL.get()));
        var components = List.of(new ItemStack(TTBulkItems.ITEMS.get("brain").get()), new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SPIDER_EYE));
        var offsets = List.of(new BlockPos(-3, -2, 0), new BlockPos(3, -2, 0), new BlockPos(0, -2, -3), new BlockPos(0, -2, 3));
        for (int i = 0; i < components.size(); i++) {
            BlockPos at = matrixPos.offset(offsets.get(i));
            helper.setBlock(at, bulk("pedestal_arcane"));
            ((InfusionPedestal) helper.getBlockEntity(at)).setItem(components.get(i));
        }
        var candles = List.of("orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red");
        for (int i = 0; i < candles.size(); i++) {
            int x = 2 + i / 7;
            int z = i % 7 - 3;
            if (x == 3 && z >= 0) z++;
            BlockPos first = matrixPos.offset(x, -2, z);
            BlockPos second = matrixPos.offset(-x, -2, -z);
            helper.setBlock(first.below(), Blocks.STONE);
            helper.setBlock(second.below(), Blocks.STONE);
            helper.setBlock(first, bulk("candle_" + candles.get(i)));
            helper.setBlock(second, bulk("candle_" + candles.get(i)));
        }
        List<BlockPos> jars = List.of(matrixPos.offset(-1, -2, 4), matrixPos.offset(0, -2, 4), matrixPos.offset(1, -2, 4));
        var aspects = List.of(TTAspects.COGNITIO, TTAspects.SENSUS, TTAspects.EXANIMIS);
        for (int i = 0; i < aspects.size(); i++) {
            helper.setBlock(jars.get(i), TTBlocks.JAR_NORMAL.get());
            var jar = (EssentiaJarBlockEntity) helper.getBlockEntity(jars.get(i));
            if (i == 0) jar.tank().insert(aspects.get(i), 2, false);
        }
        helper.assertTrue(matrix.activate(level, player), "Wand could not activate the altar");
        // Saves made by the former all-at-once ritual have spent no resources.
        var legacy = matrix.saveWithoutMetadata();
        legacy.putString("Recipe", "thaumaturge:infusion/jar_brain");
        legacy.putUUID("Owner", player.getUUID());
        legacy.putInt("Work", 1);
        matrix.load(legacy);

        helper.runAfterDelay(6, () -> {
            var first = (EssentiaJarBlockEntity) helper.getBlockEntity(jars.get(0));
            helper.assertTrue(first.tank().stored() < 2, "The migrated job did not start draining essentia");
            helper.assertTrue(matrix.saveWithoutMetadata().contains("Job"), "The legacy job was not converted to the staged format");
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(center.item().is(TTItems.JAR_NORMAL.get()), "Infusion finished before it had essentia");
            for (int i = 0; i < components.size(); i++) {
                var pedestal = (InfusionPedestal) helper.getBlockEntity(matrixPos.offset(offsets.get(i)));
                helper.assertTrue(ItemStack.matches(pedestal.item(), components.get(i)), "Essentia phase consumed an item");
            }
            var partial = matrix.saveWithoutMetadata();
            helper.setBlock(matrixPos, Blocks.AIR);
            helper.setBlock(matrixPos, TTBlocks.INFUSION_MATRIX.get());
            ((InfusionMatrix) helper.getBlockEntity(matrixPos)).load(partial);
            for (int i = 0; i < aspects.size(); i++) {
                var jar = (EssentiaJarBlockEntity) helper.getBlockEntity(jars.get(i));
                jar.tank().insert(aspects.get(i), i == 0 ? 23 : 25, false);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(center.item().is(bulk("jar_brain").asItem()), "Infusion did not resume and produce the brain jar");
            helper.assertTrue(((InfusionPedestal) helper.getBlockEntity(matrixPos.offset(offsets.get(2)))).item().is(Items.BUCKET),
                    "The infusion did not return the empty bucket");
            for (BlockPos pos : jars) {
                var jar = (EssentiaJarBlockEntity) helper.getBlockEntity(pos);
                helper.assertTrue(jar.tank().stored() == 0, "Infusion did not take the correct essentia");
            }
        });
    }

    @GameTest(template = "vis_lab", timeoutTicks = 200)
    public static void advancedFurnaceFormsProcessesAndRestores(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_advanced_furnace");
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("advanced_alchemical_furnace"), 1);
        BlockPos origin = new BlockPos(2, 1, 2);
        build(helper, player, origin, "advanced_alchemical_furnace");
        var furnace = (AdvancedFurnace) helper.getBlockEntity(origin);
        BlockPos relativeNode = origin.above(2).east(9);
        BlockPos relay = origin.above(2).east(5);
        var aspects = AspectList.ofEntries(List.of(new AspectInstance(TTAspects.IGNIS, 80), new AspectInstance(TTAspects.AQUA, 80),
                new AspectInstance(TTAspects.PERDITIO, 80)));
        helper.setBlock(relativeNode, TTBlocks.NODE.get());
        var node = (com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode) helper.getBlockEntity(relativeNode);
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), aspects, aspects));
        helper.setBlock(relay.below(), Blocks.STONE);
        helper.setBlock(relay, bulk("vis_relay"));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(furnace.saveWithoutMetadata().getInt("Aqua") == 0 && node.getAspects().entries().equals(aspects.entries()),
                    "The furnace drew power from an ordinary node");
            helper.setBlock(relativeNode.below(), bulk("node_stabilizer"));
            helper.setBlock(relativeNode.above(), bulk("node_transducer"));
            var transducer = helper.getBlockEntity(relativeNode.above());
            var almostCharged = transducer.saveWithoutMetadata();
            almostCharged.putInt("Count", 999);
            transducer.load(almostCharged);
            helper.setBlock(relativeNode.above().north(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(75, () -> {
            var reserves = furnace.saveWithoutMetadata();
            helper.assertTrue(reserves.getInt("Heat") > 0 && reserves.getInt("Aqua") > 0 && reserves.getInt("Perditio") > 0,
                    "The idle furnace did not charge through its relay");
            var copy = new AdvancedFurnace(helper.absolutePos(origin), furnace.getBlockState());
            copy.load(reserves);
            var roundTrip = copy.saveWithoutMetadata();
            for (String reserve : List.of("Heat", "Aqua", "Perditio")) {
                helper.assertTrue(roundTrip.getInt(reserve) == reserves.getInt(reserve), "Lost saved " + reserve + " while restoring the furnace");
            }
            helper.assertTrue(furnace.insert(new ItemStack(Items.IRON_INGOT)), "Furnace did not accept the iron ingot");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!furnace.stock().isEmpty(), "Furnace did not process the input from relayed power");
            BlockPos nozzle = helper.absolutePos(origin.east());
            var handler = EssentiaAccess.handler(level, nozzle, Direction.EAST);
            var aspect = furnace.stock().entries().get(0);
            helper.assertTrue(handler != null && handler.extract(aspect.aspectId(), 1, false) == 1
                    && EssentiaAccess.handler(level, nozzle, Direction.WEST) == null, "Nozzle did not enforce sided extraction");
            helper.setBlock(origin.east(), Blocks.AIR);
            helper.assertTrue(helper.getBlockState(origin).is(TTBlocks.SMELTER_BASIC.get()), "Furnace did not disassemble");
            helper.assertTrue(helper.getBlockState(origin.offset(1, 0, 1)).is(bulk("advanced_alchemical_construct")), "An advanced construct was lost");
            helper.assertTrue(helper.getBlockState(origin.offset(1, 1, 1)).is(TTBlocks.ALEMBIC.get()), "An alembic was lost");
        });
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 120)
    public static void pureNodeCleansFlux(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, Blocks.AIR);
        var pos = helper.absolutePos(relative);
        var list = AspectList.EMPTY.add(new AspectInstance(TTAspects.ORDO, 15));
        helper.assertTrue(NodeGenerator.createNodeAt(level, pos, new NodeData(NodeType.PURE, Optional.empty(), list, list)), "Pure node placement failed");
        AuraHelper.addFlux(level, pos, 4);
        float start = AuraHelper.getFlux(level, pos);
        var node = (com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode) helper.getBlockEntity(relative);
        for (int tick = 0; tick < 50; tick++) node.serverTick(level, pos);
        helper.assertTrue(AuraHelper.getFlux(level, pos) < start, "Pure node did not clean flux");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void chargerEnablesRecipesAboveLocalVis(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_charged_workbench");
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("infusion_boost"), 2);
        var bench = new BlockPos(1, 1, 1);
        helper.setBlock(bench, TTBlocks.ARCANE_WORKBENCH.get());
        var machine = (com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlockEntity) helper.getBlockEntity(bench);
        var menu = (com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu) machine.createMenu(1, player.getInventory(), player);
        var worldPos = helper.absolutePos(bench);
        var origin = new net.minecraft.world.level.ChunkPos(worldPos);
        var saved = new java.util.IdentityHashMap<com.leclowndu93150.thaumaturge.content.aura.AuraData, Float>();
        try {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    var anchor = new net.minecraft.world.level.ChunkPos(origin.x + x, origin.z + z).getMiddleBlockPosition(worldPos.getY());
                    var aura = com.leclowndu93150.thaumaturge.content.aura.AuraManager.chunkAt(level, anchor);
                    helper.assertTrue(aura != null, "Adjacent aura chunk is missing");
                    saved.put(aura, aura.getVis());
                    aura.setVis(100);
                }
            }
            var id = TTIds.rl("arcane_workbench/matrix_speed");
            var recipe = (com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe) level.getRecipeManager().byKey(id).orElseThrow();
            for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
                var ingredient = recipe.getIngredients().get(slot);
                if (!ingredient.isEmpty()) menu.getSlot(1 + slot).set(ingredient.getItems()[0].copy());
            }
            for (var cost : recipe.crystals().entries()) {
                var crystal = com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory.of(cost.aspectId(), cost.amount());
                int index = com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(cost.aspectId());
                menu.getSlot(10 + index).set(crystal);
            }
            menu.broadcastChanges();
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getCachedVis() == 100, "Recipe used remote aura without a charger");
            helper.setBlock(bench.above(), bulk("arcane_workbench_charger"));
            menu.broadcastChanges();
            helper.assertTrue(menu.getCachedVis() == 900 && menu.getRequiredVis() == 625
                    && menu.getSlot(0).getItem().is(bulk("matrix_speed").asItem()), "Charger did not unlock the crystal-paid recipe");
            helper.assertTrue(menu.getSlot(0).remove(1).is(bulk("matrix_speed").asItem()), "Charger could not pay a combined aura cost");
            for (var aura : saved.keySet()) helper.assertTrue(aura.getVis() > 30 && aura.getVis() < 31, "Charger did not share the drain across chunks");
        } finally {
            saved.forEach(com.leclowndu93150.thaumaturge.content.aura.AuraData::setVis);
        }
        helper.succeed();
    }

    private static void build(GameTestHelper helper, ServerPlayer player, BlockPos pos, String id) {
        var recipe = (DustStructureRecipe) helper.getLevel().getRecipeManager().byKey(TTIds.rl("dust_trigger/" + id)).orElseThrow();
        for (var cell : recipe.cells()) {
            var source = id.equals("infusion_altar") && cell.source() == TTBlocks.ARCANE_STONE.get()
                    ? bulk("stone_arcane") : cell.source();
            helper.setBlock(pos.offset(cell.offset()), source);
        }
        BlockPos at = helper.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        var dust = new ItemStack(TTItems.SALIS_MUNDUS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, dust);
        var use = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        if (id.equals("advanced_alchemical_furnace")) {
            BlockPos alembic = helper.absolutePos(pos.offset(1, 1, 1));
            var tank = new com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank(128);
            tank.insert(TTAspects.AQUA, 10, false);
            var state = new net.minecraft.nbt.CompoundTag();
            state.put("Tank", tank.save());
            helper.getLevel().getBlockEntity(alembic).load(state);
            var input = new com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput(dust, helper.getLevel(), at,
                    helper.getLevel().getBlockState(at));
            helper.assertTrue(!recipe.matches(input, helper.getLevel()), "The furnace would destroy a filled alembic");
            helper.getLevel().getBlockEntity(alembic).load(new net.minecraft.nbt.CompoundTag());
        }
        helper.assertTrue(dust.getItem().onItemUseFirst(dust, use).consumesAction(), "Salis Mundus could not assemble " + id);
    }

    private static Block bulk(String id) {
        return TTBulkContent.BLOCKS.get(id).get();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
    }
}
