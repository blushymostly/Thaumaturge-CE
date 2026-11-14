package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.AuraData;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerSwapQueue;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.warp.PlayerWarpState;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundKnowledgeSyncPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forge replacement for NeoForge Attachments: player knowledge/warp + chunk aura/dust queue via capabilities.
 */
public final class TTAttachments {
    public static final ResourceLocation PLAYER_CAPS = TTIds.rl("player_data");
    public static final ResourceLocation CHUNK_CAPS = TTIds.rl("chunk_data");

    public static final Capability<PlayerKnowledge> KNOWLEDGE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<PlayerWarpState> WARP = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<AuraData> AURA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<DustTriggerSwapQueue> DUST_QUEUE = CapabilityManager.get(new CapabilityToken<>() {});

    private TTAttachments() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TTAttachments::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerKnowledge.class);
        event.register(PlayerWarpState.class);
        event.register(AuraData.class);
        event.register(DustTriggerSwapQueue.class);
    }

    @SubscribeEvent
    public static void attachEntity(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PLAYER_CAPS, new PlayerCapabilityProvider());
        }
    }

    @SubscribeEvent
    public static void attachChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        event.addCapability(CHUNK_CAPS, new ChunkCapabilityProvider());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(KNOWLEDGE).ifPresent(oldKnowledge -> event.getEntity().getCapability(KNOWLEDGE).ifPresent(fresh -> fresh.copyFrom(oldKnowledge)));
        event.getOriginal().getCapability(WARP).ifPresent(oldWarp -> event.getEntity().getCapability(WARP).ifPresent(fresh -> fresh.copyFrom(oldWarp)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AspectPools.seedIfNew(serverPlayer);
            syncKnowledge(serverPlayer);
        }
    }

    public static void syncKnowledge(ServerPlayer player) {
        player.getCapability(KNOWLEDGE).ifPresent(knowledge -> TTNetwork.sendToPlayer(new ClientboundKnowledgeSyncPacket(knowledge), player));
    }

    public static PlayerKnowledge knowledge(Player player) {
        return player.getCapability(KNOWLEDGE).orElseGet(PlayerKnowledge::new);
    }

    public static PlayerWarpState warp(Player player) {
        return player.getCapability(WARP).orElseGet(PlayerWarpState::new);
    }

    public static AuraData aura(LevelChunk chunk) {
        return chunk.getCapability(AURA).orElseGet(AuraData::new);
    }

    /** Pending Salis Mundus block swaps stored on the chunk. */
    public static DustTriggerSwapQueue dustQueue(LevelChunk chunk) {
        return chunk.getCapability(DUST_QUEUE).orElseGet(DustTriggerSwapQueue::new);
    }

    private static final class PlayerCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final PlayerKnowledge knowledge = new PlayerKnowledge();
        private final PlayerWarpState warp = new PlayerWarpState();
        private final LazyOptional<PlayerKnowledge> knowledgeOpt = LazyOptional.of(() -> knowledge);
        private final LazyOptional<PlayerWarpState> warpOpt = LazyOptional.of(() -> warp);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == KNOWLEDGE) {
                return knowledgeOpt.cast();
            }
            if (cap == WARP) {
                return warpOpt.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("Knowledge", knowledge.save());
            tag.put("Warp", warp.save());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            knowledge.load(nbt.getCompound("Knowledge"));
            warp.load(nbt.getCompound("Warp"));
        }
    }

    private static final class ChunkCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final AuraData aura = new AuraData();
        private final DustTriggerSwapQueue dustQueue = new DustTriggerSwapQueue();
        private final LazyOptional<AuraData> auraOpt = LazyOptional.of(() -> aura);
        private final LazyOptional<DustTriggerSwapQueue> dustOpt = LazyOptional.of(() -> dustQueue);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == AURA) {
                return auraOpt.cast();
            }
            if (cap == DUST_QUEUE) {
                return dustOpt.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("Aura", aura.save());
            tag.put("DustQueue", dustQueue.save());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains("Aura")) {
                aura.load(nbt.getCompound("Aura"));
            } else {
                // Legacy single-compound aura save
                aura.load(nbt);
            }
            if (nbt.contains("DustQueue")) {
                dustQueue.load(nbt.getCompound("DustQueue"));
            }
        }
    }
}
