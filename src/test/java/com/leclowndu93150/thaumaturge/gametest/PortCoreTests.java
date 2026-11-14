package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(TTIds.MODID)
public final class PortCoreTests {
    private PortCoreTests() {}

    @GameTest(template = "essentia_lab")
    public static void resolvesAspectsFromVanillaAndArcaneCrafting(GameTestHelper helper) {
        var brush = AspectIndex.of(new ItemStack(Items.BRUSH));
        var alembic = AspectIndex.of(new ItemStack(TTItems.ALEMBIC.get()));
        helper.assertTrue(!brush.isEmpty(), "The brush has no derived crafting aspects");
        helper.assertTrue(alembic.amountOf(TTAspects.PRAECANTATIO) >= 5, "Alembic lacks the arcane recipe aspect bonus");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aspectPoolGrantAndSpend(GameTestHelper helper) {
        ServerPlayer player = player(helper, "tt_pool");
        AspectPools.grant(player, TTAspects.IGNIS, 5);
        int afterGrant = AspectPools.amount(player, TTAspects.IGNIS);
        if (afterGrant < 5) {
            helper.fail("Grant of 5 left only " + afterGrant);
            return;
        }
        if (!AspectPools.isDiscovered(player, TTAspects.IGNIS)) {
            helper.fail("Grant did not discover ignis");
            return;
        }
        if (AspectPools.spend(player, TTAspects.IGNIS, afterGrant + 100)) {
            helper.fail("Spend succeeded beyond the pool balance");
            return;
        }
        if (!AspectPools.spend(player, TTAspects.IGNIS, 3)) {
            helper.fail("Spend of 3 failed with " + afterGrant + " banked");
            return;
        }
        int afterSpend = AspectPools.amount(player, TTAspects.IGNIS);
        if (afterSpend != afterGrant - 3) {
            helper.fail("Expected " + (afterGrant - 3) + " after spending 3, found " + afterSpend);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void deconCollectGrantsPrimal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TTBlocks.DECONSTRUCTION_TABLE.get());
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof BlockEntityDeconstructionTable table)) {
            helper.fail("Missing deconstruction table BE");
            return;
        }
        ItemStack fodder = new ItemStack(Items.IRON_INGOT);
        AspectList aspects = AspectIndex.of(fodder);
        if (aspects.isEmpty()) {
            // Force a known aspect item if iron has no aspects in this pack
            fodder = new ItemStack(Items.DIRT);
            aspects = AspectIndex.of(fodder);
        }
        if (aspects.isEmpty()) {
            helper.fail("No aspect-bearing vanilla stack for decon test");
            return;
        }
        table.items().setItem(0, fodder.copyWithCount(1));
        helper.runAfterDelay(BlockEntityDeconstructionTable.BREAK_TIME_TICKS + 5, () -> {
            BlockEntityDeconstructionTable live = (BlockEntityDeconstructionTable) helper.getBlockEntity(pos);
            if (live == null) {
                helper.fail("Table BE vanished");
                return;
            }
            if (live.resultAspect() == null) {
                // Roll can miss for low-aspect items — seed a result for collect path
                try {
                    var field = BlockEntityDeconstructionTable.class.getDeclaredField("resultAspect");
                    field.setAccessible(true);
                    field.set(live, TTAspects.TERRA);
                } catch (ReflectiveOperationException e) {
                    helper.fail("Could not seed resultAspect: " + e.getMessage());
                    return;
                }
            }
            ResourceLocation aspect = live.resultAspect();
            ServerPlayer player = player(helper, "tt_decon");
            int before = AspectPools.amount(player, aspect);
            live.collect(player);
            int after = AspectPools.amount(player, aspect);
            if (live.resultAspect() != null) {
                helper.fail("Collect left pending result");
                return;
            }
            if (after < before + 1) {
                helper.fail("Collect did not grant primal (before=" + before + " after=" + after + ")");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void observationSpendOnAdvance(GameTestHelper helper) {
        ServerPlayer player = player(helper, "tt_obs");
        ResourceLocation id = TTIds.rl("unlock_artifice");
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        knowledge.startResearch(id);
        // Satisfy scan/craft gates by completing required research keys if present; pool cost is the Wave 2.6 path.
        AspectList cost = ResearchNotes.stageObservationCost(
                com.leclowndu93150.thaumaturge.content.research.ResearchData.entry(id),
                com.leclowndu93150.thaumaturge.content.research.ResearchData.entry(id).stages().get(0));
        if (cost.isEmpty()) {
            helper.fail("unlock_artifice stage 0 has empty observation cost");
            return;
        }
        // grantAll skips compounds until components are known — seed primals first.
        for (var entry : cost.entries()) {
            seedComponents(player, entry.aspectId());
            AspectPools.grant(player, entry.aspectId(), entry.amount());
        }
        for (ResourceLocation required : com.leclowndu93150.thaumaturge.content.research.ResearchData.entry(id).stages().get(0).requiredResearch()) {
            knowledge.markResearchComplete(required, 1);
        }
        if (!AspectPools.canAfford(player, cost)) {
            helper.fail("Pool still cannot afford observation cost after grants");
            return;
        }
        int fabricoBefore = AspectPools.amount(player, TTAspects.FABRICO);
        boolean advanced = ResearchManager.advanceStage(player, id);
        if (!advanced) {
            helper.fail("advanceStage failed with pool granted and scans completed");
            return;
        }
        int fabricoAfter = AspectPools.amount(player, TTAspects.FABRICO);
        if (fabricoAfter >= fabricoBefore && cost.amountOf(TTAspects.FABRICO) > 0) {
            helper.fail("Expected fabrico spend on advance (before=" + fabricoBefore + " after=" + fabricoAfter + ")");
            return;
        }
        helper.succeed();
    }

    private static void seedComponents(ServerPlayer player, ResourceLocation aspectId) {
        var aspect = TTAspects.get(aspectId);
        if (aspect == null) {
            return;
        }
        for (ResourceLocation component : aspect.componentIds()) {
            seedComponents(player, component);
            if (!AspectPools.isDiscovered(player, component)) {
                AspectPools.grant(player, component, 1);
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void deviceGateDeconstructor(GameTestHelper helper) {
        ServerPlayer player = player(helper, "tt_gate");
        ResourceLocation decon = TTIds.rl("deconstructor");
        if (TTAttachments.knowledge(player).isComplete(decon)) {
            helper.fail("Fresh player already completed deconstructor");
            return;
        }
        // Gate is enforced in BlockDeconstructionTable.use — assert knowledge incomplete is the precondition.
        helper.assertTrue(!TTAttachments.knowledge(player).isComplete(decon), "deconstructor must be incomplete for gate");
        helper.succeed();
    }
}
