package com.leclowndu93150.thaumaturge.network;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAspectGainPacket;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAuraDrawPacket;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAuraSnapshotPacket;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundKnowledgeSyncPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundFocalDraftPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundRequestAuraChunkPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundScanSlotPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundScanTargetPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundAdvanceStagePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundClearResearchFlagsPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundDeconCollectPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundObtainNotePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTableCombinePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTableDuplicatePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTablePlaceAspectPacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundUnlockResearchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TTNetwork {
    private static final String PROTOCOL = "8";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(TTIds.rl("main")).networkProtocolVersion(() -> PROTOCOL).clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals).simpleChannel();

    private static int id;

    private TTNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ClientboundKnowledgeSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundKnowledgeSyncPacket::encode)
                .decoder(ClientboundKnowledgeSyncPacket::decode).consumerMainThread(ClientboundKnowledgeSyncPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundAuraSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuraSnapshotPacket::encode).decoder(ClientboundAuraSnapshotPacket::decode)
                .consumerMainThread(ClientboundAuraSnapshotPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundAspectGainPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAspectGainPacket::encode).decoder(ClientboundAspectGainPacket::decode)
                .consumerMainThread(ClientboundAspectGainPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundRequestAuraChunkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundRequestAuraChunkPacket::encode)
                .decoder(ServerboundRequestAuraChunkPacket::decode).consumerMainThread(ServerboundRequestAuraChunkPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundScanTargetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundScanTargetPacket::encode).decoder(ServerboundScanTargetPacket::decode)
                .consumerMainThread(ServerboundScanTargetPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundScanSlotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundScanSlotPacket::encode).decoder(ServerboundScanSlotPacket::decode)
                .consumerMainThread(ServerboundScanSlotPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundUnlockResearchPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundUnlockResearchPacket::encode)
                .decoder(ServerboundUnlockResearchPacket::decode).consumerMainThread(ServerboundUnlockResearchPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundAdvanceStagePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundAdvanceStagePacket::encode).decoder(ServerboundAdvanceStagePacket::decode)
                .consumerMainThread(ServerboundAdvanceStagePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundClearResearchFlagsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundClearResearchFlagsPacket::encode)
                .decoder(ServerboundClearResearchFlagsPacket::decode).consumerMainThread(ServerboundClearResearchFlagsPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundTablePlaceAspectPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundTablePlaceAspectPacket::encode)
                .decoder(ServerboundTablePlaceAspectPacket::decode).consumerMainThread(ServerboundTablePlaceAspectPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundTableCombinePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundTableCombinePacket::encode).decoder(ServerboundTableCombinePacket::decode)
                .consumerMainThread(ServerboundTableCombinePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundTableDuplicatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundTableDuplicatePacket::encode)
                .decoder(ServerboundTableDuplicatePacket::decode).consumerMainThread(ServerboundTableDuplicatePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundObtainNotePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundObtainNotePacket::encode).decoder(ServerboundObtainNotePacket::decode)
                .consumerMainThread(ServerboundObtainNotePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundDeconCollectPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundDeconCollectPacket::encode).decoder(ServerboundDeconCollectPacket::decode)
                .consumerMainThread(ServerboundDeconCollectPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundAuraDrawPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAuraDrawPacket::encode).decoder(ClientboundAuraDrawPacket::decode)
                .consumerMainThread(ClientboundAuraDrawPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundFocalDraftPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundFocalDraftPacket::encode).decoder(ServerboundFocalDraftPacket::decode)
                .consumerMainThread(ServerboundFocalDraftPacket::handle).add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
