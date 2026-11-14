package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.JarNodeBlock;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeJarRitual;
import com.leclowndu93150.thaumaturge.content.device.VisBatteryBlock;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalFurnace;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    @GameTest(template = "essentia_lab")
    public static void silverwoodWandCapturesAndReleasesNode(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("1253687e-071c-4a47-824c-f36f7d20ecb2"), "tt_node_jar"));
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("node_jar"), 2);
        var wand = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_silverwood")).orElseThrow()
                .getResultItem(level.registryAccess()).copy();
        helper.assertTrue(WandCharge.capacity(wand) == 10000, "The assembled silverwood wand does not have 100 vis per primal");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(pos, TTBlocks.NODE.get().defaultBlockState());
        var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 12));
        var node = (BlockEntityNode) level.getBlockEntity(pos);
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.of(NodeModifier.PALE), aspects, aspects));
        for (var cell : NodeJarRitual.structurePositions(pos)) {
            level.setBlockAndUpdate(cell, (cell.getY() == pos.getY() + 2 ? Blocks.OAK_SLAB : Blocks.GLASS).defaultBlockState());
        }
        var glass = pos.east();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(glass), Direction.EAST, glass, false));
        for (var primal : WandCharge.PRIMALS) WandCharge.add(wand, primal, 6999);
        wand.getItem().useOn(context);
        helper.assertTrue(level.getBlockEntity(pos) == node && WandCharge.stored(wand, TTAspects.AER) == 6999,
                "A failed capture changed the node or charged the wand");
        for (var primal : WandCharge.PRIMALS) WandCharge.add(wand, primal, 1001);
        wand.getItem().useOn(context);
        helper.assertTrue(level.getBlockEntity(pos) instanceof JarNodeBlock.Entity && level.isEmptyBlock(glass),
                "Using the wand on the shell did not capture the node");
        helper.assertTrue(WandCharge.PRIMALS.stream().allMatch(primal -> WandCharge.stored(wand, primal) == 1000),
                "Capture did not charge exactly 70 vis of every primal");

        var jar = (JarNodeBlock.Entity) level.getBlockEntity(pos);
        var dropped = net.minecraft.world.level.block.Block.getDrops(jar.getBlockState(), level, pos, jar);
        helper.assertTrue(dropped.size() == 1 && TTItemData.getNodeData(dropped.get(0)) != null, "The jar dropped without its node");
        level.removeBlock(pos, false);
        BlockPos destination = pos.east(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped.get(0));
        dropped.get(0).getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(destination), Direction.UP, destination, false)));
        helper.assertTrue(level.getBlockEntity(destination) instanceof JarNodeBlock.Entity, "The jar could not be placed again");
        var moved = (JarNodeBlock.Entity) level.getBlockEntity(destination);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        moved.getBlockState().use(level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(destination), Direction.UP, destination, false));
        helper.assertTrue(level.getBlockState(destination).is(TTBlocks.NODE.get()), "Using the wand on the jar did not release its node");
        var released = (BlockEntityNode) level.getBlockEntity(destination);
        helper.assertTrue(released.kind() == NodeType.NORMAL && released.capacity().amountOf(TTAspects.AER) == 12
                && released.getAspects().amountOf(TTAspects.AER) == 12, "Transport changed the type or aspects");
        helper.assertTrue(released.trait() == NodeModifier.PALE || released.trait() == NodeModifier.FADING,
                "Transport gave the node an invalid modifier");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void visBatteryExchangesAuraAndReportsCharge(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 2, 4));
        var aura = AuraManager.chunkAt(level, pos);
        float previous = aura.getVis();
        short previousBase = aura.getBase();
        try {
            aura.setBase((short) 100);
            aura.setVis(100);
            var battery = (VisBatteryBlock) TTBulkContent.BLOCKS.get("vis_battery").get();
            level.setBlockAndUpdate(pos, battery.defaultBlockState());
            battery.tick(level.getBlockState(pos), level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 1 && aura.getVis() == 99,
                    "The battery did not store one vis or report one comparator level");
            level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            battery.tick(level.getBlockState(pos), level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 0 && aura.getVis() == 100,
                    "Redstone did not discharge the battery back to its chunk");
            helper.assertTrue(level.getRecipeManager().byKey(TTIds.rl("arcane_workbench/vis_battery")).isPresent(),
                    "The Vis Battery recipe did not load");
            var rod = (InfusionRecipe) level.getRecipeManager().byKey(TTIds.rl("infusion/wand_rod_silverwood")).orElseThrow();
            helper.assertTrue(rod.components().size() == WandCharge.PRIMALS.size()
                    && WandCharge.PRIMALS.stream().allMatch(primal -> rod.components().stream()
                            .filter(ingredient -> ingredient.test(EssentiaCrystalFactory.of(primal, 1))).count() == 1),
                    "Silverwood infusion must accept exactly one crystal of each primal");
        } finally {
            aura.setBase(previousBase);
            aura.setVis(previous);
        }
        helper.succeed();
    }
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
            var clientNode = new BlockEntityNode(at, node.getBlockState());
            clientNode.setLevel(level);
            clientNode.handleUpdateTag(node.getUpdateTag());
            var effect = clientNode.drainEffect();
            helper.assertTrue(effect != null && effect.player().equals(player.getUUID())
                    && effect.color() == TTAspects.get(TTAspects.IGNIS).color(),
                    "The node did not synchronize the caster and aspect for the extraction ribbon");
            helper.assertTrue(!node.saveWithoutMetadata().hasUUID("DrainPlayer"), "A transient extraction effect was saved to disk");
        } finally {
            aura.setVis(originalVis);
            player.stopUsingItem();
            level.setBlockAndUpdate(at.south(), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!WandCharge.payTouchFocus(wand, TTAspects.IGNIS, 300, true)
                && WandCharge.stored(wand, TTAspects.IGNIS) == 100, "Failed cast spent some of the wand charge");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void nodeResearchChangesWandTapping(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(at, TTBlocks.NODE.get().defaultBlockState());
        var node = (BlockEntityNode) level.getBlockEntity(at);
        var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.IGNIS, 8));
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), aspects, aspects));
        var wand = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_greatwood")).orElseThrow()
                .getResultItem(level.registryAccess()).copy();
        var player = player(helper, "tt_node_research");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.moveTo(at.getX() + 0.5, at.getY() + 0.5 - player.getEyeHeight(), at.getZ() + 2.5, 180, 0);
        var knowledge = TTAttachments.knowledge(player);
        var item = (WandItem) wand.getItem();
        item.use(level, player, InteractionHand.MAIN_HAND);
        try {
            item.onUseTick(level, player, wand, 25);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 100, "An untrained caster should draw one point");
            knowledge.markResearchComplete(TTIds.rl("node_tapper_1"), 2);
            item.onUseTick(level, player, wand, 20);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 300, "Advanced tapping should draw two points");
            knowledge.markResearchComplete(TTIds.rl("node_tapper_2"), 2);
            knowledge.markResearchComplete(TTIds.rl("node_preserve"), 2);
            item.onUseTick(level, player, wand, 15);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 600, "Master tapping should draw three points");
            item.onUseTick(level, player, wand, 10);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 700
                    && node.getAspects().amountOf(TTAspects.IGNIS) == 1, "Preservation should leave the last point");
            player.setShiftKeyDown(true);
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 800
                    && node.getAspects().amountOf(TTAspects.IGNIS) == 0, "Crouching should override preservation");
        } finally {
            player.stopUsingItem();
            player.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 1250)
    public static void emptyAspectsDecayAndPreserveNodeHosts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos free = new BlockPos(2, 2, 2);
        BlockPos log = free.east();
        BlockPos totem = log.east();
        BlockPos jar = free.south();
        var chargedTotem = TTBulkContent.BLOCKS.get("obsidian_totem_charged").get();
        var ordinaryTotem = TTBulkContent.BLOCKS.get("obsidian_totem").get();
        helper.setBlock(free, TTBlocks.NODE.get());
        helper.setBlock(jar, TTBulkContent.BLOCKS.get("jar_node").get());
        helper.setBlock(log, TTBlocks.SILVERWOOD_NODE_LOG.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        helper.setBlock(totem.above(), ordinaryTotem);
        helper.setBlock(totem, chargedTotem.defaultBlockState().setValue(BlockStateProperties.UP, true));
        var aer = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 1));
        var mixed = aer.add(new AspectInstance(TTAspects.IGNIS, 1));
        for (var pos : java.util.List.of(log, totem, jar)) {
            var node = (BlockEntityNode) helper.getBlockEntity(pos);
            node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), AspectList.EMPTY, aer));
            node.load(node.saveWithoutMetadata());
        }
        var node = (BlockEntityNode) helper.getBlockEntity(free);
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), aer, mixed));

        helper.runAfterDelay(1210, () -> {
            helper.assertTrue(node.capacity().amountOf(TTAspects.IGNIS) == 0
                    && node.getAspects().amountOf(TTAspects.AER) == 1,
                    "The empty aspect should expire while the stocked aspect survives");
            var remainingLog = helper.getBlockState(log);
            helper.assertTrue(remainingLog.is(TTBlocks.LOG_SILVERWOOD.get())
                    && remainingLog.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                    "An exhausted node log should keep the original log axis");
            var remainingTotem = helper.getBlockState(totem);
            helper.assertTrue(remainingTotem.is(ordinaryTotem) && remainingTotem.getValue(BlockStateProperties.UP),
                    "An exhausted totem should keep its connection to the next totem");
            helper.assertTrue(level.isEmptyBlock(helper.absolutePos(jar)), "A fully exhausted jarred node should collapse");
            helper.succeed();
        });
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }
}
