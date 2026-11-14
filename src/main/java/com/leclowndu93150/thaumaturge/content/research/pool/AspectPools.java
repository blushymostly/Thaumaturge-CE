package com.leclowndu93150.thaumaturge.content.research.pool;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAspectGainPacket;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Forge port of CE AspectPools: player aspect amounts + discovery, backed by {@link PlayerKnowledge}. */
public final class AspectPools {
    public static final int SOFT_CAP = 100;

    private static final double HARD_CAP_THRESHOLD = SOFT_CAP * 1.25;
    private static final int FIRST_DISCOVERY_BONUS = 2;
    private static final int STARTING_LOW = 15;
    private static final int STARTING_HIGH = 19;
    private static final List<ResourceLocation> STARTING_PRIMALS = List.of(TTAspects.AER, TTAspects.TERRA, TTAspects.IGNIS, TTAspects.AQUA, TTAspects.ORDO, TTAspects.PERDITIO);

    private AspectPools() {}

    /** Server-authoritative on the logical server, synced copy on the client. */
    public static PlayerKnowledge of(Player player) {
        return player.level().isClientSide() ? ClientKnowledge.get() : TTAttachments.knowledge(player);
    }

    public static void sync(ServerPlayer player) {
        TTAttachments.syncKnowledge(player);
    }

    public static void seedIfNew(ServerPlayer player) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (knowledge.aspectPoolEmpty()) {
            for (ResourceLocation primal : STARTING_PRIMALS) {
                knowledge.addAspect(primal, STARTING_LOW + player.getRandom().nextInt(STARTING_HIGH - STARTING_LOW + 1));
            }
        }
    }

    public static boolean isPrimal(ResourceLocation aspect) {
        IAspect value = TTAspects.get(aspect);
        return value != null && value.isPrimal();
    }

    public static boolean isDiscovered(Player player, ResourceLocation aspect) {
        return isPrimal(aspect) || of(player).isAspectDiscovered(aspect);
    }

    public static int amount(Player player, ResourceLocation aspect) {
        return of(player).aspectAmount(aspect);
    }

    public static boolean hasDiscoveredComponents(Player player, ResourceLocation aspect) {
        IAspect value = TTAspects.get(aspect);
        return value != null && value.componentIds().stream().allMatch(component -> isDiscovered(player, component));
    }

    /** Hint naming the first undiscovered component of an aspect, or null when every component is known. */
    public static Component missingComponentHint(Player player, ResourceLocation aspect) {
        IAspect value = TTAspects.get(aspect);
        if (value == null) {
            return null;
        }
        for (ResourceLocation component : value.componentIds()) {
            if (!isDiscovered(player, component)) {
                return Component.translatable("message.thaumaturge.research.discovery_error", AspectNames.name(component));
            }
        }
        return null;
    }

    public static int grant(ServerPlayer player, ResourceLocation aspect, int amount) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        boolean isNew = !isDiscovered(player, aspect);
        int requested = isNew ? amount + FIRST_DISCOVERY_BONUS : amount;
        int applied = reduceForCap(knowledge.aspectAmount(aspect), requested);
        if (applied <= 0 && !isNew) {
            return 0;
        }
        knowledge.addAspect(aspect, Math.max(applied, 0));
        knowledge.discoverAspect(aspect);
        sync(player);
        if (isNew) {
            player.sendSystemMessage(Component.translatable("message.thaumaturge.research.aspect_discovered", AspectNames.name(aspect)).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (applied > 0) {
            playPickup(player);
            sendGain(player, aspect, applied);
        } else if (isNew) {
            sendGain(player, aspect, FIRST_DISCOVERY_BONUS);
        }
        return applied;
    }

    public static void grantAll(ServerPlayer player, AspectList aspects) {
        for (AspectInstance entry : aspects.entries()) {
            if (hasDiscoveredComponents(player, entry.aspectId())) {
                grant(player, entry.aspectId(), entry.amount());
            }
        }
    }

    private static void sendGain(ServerPlayer player, ResourceLocation aspect, int amount) {
        TTNetwork.sendToPlayer(new ClientboundAspectGainPacket(aspect, amount), player);
    }

    private static int reduceForCap(int stored, int wanted) {
        if (stored >= HARD_CAP_THRESHOLD) {
            return Math.sqrt(wanted) >= 1 ? 1 : 0;
        }
        return stored >= SOFT_CAP ? (int) Math.sqrt(Math.max(wanted, 0)) : wanted;
    }

    public static boolean spend(ServerPlayer player, ResourceLocation aspect, int amount) {
        if (amount(player, aspect) < amount) {
            return false;
        }
        TTAttachments.knowledge(player).addAspect(aspect, -amount);
        sync(player);
        return true;
    }

    public static boolean canAfford(Player player, AspectList cost) {
        return cost.entries().stream().noneMatch(entry -> amount(player, entry.aspectId()) < entry.amount());
    }

    public static boolean spendAll(ServerPlayer player, AspectList cost) {
        if (!canAfford(player, cost)) {
            return false;
        }
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        for (AspectInstance entry : cost.entries()) {
            knowledge.addAspect(entry.aspectId(), -entry.amount());
        }
        sync(player);
        return true;
    }

    public static void refund(ServerPlayer player, ResourceLocation aspect, int amount) {
        TTAttachments.knowledge(player).addAspect(aspect, amount);
        sync(player);
        playPickup(player);
    }

    private static void playPickup(ServerPlayer player) {
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.LEARN.get(), SoundSource.PLAYERS, 0.2F, pitch);
    }
}
