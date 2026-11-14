package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalFurnace;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class AuromancyWaveTests {
    private AuromancyWaveTests() {}

    @GameTest(template = "essentia_lab", timeoutTicks = 230)
    public static void infernalFurnaceFormsFromGrateAndSmeltsIntoChest(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_infernal");
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("infernal_furnace"), 2);
        BlockPos center = new BlockPos(2, 2, 2);
        var recipe = (DustStructureRecipe) level.getRecipeManager().byKey(TTIds.rl("dust_trigger/infernal_furnace")).orElseThrow();
        for (var cell : recipe.cells()) {
            var offset = cell.offset();
            helper.setBlock(center.offset(offset.getZ(), offset.getY(), -offset.getX()), cell.source());
        }
        var grate = helper.absolutePos(center.east());
        var hit = new BlockHitResult(Vec3.atCenterOf(grate), Direction.EAST, grate, false);
        var dust = new ItemStack(TTItems.SALIS_MUNDUS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, dust);
        helper.assertTrue(dust.getItem().onItemUseFirst(dust, new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Salis Mundus did not form the rotated furnace from its grate");
        helper.assertTrue(helper.getBlockState(center).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST,
                "The model and outlet face the wrong way");
        var furnace = (InfernalFurnace) helper.getBlockEntity(center);
        helper.setBlock(center.east(2), Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getBlockEntity(center.east(2));
        helper.assertTrue(furnace.feed(level, new ItemStack(Items.RAW_IRON)).isEmpty(), "Furnace rejected smeltable input");
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.IRON_INGOT), "The iron did not reach the chest");
            helper.setBlock(center.offset(1, -1, 1), Blocks.AIR);
            helper.assertTrue(helper.getBlockState(center).is(Blocks.LAVA), "Breaking the shell did not restore the lava");
            helper.assertTrue(helper.getBlockState(center.east()).is(Blocks.IRON_BARS), "Breaking the shell did not restore the grate");
        });
    }

    @GameTest(template = "essentia_lab")
    public static void basicFocusImprintsSocketsAndCasts(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_basic_focus");
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("base_auromancy"), 3);
        var pos = new BlockPos(1, 1, 1);
        var absolute = helper.absolutePos(pos);
        var bench = TTBulkContent.BLOCKS.get("focal_manipulator").get();
        helper.setBlock(pos, bench);
        var recipe = (com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe)
                level.getRecipeManager().byKey(TTIds.rl("crucible/focus_1")).orElseThrow();
        helper.assertTrue(recipe.matchesCatalyst(EssentiaCrystalFactory.ordo()) && !recipe.matchesCatalyst(EssentiaCrystalFactory.ignis()),
                "Blank focus did not require an Ordo crystal");
        var aura = AuraManager.chunkAt(level, absolute);
        float oldVis = aura.getVis();
        try {
            aura.setVis(50);
            player.experienceLevel = 3;
            var focus = new ItemStack(TTBulkItems.ITEMS.get("focus_1").get());
            player.setItemInHand(InteractionHand.MAIN_HAND, focus);
            player.setItemInHand(InteractionHand.OFF_HAND, EssentiaCrystalFactory.ignis());
            var click = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            helper.assertTrue(level.getBlockState(absolute).use(level, player, InteractionHand.MAIN_HAND, click).consumesAction(),
                    "The focal manipulator did not inscribe the focus");
            helper.assertTrue(player.experienceLevel == 2 && aura.getVis() == 40 && player.getOffhandItem().isEmpty(),
                    "Focus imprint cost was wrong");
            var wand = new ItemStack(TTItems.WAND.get());
            TTItemData.setWandVis(wand, 10);
            int before = WandCharge.PRIMALS.stream().mapToInt(id -> WandCharge.stored(wand, id)).sum();
            helper.assertTrue(before == 1000, "Old wand charge was not preserved during conversion");
            int ignis = WandCharge.stored(wand, TTAspects.IGNIS);
            var savedGraph = com.leclowndu93150.thaumaturge.registry.TTItemData.root(focus).getCompound("Spell");
            helper.assertTrue(savedGraph.getCompound("root").getString("part").equals("thaumaturge:origin"),
                    "The focal manipulator did not save a spell graph");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            player.setItemInHand(InteractionHand.OFF_HAND, focus);
            player.setShiftKeyDown(true);
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getOffhandItem().isEmpty(), "The focus did not socket");
            player.setShiftKeyDown(false);
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == ignis - 16
                    && WandCharge.stored(wand, TTAspects.ORDO) == 166 && TTItemData.getWandVis(wand) == 0,
                    "Fire Touch did not debit its primal costs or migrate the old charge");
            var greatwood = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_greatwood")).orElseThrow().getResultItem(level.registryAccess());
            helper.assertTrue(WandItem.capacity(greatwood) == 50, "Greatwood wand did not retain its rod data");
        } finally {
            aura.setVis(oldVis);
            player.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void wandRefinesAuraOrDrawsDirectlyFromNode(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var contents = com.leclowndu93150.thaumaturge.api.aspect.AspectList.ofEntries(java.util.List.of(
                new com.leclowndu93150.thaumaturge.api.aspect.AspectInstance(TTAspects.IGNIS, 5)));
        var data = new com.leclowndu93150.thaumaturge.content.aura.node.NodeData(
                com.leclowndu93150.thaumaturge.api.nodes.NodeType.NORMAL, java.util.Optional.empty(), contents, contents);
        helper.assertTrue(com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator.createNodeAt(level, at, data),
                "Could not create a test node");
        var node = (com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode) level.getBlockEntity(at);
        var wand = new ItemStack(TTItems.WAND.get());
        var player = player(helper, "tt_node_charge");
        var item = (WandItem) wand.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.moveTo(at.getX() + 0.5, at.getY() + 0.5 - player.getEyeHeight(), at.getZ() + 2.5, 0, 0);
        var aura = AuraManager.chunkAt(level, player.blockPosition());
        float originalVis = aura.getVis();
        try {
            aura.setVis(50);
            helper.assertTrue(item.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "Wand without a focus did not begin refining");
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.AER) == 10 && Math.abs(aura.getVis() - 49.7F) < 0.001F,
                    "Aura refinement did not transfer 0.1 primal vis for 0.3 raw vis");

            player.setYRot(180);
            level.setBlockAndUpdate(at.south(), Blocks.STONE.defaultBlockState());
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(node.getAspects().amountOf(TTAspects.IGNIS) == 5 && Math.abs(aura.getVis() - 49.4F) < 0.001F,
                    "Wand reached a node through a block instead of refining the local aura");

            level.setBlockAndUpdate(at.south(), Blocks.AIR.defaultBlockState());
            float beforeNode = aura.getVis();
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 100
                    && node.getAspects().amountOf(TTAspects.IGNIS) == 4 && aura.getVis() == beforeNode,
                    "Ignis was not transferred directly from the node");
        } finally {
            aura.setVis(originalVis);
            player.stopUsingItem();
            level.setBlockAndUpdate(at.south(), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!WandCharge.payTouchFocus(wand, TTAspects.IGNIS, 300, true)
                && WandCharge.stored(wand, TTAspects.IGNIS) == 100, "Failed cast spent some of the wand charge");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }
}
