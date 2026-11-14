package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.content.research.scan.ScanBindings;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanRaycastHelper;
import com.leclowndu93150.thaumaturge.content.research.scan.SkyScan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThaumometerItem extends Item {
    public static final int USE_DURATION_TICKS = 25;
    public static final int SCAN_COMPLETE_ELAPSED_TICKS = 20;
    private static final int SCAN_RELEASE_TOLERANCE_TICKS = 18;

    public sealed interface ScanTarget {
        record Block(BlockPos pos) implements ScanTarget {}
        record Living(Entity entity) implements ScanTarget {}
        record Sky() implements ScanTarget {}
    }

    public ThaumometerItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!canBeginScan(context.getLevel(), player)) return InteractionResult.PASS;
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginScan(level, player)) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof Player)) {
            return;
        }
        int elapsed = USE_DURATION_TICKS - remainingUseDuration;
        if (elapsed >= SCAN_COMPLETE_ELAPSED_TICKS) {
            entity.releaseUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int elapsed = USE_DURATION_TICKS - remainingUseDuration;
        if (elapsed < SCAN_RELEASE_TOLERANCE_TICKS) {
            return;
        }
        performScan(player);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            performScan(player);
        }
        return stack;
    }

    public static ScanTarget resolveTarget(Level level, Player player) {
        HitResult hitResult = ScanRaycastHelper.performRaycast(player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            return new ScanTarget.Block(((BlockHitResult) hitResult).getBlockPos());
        }
        if (hitResult instanceof EntityHitResult entityHit) {
            return new ScanTarget.Living(entityHit.getEntity());
        }
        return new ScanTarget.Sky();
    }

    private static boolean canBeginScan(Level level, Player player) {
        return !(resolveTarget(level, player) instanceof ScanTarget.Sky) || SkyScan.canScan(player);
    }

    private static void performScan(ServerPlayer player) {
        ScanTarget target = resolveTarget(player.level(), player);
        if (target instanceof ScanTarget.Block block) {
            ScanBindings.scanBlock(player, block.pos());
        } else if (target instanceof ScanTarget.Living living) {
            ScanBindings.scanEntity(player, living.entity());
        } else {
            SkyScan.scan(player);
        }
    }
}
