package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.decor.BlockObsidianTotem;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class WorldgenTests {
    private WorldgenTests() {}

    @GameTest(template = "vis_lab")
    public static void generatedTotemContainsANode(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var totem = features.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, TTIds.rl("obsidian_totem")));
        var at = new BlockPos(2, 2, 2);
        helper.setBlock(at.below(), Blocks.GRASS_BLOCK);
        for (int y = 0; y < 5; y++) helper.setBlock(at.above(y), Blocks.AIR);
        helper.assertTrue(totem.place(level, generator, RandomSource.create(0), helper.absolutePos(at)), "Totem refused valid terrain");
        helper.assertBlockPresent(TTBulkContent.BLOCKS.get("obsidian_tile").get(), at.below());
        helper.assertTrue(helper.getBlockState(at).getValue(BlockObsidianTotem.UP), "Totem sections did not connect");
        boolean hasNode = false;
        for (int height = 1; height < 5; height++) {
            hasNode |= helper.getBlockEntity(at.above(height)) instanceof BlockEntityNode;
        }
        helper.assertTrue(hasNode, "The generated cap did not contain an aura node");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void cinderpearlPlacesOnSand(GameTestHelper helper) {
        var level = helper.getLevel();
        var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var flower = features.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, TTIds.rl("cinderpearl_patch")));
        var at = new BlockPos(2, 2, 2);
        helper.setBlock(at.below(), Blocks.RED_SAND);
        helper.setBlock(at, Blocks.AIR);
        helper.assertTrue(flower.place(level, level.getChunkSource().getGenerator(), RandomSource.create(0), helper.absolutePos(at)),
                "Cinderpearl refused red sand");
        helper.assertBlockPresent(TTBulkContent.BLOCKS.get("cinderpearl").get(), at);
        helper.succeed();
    }
}
