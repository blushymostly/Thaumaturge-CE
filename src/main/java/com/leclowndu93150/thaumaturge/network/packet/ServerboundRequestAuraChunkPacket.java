package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.aura.AuraHelperBindings;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;

public final class ServerboundRequestAuraChunkPacket {
    private final ChunkPos pos;

    public ServerboundRequestAuraChunkPacket(ChunkPos pos) {
        this.pos = pos;
    }

    public static void encode(ServerboundRequestAuraChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.pos.x);
        buf.writeInt(packet.pos.z);
    }

    public static ServerboundRequestAuraChunkPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRequestAuraChunkPacket(new ChunkPos(buf.readInt(), buf.readInt()));
    }

    public static void handle(ServerboundRequestAuraChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            LevelChunk chunk = player.serverLevel().getChunk(packet.pos.x, packet.pos.z);
            var aura = TTAttachments.aura(chunk);
            AuraHelperBindings.ensureInitialized(player.serverLevel(), chunk, aura);
            TTNetwork.sendToPlayer(new ClientboundAuraSnapshotPacket(packet.pos, aura), player);
        });
        context.setPacketHandled(true);
    }
}
