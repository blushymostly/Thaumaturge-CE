package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ServerboundAdvanceStagePacket {
    private final ResourceLocation research;

    public ServerboundAdvanceStagePacket(ResourceLocation research) {
        this.research = research;
    }

    public static void encode(ServerboundAdvanceStagePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.research);
    }

    public static ServerboundAdvanceStagePacket decode(FriendlyByteBuf buf) {
        return new ServerboundAdvanceStagePacket(buf.readResourceLocation());
    }

    public static void handle(ServerboundAdvanceStagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ResearchManager.advanceStage(player, packet.research);
            }
        });
        context.setPacketHandled(true);
    }
}
