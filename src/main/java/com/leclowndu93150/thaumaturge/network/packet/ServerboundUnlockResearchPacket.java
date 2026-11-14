package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ServerboundUnlockResearchPacket {
    private final ResourceLocation research;

    public ServerboundUnlockResearchPacket(ResourceLocation research) {
        this.research = research;
    }

    public static void encode(ServerboundUnlockResearchPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.research);
    }

    public static ServerboundUnlockResearchPacket decode(FriendlyByteBuf buf) {
        return new ServerboundUnlockResearchPacket(buf.readResourceLocation());
    }

    public static void handle(ServerboundUnlockResearchPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ResearchManager.unlockRequested(player, packet.research);
            }
        });
        context.setPacketHandled(true);
    }
}
