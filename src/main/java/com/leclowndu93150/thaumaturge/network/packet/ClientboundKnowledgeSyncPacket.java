package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class ClientboundKnowledgeSyncPacket {
    private final PlayerKnowledge knowledge;

    public ClientboundKnowledgeSyncPacket(PlayerKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public static void encode(ClientboundKnowledgeSyncPacket packet, FriendlyByteBuf buf) {
        packet.knowledge.write(buf);
    }

    public static ClientboundKnowledgeSyncPacket decode(FriendlyByteBuf buf) {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.read(buf);
        return new ClientboundKnowledgeSyncPacket(knowledge);
    }

    public static void handle(ClientboundKnowledgeSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientKnowledge.apply(packet.knowledge)));
        ctx.get().setPacketHandled(true);
    }
}
