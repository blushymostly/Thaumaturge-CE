package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.client.ClientAuraCache;
import com.leclowndu93150.thaumaturge.content.aura.AuraData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class ClientboundAuraSnapshotPacket {
    private final ChunkPos pos;
    private final AuraData data;

    public ClientboundAuraSnapshotPacket(ChunkPos pos, AuraData data) {
        this.pos = pos;
        this.data = data;
    }

    public static void encode(ClientboundAuraSnapshotPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.pos.x);
        buf.writeInt(packet.pos.z);
        packet.data.write(buf);
    }

    public static ClientboundAuraSnapshotPacket decode(FriendlyByteBuf buf) {
        ChunkPos pos = new ChunkPos(buf.readInt(), buf.readInt());
        AuraData data = AuraData.read(buf);
        data.setChunkPos(pos);
        return new ClientboundAuraSnapshotPacket(pos, data);
    }

    public static void handle(ClientboundAuraSnapshotPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAuraCache.put(packet.pos, packet.data)));
        ctx.get().setPacketHandled(true);
    }
}
