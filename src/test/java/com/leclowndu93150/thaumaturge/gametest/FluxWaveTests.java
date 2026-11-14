package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanBindings;
import com.leclowndu93150.thaumaturge.content.taint.EtherealBloom;
import com.leclowndu93150.thaumaturge.content.taint.FluxRift;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFluxEcology;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFluxIndex;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class FluxWaveTests {
    private FluxWaveTests() {}

    @GameTest(template = "essentia_lab")
    public static void crucibleOverflowLeaksAndBreakPolluteSealedArea(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 2, 3);
        BlockPos at = helper.absolutePos(pos);
        for (BlockPos around : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            helper.setBlock(around, Blocks.STONE);
        }
        helper.setBlock(pos, TTBlocks.CRUCIBLE.get());
        var crucible = (CrucibleBlockEntity) helper.getBlockEntity(pos);
        crucible.setAspects(AspectList.EMPTY.add(new AspectInstance(TTAspects.IGNIS, 101)));
        float before = AuraHelper.getFlux(level, at);

        for (int tick = 0; tick < 5; tick++) CrucibleBlockEntity.serverTick(level, at, crucible.getBlockState(), crucible);
        helper.assertTrue(crucible.aspects().amountOf(TTAspects.IGNIS) == 100, "Overflow did not shed one aspect");
        helper.assertTrue(AuraHelper.getFlux(level, at) == before + 1, "Sealed overflow did not add one flux");

        var saved = crucible.saveWithoutMetadata();
        var replica = new CrucibleBlockEntity(at, crucible.getBlockState());
        replica.load(saved);
        helper.assertTrue(replica.saveWithoutMetadata().getInt("LeakCounter") == saved.getInt("LeakCounter"), "Leak timer did not survive reload");
        for (int tick = 5; tick < 200; tick++) CrucibleBlockEntity.serverTick(level, at, crucible.getBlockState(), crucible);
        helper.assertTrue(crucible.aspects().amountOf(TTAspects.IGNIS) == 99, "Idle leak did not shed one aspect");
        helper.assertTrue(AuraHelper.getFlux(level, at) == before + 1.25F, "Sealed idle leak did not add a quarter flux");

        helper.setBlock(pos, Blocks.AIR);
        helper.assertTrue(AuraHelper.getFlux(level, at) == before + 100.25F, "Breaking lost the remaining aspects");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void bloomRecipeConsumesExactAspectsAndWater(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, TTBlocks.CRUCIBLE.get());
        var crucible = (CrucibleBlockEntity) helper.getBlockEntity(pos);
        var recipe = (CrucibleRecipe) level.getRecipeManager().byKey(TTIds.rl("crucible/ethereal_bloom")).orElseThrow();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "tt_bloom_recipe"));
        var catalyst = new ItemStack(TTBulkContent.BLOCK_ITEMS.get("shimmerleaf").get(), 2);
        helper.assertTrue(recipe.matchesCatalyst(catalyst) && !recipe.matchesCatalyst(new ItemStack(Items.POPPY)),
                "Bloom recipe does not require shimmerleaf");
        helper.assertTrue(recipe.aspects().amountOf(TTAspects.LUX) == 8 && recipe.aspects().amountOf(TTAspects.HERBA) == 16
                && recipe.aspects().amountOf(TTAspects.VICTUS) == 16 && recipe.aspects().amountOf(TTAspects.VITIUM) == 16,
                "Bloom costs differ from CE");
        helper.assertTrue(!recipe.researchAllows(player), "Bloom recipe was not gated");
        TTAttachments.knowledge(player).complete(TTIds.rl("ethereal_bloom"));
        crucible.setAspects(recipe.aspects().add(new AspectInstance(TTAspects.IGNIS, 3)));
        crucible.setHeat(200);
        crucible.waterTank().fill(new FluidStack(Fluids.WATER, 49), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(!crucible.tryInsertFromPlayer(player, catalyst) && catalyst.getCount() == 2,
                "Insufficient water consumed the catalyst");
        crucible.waterTank().fill(new FluidStack(Fluids.WATER, 51), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(crucible.tryInsertFromPlayer(player, catalyst) && catalyst.getCount() == 1,
                "Valid recipe did not consume exactly one shimmerleaf");
        helper.assertTrue(crucible.waterTank().getFluidAmount() == 50 && crucible.aspects().entries().size() == 1
                && crucible.aspects().amountOf(TTAspects.IGNIS) == 3, "Recipe consumed the wrong water or aspects");
        var result = TTBulkContent.BLOCK_ITEMS.get("ethereal_bloom").get();
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1),
                item -> item.getItem().is(result)).size() == 1, "No bloom crafted");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void finiteFluxMovesVerticallyAndMaintainsAuraFloor(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos goo = new BlockPos(2, 2, 2);
        BlockPos gas = new BlockPos(4, 1, 2);
        helper.setBlock(goo.below(2), Blocks.STONE);
        helper.setBlock(gas.above(2), Blocks.STONE);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(goo.below().relative(direction), Blocks.STONE);
            helper.setBlock(gas.above().relative(direction), Blocks.STONE);
        }
        var gooPos = helper.absolutePos(goo);
        var gasPos = helper.absolutePos(gas);
        helper.assertTrue(PhysicalFlux.placeGoo(level, gooPos, 5), "Could not place goo");
        helper.assertTrue(PhysicalFlux.placeGas(level, gasPos, 4), "Could not place gas");
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(PhysicalFlux.amount(level, gooPos) == 0 && PhysicalFlux.amount(level, gooPos.below()) == 5,
                    "Goo lost volume or did not flow down");
            helper.assertTrue(PhysicalFlux.amount(level, gasPos) == 0 && PhysicalFlux.amount(level, gasPos.above()) == 4,
                    "Gas lost volume or did not rise");
            var chunk = level.getChunkAt(gooPos);
            PhysicalFluxIndex.rebuild(chunk);
            var cells = PhysicalFluxIndex.positions(chunk);
            helper.assertTrue(cells.contains(gooPos.below()) && !cells.contains(gooPos), "Chunk load did not index active goo");
            helper.assertTrue(PhysicalFluxEcology.auraFloor(chunk, java.util.List.of(gooPos.below()), 100) == 2.5F,
                    "Goo did not sustain the expected aura floor");
            helper.succeed();
        });
    }

    @GameTest(template = "essentia_lab")
    public static void bloomProtectionIsBoundedAndEndsOnRemoval(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 1, 3);
        BlockPos at = helper.absolutePos(pos);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, TTBulkContent.BLOCKS.get("ethereal_bloom").get());
        helper.assertTrue(EtherealBloom.protects(level, at.east(8)), "Bloom failed to protect the eighth block");
        helper.assertTrue(!EtherealBloom.protects(level, at.east(9)), "Bloom protection reached beyond eight blocks");
        helper.assertTrue(!PhysicalFluxEcology.plantSeed(level, at.east()), "A seed took root next to a bloom");
        BlockPos nodePos = at.east(2).above();
        var pool = AspectList.EMPTY.add(new AspectInstance(TTAspects.AER, 2));
        helper.assertTrue(NodeGenerator.createNodeAt(level, nodePos,
                new NodeData(NodeType.TAINTED, java.util.Optional.empty(), AspectList.EMPTY, pool)), "Tainted node did not place");
        var node = (BlockEntityNode) level.getBlockEntity(nodePos);
        var saved = node.saveWithoutMetadata();
        saved.putInt("TaintPurifyTicks", ThaumaturgeCommonConfig.TAINT_STARVE_TICKS.get() - 20);
        node.load(saved);
        for (int tick = 0; tick < 19; tick++) node.serverTick(level, nodePos);
        helper.assertTrue(node.kind() == NodeType.TAINTED && node.getAspects().isEmpty(), "Protected node still fed on flux");
        node.serverTick(level, nodePos);
        helper.assertTrue(node.kind() == NodeType.NORMAL && node.capacity().entries().equals(pool.entries()), "Bloom did not purify the node in place");
        helper.setBlock(pos, Blocks.AIR);
        helper.assertTrue(!EtherealBloom.protects(level, at), "Removed bloom still protects its position");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void riftScanningAndCollapseSurviveReload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "tt_rift_scan"));
        FluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        rift.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        level.addFreshEntity(rift);
        ScanBindings.scanEntity(player, rift);
        helper.assertTrue(TTAttachments.knowledge(player).isKnown(TTIds.rl("scanned/fluxrift")), "Rift scan did not unlock the book gate");
        rift.beginCollapse();
        var saved = rift.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        rift.discard();
        FluxRift restored = TTEntities.FLUX_RIFT.get().create(level);
        restored.load(saved);
        level.addFreshEntity(restored);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(restored.isRemoved(), "Rift lost its collapse state on reload");
            var voidSeed = TTBulkItems.ITEMS.get("void_seed").get();
            int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3), item -> item.getItem().is(voidSeed))
                    .stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(count == 2, "Size five rift should produce two void seeds, produced " + count);
            helper.succeed();
        });
    }
}
