package com.leclowndu93150.thaumaturge.content.research.scan;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeType;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ScanBindings {
    private ScanBindings() {}

    public static void scanBlock(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        AspectList aspects;
        if (player.level().getBlockEntity(pos) instanceof BlockEntityNode node) {
            aspects = node.getAspects();
        } else {
            ItemStack form = state.getBlock().getCloneItemStack(player.level(), pos, state);
            aspects = form.isEmpty() ? AspectList.EMPTY : AspectIndex.of(form);
        }
        unlock(player, BuiltInRegistries.BLOCK.getKey(state.getBlock()), ScanKeys.block(state.getBlock()), aspects);
    }

    public static void scanEntity(ServerPlayer player, Entity entity) {
        unlock(player, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), ScanKeys.entity(entity.getType()), EntityAspects.of(entity));
    }

    public static void scanHeld(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            stack = player.getOffhandItem();
        }
        scanItem(player, stack);
    }

    public static void scanItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        unlock(player, BuiltInRegistries.ITEM.getKey(stack.getItem()), ScanKeys.item(stack.getItem()), AspectIndex.of(stack));
    }

    /** Whether a world/inventory scan of this target can still teach anything. */
    public static boolean isStillScannable(ServerPlayer player, ResourceLocation target, ResourceLocation scanKey, AspectList aspects) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (!knowledge.isScanned(target) || !knowledge.isKnown(scanKey)) {
            return true;
        }
        for (AspectInstance entry : aspects.entries()) {
            if (!AspectPools.hasDiscoveredComponents(player, entry.aspectId())) {
                continue;
            }
            if (!AspectPools.isDiscovered(player, entry.aspectId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isItemStillScannable(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return isStillScannable(player, BuiltInRegistries.ITEM.getKey(stack.getItem()), ScanKeys.item(stack.getItem()), AspectIndex.of(stack));
    }

    private static void unlock(ServerPlayer player, ResourceLocation target, ResourceLocation scanKey, AspectList aspects) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        boolean discovery = DiscoveryScans.record(player, target);
        if (!discovery && !isStillScannable(player, target, scanKey, aspects)) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturge.scan.nothing_new").withStyle(ChatFormatting.GRAY),
                    true);
            return;
        }

        boolean changed = knowledge.scan(target) | discovery;
        changed |= ResearchManager.progressResearch(player, scanKey);

        boolean gainedAspect = false;
        if (!aspects.isEmpty()) {
            for (AspectInstance entry : aspects.entries()) {
                if (!AspectPools.hasDiscoveredComponents(player, entry.aspectId())) {
                    continue;
                }
                boolean wasKnown = AspectPools.isDiscovered(player, entry.aspectId());
                int applied = AspectPools.grant(player, entry.aspectId(), entry.amount());
                if (applied > 0 || !wasKnown) {
                    gainedAspect = true;
                    changed = true;
                }
                // CE ScanAspectDiscovery: discovering/carrying an aspect completes scanned/aspect/<ns>/<path>.
                if (AspectPools.isDiscovered(player, entry.aspectId())) {
                    changed |= ResearchManager.progressResearch(player, ScanKeys.aspect(entry.aspectId()));
                }
            }
        }

        // CE Scans.aspect: a successful aspect-bearing scan grants observation in Basics, Alchemy and Auromancy.
        if (changed) {
            knowledge.addKnowledge(KnowledgeType.OBSERVATION, TTIds.rl("basics"), 1);
            if (!aspects.isEmpty()) {
                knowledge.addKnowledge(KnowledgeType.OBSERVATION, TTIds.rl("alchemy"), 1);
                knowledge.addKnowledge(KnowledgeType.OBSERVATION, TTIds.rl("auromancy"), 1);
            }
            TTAttachments.syncKnowledge(player);
        }

        boolean found = changed || gainedAspect;
        player.displayClientMessage(
                Component.translatable(found ? "message.thaumaturge.scan.learned" : "message.thaumaturge.scan.nothing_new")
                        .withStyle(found ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                true);
    }
}
