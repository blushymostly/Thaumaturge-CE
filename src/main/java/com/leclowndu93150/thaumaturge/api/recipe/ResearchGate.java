package com.leclowndu93150.thaumaturge.api.recipe;

import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Optional research requirement on recipes: id or id@minStage. */
public final class ResearchGate {
    private ResearchGate() {}

    public static boolean isOpen(Player player, @Nullable ResourceLocation research) {
        if (research == null) {
            return true;
        }
        return isOpen(TTAttachments.knowledge(player), research, 0);
    }

    public static boolean isOpen(PlayerKnowledge knowledge, @Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        int stage = 0;
        String body = raw;
        int at = raw.lastIndexOf('@');
        if (at >= 0) {
            try {
                stage = Integer.parseInt(raw.substring(at + 1));
                body = raw.substring(0, at);
            } catch (NumberFormatException ignored) {
                body = raw;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(body);
        if (id == null) {
            return true;
        }
        return isOpen(knowledge, id, stage);
    }

    public static boolean isOpen(PlayerKnowledge knowledge, ResourceLocation id, int minStage) {
        if (knowledge.isComplete(id)) {
            return true;
        }
        return knowledge.isKnown(id) && knowledge.stage(id) >= minStage;
    }
}
