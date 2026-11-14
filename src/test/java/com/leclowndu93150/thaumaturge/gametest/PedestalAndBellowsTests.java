package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.BlockEntityRechargePedestal;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeGenerator;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockBellows;
import com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockEntityBellows;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.mixin.world.level.block.entity.AbstractFurnaceBlockEntityAccessor;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class PedestalAndBellowsTests {
    private PedestalAndBellowsTests() {}

    @GameTest(template = "essentia_lab")
    public static void rechargePedestalSyncsPlacedAndChargedWand(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(2, 1, 2);
        var worldPos = helper.absolutePos(pos);
        helper.setBlock(pos, TTBlocks.RECHARGE_PEDESTAL.get());
        var pedestal = (BlockEntityRechargePedestal) helper.getBlockEntity(pos);
        var replica = new BlockEntityRechargePedestal(worldPos, pedestal.getBlockState());
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(new byte[] {42}), "tt_recharge_sync"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TTItems.WAND.get()));
        var hit = new BlockHitResult(Vec3.atCenterOf(worldPos), Direction.UP, worldPos, false);
        var state = pedestal.getBlockState();
        helper.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "Could not put the wand on the pedestal");
        replica.load(pedestal.getUpdateTag());
        helper.assertTrue(replica.getItem().is(TTItems.WAND.get()), "The update tag hid the placed wand");

        var aspects = AspectList.EMPTY.add(new AspectInstance(TTAspects.IGNIS, 3));
        helper.assertTrue(NodeGenerator.createNodeAt(level, worldPos.east(2),
                new NodeData(NodeType.NORMAL, Optional.empty(), aspects, aspects)), "Test node could not be placed");
        helper.succeedWhen(() -> {
            replica.load(pedestal.getUpdateTag());
            helper.assertTrue(WandCharge.stored(replica.getItem(), TTAspects.IGNIS) > 0, "The charged wand did not sync");
            helper.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                    "Could not recover the wand");
            replica.load(pedestal.getUpdateTag());
            helper.assertTrue(replica.getItem().isEmpty(), "A ghost wand remained after recovery");
        });
    }

    @GameTest(template = "essentia_lab")
    public static void bellowsBoostOnlyLitFurnaceFacingNozzleAndStopOnPower(GameTestHelper helper) {
        BlockPos boostedPos = new BlockPos(1, 1, 2);
        BlockPos controlPos = new BlockPos(5, 1, 2);
        var boosted = furnaceAt(helper, boostedPos);
        var control = furnaceAt(helper, controlPos);
        helper.setBlock(boostedPos.south(), bellows(Direction.NORTH));
        helper.setBlock(controlPos.south(), bellows(Direction.SOUTH));

        BlockPos legacyPos = helper.absolutePos(boostedPos.south());
        var chunk = helper.getLevel().getChunkAt(legacyPos);
        chunk.removeBlockEntity(legacyPos);
        BlockEntityBellows.restoreLegacyBellows(new ChunkEvent.Load(chunk, false));
        helper.assertTrue(chunk.getBlockEntity(legacyPos) instanceof BlockEntityBellows,
                "Legacy bellows did not regain its block entity");

        helper.runAfterDelay(25, () -> {
            int boostedBefore = boosted.thaumaturge$getCookTime();
            int controlBefore = control.thaumaturge$getCookTime();
            helper.assertTrue(boostedBefore > controlBefore && controlBefore > 0, "Airflow did not speed up the facing furnace");
            helper.setBlock(boostedPos.south().west(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(!helper.getBlockState(boostedPos.south()).getValue(BlockBellows.ENABLED),
                    "Redstone did not stop the bellows");
            helper.runAfterDelay(20, () -> {
                int boostedElapsed = boosted.thaumaturge$getCookTime() - boostedBefore;
                int controlElapsed = control.thaumaturge$getCookTime() - controlBefore;
                helper.assertTrue(boostedElapsed == controlElapsed && controlElapsed > 0,
                        "Powered bellows still accelerated the furnace");
                helper.setBlock(boostedPos.south().west(), Blocks.AIR);
                helper.assertTrue(helper.getBlockState(boostedPos.south()).getValue(BlockBellows.ENABLED),
                        "Bellows did not restart when power was removed");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "essentia_lab")
    public static void bellowsSpeedCrucibleHeatingWithoutReplacingHeatSource(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(2, 2, 2);
        var worldPos = helper.absolutePos(pos);
        helper.setBlock(pos, TTBlocks.CRUCIBLE.get());
        helper.setBlock(pos.south(), bellows(Direction.NORTH));
        var crucible = (CrucibleBlockEntity) helper.getBlockEntity(pos);
        crucible.waterTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        CrucibleBlockEntity.serverTick(level, worldPos, crucible.getBlockState(), crucible);
        helper.assertTrue(crucible.getHeat() == 0, "Bellows heated the crucible without a heat source");
        helper.setBlock(pos.below(), Blocks.LAVA);
        CrucibleBlockEntity.serverTick(level, worldPos, crucible.getBlockState(), crucible);
        helper.assertTrue(crucible.getHeat() == 2, "Facing bellows did not accelerate heating");
        helper.setBlock(pos.south(), bellows(Direction.SOUTH));
        CrucibleBlockEntity.serverTick(level, worldPos, crucible.getBlockState(), crucible);
        helper.assertTrue(crucible.getHeat() == 3, "An outward nozzle affected the crucible");
        helper.succeed();
    }

    private static net.minecraft.world.level.block.state.BlockState bellows(Direction facing) {
        return TTBulkContent.BLOCKS.get("bellows").get().defaultBlockState().setValue(BlockBellows.FACING, facing);
    }

    private static AbstractFurnaceBlockEntityAccessor furnaceAt(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        return (AbstractFurnaceBlockEntityAccessor) furnace;
    }
}
