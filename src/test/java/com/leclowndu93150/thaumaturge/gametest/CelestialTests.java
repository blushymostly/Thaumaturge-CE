package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanKeys;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class CelestialTests {
    private CelestialTests() {}

    @GameTest(template = "essentia_lab")
    public static void celestialNotesRequireMaterialsAndOneObservationPerDay(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "tt_sky"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX() + 0.5, level.getMaxBuildHeight() - 2, pos.getZ() + 0.5);
        player.setYRot(180);
        player.setXRot(-35);
        long previousTime = level.getDayTime();
        var knowledge = TTAttachments.knowledge(player);
        var meter = TTItems.THAUMOMETER.get();
        var meterStack = new ItemStack(meter);
        player.setItemInHand(InteractionHand.MAIN_HAND, meterStack);
        try {
            level.setDayTime(18000);
            var paper = new ItemStack(Items.PAPER, 3);
            player.getInventory().setItem(1, paper);
            var northStars = TTBulkItems.ITEMS.get("celestial_notes_stars_1").get();
            long today = level.getGameTime() / 24000L;
            var old = ScanKeys.celestial(today - 1, "star0");
            knowledge.complete(old);

            helper.assertTrue(meter.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS, "Sky is scannable before Aura Nodes research");
            knowledge.complete(TTIds.rl("nodes"));
            helper.assertTrue(meter.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME,
                    "Exposed north sky is not scannable after Aura Nodes research (" + level.dimension().location() + ", sky=" + level.canSeeSky(player.blockPosition().above()) + ", target="
                            + com.leclowndu93150.thaumaturge.content.item.ThaumometerItem.resolveTarget(level, player) + ")");
            meter.releaseUsing(meterStack, level, player, 5);
            helper.assertTrue(paper.getCount() == 3 && player.getInventory().countItem(northStars) == 0, "Survey spent paper without scribing tools");

            player.getInventory().setItem(2, new ItemStack(TTItems.SCRIBING_TOOLS.get()));
            meter.releaseUsing(meterStack, level, player, 5);
            helper.assertTrue(paper.getCount() == 2 && player.getInventory().countItem(northStars) == 1, "Northern quadrant did not produce the matching note");
            helper.assertTrue(!knowledge.isKnown(old) && knowledge.isComplete(ScanKeys.celestial(today, "star0")), "The celestial ledger did not preserve only current-day observations");
            meter.releaseUsing(meterStack, level, player, 5);
            helper.assertTrue(paper.getCount() == 2 && player.getInventory().countItem(northStars) == 1, "The same observation consumed a second sheet");

            player.setYRot(-90);
            player.setXRot(-90);
            meter.releaseUsing(meterStack, level, player, 5);
            var fullMoon = TTBulkItems.ITEMS.get("celestial_notes_moon_1").get();
            helper.assertTrue(paper.getCount() == 1 && player.getInventory().countItem(fullMoon) == 1, "An aligned look at midnight did not produce the full-moon note");

            level.setDayTime(6000);
            player.setYRot(-90);
            meter.releaseUsing(meterStack, level, player, 5);
            var solar = TTBulkItems.ITEMS.get("celestial_notes_sun").get();
            helper.assertTrue(paper.isEmpty() && player.getInventory().countItem(solar) == 1, "An aligned look at noon did not produce the solar note");

            knowledge.discoverAspect(TTAspects.LUX);
            knowledge.discoverAspect(TTAspects.VACUOS);
            knowledge.discoverAspect(TTAspects.ALIENIS);
            var note = new ItemStack(northStars);
            player.setItemInHand(InteractionHand.MAIN_HAND, note);
            northStars.use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(note.isEmpty() && knowledge.aspectAmount(TTAspects.LUX) == 2 && knowledge.aspectAmount(TTAspects.VACUOS) == 2 && knowledge.aspectAmount(TTAspects.ALIENIS) == 2,
                    "Studying the stellar note did not consume it and award the three CE aspects");
        } finally {
            level.setDayTime(previousTime);
        }
        helper.succeed();
    }
}
