package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityArcaneEar;
import com.leclowndu93150.thaumaturge.content.device.levitator.BlockEntityLevitator;
import com.leclowndu93150.thaumaturge.content.device.levitator.BlockLevitator;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(TTIds.MODID)
public final class ArtificeUtilityTests {
    private ArtificeUtilityTests() {}

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void levitatorLiftsEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TTBlocks.LEVITATOR.get().defaultBlockState()
                .setValue(BlockLevitator.FACING, Direction.UP)
                .setValue(BlockStateProperties.ENABLED, true));
        if (!(helper.getBlockEntity(pos) instanceof BlockEntityLevitator levitator)) {
            helper.fail("Missing levitator BE");
            return;
        }
        var tag = levitator.saveWithoutMetadata();
        tag.putInt("charge", 10_000);
        levitator.load(tag);
        ItemEntity item = helper.spawnItem(Items.IRON_NUGGET, pos.above());
        double startY = item.getY();
        helper.runAfterDelay(10, () -> {
            if (!item.isAlive()) {
                helper.fail("Item despawned");
                return;
            }
            if (item.getY() <= startY + 0.05 && item.getDeltaMovement().y <= 0.01) {
                helper.fail("Levitator did not lift item (y=" + item.getY() + " start=" + startY + " vy=" + item.getDeltaMovement().y + ")");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void arcaneEarPulseOnNote(GameTestHelper helper) {
        BlockPos base = new BlockPos(1, 1, 1);
        BlockPos earPos = base.above();
        helper.setBlock(base, Blocks.OAK_PLANKS);
        helper.setBlock(earPos, TTBlocks.ARCANE_EAR.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP)
                .setValue(BlockStateProperties.ENABLED, false));
        if (!(helper.getBlockEntity(earPos) instanceof BlockEntityArcaneEar ear)) {
            helper.fail("Missing arcane ear BE");
            return;
        }
        ear.updateTone();
        ear.trigger();
        BlockState pulsed = helper.getBlockState(earPos);
        if (!pulsed.getValue(BlockStateProperties.ENABLED)) {
            helper.fail("Ear did not enable on trigger");
            return;
        }
        helper.runAfterDelay(12, () -> {
            if (helper.getBlockState(earPos).getValue(BlockStateProperties.ENABLED)) {
                helper.fail("Ear stayed powered after pulse window");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hellAndBackInNether(GameTestHelper helper) {
        ServerPlayer player = player(helper, "tt_hell");
        ResourceLocation research = TTIds.rl("m_hellandback");
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (knowledge.isComplete(research)) {
            helper.fail("Fresh player already has m_hellandback");
            return;
        }
        boolean progressed = ResearchManager.progressResearch(player, research);
        if (!progressed && !TTAttachments.knowledge(player).isComplete(research)) {
            helper.fail("progressResearch did not complete m_hellandback");
            return;
        }
        helper.assertTrue(TTAttachments.knowledge(player).isComplete(research), "m_hellandback should be complete");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hungryChestBerRegistered(GameTestHelper helper) {
        helper.assertTrue(TTBlockEntities.HUNGRY_CHEST.get() != null, "HUNGRY_CHEST BE type missing");
        helper.setBlock(new BlockPos(1, 1, 1), TTBlocks.HUNGRY_CHEST.get());
        if (helper.getBlockEntity(new BlockPos(1, 1, 1)) == null
                || helper.getBlockEntity(new BlockPos(1, 1, 1)).getType() != TTBlockEntities.HUNGRY_CHEST.get()) {
            helper.fail("Hungry chest BE type mismatch");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void unlockArtificeHasBaseSibling(GameTestHelper helper) {
        var entry = com.leclowndu93150.thaumaturge.content.research.ResearchData.entry(TTIds.rl("unlock_artifice"));
        if (entry == null) {
            helper.fail("unlock_artifice missing");
            return;
        }
        boolean hasSibling = entry.siblings().stream().anyMatch(id -> id.equals(TTIds.rl("base_artifice")));
        helper.assertTrue(hasSibling, "unlock_artifice must sibling base_artifice");
        var category = com.leclowndu93150.thaumaturge.content.research.ResearchData.category(TTIds.rl("artifice"));
        if (category == null || category.requiredResearch().isEmpty()
                || !category.requiredResearch().get().equals(TTIds.rl("unlock_artifice"))) {
            helper.fail("artifice category must gate on unlock_artifice");
            return;
        }
        helper.succeed();
    }
}
