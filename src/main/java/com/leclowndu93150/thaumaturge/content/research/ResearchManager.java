package com.leclowndu93150.thaumaturge.content.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ResearchManager {
    private ResearchManager() {}

    public static ResourceLocation craftedKey(ResourceLocation itemId) {
        return TTIds.rl("crafted/" + itemId.getNamespace() + "/" + itemId.getPath());
    }

    public static ResourceLocation obtainedKey(ResourceLocation itemId) {
        return TTIds.rl("obtained/" + itemId.getNamespace() + "/" + itemId.getPath());
    }

    public static boolean unlockRequested(ServerPlayer player, ResourceLocation id) {
        ResearchEntry entry = ResearchData.entry(id);
        if (entry != null && entry.hasMeta("hidden") && entry.parents().isEmpty()) {
            return false;
        }
        if (entry != null && !categoryOpen(player, entry)) {
            return false;
        }
        return unlock(player, id);
    }

    private static boolean categoryOpen(ServerPlayer player, ResearchEntry entry) {
        // Hidden opener entries (e.g. base_alchemy) must be unlockable before their category tab opens.
        if (entry.hasMeta("hidden") && parentsSatisfied(knowledge(player), entry)) {
            return true;
        }
        ResearchCategory category = ResearchData.category(entry.category());
        if (category == null || category.requiredResearch().isEmpty()) {
            return true;
        }
        return knowledge(player).isComplete(category.requiredResearch().get());
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation id) {
        if (id == null) {
            return false;
        }
        PlayerKnowledge knowledge = knowledge(player);
        if (knowledge.isKnown(id)) {
            return false;
        }
        ResearchEntry entry = ResearchData.entry(id);
        if (entry != null && !parentsSatisfied(knowledge, entry)) {
            return false;
        }
        knowledge.startResearch(id);
        TTAttachments.syncKnowledge(player);
        return true;
    }

    public static boolean advanceStage(ServerPlayer player, ResourceLocation id) {
        ResearchEntry entry = ResearchData.entry(id);
        PlayerKnowledge knowledge = knowledge(player);
        if (entry == null || !knowledge.isKnown(id) || knowledge.isComplete(id)) {
            return false;
        }
        int current = knowledge.stage(id);
        if (current >= entry.stages().size()) {
            return false;
        }
        if (!stageRequirementsMet(player, knowledge, id, entry, current)) {
            return false;
        }
        ResearchStage stage = entry.stages().get(current);
        int[] submissions = submissions(player, stage);
        if (submissions == null) return false;
        AspectList cost = ResearchNotes.stageObservationCost(entry, stage);
        if (!cost.isEmpty() && !AspectPools.spendAll(player, cost)) {
            return false;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < submissions.length; slot++) {
            if (submissions[slot] > 0) inventory.removeItem(slot, submissions[slot]);
        }
        inventory.setChanged();
        player.giveExperiencePoints(5);
        if (stage.warp() > 0 && !com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig.WUSS_MODE.get()) {
            var warp = TTAttachments.warp(player);
            int normal = stage.warp() / 2;
            warp.addNormal(normal);
            warp.addPermanent(stage.warp() - normal);
        }
        int next = current + 1;
        if (next >= entry.stages().size()) {
            return complete(player, id);
        }
        knowledge.setStage(id, next);
        TTAttachments.syncKnowledge(player);
        return true;
    }

    public static boolean complete(ServerPlayer player, ResourceLocation id) {
        if (id == null) {
            return false;
        }
        PlayerKnowledge knowledge = knowledge(player);
        if (!knowledge.isKnown(id) && !knowledge.startResearch(id)) {
            // allow completing scan-only keys without an explicit unlock
            if (ResearchData.entry(id) != null) {
                return false;
            }
        }
        if (knowledge.isComplete(id)) {
            return false;
        }
        ResearchEntry entry = ResearchData.entry(id);
        int finalStage = entry == null ? 1 : entry.stages().size();
        knowledge.markResearchComplete(id, finalStage);
        if (entry != null) {
            completeSiblings(player, knowledge, entry);
        }
        TTAttachments.syncKnowledge(player);
        return true;
    }

    /** CE: completing an entry also completes listed siblings when their parents are satisfied. */
    private static void completeSiblings(ServerPlayer player, PlayerKnowledge knowledge, ResearchEntry entry) {
        for (ResourceLocation sibling : entry.siblings()) {
            if (knowledge.isComplete(sibling)) {
                continue;
            }
            ResearchEntry siblingEntry = ResearchData.entry(sibling);
            if (siblingEntry != null && !parentsSatisfied(knowledge, siblingEntry)) {
                continue;
            }
            if (!knowledge.isKnown(sibling)) {
                knowledge.startResearch(sibling);
            }
            complete(player, sibling);
        }
    }

    public static boolean progressResearch(ServerPlayer player, ResourceLocation research) {
        ResearchEntry entry = ResearchData.entry(research);
        PlayerKnowledge knowledge = knowledge(player);
        if (entry != null) {
            if (!knowledge.isKnown(research)) {
                if (!unlock(player, research)) {
                    return false;
                }
                return advanceStage(player, research);
            }
            return advanceStage(player, research);
        }
        if (knowledge.isKnown(research)) {
            return false;
        }
        knowledge.startResearch(research);
        knowledge.markResearchComplete(research, 1);
        TTAttachments.syncKnowledge(player);
        return true;
    }

    public static boolean parentsSatisfied(PlayerKnowledge knowledge, ResearchEntry entry) {
        return entry.parents().stream().allMatch(parent -> parent.isSatisfiedBy(knowledge));
    }

    /**
     * CE parity: observation requirements are paid from the aspect pool ({@code note_aspects × amount}),
     * not from per-category knowledge counters.
     */
    public static boolean stageRequirementsMet(Player player, PlayerKnowledge knowledge, ResourceLocation entryId, ResearchEntry entry, int stageIndex) {
        if (stageIndex < 0 || stageIndex >= entry.stages().size()) {
            return false;
        }
        ResearchStage stage = entry.stages().get(stageIndex);
        if (submissions(player, stage) == null) {
            return false;
        }
        for (ResourceLocation required : stage.requiredResearch()) {
            if (!knowledge.isComplete(required)) {
                return false;
            }
        }
        int theoryOrdinal = ResearchNotes.theoryRowsBefore(entry, stageIndex);
        for (KnowledgeReward reward : stage.requiredKnowledge()) {
            if (reward.type() == KnowledgeType.THEORY) {
                if (!knowledge.isKnown(ResearchNoteData.learnKey(entryId, theoryOrdinal++))) {
                    return false;
                }
            } else if (!AspectPools.canAfford(player, ResearchNotes.observationCost(entry, reward.amount()))) {
                return false;
            }
        }
        for (ResearchRequirement requirement : stage.craft()) {
            if (!craftSatisfied(player, knowledge, requirement)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable int[] submissions(Player player, ResearchStage stage) {
        var inventory = player.getInventory();
        int[] take = new int[inventory.getContainerSize()];
        for (var requirement : stage.obtain()) {
            int remaining = requirement.amount();
            for (int slot = 0; remaining > 0 && slot < take.length; slot++) {
                var item = inventory.getItem(slot);
                if (!requirement.matches(item)) continue;
                int count = Math.min(remaining, item.getCount() - take[slot]);
                take[slot] += count;
                remaining -= count;
            }
            if (remaining > 0) return null;
        }
        return take;
    }

    private static boolean craftSatisfied(Player player, PlayerKnowledge knowledge, ResearchRequirement requirement) {
        for (ResourceLocation itemId : requirement.candidateIds()) {
            if (knowledge.isComplete(craftedKey(itemId))) {
                return true;
            }
        }
        return requirement.obtainMet(player);
    }

    private static PlayerKnowledge knowledge(ServerPlayer player) {
        return TTAttachments.knowledge(player);
    }
}
