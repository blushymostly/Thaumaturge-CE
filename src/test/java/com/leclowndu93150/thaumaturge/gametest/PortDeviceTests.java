package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.aura.BlockEntityRechargePedestal;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayNetwork;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityVisGenerator;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.item.AmuletVisItem;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.research.scan.DiscoveryScans;
import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlockEntity;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;
import com.leclowndu93150.thaumaturge.mixin.world.entity.ExperienceOrbAccessor;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.SlotContext;

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
            helper.assertTrue(drops.size() == 1 && drops.get(0).getTagElement("BlockEntityTag").getInt("XP") == 21, "Broken brain jar did not keep its XP");
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
            int released = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(1)).stream().mapToInt(xp -> xp.getValue() * ((ExperienceOrbAccessor) xp).thaumaturge$getCount()).sum();
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

    @GameTest(template = "essentia_lab")
    public static void workbenchUsesWandVisForSimpleMechanism(GameTestHelper helper) {
        var level = helper.getLevel();
        var bench = new BlockPos(2, 1, 2);
        helper.setBlock(bench, TTBlocks.ARCANE_WORKBENCH.get());
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("ad845cc2-ea21-42a2-a83e-0492ec6cf353"), "tt_wand_payment"));
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("base_artifice"), 2);
        var block = (ArcaneWorkbenchBlockEntity) helper.getBlockEntity(bench);
        var aura = AuraManager.chunkAt(level, helper.absolutePos(bench));
        float previousVis = aura.getVis();
        try {
            aura.setVis(100);
            var menu = (ArcaneWorkbenchMenu) block.createMenu(1, player.getInventory(), player);
            var recipe = (ArcaneCraftingRecipe) level.getRecipeManager().byKey(TTIds.rl("arcane_workbench/mechanism_simple")).orElseThrow();
            for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
                var choices = recipe.getIngredients().get(slot).getItems();
                if (choices.length > 0)
                    menu.getSlot(ArcaneWorkbenchMenu.GRID_START + slot).set(choices[0].copyWithCount(3));
            }
            int ignis = ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(TTAspects.IGNIS);
            int aqua = ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(TTAspects.AQUA);
            var wand = new ItemStack(TTItems.WAND.get());
            WandCharge.add(wand, TTAspects.IGNIS, 400);
            menu.getSlot(ArcaneWorkbenchMenu.WAND_MENU_SLOT).set(wand);
            helper.assertTrue(!menu.getSlot(0).hasItem() && menu.wandVisToSpend(ignis) == 2 && menu.crystalsToConsume(aqua) == 1,
                    "Missing Aqua must block crafting and show only the planned Ignis debit");
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 400 && aura.getVis() == 100, "The payment preview must not spend wand charge or aura");

            var aquaSlot = menu.getSlot(ArcaneWorkbenchMenu.CRYSTAL_MENU_START + aqua);
            aquaSlot.set(EssentiaCrystalFactory.of(TTAspects.AQUA, 1));
            helper.assertTrue(menu.getRequiredVis() == 13 && menu.wandVisToSpend(aqua) == 0, "Mixed crystal payment should use the 25 percent aura surcharge");
            var result = recipe.getResultItem(level.registryAccess());
            helper.assertTrue(menu.getSlot(0).remove(1).is(result.getItem()), "Mixed crystal payment did not craft");
            helper.assertTrue(aquaSlot.getItem().isEmpty() && aura.getVis() == 87 && WandCharge.stored(wand, TTAspects.IGNIS) == 200,
                    "The craft must debit two Ignis vis, one Aqua crystal and thirteen aura vis");

            aquaSlot.set(EssentiaCrystalFactory.of(TTAspects.AQUA, 1));
            WandCharge.add(wand, TTAspects.AQUA, 200);
            menu.broadcastChanges();
            helper.assertTrue(menu.getRequiredVis() == 11 && menu.wandVisToSpend(aqua) == 2 && menu.crystalsToConsume(aqua) == 0,
                    "Fully funded wand payment must replace the crystal and use the iron cap modifier");
            helper.assertTrue(menu.quickMoveStack(player, 0).is(result.getItem()), "Shift-click did not use the wand");
            helper.assertTrue(aquaSlot.getItem().getCount() == 1 && aura.getVis() == 76 && WandCharge.stored(wand, TTAspects.IGNIS) == 0 && WandCharge.stored(wand, TTAspects.AQUA) == 0,
                    "Shift-click spent a crystal or used the wrong amount of wand vis and aura");
        } finally {
            aura.setVis(previousVis);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void generatorPushesOnlyThroughItsOutput(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = new BlockPos(2, 1, 2);
        helper.setBlock(base, block("vis_generator").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        var pos = helper.absolutePos(base);
        var generator = (BlockEntityVisGenerator) helper.getBlockEntity(base);
        var face = level.getBlockState(pos).getValue(BlockStateProperties.FACING);
        var outputAt = pos.relative(face);
        level.setBlockAndUpdate(outputAt, Blocks.CHEST.defaultBlockState());
        var receiver = new EnergyReceiver(outputAt, level.getBlockState(outputAt), face.getOpposite());
        level.setBlockEntity(receiver);
        var aura = AuraManager.chunkAt(level, pos);
        float previousVis = aura.getVis();
        try {
            aura.setVis(4);
            BlockEntityVisGenerator.serverTick(level, pos, level.getBlockState(pos), generator);
            helper.assertTrue(receiver.storage.getEnergyStored() == 20 && generator.getEnergyStored() == 980 && aura.getVis() == 3,
                    "One aura vis must fill 1000 FE and push 20 through the output only");
            var energy = generator.getCapability(ForgeCapabilities.ENERGY, face).resolve().orElseThrow();
            helper.assertTrue(!energy.canReceive() && !energy.canExtract() && !generator.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).isPresent(),
                    "Generator capability must expose the read-only buffer on its output face");
            level.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.assertTrue(!level.getBlockState(pos).getValue(BlockStateProperties.ENABLED), "Redstone did not disable the generator");
            BlockEntityVisGenerator.serverTick(level, pos, level.getBlockState(pos), generator);
            helper.assertTrue(receiver.storage.getEnergyStored() == 20 && aura.getVis() == 3, "Powered generator still operated");
            var saved = generator.saveWithoutMetadata();
            var restored = new BlockEntityVisGenerator(pos, level.getBlockState(pos));
            restored.load(saved);
            helper.assertTrue(restored.getEnergyStored() == 980, "Generator lost its energy buffer after saving");
            helper.assertTrue(level.getRecipeManager().byKey(TTIds.rl("arcane_workbench/vis_generator")).isPresent(), "Generator survival recipe is missing");
        } finally {
            aura.setVis(previousVis);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void wornVisAmuletsChargeTheLowestPrimal(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("1e4d62a8-022a-4414-9f43-8ef98ec72797"), "tt_amulet_vis"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(Vec3.atCenterOf(pos));
        var aura = AuraManager.chunkAt(level, pos);
        float previousVis = aura.getVis();
        try {
            aura.setVis(4);
            var first = new ItemStack(TTItems.WAND.get());
            var offhand = new ItemStack(TTItems.WAND.get());
            for (var primal : WandCharge.PRIMALS) {
                WandCharge.add(first, primal, 500);
                if (!primal.equals(TTAspects.IGNIS))
                    WandCharge.add(offhand, primal, 1000);
            }
            player.getInventory().setItem(1, first);
            player.setItemInHand(InteractionHand.OFF_HAND, offhand);
            var found = (AmuletVisItem) TTBulkItems.ITEMS.get("amulet_vis").get();
            var crafted = (AmuletVisItem) TTBulkItems.ITEMS.get("amulet_vis_crafted").get();
            var slot = new SlotContext("necklace", player, 0, false, true);
            player.tickCount = 20;
            found.curioTick(slot, new ItemStack(found));
            helper.assertTrue(aura.getVis() == 4, "Vis Stone should pulse once every 100 ticks");
            crafted.curioTick(slot, new ItemStack(crafted));
            player.tickCount = 100;
            found.curioTick(slot, new ItemStack(found));
            helper.assertTrue(aura.getVis() == 2 && WandCharge.stored(offhand, TTAspects.IGNIS) == 200 && WandCharge.stored(first, TTAspects.IGNIS) == 500,
                    "Amulets did not spend aura on the lowest pool");
            var loot = level.getServer().getLootData().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
            helper.assertTrue(loot.getPool("thaumaturge_vis_stone") != null, "Vis Stone is missing from dungeon treasure");
            var recipe = (InfusionRecipe) level.getRecipeManager().byKey(TTIds.rl("infusion/amulet_vis_crafted")).orElseThrow();
            helper.assertTrue(recipe.components().size() == 6 && !recipe.isUnlocked(player) && level.getRecipeManager().byKey(TTIds.rl("amulet_mundane")).isPresent(),
                    "Amulet progression recipes are incomplete");
            TTAttachments.knowledge(player).scan(TTIds.rl("crystal_aer"));
            DiscoveryScans.restoreKnownScans(player);
            helper.assertTrue(TTAttachments.knowledge(player).isComplete(TTIds.rl("scanned/orecrystal")), "Old crystal scans must unlock the Crystal Farmer prerequisite");
            TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("vis_amulet"), 2);
            helper.assertTrue(recipe.isUnlocked(player), "Completing the amulet research did not unlock its infusion");
        } finally {
            aura.setVis(previousVis);
        }
        helper.succeed();
    }

    private static final class EnergyReceiver extends ChestBlockEntity {
        private final EnergyStorage storage = new EnergyStorage(1000);
        private final LazyOptional<EnergyStorage> capability = LazyOptional.of(() -> storage);
        private final Direction input;

        private EnergyReceiver(BlockPos pos, BlockState state, Direction input) {
            super(pos, state);
            this.input = input;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> type, Direction side) {
            return type == ForgeCapabilities.ENERGY && side == input ? capability.cast() : super.getCapability(type, side);
        }
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
        for (BlockPos relay : new BlockPos[]{firstRelay, secondRelay}) {
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
            } finally {
                aura.setVis(savedVis);
            }
            helper.setBlock(bench, TTBlocks.RECHARGE_PEDESTAL.get());
            var pedestal = (BlockEntityRechargePedestal) helper.getBlockEntity(bench);
            pedestal.setItem(new net.minecraft.world.item.ItemStack(com.leclowndu93150.thaumaturge.registry.TTItems.WAND.get()));
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(com.leclowndu93150.thaumaturge.content.item.WandCharge.stored(pedestal.getItem(), TTAspects.AER) > 0,
                        "The pedestal did not receive vis through relays beyond direct node range");
                helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
                helper.assertTrue(VisRelayNetwork.connections(level, helper.absolutePos(bench)).isEmpty(), "The relay still drew through a solid block");
                helper.succeed();
            });
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
            helper.assertTrue(saved.getLong("Parent") == helper.absolutePos(original).asLong() && saved.getInt("Depth") == 1, "Relay did not save its original source");
            helper.setBlock(relay, Blocks.AIR);
            helper.setBlock(relay, block("vis_relay"));
            helper.getBlockEntity(relay).load(saved);
            placeAlmostEnergizedNode(helper, closer, capacity);
        });
        helper.runAfterDelay(90, () -> {
            var retained = helper.getBlockEntity(relay).saveWithoutMetadata();
            helper.assertTrue(retained.getLong("Parent") == helper.absolutePos(original).asLong(), "Relay abandoned its saved source when a closer source became available");
            helper.assertTrue(VisRelayNetwork.nearest(helper.getLevel(), helper.absolutePos(consumer)) != null, "Restored relay did not supply its consumer");
            helper.setBlock(relay.east(2), Blocks.STONE);
            helper.assertTrue(VisRelayNetwork.nearest(helper.getLevel(), helper.absolutePos(consumer)) == null, "An obstructed saved link still supplied vis");
        });
        helper.runAfterDelay(135, () -> {
            var rerouted = helper.getBlockEntity(relay).saveWithoutMetadata();
            helper.assertTrue(rerouted.getLong("Parent") == helper.absolutePos(closer).asLong(), "Relay did not find the alternate source after its path broke");
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
        helper.assertTrue(com.leclowndu93150.thaumaturge.content.research.ResearchManager.advanceStage(player, id), "The brain jar research did not reach its final stage");
        boolean wuss = com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig.WUSS_MODE.get();
        helper.assertTrue(warp.getNormal() == (wuss ? 0 : 1) && warp.getPermanent() == (wuss ? 0 : 2), "Research did not award the CE26 split of normal and permanent warp");
        int awarded = warp.total();
        helper.assertTrue(!com.leclowndu93150.thaumaturge.content.research.ResearchManager.advanceStage(player, id) && warp.total() == awarded, "Completed research awarded warp twice");
        helper.succeed();
    }

    private static Block block(String id) {
        return TTBulkContent.BLOCKS.get(id).get();
    }
}
