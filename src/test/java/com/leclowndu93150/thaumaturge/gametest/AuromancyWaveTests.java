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
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorBlockEntity;
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorMenu;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell.Part;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.item.WandParts;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundFocalDraftPacket;
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
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class AuromancyWaveTests {
    @GameTest(template = "essentia_lab")
    public static void wandAssemblyLoadsAllCE26Forms(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getRecipeManager();
        for (var cap : WandParts.Cap.values()) {
            for (var rod : WandParts.Rod.values()) {
                String suffix = cap.id() + "_" + rod.id();
                var assembly = recipes.byKey(TTIds.rl("wand/assembly/" + suffix)).orElseThrow();
                var wand = assembly.getResultItem(level.registryAccess());
                helper.assertTrue(WandParts.rod(wand) == rod && WandParts.cap(wand) == cap && !WandParts.sceptre(wand) && WandCharge.capacity(wand) == rod.capacity() * 100,
                        "Assembly lost parts or capacity for " + suffix);
                var sceptreRecipe = recipes.byKey(TTIds.rl("wand/sceptre/" + suffix));
                if (rod.staff()) {
                    helper.assertTrue(sceptreRecipe.isEmpty(), "A staff core must not yield a sceptre");
                } else {
                    var sceptre = sceptreRecipe.orElseThrow().getResultItem(level.registryAccess());
                    helper.assertTrue(sceptreRecipe.get() instanceof ArcaneCraftingRecipe awb && awb.visCost() > 0 && WandParts.sceptre(sceptre) && WandParts.cap(sceptre) == cap
                            && WandParts.rod(sceptre) == rod && WandCharge.capacity(sceptre) == rod.capacity() * 150, "Sceptre assembly lost parts or capacity for " + suffix);
                }
                boolean starter = rod == WandParts.Rod.WOOD && cap == WandParts.Cap.IRON;
                helper.assertTrue(starter != (assembly instanceof ArcaneCraftingRecipe), "Wrong workstation for " + suffix);
            }
        }
        var copper = (ArcaneCraftingRecipe) recipes.byKey(TTIds.rl("wand/part/wand_cap_copper")).orElseThrow();
        helper.assertTrue(copper.getIngredients().get(0).test(new ItemStack(TTBulkItems.ITEMS.get("nugget_copper").get())), "Native copper nuggets must match the Forge cap ingredient");

        var sceptreRecipe = (ArcaneCraftingRecipe) recipes.byKey(TTIds.rl("wand/sceptre/silver_wood")).orElseThrow();
        var silverSceptre = sceptreRecipe.getResultItem(level.registryAccess());
        helper.assertTrue(sceptreRecipe.visCost() == 36 && sceptreRecipe.research().equals("thaumaturge:sceptre") && WandParts.costPercent(silverSceptre, TTAspects.AER) == 85
                && WandParts.costPercent(silverSceptre, TTAspects.ORDO) == 90, "Silver sceptre cost or primal discounts differ from CE26");
        var silverNuggets = net.minecraft.tags.ItemTags.create(TTIds.rl("forge", "nuggets/silver"));
        var taggedSilver = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(silverNuggets);
        var silverRecipe = recipes.byKey(TTIds.rl("wand/part/wand_cap_silver_inert"));
        helper.assertTrue(silverRecipe.isPresent() == taggedSilver.iterator().hasNext(), "Silver cap crafting must load only if the common silver nugget tag has items");
        helper.assertTrue(!new ItemStack(TTBulkItems.ITEMS.get("nugget_silver").get()).is(silverNuggets) && recipes.byKey(TTIds.rl("crucible/cluster_silver_from_iron")).isEmpty(),
                "Thaumaturge must not supply silver for new caps in standalone Forge");

        var staff = recipes.byKey(TTIds.rl("wand/assembly/iron_primal_staff")).orElseThrow().getResultItem(level.registryAccess()).copy();
        var blaze = recipes.byKey(TTIds.rl("wand/assembly/gold_blaze")).orElseThrow().getResultItem(level.registryAccess()).copy();
        var player = helper.makeMockSurvivalPlayer();
        for (var primal : WandCharge.PRIMALS) {
            if (!primal.equals(TTAspects.PERDITIO))
                WandCharge.add(staff, primal, 2500);
        }
        helper.runAfterDelay(60 - level.getGameTime() % 60, () -> {
            TTItems.WAND.get().inventoryTick(blaze, level, player, 1, false);
            TTItems.WAND.get().inventoryTick(staff, level, player, 2, false);
            helper.assertTrue(WandCharge.stored(blaze, TTAspects.IGNIS) == 100 && WandCharge.stored(blaze, TTAspects.AER) == 0 && WandCharge.stored(staff, TTAspects.PERDITIO) == 100,
                    "Elemental and primal cores must restore only eligible primals in the inventory");
            helper.succeed();
        });
    }

    @GameTest(template = "essentia_lab")
    public static void silverwoodWandCapturesAndReleasesNode(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("1253687e-071c-4a47-824c-f36f7d20ecb2"), "tt_node_jar"));
        TTAttachments.knowledge(player).markResearchComplete(TTIds.rl("node_jar"), 2);
        var wand = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_silverwood")).orElseThrow().getResultItem(level.registryAccess()).copy();
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
        player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 90, 0);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(glass), Direction.EAST, glass, false));
        for (var primal : WandCharge.PRIMALS)
            WandCharge.add(wand, primal, 6999);
        wand.getItem().useOn(context);
        helper.assertTrue(level.getBlockEntity(pos) == node && WandCharge.stored(wand, TTAspects.AER) == 6999, "A failed capture changed the node or charged the wand");
        for (var primal : WandCharge.PRIMALS)
            WandCharge.add(wand, primal, 1001);
        wand.getItem().useOn(context);
        helper.assertTrue(node.isCapturing() && level.getBlockState(glass).is(Blocks.GLASS), "Using the wand should start a timed capture");
        helper.assertTrue(WandCharge.PRIMALS.stream().allMatch(primal -> WandCharge.stored(wand, primal) == 1000), "Capture did not charge exactly 70 vis of every primal");
        helper.assertTrue(node.drain(TTAspects.AER, 1) == 0, "A capturing node must not provide vis");
        var replaced = pos.west();
        level.setBlockAndUpdate(replaced, Blocks.OBSIDIAN.defaultBlockState());
        var replacedWithGlass = pos.north();
        level.removeBlock(replacedWithGlass, false);
        var glassStack = new ItemStack(Blocks.GLASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, glassStack);
        glassStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(replacedWithGlass), Direction.UP, replacedWithGlass, false)));
        helper.assertTrue(level.getBlockState(replacedWithGlass).is(Blocks.GLASS), "The replacement glass did not place");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var structure = NodeJarRitual.structurePositions(pos);
        for (int tick = 0; tick < 160 && structure.stream().noneMatch(level::isEmptyBlock); tick++)
            node.serverTick(level, pos);
        helper.assertTrue(structure.stream().anyMatch(level::isEmptyBlock) && structure.stream().anyMatch(cell -> level.getBlockState(cell).is(Blocks.GLASS)),
                "The ritual should disassemble the shell in stages");
        var savedCapture = node.saveWithoutMetadata();
        level.removeBlockEntity(pos);
        var resumed = new BlockEntityNode(pos, TTBlocks.NODE.get().defaultBlockState());
        resumed.load(savedCapture);
        level.setBlockEntity(resumed);
        for (int tick = 0; tick < 200 && !resumed.isRemoved(); tick++)
            resumed.serverTick(level, pos);
        helper.assertTrue(level.getBlockEntity(pos) instanceof JarNodeBlock.Entity && level.isEmptyBlock(glass) && level.getBlockState(replaced).is(Blocks.OBSIDIAN)
                && level.getBlockState(replacedWithGlass).is(Blocks.GLASS), "The resumed capture should preserve replacement blocks and finish");

        var jar = (JarNodeBlock.Entity) level.getBlockEntity(pos);
        var dropped = net.minecraft.world.level.block.Block.getDrops(jar.getBlockState(), level, pos, jar);
        helper.assertTrue(dropped.size() == 1 && TTItemData.getNodeData(dropped.get(0)) != null, "The jar dropped without its node");
        level.removeBlock(pos, false);
        BlockPos destination = pos.east(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped.get(0));
        dropped.get(0).getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(destination), Direction.UP, destination, false)));
        helper.assertTrue(level.getBlockEntity(destination) instanceof JarNodeBlock.Entity, "The jar could not be placed again");
        var moved = (JarNodeBlock.Entity) level.getBlockEntity(destination);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        moved.getBlockState().use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(destination), Direction.UP, destination, false));
        helper.assertTrue(level.getBlockState(destination).is(TTBlocks.NODE.get()), "Using the wand on the jar did not release its node");
        var released = (BlockEntityNode) level.getBlockEntity(destination);
        helper.assertTrue(released.kind() == NodeType.NORMAL && released.capacity().amountOf(TTAspects.AER) == 12 && released.getAspects().amountOf(TTAspects.AER) == 12,
                "Transport changed the type or aspects");
        helper.assertTrue(released.trait() == NodeModifier.PALE || released.trait() == NodeModifier.FADING, "Transport gave the node an invalid modifier");
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
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 1 && aura.getVis() == 99, "The battery did not store one vis or report one comparator level");
            level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            battery.tick(level.getBlockState(pos), level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 0 && aura.getVis() == 100, "Redstone did not discharge the battery back to its chunk");
            helper.assertTrue(level.getRecipeManager().byKey(TTIds.rl("arcane_workbench/vis_battery")).isPresent(), "The Vis Battery recipe did not load");
            var rod = (InfusionRecipe) level.getRecipeManager().byKey(TTIds.rl("infusion/wand_rod_silverwood")).orElseThrow();
            helper.assertTrue(
                    rod.components().size() == WandCharge.PRIMALS.size() + 1 && rod.components().stream().filter(ingredient -> ingredient.test(new ItemStack(TTItems.SALIS_MUNDUS.get()))).count() == 1
                            && WandCharge.PRIMALS.stream().allMatch(primal -> rod.components().stream().filter(ingredient -> ingredient.test(EssentiaCrystalFactory.of(primal, 1))).count() == 1),
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
        helper.assertTrue(helper.getBlockState(center).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST, "The model and outlet face the wrong way");
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
    public static void basicFocusInscribesResumesAndCasts(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper, "tt_basic_focus");
        var knowledge = TTAttachments.knowledge(player);
        knowledge.markResearchComplete(TTIds.rl("unlock_auromancy"), 3);
        knowledge.markResearchComplete(TTIds.rl("base_auromancy"), 3);
        var pos = new BlockPos(1, 1, 1);
        var absolute = helper.absolutePos(pos);
        var bench = TTBulkContent.BLOCKS.get("focal_manipulator").get();
        helper.setBlock(pos, bench);
        var table = (FocalManipulatorBlockEntity) level.getBlockEntity(absolute);
        player.moveTo(absolute.getX() + 1.5, absolute.getY(), absolute.getZ() + 0.5);
        var recipe = (com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe) level.getRecipeManager().byKey(TTIds.rl("crucible/focus_1")).orElseThrow();
        helper.assertTrue(recipe.matchesCatalyst(EssentiaCrystalFactory.ordo()) && !recipe.matchesCatalyst(EssentiaCrystalFactory.ignis()), "Blank focus did not require an Ordo crystal");

        var aura = AuraManager.chunkAt(level, absolute);
        float oldVis = aura.getVis();
        aura.setVis(100);
        player.experienceLevel = 3;
        player.setItemInHand(InteractionHand.OFF_HAND, EssentiaCrystalFactory.ignis());
        var focus = new ItemStack(TTBulkItems.ITEMS.get("focus_1").get());
        var menu = (FocalManipulatorMenu) table.createMenu(1, player.getInventory(), player);
        menu.getSlot(0).set(focus);
        var touch = Part.TOUCH.node();
        var incomplete = new FocusSpell(Part.ORIGIN.node().withChildren(java.util.List.of(touch)));
        var fire = Part.FIRE.node();
        var draft = new FocusSpell(Part.ORIGIN.node().withChildren(java.util.List.of(touch.withChildren(java.util.List.of(fire)))));
        var frost = new FocusSpell(Part.ORIGIN.node().withChildren(java.util.List.of(touch.withChildren(java.util.List.of(Part.FROST.node())))));
        int version = menu.focusVersion();
        helper.assertTrue(!menu.submitDraft(player, version - 1, draft, "", false) && !menu.submitDraft(player, version, frost, "", false), "A stale draft or an unresearched part was accepted");
        helper.assertTrue(menu.submitDraft(player, version, incomplete, "Fire Touch", false) && !menu.submitDraft(player, version, incomplete, "Fire Touch", true) && player.experienceLevel == 3,
                "The incomplete graph was not editable or started writing");
        table.load(table.saveWithoutMetadata());
        helper.assertTrue(table.draft().equals(incomplete) && table.draftName().equals("Fire Touch"), "Reload did not restore the unfinished graph and its name");
        helper.assertTrue(!menu.submitDraft(player, version, draft, "Fire Touch", true) && player.experienceLevel == 3, "Incomplete payment started inscription");
        player.setItemInHand(InteractionHand.MAIN_HAND, EssentiaCrystalFactory.of(TTAspects.AVERSIO, 1));
        helper.assertTrue(menu.submitDraft(player, version, draft, "Fire Touch", true) && table.busy() && !menu.getSlot(0).mayPickup(player)
                && !menu.submitDraft(player, version, draft, "Changed", true) && player.experienceLevel == 1 && player.getOffhandItem().isEmpty() && player.getMainHandItem().isEmpty(),
                "The focal manipulator did not charge once and lock the slot");

        helper.runAfterDelay(22, () -> {
            var saved = table.saveWithoutMetadata();
            float remaining = saved.getFloat("RemainingVis");
            helper.assertTrue(remaining > 0 && remaining < saved.getFloat("TotalVis") && !TTItemData.root(focus).contains("Spell"), "Inscription did not advance gradually");
            table.load(saved);
            helper.assertTrue(table.busy() && table.saveWithoutMetadata().getFloat("RemainingVis") == remaining, "Reload did not restore the outstanding vis");
        });
        helper.runAfterDelay(80, () -> {
            try {
                helper.assertTrue(!table.busy() && menu.getSlot(0).mayPickup(player) && player.experienceLevel == 1, "Inscription did not finish after reload or charged again");
                var result = table.focus().removeItem(0, 1);
                var savedGraph = TTItemData.root(result).getCompound("Spell");
                helper.assertTrue(result.hasCustomHoverName() && result.getHoverName().getString().equals("Fire Touch"), "Completed focus did not get the saved name");
                menu.getSlot(0).set(result.copy());
                helper.assertTrue(menu.draft().equals(draft) && menu.draftName().equals("Fire Touch"), "A recorded focus did not reopen in the editor");
                table.focus().removeItem(0, 1);
                helper.assertTrue(savedGraph.getCompound("root").getString("part").equals("thaumaturge:origin"), "The focal manipulator did not save the spell graph");
                var wand = new ItemStack(TTItems.WAND.get());
                TTItemData.setWandVis(wand, 10);
                int ignis = WandCharge.stored(wand, TTAspects.IGNIS);
                player.setItemInHand(InteractionHand.MAIN_HAND, wand);
                player.setItemInHand(InteractionHand.OFF_HAND, result);
                player.setShiftKeyDown(true);
                wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
                helper.assertTrue(player.getOffhandItem().isEmpty(), "The focus did not socket");
                player.setShiftKeyDown(false);
                wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
                helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == ignis - 16 && WandCharge.stored(wand, TTAspects.ORDO) == 166 && TTItemData.getWandVis(wand) == 0,
                        "Fire Touch did not debit its primal costs or migrate the old charge");
                var greatwood = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_greatwood")).orElseThrow().getResultItem(level.registryAccess());
                helper.assertTrue(WandItem.capacity(greatwood) == 50, "Greatwood wand did not retain its rod data");

                menu.getSlot(0).set(new ItemStack(TTBulkItems.ITEMS.get("focus_1").get()));
                player.experienceLevel = 3;
                player.setItemInHand(InteractionHand.MAIN_HAND, EssentiaCrystalFactory.of(TTAspects.AVERSIO, 1));
                player.setItemInHand(InteractionHand.OFF_HAND, EssentiaCrystalFactory.ignis());
                helper.assertTrue(menu.submitDraft(player, menu.focusVersion(), draft, "", true) && table.busy(), "The second inscription did not start");
                level.destroyBlock(absolute, true);
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1));
                long blankFoci = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(TTBulkItems.ITEMS.get("focus_1").get()) && !TTItemData.root(stack).contains("Spell")).count();
                helper.assertTrue(blankFoci == 1 && drops.stream().anyMatch(drop -> drop.getItem().is(bench.asItem())),
                        "Breaking the manipulator did not return the block and exactly one unfinished focus");
                helper.succeed();
            } finally {
                aura.setVis(oldVis);
                player.setShiftKeyDown(false);
            }
        });
    }

    @GameTest(template = "essentia_lab")
    public static void focalDraftPacketKeepsBranchSettings(GameTestHelper helper) {
        var touch = Part.TOUCH.node().withChildren(java.util.List.of(Part.FIRE.node()));
        var bolt = Part.BOLT.node().withChildren(java.util.List.of(Part.FROST.node().withSetting("duration", 5)));
        var fork = Part.BRANCH.node().withChildren(java.util.List.of(touch, bolt));
        var draft = new FocusSpell(Part.ORIGIN.node().withChildren(java.util.List.of(fork)));
        var packet = new ServerboundFocalDraftPacket(4, 19, "Линза", draft, true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            ServerboundFocalDraftPacket.encode(packet, buffer);
            var decoded = ServerboundFocalDraftPacket.decode(buffer);
            helper.assertTrue(decoded.equals(packet) && decoded.draft().complete() && !buffer.isReadable(), "Draft packet changed the branch, settings, or name");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void wandRefinesAuraOrDrawsDirectlyFromNode(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var contents = com.leclowndu93150.thaumaturge.api.aspect.AspectList.ofEntries(java.util.List.of(new com.leclowndu93150.thaumaturge.api.aspect.AspectInstance(TTAspects.IGNIS, 5)));
        var data = new com.leclowndu93150.thaumaturge.content.aura.node.NodeData(com.leclowndu93150.thaumaturge.api.nodes.NodeType.NORMAL, java.util.Optional.empty(), contents, contents);
        helper.assertTrue(com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator.createNodeAt(level, at, data), "Could not create a test node");
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
            helper.assertTrue(item.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Wand without a focus did not begin refining");
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.AER) == 10 && Math.abs(aura.getVis() - 49.7F) < 0.001F, "Aura refinement did not transfer 0.1 primal vis for 0.3 raw vis");

            player.setYRot(180);
            level.setBlockAndUpdate(at.south(), Blocks.STONE.defaultBlockState());
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(node.getAspects().amountOf(TTAspects.IGNIS) == 5 && Math.abs(aura.getVis() - 49.4F) < 0.001F, "Wand reached a node through a block instead of refining the local aura");

            level.setBlockAndUpdate(at.south(), Blocks.AIR.defaultBlockState());
            float beforeNode = aura.getVis();
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 100 && node.getAspects().amountOf(TTAspects.IGNIS) == 4 && aura.getVis() == beforeNode,
                    "Ignis was not transferred directly from the node");
            var clientNode = new BlockEntityNode(at, node.getBlockState());
            clientNode.setLevel(level);
            clientNode.handleUpdateTag(node.getUpdateTag());
            var effect = clientNode.drainEffect();
            helper.assertTrue(effect != null && effect.player().equals(player.getUUID()) && effect.color() == TTAspects.get(TTAspects.IGNIS).color(),
                    "The node did not synchronize the caster and aspect for the extraction ribbon");
            helper.assertTrue(!node.saveWithoutMetadata().hasUUID("DrainPlayer"), "A transient extraction effect was saved to disk");
        } finally {
            aura.setVis(originalVis);
            player.stopUsingItem();
            level.setBlockAndUpdate(at.south(), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!WandCharge.payTouchFocus(wand, TTAspects.IGNIS, 300, true) && WandCharge.stored(wand, TTAspects.IGNIS) == 100, "Failed cast spent some of the wand charge");
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
        var wand = level.getRecipeManager().byKey(TTIds.rl("wand/assembly/iron_greatwood")).orElseThrow().getResultItem(level.registryAccess()).copy();
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
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 700 && node.getAspects().amountOf(TTAspects.IGNIS) == 1, "Preservation should leave the last point");
            player.setShiftKeyDown(true);
            item.onUseTick(level, player, wand, 5);
            helper.assertTrue(WandCharge.stored(wand, TTAspects.IGNIS) == 800 && node.getAspects().amountOf(TTAspects.IGNIS) == 0, "Crouching should override preservation");
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
        helper.setBlock(log, TTBlocks.SILVERWOOD_NODE_LOG.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
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
            helper.assertTrue(node.capacity().amountOf(TTAspects.IGNIS) == 0 && node.getAspects().amountOf(TTAspects.AER) == 1, "The empty aspect should expire while the stocked aspect survives");
            var remainingLog = helper.getBlockState(log);
            helper.assertTrue(remainingLog.is(TTBlocks.LOG_SILVERWOOD.get()) && remainingLog.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                    "An exhausted node log should keep the original log axis");
            var remainingTotem = helper.getBlockState(totem);
            helper.assertTrue(remainingTotem.is(ordinaryTotem) && remainingTotem.getValue(BlockStateProperties.UP), "An exhausted totem should keep its connection to the next totem");
            helper.assertTrue(level.isEmptyBlock(helper.absolutePos(jar)), "A fully exhausted jarred node should collapse");
            helper.succeed();
        });
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }

    @GameTest(template = "essentia_lab")
    public static void strongerNodeDrainsOnlyUnprotectedDonors(GameTestHelper helper) {
        var level = helper.getLevel();
        var at = helper.absolutePos(new BlockPos(3, 2, 3));
        var donorAt = at.east();
        var nodeState = TTBlocks.NODE.get().defaultBlockState();
        level.setBlockAndUpdate(at, nodeState);
        level.setBlockAndUpdate(donorAt, nodeState);
        var receiver = (BlockEntityNode) level.getBlockEntity(at);
        var donor = (BlockEntityNode) level.getBlockEntity(donorAt);
        var one = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 1));
        var five = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 5));
        receiver.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), one, five));
        donor.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), one, one));

        var stabilizer = TTBulkContent.BLOCKS.get("node_stabilizer").get();
        level.setBlockAndUpdate(donorAt.below(), stabilizer.defaultBlockState());
        dischargeEast(level, receiver);
        helper.assertTrue(donor.getAspects().amountOf(TTAspects.AER) == 1, "A stabilized donor was drained");

        level.removeBlock(donorAt.below(), false);
        var energized = donor.saveWithoutMetadata();
        energized.put("OriginalCapacity", energized.getCompound(BlockEntityNode.NBT_NODE).get("aspects_base").copy());
        donor.load(energized);
        dischargeEast(level, receiver);
        helper.assertTrue(donor.isEnergized() && level.getBlockEntity(donorAt) == donor && donor.getAspects().isEmpty(),
                "An unprotected activated donor should lose vis and leave transducer handling in place");

        donor.applyNodeData(new NodeData(NodeType.NORMAL, Optional.empty(), one, one));
        dischargeEast(level, receiver);
        helper.assertTrue(level.isEmptyBlock(donorAt) && receiver.getAspects().amountOf(TTAspects.AER) == 3 && receiver.capacity().amountOf(TTAspects.AER) == 5,
                "The stronger node should receive a point and the ordinary empty donor should collapse");
        helper.succeed();
    }

    private static void dischargeEast(net.minecraft.server.level.ServerLevel level, BlockEntityNode receiver) {
        receiver.serverTick(level, receiver.getBlockPos());
        level.getRandom().setSeed(168); // The CE26 offset draws select one block east.
        receiver.serverTick(level, receiver.getBlockPos());
    }

    @GameTest(template = "essentia_lab")
    public static void highFluxReclassifiesNodesButLeavesPureNodesImmune(GameTestHelper helper) {
        var level = helper.getLevel();
        var at = helper.absolutePos(new BlockPos(3, 2, 3));
        var aura = AuraManager.chunkAt(level, at);
        float oldFlux = aura.getFlux();
        short oldBase = aura.getBase();
        try {
            aura.setBase((short) 100);
            aura.setFlux(60);
            var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 5));
            for (NodeType type : java.util.List.of(NodeType.NORMAL, NodeType.PURE)) {
                level.removeBlock(at, false);
                level.setBlockAndUpdate(at, TTBlocks.NODE.get().defaultBlockState());
                var node = (BlockEntityNode) level.getBlockEntity(at);
                node.applyNodeData(new NodeData(type, Optional.of(NodeModifier.BRIGHT), aspects, aspects));
                for (int tick = 0; tick < 399; tick++)
                    node.serverTick(level, at);
                aura.setFlux(60);
                level.getRandom().setSeed(0); // Its first draw from 20 is zero.
                node.serverTick(level, at);
                helper.assertTrue(node.kind() == (type == NodeType.PURE ? NodeType.PURE : NodeType.TAINTED), "High flux should taint ordinary nodes but not pure nodes");
            }
        } finally {
            aura.setBase(oldBase);
            aura.setFlux(oldFlux);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void cleanMagicalForestImprovesNodeModifier(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(3, 2, 3)).above(16);
        var at = new BlockPos((origin.getX() & ~15) + 8, (origin.getY() & ~15) + 8, (origin.getZ() & ~15) + 8);
        var chunk = level.getChunkAt(at);
        var section = chunk.getSection(chunk.getSectionIndex(at.getY()));
        var previousBiomes = section.getBiomes();
        var forest = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TTBiomeKeys.MAGICAL_FOREST);
        var sampler = level.getChunkSource().randomState().sampler();
        int x = QuartPos.fromBlock(at.getX() & ~15);
        int y = QuartPos.fromBlock(at.getY() & ~15);
        int z = QuartPos.fromBlock(at.getZ() & ~15);
        var aura = AuraManager.chunkAt(level, at);
        float oldFlux = aura.getFlux();
        float oldVis = aura.getVis();
        short oldBase = aura.getBase();
        var previousBlock = level.getBlockState(at);
        try {
            section.fillBiomesFromNoise((qx, qy, qz, climate) -> forest, sampler, x, y, z);
            helper.assertTrue(level.getBiome(at).is(TTBiomeKeys.MAGICAL_FOREST), "The test biome must be Magical Forest");
            aura.setBase((short) 100);
            aura.setFlux(0);
            aura.setVis(100);
            level.setBlockAndUpdate(at, TTBlocks.NODE.get().defaultBlockState());
            var node = (BlockEntityNode) level.getBlockEntity(at);
            var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 5));
            node.applyNodeData(new NodeData(NodeType.NORMAL, Optional.of(NodeModifier.PALE), aspects, aspects));
            for (int tick = 0; tick < 899; tick++)
                node.serverTick(level, at);
            level.getRandom().setSeed(18); // Its first draw from 50 is zero.
            node.serverTick(level, at);
            helper.assertTrue(node.trait() == null, "Clean Magical Forest aura should improve a pale node");
        } finally {
            level.setBlockAndUpdate(at, previousBlock);
            section.fillBiomesFromNoise((qx, qy, qz, climate) -> previousBiomes.get(qx - x, qy - y, qz - z), sampler, x, y, z);
            aura.setBase(oldBase);
            aura.setFlux(oldFlux);
            aura.setVis(oldVis);
        }
        helper.succeed();
    }
}
