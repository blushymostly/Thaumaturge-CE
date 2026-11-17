package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.AuraManager;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.leclowndu93150.thaumaturge.content.device.EverfullUrnBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class UrnTests {
    private UrnTests() {}

    @GameTest(template = "essentia_lab")
    public static void urnGeneratesAWaterBucketAndFillsContainers(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(4, 2, 4);
        var pos = helper.absolutePos(origin);
        var block = TTBulkContent.BLOCKS.get("everfull_urn").get();
        helper.setBlock(origin, block);
        var urn = (EverfullUrnBlockEntity) helper.getBlockEntity(origin);
        var aura = AuraManager.chunkAt(level, pos);
        helper.runAfterDelay(52, () -> {
            var handler = FluidUtil.getFluidHandler(level, pos, Direction.UP).orElseThrow(AssertionError::new);
            helper.assertTrue(urn.water() == 1000 && handler.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.SIMULATE) == 0, "Urn did not generate a bucket or accepted lava");
            var player = helper.makeMockSurvivalPlayer();
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.BUCKET.getDefaultInstance());
            helper.assertTrue(
                    block.use(block.defaultBlockState(), level, pos, player, InteractionHand.MAIN_HAND, hit).consumesAction() && player.getMainHandItem().is(Items.WATER_BUCKET) && urn.water() == 0,
                    "Urn did not fill a bucket");
            handler.fill(new FluidStack(Fluids.WATER, 666), FluidAction.EXECUTE);
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.GLASS_BOTTLE.getDefaultInstance());
            helper.assertTrue(block.use(block.defaultBlockState(), level, pos, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                    && PotionUtils.getPotion(player.getMainHandItem()) == Potions.WATER && urn.water() == 333, "Urn did not spend 333 mB filling a bottle");
            float before = aura.getVis();
            var saved = urn.saveWithoutMetadata();
            helper.setBlock(origin, Blocks.AIR);
            helper.setBlock(origin, block);
            var restored = (EverfullUrnBlockEntity) helper.getBlockEntity(origin);
            restored.load(saved);
            helper.assertTrue(restored.water() == 333 && aura.getVis() == before, "Urn lost stored water or charged aura on load");
            helper.succeed();
        });
    }

    @GameTest(template = "essentia_lab")
    public static void urnDoesNotProduceWaterWithoutVis(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(4, 2, 4);
        var pos = helper.absolutePos(origin);
        helper.setBlock(origin, TTBulkContent.BLOCKS.get("everfull_urn").get());
        var urn = (EverfullUrnBlockEntity) helper.getBlockEntity(origin);
        var aura = AuraManager.chunkAt(level, pos);
        var tick = new Runnable() {
            @Override
            public void run() {
                var state = level.getBlockState(pos);
                var ticker = ((com.leclowndu93150.thaumaturge.content.device.EverfullUrnBlock) state.getBlock()).getTicker(level, state,
                        com.leclowndu93150.thaumaturge.registry.TTBlockEntities.EVERFULL_URN.get());
                for (int i = 0; i < 5; i++)
                    ticker.tick(level, pos, state, urn);
            }
        };
        float savedVis = aura.getVis();
        try {
            aura.setVis(0);
            tick.run();
            helper.assertTrue(urn.water() == 0, "Urn created water without aura");
            aura.setVis(1);
            for (int i = 0; i < 10; i++)
                tick.run();
            helper.assertTrue(urn.water() == 1000 && aura.getVis() < 0.0001F, "Urn did not exchange exactly one vis for one bucket");
        } finally {
            aura.setVis(savedVis);
        }
        helper.succeed();
    }

    @GameTest(template = "essentia_lab", timeoutTicks = 450)
    public static void urnFeedsCruciblesAndCauldrons(GameTestHelper helper) {
        var origin = new BlockPos(4, 2, 4);
        var crucible = origin.offset(0, -1, -1);
        var cauldron = origin.offset(1, -1, 0);
        helper.setBlock(origin, TTBulkContent.BLOCKS.get("everfull_urn").get());
        helper.setBlock(crucible, TTBlocks.CRUCIBLE.get());
        helper.setBlock(cauldron, Blocks.CAULDRON);
        helper.succeedWhen(() -> {
            helper.assertTrue(((CrucibleBlockEntity) helper.getBlockEntity(crucible)).waterTank().getFluidAmount() > 0, "Urn did not feed the crucible through its upper face");
            var state = helper.getBlockState(cauldron);
            helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3, "Urn did not fill all three cauldron levels");
        });
    }
}
