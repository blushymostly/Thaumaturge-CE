package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayNetwork;
import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;
import com.leclowndu93150.thaumaturge.mixin.world.entity.ExperienceOrbAccessor;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class PortDeviceTests {
    private PortDeviceTests() {}

    @GameTest(template = "essentia_lab")
    public static void brainJarCollectsMergedOrbsAndKeepsXp(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, block("jar_brain"));
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(at);
        Vec3 center = Vec3.atCenterOf(pos);
        var orb = new ExperienceOrb(level, center.x, center.y, center.z, 7);
        ((ExperienceOrbAccessor) orb).thaumaturge$setCount(3);
        level.addFreshEntity(orb);
        helper.runAfterDelay(3, () -> {
            var tag = helper.getBlockEntity(at).saveWithoutMetadata();
            helper.assertTrue(tag.getInt("XP") == 21 && orb.isRemoved(), "Brain jar lost merged XP");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, helper.getBlockEntity(at));
            helper.assertTrue(drops.size() == 1 && drops.get(0).getTagElement("BlockEntityTag").getInt("XP") == 21,
                    "Broken brain jar did not keep its XP");
            helper.setBlock(at, Blocks.AIR);
            helper.setBlock(at, block("jar_brain"));
            helper.getBlockEntity(at).load(tag);
            var state = level.getBlockState(pos);
            helper.assertTrue(state.getEnchantPowerBonus(level, pos) == 5, "Brain jar gave the wrong enchanting power");
            var player = helper.makeMockSurvivalPlayer();
            for (int i = 0; i < 20 && helper.getBlockEntity(at).saveWithoutMetadata().getInt("XP") == 21; i++) {
                helper.useBlock(at, player);
            }
            int remaining = helper.getBlockEntity(at).saveWithoutMetadata().getInt("XP");
            int released = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(1)).stream()
                    .mapToInt(xp -> xp.getValue() * ((ExperienceOrbAccessor) xp).thaumaturge$getCount()).sum();
            helper.assertTrue(released > 0 && remaining + released == 21, "Brain jar did not conserve XP after reloading and releasing it");
            helper.succeed();
        });
    }

    @GameTest(template = "essentia_lab")
    public static void stabilizerChargesAndStabilizesARift(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos device = new BlockPos(2, 1, 2);
        helper.setBlock(device, block("stabilizer"));
        BlockPos pos = helper.absolutePos(device);
        helper.runAfterDelay(45, () -> {
            var state = level.getBlockState(pos);
            helper.assertTrue(state.getAnalogOutputSignal(level, pos) >= 2, "Stabilizer comparator did not report charge");
            FluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
            Vec3 center = Vec3.atCenterOf(pos.above());
            rift.setPos(center.x, center.y, center.z);
            level.addFreshEntity(rift);
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(rift.stability() > 0, "Charged stabilizer did not stabilize a nearby rift");
                rift.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "vis_lab", timeoutTicks = 1150)
    public static void poweredNodeSuppliesWorkbenchThroughRelays(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos top = new BlockPos(1, 3, 2);
        BlockPos nodeAt = top.below();
        BlockPos bench = new BlockPos(12, 2, 2);
        BlockPos firstRelay = new BlockPos(3, 2, 2);
        BlockPos secondRelay = new BlockPos(10, 2, 2);
        helper.setBlock(nodeAt.below(), block("node_stabilizer"));
        helper.setBlock(nodeAt, TTBlocks.NODE.get());
        var node = (BlockEntityNode) helper.getBlockEntity(nodeAt);
        var capacity = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 100));
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), capacity, capacity));
        helper.setBlock(top, block("node_transducer"));
        helper.setBlock(top.north(), Blocks.REDSTONE_BLOCK);
        for (BlockPos relay : new BlockPos[] {firstRelay, secondRelay}) {
            helper.setBlock(relay.below(), Blocks.STONE);
            helper.setBlock(relay, block("vis_relay"));
        }
        helper.setBlock(bench, TTBlocks.ARCANE_WORKBENCH.get());
        helper.runAfterDelay(1090, () -> {
            var benchPos = helper.absolutePos(bench);
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("dc777f75-8b32-47ec-9a2a-cb25c6dfd129"), "tt_relay_test"));
            var aura = AuraManager.chunkAt(level, benchPos);
            float savedVis = aura.getVis();
            try {
                aura.setVis(0);
                TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("infusion_boost"), 2);
                var benchEntity = (com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlockEntity) helper.getBlockEntity(bench);
                var menu = (ArcaneWorkbenchMenu) benchEntity.createMenu(1, player.getInventory(), player);
                var recipe = (ArcaneCraftingRecipe) level.getRecipeManager().byKey(TTIds.rl("arcane_workbench/matrix_speed")).orElseThrow();
                for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
                    menu.getSlot(slot + 1).set(recipe.getIngredients().get(slot).getItems()[0].copyWithCount(2));
                }
                helper.assertTrue(node.isEnergized(), "Redstone transduction did not energize the node");
                int before = VisRelayNetwork.connections(level, benchPos).stream().mapToInt(VisRelayNetwork.Connection::available).sum();
                helper.assertTrue(before >= 200, "Relay chain does not reach the workbench");
                var wand = new net.minecraft.world.item.ItemStack(com.leclowndu93150.thaumaturge.registry.TTItems.WAND.get());
                com.leclowndu93150.thaumaturge.content.item.WandCharge.add(wand, TTAspects.ORDO, 200);
                menu.getSlot(ArcaneWorkbenchMenu.WAND_MENU_SLOT).set(wand);
                menu.broadcastChanges();
                helper.assertTrue(!menu.getSlot(0).hasItem(), "Relayed primal vis incorrectly paid the raw aura cost");
                aura.setVis(10000);
                float raw = aura.getVis();
                menu.broadcastChanges();
                helper.assertTrue(menu.getRequiredVis() == 500, "Mixed wand/relay payment unexpectedly added a surcharge");
                helper.assertTrue(menu.getSlot(0).remove(1).is(recipe.getResultItem(level.registryAccess()).getItem()), "Workbench rejected mixed wand and relay payment");
                int after = VisRelayNetwork.connections(level, benchPos).stream().mapToInt(VisRelayNetwork.Connection::available).sum();
                helper.assertTrue(before - after == 200 && raw - aura.getVis() == 500, "Workbench debited the wrong relay or aura cost");
                helper.assertTrue(com.leclowndu93150.thaumaturge.content.item.WandCharge.stored(wand, TTAspects.ORDO) == 0, "Workbench did not debit the wand");
                com.leclowndu93150.thaumaturge.content.item.WandCharge.add(wand, TTAspects.ORDO, 200);
                menu.broadcastChanges();
                var crafted = menu.quickMoveStack(player, 0);
                helper.assertTrue(crafted.is(recipe.getResultItem(level.registryAccess()).getItem()), "Shift-click did not return the crafted result");
                var nodeTag = node.saveWithoutMetadata();
                helper.setBlock(nodeAt, Blocks.AIR);
                helper.setBlock(nodeAt, TTBlocks.NODE.get());
                var restored = (BlockEntityNode) helper.getBlockEntity(nodeAt);
                restored.load(nodeTag);
                helper.assertTrue(restored.isEnergized() && VisRelayNetwork.connections(level, benchPos).size() == 1,
                        "The relay did not recover its energized source after reloading block-entity data");
                helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
                helper.assertTrue(VisRelayNetwork.connections(level, benchPos).isEmpty(), "The relay still drew through a solid block");
            } finally {
                aura.setVis(savedVis);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "vis_lab", timeoutTicks = 180)
    public static void visRelayKeepsItsSourceUntilThePathBreaks(GameTestHelper helper) {
        BlockPos consumer = new BlockPos(0, 2, 1);
        BlockPos relay = new BlockPos(4, 2, 1);
        BlockPos original = new BlockPos(8, 2, 1);
        BlockPos closer = new BlockPos(4, 2, 4);
        var capacity = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 20));
        placeAlmostEnergizedNode(helper, original, capacity);
        helper.setBlock(relay.below(), Blocks.STONE);
        helper.setBlock(relay, block("vis_relay"));
        helper.runAfterDelay(45, () -> {
            var saved = helper.getBlockEntity(relay).saveWithoutMetadata();
            helper.assertTrue(saved.getLong("Parent") == helper.absolutePos(original).asLong() && saved.getInt("Depth") == 1,
                    "Relay did not save its original source");
            helper.setBlock(relay, Blocks.AIR);
            helper.setBlock(relay, block("vis_relay"));
            helper.getBlockEntity(relay).load(saved);
            placeAlmostEnergizedNode(helper, closer, capacity);
        });
        helper.runAfterDelay(90, () -> {
            var retained = helper.getBlockEntity(relay).saveWithoutMetadata();
            helper.assertTrue(retained.getLong("Parent") == helper.absolutePos(original).asLong(),
                    "Relay abandoned its saved source when a closer source became available");
            helper.assertTrue(VisRelayNetwork.nearest(helper.getLevel(), helper.absolutePos(consumer)) != null,
                    "Restored relay did not supply its consumer");
            helper.setBlock(relay.east(2), Blocks.STONE);
            helper.assertTrue(VisRelayNetwork.nearest(helper.getLevel(), helper.absolutePos(consumer)) == null,
                    "An obstructed saved link still supplied vis");
        });
        helper.runAfterDelay(135, () -> {
            var rerouted = helper.getBlockEntity(relay).saveWithoutMetadata();
            helper.assertTrue(rerouted.getLong("Parent") == helper.absolutePos(closer).asLong(),
                    "Relay did not find the alternate source after its path broke");
            helper.succeed();
        });
    }

    private static void placeAlmostEnergizedNode(GameTestHelper helper, BlockPos pos, AspectList aspects) {
        helper.setBlock(pos.below(), block("node_stabilizer"));
        helper.setBlock(pos, TTBlocks.NODE.get());
        var node = (BlockEntityNode) helper.getBlockEntity(pos);
        node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), aspects, aspects));
        helper.setBlock(pos.above(), block("node_transducer"));
        var transducer = helper.getBlockEntity(pos.above());
        var saved = transducer.saveWithoutMetadata();
        saved.putInt("Count", 999);
        transducer.load(saved);
        helper.setBlock(pos.above().north(), Blocks.REDSTONE_BLOCK);
    }

    @GameTest(template = "essentia_lab")
    public static void mirroredInlaysImproveAltarStability(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos matrix = new BlockPos(4, 3, 4);
        BlockPos origin = helper.absolutePos(matrix);
        BlockPos left = matrix.offset(-1, -2, 0);
        BlockPos right = matrix.offset(1, -2, 0);
        helper.setBlock(left.below(), Blocks.STONE);
        helper.setBlock(right.below(), Blocks.STONE);
        float initial = InfusionMatrix.symmetryScore(level, origin);
        helper.setBlock(left, block("inlay"));
        helper.assertTrue(InfusionMatrix.symmetryScore(level, origin) < initial, "Unbalanced inlay did not increase risk");
        helper.setBlock(right, block("inlay"));
        helper.assertTrue(InfusionMatrix.symmetryScore(level, origin) > initial, "Mirrored inlay did not improve stability");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void brainJarResearchAwardsWarpOnlyOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "tt_brain_warp"));
        var id = TTIds.rl("jar_brain");
        var knowledge = TTAttachments.knowledge(player);
        knowledge.startResearch(id);
        knowledge.setStage(id, 1);
        var warp = TTAttachments.warp(player);
        helper.assertTrue(com.leclowndu93150.thaumaturge.content.research.ResearchManager.advanceStage(player, id),
                "The brain jar research did not reach its final stage");
        boolean wuss = com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig.WUSS_MODE.get();
        helper.assertTrue(warp.getNormal() == (wuss ? 0 : 1) && warp.getPermanent() == (wuss ? 0 : 2),
                "Research did not award the CE26 split of normal and permanent warp");
        int awarded = warp.total();
        helper.assertTrue(!com.leclowndu93150.thaumaturge.content.research.ResearchManager.advanceStage(player, id)
                && warp.total() == awarded, "Completed research awarded warp twice");
        helper.succeed();
    }

    private static Block block(String id) {
        return TTBulkContent.BLOCKS.get(id).get();
    }
}
