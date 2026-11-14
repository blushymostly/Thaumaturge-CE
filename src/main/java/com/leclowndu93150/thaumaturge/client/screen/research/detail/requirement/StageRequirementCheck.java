package com.leclowndu93150.thaumaturge.client.screen.research.detail.requirement;

import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeReward;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeType;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.research.ResearchRequirement;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class StageRequirementCheck {
    private StageRequirementCheck() {}

    public static boolean hasRequirements(ResearchStage stage) {
        return !stage.requiredResearch().isEmpty() || !stage.obtain().isEmpty() || !stage.craft().isEmpty() || !stage.requiredKnowledge().isEmpty();
    }

    public static boolean obtainMet(Player player, ResearchRequirement requirement) {
        return requirement.obtainMet(player);
    }

    public static boolean craftMet(PlayerKnowledge knowledge, ResearchRequirement requirement) {
        for (ResourceLocation itemId : requirement.candidateIds()) {
            if (knowledge.isComplete(ResearchManager.craftedKey(itemId))) {
                return true;
            }
        }
        return false;
    }

    /** Theory uses learn keys; observation uses aspect-pool affordability (CE parity). */
    public static boolean knowledgeMet(Player player, ResearchEntry entry, ResourceLocation entryId, KnowledgeReward reward, int theoryOrdinal) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        if (reward.type() == KnowledgeType.THEORY) {
            return knowledge.isKnown(ResearchNoteData.learnKey(entryId, theoryOrdinal));
        }
        return AspectPools.canAfford(player, ResearchNotes.observationCost(entry, reward.amount()));
    }

    public static boolean allSatisfied(Player player, ResearchEntry entry, ResourceLocation entryId, ResearchStage stage, int displayedStage) {
        if (!hasRequirements(stage)) {
            return false;
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        if (!stage.requiredResearch().stream().allMatch(knowledge::isComplete)
                || !stage.obtain().stream().allMatch(requirement -> obtainMet(player, requirement))
                || !stage.craft().stream().allMatch(requirement -> craftMet(knowledge, requirement))) {
            return false;
        }
        int theoryOrdinal = ResearchNotes.theoryRowsBefore(entry, displayedStage);
        for (KnowledgeReward reward : stage.requiredKnowledge()) {
            if (reward.type() == KnowledgeType.THEORY) {
                if (!knowledgeMet(player, entry, entryId, reward, theoryOrdinal++)) {
                    return false;
                }
            } else if (!knowledgeMet(player, entry, entryId, reward, theoryOrdinal)) {
                return false;
            }
        }
        return true;
    }
}
