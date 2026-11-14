package com.leclowndu93150.thaumaturge.api.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * CE-shaped research gate for dust triggers: {@code {"entry": "ns:id", "stage": 0, "negate": false}}.
 *
 * <p>Without {@code stage} the entry must be complete; with {@code stage} the entry must be known at that stage or
 * later. The string form {@code ns:id[@stage]} (used by the other Forge recipes) is accepted as well.
 */
public record DustGate(ResourceLocation entry, @Nullable Integer stage, boolean negate) {
    public static DustGate fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            String raw = json.getAsString();
            Integer stage = null;
            String body = raw;
            int at = raw.lastIndexOf('@');
            if (at >= 0) {
                try {
                    stage = Integer.parseInt(raw.substring(at + 1));
                    body = raw.substring(0, at);
                } catch (NumberFormatException ex) {
                    throw new JsonSyntaxException("Invalid research gate stage: " + raw);
                }
            }
            return new DustGate(parseId(body), stage, false);
        }
        JsonObject obj = GsonHelper.convertToJsonObject(json, "research");
        Integer stage = obj.has("stage") ? GsonHelper.getAsInt(obj, "stage") : null;
        return new DustGate(parseId(GsonHelper.getAsString(obj, "entry")), stage, GsonHelper.getAsBoolean(obj, "negate", false));
    }

    private static ResourceLocation parseId(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            throw new JsonSyntaxException("Invalid research entry id: " + raw);
        }
        return id;
    }

    public static DustGate fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation entry = buf.readResourceLocation();
        Integer stage = buf.readBoolean() ? buf.readVarInt() : null;
        return new DustGate(entry, stage, buf.readBoolean());
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.entry);
        buf.writeBoolean(this.stage != null);
        if (this.stage != null) {
            buf.writeVarInt(this.stage);
        }
        buf.writeBoolean(this.negate);
    }

    public boolean passes(Player player) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        boolean known = this.stage == null ? knowledge.isComplete(this.entry) : ResearchGate.isOpen(knowledge, this.entry, this.stage);
        return this.negate != known;
    }
}
