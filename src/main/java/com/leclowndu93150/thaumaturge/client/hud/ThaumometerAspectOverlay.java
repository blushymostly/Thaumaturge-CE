package com.leclowndu93150.thaumaturge.client.hud;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagWorldRenderer;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanKeys;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanRaycastHelper;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Third-person aspect tag cloud while holding a thaumometer (CE ThaumometerAspectOverlay). */
@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class ThaumometerAspectOverlay {
    private static final float SCALE = AspectTagWorldRenderer.DEFAULT_SCALE;
    private static final double CLOUD_LIFT = 0.4;
    private static final double ENTITY_CLOUD_SHIFT = 0.5;

    private ThaumometerAspectOverlay() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        boolean holdsMeter = player.getMainHandItem().is(TTItems.THAUMOMETER.get())
                || player.getOffhandItem().is(TTItems.THAUMOMETER.get());
        boolean meterOverlay = holdsMeter && !minecraft.options.getCameraType().isFirstPerson();
        boolean goggles = GogglesAccess.wearsRevealingGear(player) && !holdsMeter;
        if (!meterOverlay && !goggles) {
            return;
        }
        HitResult hit = goggles ? minecraft.hitResult : ScanRaycastHelper.performRaycast(player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            AspectList essentia = EssentiaAccess.contentsOf(minecraft.level, pos);
            if (goggles) {
                if (!essentia.isEmpty()) {
                    boolean topFree = minecraft.level.isEmptyBlock(pos.above());
                    Direction face = topFree ? Direction.UP : player.getDirection().getOpposite();
                    double y = pos.getY() + (topFree ? 0.4 : 0.0);
                    AspectTagWorldRenderer.renderTagCloud(pose, minecraft, pos.getX(), y, pos.getZ(),
                            essentia, face, 0.27F, 0.75F, id -> true);
                }
                return;
            }
            AspectList aspects;
            if (!essentia.isEmpty()) {
                aspects = essentia;
            } else {
                if (!meterOverlay || !ClientKnowledge.get().isScanned(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                    return;
                }
                ItemStack form = state.getBlock().getCloneItemStack(minecraft.level, pos, state);
                aspects = form.isEmpty() ? AspectList.EMPTY : AspectIndex.of(form);
            }
            if (aspects.isEmpty()) {
                return;
            }
            Direction face = blockHit.getDirection();
            AspectTagWorldRenderer.renderTagCloud(pose, minecraft, pos.getX(), pos.getY(), pos.getZ(), aspects, face, SCALE, 1.0F,
                    id -> AspectPools.isDiscovered(player, id));
            return;
        }
        if (meterOverlay && hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (!ClientKnowledge.get().isScanned(typeId) && !ClientKnowledge.get().isKnown(ScanKeys.entity(entity.getType()))) {
                return;
            }
            AspectList aspects = EntityAspects.of(entity);
            if (aspects.isEmpty()) {
                return;
            }
            AspectTagWorldRenderer.renderTagCloud(pose, minecraft, entity.getX() - ENTITY_CLOUD_SHIFT, entity.getY() + entity.getBbHeight() + CLOUD_LIFT,
                    entity.getZ() - ENTITY_CLOUD_SHIFT, aspects, Direction.UP, SCALE, 1.0F, id -> AspectPools.isDiscovered(player, id));
        }
    }
}
