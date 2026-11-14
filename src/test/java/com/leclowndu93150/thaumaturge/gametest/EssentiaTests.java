package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlockEntity;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchData;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class EssentiaTests {
    private EssentiaTests() {}

    @GameTest(template = "essentia_lab", timeoutTicks = 520)
    public static void smelterDistillsThroughTubes(GameTestHelper helper) {
        BlockPos base = new BlockPos(1, 1, 1);
        BlockPos alembic = base.above();
        BlockPos tube = alembic.east();
        BlockPos outlet = tube.east();
        BlockPos jar = outlet.below();
        helper.setBlock(base, TTBlocks.SMELTER_BASIC.get());
        helper.setBlock(alembic, TTBlocks.ALEMBIC.get());
        helper.setBlock(tube, TTBlocks.TUBE.get());
        helper.setBlock(outlet, TTBlocks.TUBE.get());
        helper.setBlock(jar, TTBlocks.JAR_NORMAL.get());

        SmelterBlockEntity smelter = (SmelterBlockEntity) helper.getBlockEntity(base);
        smelter.setItem(0, EssentiaCrystalFactory.of(TTAspects.AER, 32));
        smelter.setItem(1, new ItemStack(Items.COAL));
        var tank = ((EssentiaJarBlockEntity) helper.getBlockEntity(jar)).tank();

        helper.succeedWhen(() -> {
            helper.assertTrue(smelter.getItem(0).isEmpty() && smelter.getItem(1).isEmpty(), "Smelter did not consume its input and fuel");
            helper.assertTrue(tank.stored() >= 10 && tank.stored() <= 32 && TTAspects.AER.equals(tank.heldAspect()),
                    "The jar did not receive a phial's worth of Aer through both tubes");
        });
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 80)
    public static void tubeAndJarPreserveContentsOnReload(GameTestHelper helper) {
        BlockPos jarPos = new BlockPos(1, 1, 1);
        BlockPos tubePos = jarPos.above();
        helper.setBlock(jarPos, TTBlocks.JAR_NORMAL.get());
        helper.setBlock(tubePos, TTBlocks.TUBE.get());
        var jar = (EssentiaJarBlockEntity) helper.getBlockEntity(jarPos);
        var tube = (TubeBlockEntity) helper.getBlockEntity(tubePos);
        jar.tank().insert(TTAspects.AER, EssentiaJarBlockEntity.CAPACITY, false);
        tube.insert(TTAspects.IGNIS, 1, false);
        CompoundTag jarTag = jar.saveWithoutMetadata();
        CompoundTag tubeTag = tube.saveWithoutMetadata();

        helper.setBlock(jarPos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(tubePos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(jarPos, TTBlocks.JAR_NORMAL.get());
        helper.setBlock(tubePos, TTBlocks.TUBE.get());
        var restoredJar = (EssentiaJarBlockEntity) helper.getBlockEntity(jarPos);
        var restoredTube = (TubeBlockEntity) helper.getBlockEntity(tubePos);
        restoredJar.load(jarTag);
        restoredTube.load(tubeTag);

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(restoredJar.tank().stored() == EssentiaJarBlockEntity.CAPACITY && restoredTube.cellAmount() == 1,
                    "Full jar or buffered tube lost contents");
            restoredJar.tank().extract(TTAspects.AER, 10, false);
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(restoredJar.tank().stored() == EssentiaJarBlockEntity.CAPACITY - 10 && restoredTube.cellAmount() == 1,
                    "A partly filled jar accepted another aspect");
            restoredJar.tank().extract(TTAspects.AER, EssentiaJarBlockEntity.CAPACITY, false);
        });
        helper.runAfterDelay(48, () -> {
            helper.assertTrue(restoredJar.tank().getEssentia().amountOf(TTAspects.IGNIS) == 1 && restoredTube.cellAmount() == 0,
                    "Empty jar did not resume pulling after reload");
            helper.succeed();
        });
    }

    @GameTest(template = "essentia_lab")
    public static void phialRoundTripAndInfusionRequirement(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TTBlocks.JAR_NORMAL.get());
        var tank = ((EssentiaJarBlockEntity) helper.getBlockEntity(pos)).tank();
        tank.insert(TTAspects.AER, 20, false);
        ServerPlayer player = player(helper, "tt_phial");
        player.getInventory().clearContent();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TTItems.PHIAL.get()));
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);

        var infusion = ResearchData.entry(TTIds.rl("infusion"));
        var requirement = infusion.stages().get(0).obtain().get(2);
        helper.assertTrue(!requirement.matches(player.getMainHandItem()), "Empty phial passed the requirement");
        var wrong = new ItemStack(TTItems.PHIAL.get());
        TTItemData.setAspects(wrong, AspectList.ofEntries(java.util.List.of(new AspectInstance(TTAspects.IGNIS, 10))));
        helper.assertTrue(!requirement.matches(wrong), "Wrong aspect passed the requirement");

        helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction(), "Could not fill the phial");
        helper.assertTrue(requirement.matches(player.getMainHandItem()) && tank.stored() == 10, "Fill did not transfer exactly 10 Aer");
        var knowledge = TTAttachments.knowledge(player);
        knowledge.complete(ResearchNoteData.learnKey(TTIds.rl("infusion"), 0));
        for (AspectInstance cost : infusion.noteAspects().entries()) {
            AspectPools.grant(player, cost.aspectId(), cost.amount());
        }
        player.getInventory().add(new ItemStack(Items.STONE));
        player.getInventory().add(new ItemStack(Items.FEATHER));
        helper.assertTrue(ResearchManager.stageRequirementsMet(player, knowledge, TTIds.rl("infusion"), infusion, 0),
                "Server rejected the required Aer phial");

        helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction(),
                "Could not pour the phial back into the jar");
        helper.assertTrue(tank.stored() == 20 && TTItemData.getAspects(player.getMainHandItem()).isEmpty(), "Round trip lost essentia");
        helper.assertTrue(!ResearchManager.stageRequirementsMet(player, knowledge, TTIds.rl("infusion"), infusion, 0),
                "Server accepted an empty phial");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void essentiaRecipesMatchCeCostsAndProgression(GameTestHelper helper) {
        ArcaneCraftingRecipe smelter = arcane(helper, "smelter_basic");
        ArcaneCraftingRecipe alembic = arcane(helper, "alembic");
        ArcaneCraftingRecipe tube = arcane(helper, "tube");
        ArcaneCraftingRecipe jar = arcane(helper, "jar_normal");
        ArcaneCraftingRecipe filter = arcane(helper, "filter");
        ArcaneCraftingRecipe thaumometer = arcane(helper, "thaumometer");
        helper.assertTrue(smelter.visCost() == 50 && smelter.crystals().amountOf(TTAspects.IGNIS) == 1, "Wrong smelter cost");
        helper.assertTrue(alembic.visCost() == 50 && alembic.crystals().amountOf(TTAspects.AQUA) == 1, "Wrong alembic cost");
        helper.assertTrue(filter.visCost() == 15 && filter.crystals().amountOf(TTAspects.AQUA) == 1, "Wrong filter cost");
        helper.assertTrue(jar.visCost() == 5 && jar.crystals().isEmpty(), "Wrong jar cost");
        helper.assertTrue(tube.visCost() == 10 && tube.crystals().isEmpty()
                && tube.getResultItem(helper.getLevel().registryAccess()).getCount() == 8, "Wrong tube cost or output");
        helper.assertTrue(thaumometer.visCost() == 20 && thaumometer.crystals().size() == 6, "Wrong thaumometer cost");
        helper.assertTrue(tube.getIngredients().get(1).test(new ItemStack(TTItems.NUGGET_QUICKSILVER.get()))
                && tube.getIngredients().get(3).test(new ItemStack(TTItems.PLATE_IRON.get()))
                && tube.getIngredients().get(4).test(new ItemStack(Items.GLASS))
                && tube.getIngredients().get(7).test(new ItemStack(TTItems.NUGGET_BRASS.get())),
                "Wrong tube pattern or ingredients");
        helper.assertTrue(alembic.getIngredients().get(0).test(new ItemStack(TTItems.PLANK_GREATWOOD.get()))
                && alembic.getIngredients().get(1).test(new ItemStack(TTItems.FILTER.get()))
                && alembic.getIngredients().get(4).test(new ItemStack(Items.BUCKET)), "Wrong alembic pattern");

        var knowledge = new PlayerKnowledge();
        var smelterResearch = TTIds.rl("essentia_smelter");
        knowledge.startResearch(smelterResearch);
        helper.assertTrue(!ResearchGate.isOpen(knowledge, smelter.research()), "Smelter recipe unlocked too early");
        knowledge.setStage(smelterResearch, 2);
        helper.assertTrue(ResearchGate.isOpen(knowledge, smelter.research()), "Smelter recipe deadlocks its craft requirement");
        helper.assertTrue(!ResearchGate.isOpen(knowledge, alembic.research()), "Alembic unlocked before completion");
        knowledge.markResearchComplete(smelterResearch, 4);
        helper.assertTrue(ResearchGate.isOpen(knowledge, alembic.research()), "Completed smelter research did not unlock the alembic");

        var manager = helper.getLevel().getRecipeManager();
        var oreRecipe = (AbstractCookingRecipe) manager.byKey(TTIds.rl("quicksilver_from_ore_smelting")).orElseThrow();
        helper.assertTrue(oreRecipe.getIngredients().get(0).test(new ItemStack(TTItems.DEEPSLATE_ORE_CINNABAR.get()))
                && oreRecipe.getResultItem(helper.getLevel().registryAccess()).is(TTItems.QUICKSILVER.get()),
                "Cinnabar cannot be smelted to quicksilver");
        var loot = helper.getLevel().getServer().getLootData();
        helper.assertTrue(loot.getLootTable(TTIds.rl("blocks/leaves_greatwood")) != LootTable.EMPTY
                && loot.getLootTable(TTIds.rl("blocks/leaves_silverwood")) != LootTable.EMPTY,
                "Tree drops failed to load");

        var stages = ResearchData.entry(smelterResearch).stages();
        ServerPlayer player = player(helper, "tt_recipe_gate");
        var playerKnowledge = TTAttachments.knowledge(player);
        var quartz = new ItemStack(TTItems.NUGGET_QUARTZ.get());
        for (var requirement : stages.get(1).obtain()) {
            var aspect = requirement.components().crystal().orElseThrow();
            var crystal = (CrucibleRecipe) manager.byKey(TTIds.rl("crucible/vis_crystal/" + aspect.getPath())).orElseThrow();
            helper.assertTrue(!crystal.researchAllows(player) && crystal.matchesCatalyst(quartz)
                    && crystal.aspects().amountOf(aspect) == 2
                    && requirement.matches(crystal.assemble(null, helper.getLevel().registryAccess())),
                    "Wrong compound crystal recipe for " + aspect);
        }
        playerKnowledge.complete(TTIds.rl("base_alchemy"));
        var example = (CrucibleRecipe) manager.byKey(TTIds.rl("crucible/vis_crystal/vacuos")).orElseThrow();
        helper.assertTrue(example.researchAllows(player) && !example.researchAllows(null), "Crystal research gate is not enforced");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void fillsPhialFromSideOfAlembic(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TTBlocks.ALEMBIC.get());
        BlockPos absolute = helper.absolutePos(pos);
        var level = helper.getLevel();
        var alembic = helper.getBlockEntity(pos);
        var tank = new com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank(128);
        tank.insert(TTAspects.AQUA, 12, false);
        CompoundTag state = new CompoundTag();
        state.put("Tank", tank.save());
        alembic.load(state);
        var copy = new com.leclowndu93150.thaumaturge.content.essentia.smeltery.AlembicBlockEntity(absolute, alembic.getBlockState());
        copy.handleUpdateTag(alembic.getUpdateTag());
        helper.assertTrue(copy.getCapability(com.leclowndu93150.thaumaturge.registry.TTCapabilities.ESSENTIA)
                .orElseThrow(IllegalStateException::new).getEssentia().amountOf(TTAspects.AQUA) == 12,
                "Alembic did not publish its aspect to the client");

        ServerPlayer player = player(helper, "tt_alembic_phial");
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TTItems.PHIAL.get()));
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.5, 0, 0), Direction.EAST, absolute, false);
        var hand = InteractionHand.OFF_HAND;
        helper.assertTrue(player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit).consumesAction(),
                "Could not draw from the side with an offhand phial");
        helper.assertTrue(TTItemData.getAspects(player.getItemInHand(hand)).amountOf(TTAspects.AQUA) == 10
                && EssentiaAccess.contentsOf(level, absolute).amountOf(TTAspects.AQUA) == 2,
                "Phial or alembic contains the wrong amount after withdrawal");
        player.setItemInHand(hand, new ItemStack(TTItems.PHIAL.get()));
        helper.assertTrue(!player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit).consumesAction()
                && EssentiaAccess.contentsOf(level, absolute).amountOf(TTAspects.AQUA) == 2,
                "A short draw consumed the remaining essentia");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void metalPurificationUnlocksAndDoublesIron(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var entryId = TTIds.rl("metal_purification");
        var entry = ResearchData.entry(entryId);
        var id = TTIds.rl("crucible/cluster_iron");
        helper.assertTrue(entry.stages().get(0).recipes().contains(id), "The book lost its iron cluster bookmark");
        var recipe = (CrucibleRecipe) manager.byKey(id).orElseThrow();
        var player = player(helper, "tt_metal_purification");
        helper.assertTrue(!recipe.researchAllows(player) && recipe.matchesCatalyst(new ItemStack(Items.RAW_IRON))
                && recipe.aspects().amountOf(TTAspects.METALLUM) == 5 && recipe.aspects().amountOf(TTAspects.ORDO) == 5,
                "Wrong gate, catalyst or aspect costs for iron clusters");
        TTAttachments.knowledge(player).markResearchComplete(entryId, entry.stages().size());
        helper.assertTrue(recipe.researchAllows(player), "Completed Metal Purification did not unlock the recipe");
        var result = recipe.getResultItem(level.registryAccess());
        var smelting = manager.byKey(TTIds.rl("iron_ingot_from_cluster")).orElseThrow();
        helper.assertTrue(smelting.getIngredients().get(0).test(result)
                && smelting.getResultItem(level.registryAccess()).is(Items.IRON_INGOT)
                && smelting.getResultItem(level.registryAccess()).getCount() == 2, "An iron cluster should smelt into two ingots");
        helper.succeed();
    }

    private static ArcaneCraftingRecipe arcane(GameTestHelper helper, String name) {
        var recipe = (ArcaneCraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(TTIds.rl("arcane_workbench/" + name)).orElseThrow();
        helper.assertTrue(recipe.getIngredients().stream().noneMatch(ingredient ->
                ingredient != net.minecraft.world.item.crafting.Ingredient.EMPTY && ingredient.getItems().length == 0),
                "Unresolved ingredient in " + recipe.getId());
        return recipe;
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
    }
}
