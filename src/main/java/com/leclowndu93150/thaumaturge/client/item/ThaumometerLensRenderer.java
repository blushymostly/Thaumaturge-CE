package com.leclowndu93150.thaumaturge.client.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagWorldRenderer;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanKeys;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** First-person thaumometer lens: name + aspect tag grid (CE parity minus nodes/essentia BE). */
public final class ThaumometerLensRenderer {
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final float NAME_Y = -0.25F;
    private static final float NAME_FRONT_Z = 0.0005F;
    private static final float NAME_BASE_SCALE = 0.005F;
    private static final float NAME_MIN_SCALE = 0.001F;
    private static final float NAME_SCALE_STEP = 0.000025F;
    private static final int NAME_FULL_SCALE_WIDTH = 90;
    /** CE uses 0.0075; Forge hand/HEAD projection reads ~3× larger, so shrink lens tags. */
    private static final float GRID_SCALE = 0.0025F;

    private static @Nullable Cached cached;

    private ThaumometerLensRenderer() {}

    public static void renderReadout(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack, MultiBufferSource buffer) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ThaumometerItem.ScanTarget target = ThaumometerItem.resolveTarget(level, player);
        if (target instanceof ThaumometerItem.ScanTarget.Sky) {
            return;
        }
        Readout readout = readoutOf(level, player, target);
        Font font = minecraft.font;
        submitName(font, poseStack, buffer, readout.name());
        if (!readout.aspects().isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(GRID_SCALE, GRID_SCALE, GRID_SCALE);
            AspectTagWorldRenderer.renderLensGrid(poseStack, buffer, font, readout.aspects().sortedByAmount(),
                    id -> AspectPools.isDiscovered(player, id));
            poseStack.popPose();
        }
    }

    private static Readout readoutOf(ClientLevel level, AbstractClientPlayer player, ThaumometerItem.ScanTarget target) {
        long gameTime = level.getGameTime();
        Cached last = cached;
        if (last != null && last.matches(level, player, target, gameTime)) {
            return last.readout();
        }
        Readout readout = fresh(level, player, target);
        cached = new Cached(level, player, target, gameTime, readout);
        return readout;
    }

    private static Readout fresh(ClientLevel level, AbstractClientPlayer player, ThaumometerItem.ScanTarget target) {
        if (target instanceof ThaumometerItem.ScanTarget.Block block) {
            BlockState state = level.getBlockState(block.pos());
            ItemStack pick = state.getBlock().getCloneItemStack(level, block.pos(), state);
            Component name = pick.isEmpty() ? state.getBlock().getName() : pick.getHoverName();
            ResourceLocation scanId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            AspectList aspects = AspectList.EMPTY;
            var be = level.getBlockEntity(block.pos());
            if (be instanceof BlockEntityNode node) {
                return new Readout(nodeLabel(node), node.getAspects());
            }
            if (be instanceof CrucibleBlockEntity crucible && !crucible.aspects().isEmpty()) {
                aspects = crucible.aspects();
            } else if (be instanceof EssentiaJarBlockEntity jar && !jar.tank().getEssentia().isEmpty()) {
                aspects = jar.tank().getEssentia();
            } else if (!pick.isEmpty() && (ClientKnowledge.get().isScanned(scanId)
                    || ClientKnowledge.get().isComplete(ScanKeys.item(pick.getItem())))) {
                aspects = AspectIndex.of(pick);
            }
            AspectList essentia = EssentiaAccess.contentsOf(level, block.pos());
            if (!essentia.isEmpty()) {
                // Jars / crucibles: show the live essentia they hold instead of the block's own aspects.
                aspects = essentia;
            }
            return new Readout(name, aspects);
        }
        if (target instanceof ThaumometerItem.ScanTarget.Living living) {
            Entity entity = living.entity();
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();
                AspectList aspects = ClientKnowledge.get().isScanned(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        ? AspectIndex.of(stack)
                        : AspectList.EMPTY;
                return new Readout(stack.getHoverName(), aspects);
            }
            AspectList aspects = ClientKnowledge.get().isScanned(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
                    ? EntityAspects.of(entity)
                    : AspectList.EMPTY;
            return new Readout(entity.getDisplayName(), aspects);
        }
        return new Readout(Component.literal("?"), AspectList.EMPTY);
    }

    private static void submitName(Font font, PoseStack poseStack, MultiBufferSource buffer, Component name) {
        int width = font.width(name);
        float scale = width <= NAME_FULL_SCALE_WIDTH
                ? NAME_BASE_SCALE
                : Math.max(NAME_MIN_SCALE, NAME_BASE_SCALE - NAME_SCALE_STEP * (width - NAME_FULL_SCALE_WIDTH));
        poseStack.pushPose();
        poseStack.translate(0.0F, NAME_Y, NAME_FRONT_Z);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-width / 2.0F, 0.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(name, 0.0F, 0.0F, NAME_COLOR, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private record Cached(ClientLevel level, AbstractClientPlayer player, ThaumometerItem.ScanTarget target, long gameTime, Readout readout) {
        boolean matches(ClientLevel currentLevel, AbstractClientPlayer currentPlayer, ThaumometerItem.ScanTarget currentTarget, long currentGameTime) {
            return level == currentLevel && player == currentPlayer && gameTime == currentGameTime && target.equals(currentTarget);
        }
    }

    private static Component nodeLabel(BlockEntityNode node) {
        Component type = Component.translatable("nodetype.thaumaturge." + node.kind().getSerializedName());
        NodeModifier modifier = node.trait();
        if (modifier == null) {
            return Component.translatable("gui.thaumaturge.thaumometer.node").append(": ").append(type);
        }
        Component mod = Component.translatable("nodemod.thaumaturge." + modifier.getSerializedName());
        return Component.translatable("tooltip.thaumaturge.node.type_modifier", type, mod);
    }

    private record Readout(Component name, AspectList aspects) {}
}
