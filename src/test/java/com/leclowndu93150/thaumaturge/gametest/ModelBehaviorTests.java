package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityDioptra;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class ModelBehaviorTests {
    private ModelBehaviorTests() {}

    @GameTest(template = "essentia_lab")
    public static void dioptraSwitchesBetweenVisAndFlux(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        var block = TTBlocks.DIOPTRA.get();
        helper.setBlock(pos, block);
        var dioptra = (BlockEntityDioptra) helper.getBlockEntity(pos);
        var level = helper.getLevel();
        var absolute = helper.absolutePos(pos);
        var aura = AuraManager.chunkAt(level, absolute);
        helper.assertTrue(aura != null, "The test chunk has no aura");
        float oldVis = aura.getVis();
        float oldFlux = aura.getFlux();
        try {
            aura.setVis(500);
            aura.setFlux(125);
            for (boolean vis : new boolean[] {true, false}) {
                var state = block.defaultBlockState().setValue(BlockStateProperties.ENABLED, vis);
                helper.setBlock(pos, state);
                for (int tick = 0; tick < 20; tick++) {
                    BlockEntityDioptra.serverTick(level, absolute, state, dioptra);
                }
                helper.assertTrue(dioptra.gridValue(84) == (vis ? 64 : 16), "Wrong center sample for mode " + vis);
                helper.assertTrue(block.getAnalogOutputSignal(state, level, absolute) == (vis ? 15 : 4),
                        "Comparator did not report the active aura mode");
            }
        } finally {
            aura.setVis(oldVis);
            aura.setFlux(oldFlux);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void nodePlacementPreservesDataAndAvoidsWater(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 2, 1);
        var absolute = helper.absolutePos(pos);
        var random = net.minecraft.util.RandomSource.create(42);
        var data = com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator.rollRandomNodeData(
                level, absolute, random, true, false, false, 18, 100);
        helper.assertTrue(data.type() == com.leclowndu93150.thaumaturge.api.nodes.NodeType.PURE
                && !data.aspects().isEmpty() && data.aspects().entries().equals(data.aspectsBase().entries()),
                "Silverwood should roll a pure node with a full aspect pool");
        helper.setBlock(pos, Blocks.WATER);
        helper.assertTrue(!com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator.createNodeAt(level, absolute, data),
                "The node replaced water");
        helper.setBlock(pos, Blocks.AIR);
        helper.assertTrue(com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator.createNodeAt(level, absolute, data),
                "The node could not be placed in air");
        var node = (com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode) helper.getBlockEntity(pos);
        var copy = new com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode(absolute, node.getBlockState());
        copy.handleUpdateTag(node.getUpdateTag());
        helper.assertTrue(copy.kind() == data.type() && copy.trait() == data.modifier().orElse(null)
                && copy.getAspects().entries().equals(data.aspects().entries()), "Node sync lost generated properties");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 250)
    public static void lampCreatesAndClearsInvisibleLight(GameTestHelper helper) {
        BlockPos lamp = new BlockPos(2, 2, 2);
        var level = helper.getLevel();
        var origin = helper.absolutePos(lamp);
        var glimmer = TTBulkContent.BLOCKS.get("effect_glimmer").get();
        helper.setBlock(lamp, TTBlocks.LAMP_ARCANE.get());
        helper.succeedWhen(() -> {
            BlockPos found = null;
            for (BlockPos cell : BlockPos.betweenClosed(origin.offset(-15, -15, -15), origin.offset(15, 15, 15))) {
                if (level.hasChunkAt(cell) && level.getBlockState(cell).is(glimmer)) {
                    found = cell.immutable();
                    break;
                }
            }
            helper.assertTrue(found != null, "Lamp did not create a secondary light");
            var state = level.getBlockState(found);
            helper.assertTrue(state.getLightEmission(level, found) == 15 && state.getCollisionShape(level, found).isEmpty(),
                    "A glimmer should emit light without collision");
            helper.setBlock(lamp.east(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(!level.getBlockState(found).is(glimmer), "Powering the lamp left its glimmer behind");
        });
    }

    @GameTest(template = "essentia_lab")
    public static void candlesUseTheirOwnShapeAndLight(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, TTBulkContent.BLOCKS.get("candle_orange").get());
        var state = helper.getBlockState(pos);
        var level = helper.getLevel();
        var absolute = helper.absolutePos(pos);
        helper.assertTrue(!state.canOcclude() && state.getCollisionShape(level, absolute).isEmpty()
                && state.getShape(level, absolute).bounds().getXsize() <= 0.25
                && state.getLightEmission(level, absolute) == 14, "Candle acts like a full cube or emits the wrong light");
        helper.setBlock(pos.below(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(pos).isAir(), "Unsupported candle did not break");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void lampRespondsToRedstoneAndDoorHasTwoHalves(GameTestHelper helper) {
        BlockPos lamp = new BlockPos(1, 1, 1);
        helper.setBlock(lamp.below(), Blocks.STONE);
        helper.setBlock(lamp, TTBlocks.LAMP_ARCANE.get());
        helper.assertTrue(helper.getBlockState(lamp).getValue(BlockStateProperties.ENABLED), "Lamp starts dark");
        helper.setBlock(lamp.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(!helper.getBlockState(lamp).getValue(BlockStateProperties.ENABLED), "Powered lamp stays lit");
        helper.setBlock(lamp.east(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(lamp).getValue(BlockStateProperties.ENABLED), "Lamp did not relight");

        BlockPos doorPos = new BlockPos(3, 1, 1);
        helper.setBlock(doorPos.below(), Blocks.STONE);
        var door = (DoorBlock) TTBulkContent.BLOCKS.get("arcane_door").get();
        var state = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(doorPos, state);
        door.setPlacedBy(helper.getLevel(), helper.absolutePos(doorPos), state, null, new ItemStack(door));
        helper.assertTrue(helper.getBlockState(doorPos.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                "Door did not create its upper half");
        var shape = state.getShape(helper.getLevel(), helper.absolutePos(doorPos)).bounds();
        helper.assertTrue(shape.getZsize() < 0.25, "Closed door collision is a full cube");
        helper.succeed();
    }
}
