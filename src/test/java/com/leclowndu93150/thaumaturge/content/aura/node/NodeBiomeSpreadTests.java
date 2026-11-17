package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class NodeBiomeSpreadTests {
    private NodeBiomeSpreadTests() {}

    @GameTest(template = "essentia_lab")
    public static void darkNodePaintsWholeColumnAndRemovalRestoresGenerator(GameTestHelper helper) {
        var level = helper.getLevel();
        var at = new BlockPos(3, 2, 3);
        var pos = helper.absolutePos(at);
        helper.setBlock(at, TTBlocks.NODE.get());
        var node = (BlockEntityNode) helper.getBlockEntity(at);
        var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 7));
        node.applyNodeData(new NodeData(NodeType.DARK, Optional.empty(), aspects, aspects));

        var distant = pos.offset(16, 0, 16);
        helper.assertTrue(!NodeBiomeSpread.spreadAt(level, NodeType.DARK, pos, distant), "Dark spread exceeded its circular range");
        helper.assertTrue(NodeBiomeSpread.spreadAt(level, NodeType.DARK, pos, pos), "Dark node did not paint its sample");
        int x = QuartPos.fromBlock(pos.getX());
        int z = QuartPos.fromBlock(pos.getZ());
        int minY = QuartPos.fromBlock(level.getMinBuildHeight());
        int maxY = QuartPos.fromBlock(level.getMaxBuildHeight());
        var chunk = level.getChunkAt(pos);
        for (int y = minY; y < maxY; y++) {
            helper.assertTrue(chunk.getNoiseBiome(x, y, z).is(TTBiomeKeys.EERIE), "The painted biome must span the build height");
        }
        helper.setBlock(at, Blocks.AIR);
        assertGenerated(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void pureNodeNeedsSilverwoodOrTaintAndRestoresOnTypeChange(GameTestHelper helper) {
        var level = helper.getLevel();
        var at = new BlockPos(3, 2, 3);
        var pos = helper.absolutePos(at);
        helper.setBlock(at, TTBlocks.NODE.get());
        var node = (BlockEntityNode) helper.getBlockEntity(at);
        var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 7));
        node.applyNodeData(new NodeData(NodeType.PURE, Optional.empty(), aspects, aspects));
        helper.assertTrue(!NodeBiomeSpread.spreadAt(level, NodeType.PURE, pos, pos), "Pure node painted clean land without Silverwood");
        helper.setBlock(at.above(), TTBlocks.LOG_SILVERWOOD.get());
        helper.assertTrue(NodeBiomeSpread.spreadAt(level, NodeType.PURE, pos, pos), "Pure node did not paint beside Silverwood");
        int x = QuartPos.fromBlock(pos.getX());
        int y = QuartPos.fromBlock(pos.getY());
        int z = QuartPos.fromBlock(pos.getZ());
        helper.assertTrue(level.getChunkAt(pos).getNoiseBiome(x, y, z).is(TTBiomeKeys.MAGICAL_FOREST), "Pure node used the wrong biome");
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), aspects, aspects));
        assertGenerated(helper, level, pos);
        helper.succeed();
    }

    private static void assertGenerated(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        int x = QuartPos.fromBlock(pos.getX());
        int z = QuartPos.fromBlock(pos.getZ());
        int minY = QuartPos.fromBlock(level.getMinBuildHeight());
        int maxY = QuartPos.fromBlock(level.getMaxBuildHeight());
        var chunk = level.getChunkAt(pos);
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        for (int y = minY; y < maxY; y++) {
            helper.assertTrue(chunk.getNoiseBiome(x, y, z).equals(source.getNoiseBiome(x, y, z, sampler)), "Removing the node influence must restore the generator biome at quart Y " + y);
        }
    }
}
