package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.client.hud.KnowledgeGainOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** CE ClientboundAspectGainPayload: tell the client to show aspect-gain HUD chips. */
public final class ClientboundAspectGainPacket {
    private final ResourceLocation aspect;
    private final int amount;

    public ClientboundAspectGainPacket(ResourceLocation aspect, int amount) {
        this.aspect = aspect;
        this.amount = amount;
    }

    public static void encode(ClientboundAspectGainPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.aspect);
        buf.writeVarInt(packet.amount);
    }

    public static ClientboundAspectGainPacket decode(FriendlyByteBuf buf) {
        return new ClientboundAspectGainPacket(buf.readResourceLocation(), buf.readVarInt());
    }

    public static void handle(ClientboundAspectGainPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KnowledgeGainOverlay.onAspectGain(packet.aspect, packet.amount)));
        ctx.get().setPacketHandled(true);
    }
}
