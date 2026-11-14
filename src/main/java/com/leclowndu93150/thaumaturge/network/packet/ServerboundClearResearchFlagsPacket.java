package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.ResearchFlag;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client clears NEW/PAGE badges; server handler is intentionally a no-op for now. */
public final class ServerboundClearResearchFlagsPacket {
    private final ResourceLocation research;
    private final List<ResearchFlag> flags;

    public ServerboundClearResearchFlagsPacket(ResourceLocation research, List<ResearchFlag> flags) {
        this.research = research;
        this.flags = flags;
    }

    public static void encode(ServerboundClearResearchFlagsPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.research);
        buf.writeVarInt(packet.flags.size());
        for (ResearchFlag flag : packet.flags) {
            buf.writeEnum(flag);
        }
    }

    public static ServerboundClearResearchFlagsPacket decode(FriendlyByteBuf buf) {
        ResourceLocation research = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<ResearchFlag> flags = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            flags.add(buf.readEnum(ResearchFlag.class));
        }
        return new ServerboundClearResearchFlagsPacket(research, flags);
    }

    public static void handle(ServerboundClearResearchFlagsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                for (ResearchFlag flag : packet.flags) {
                    // no-op until flag persistence is implemented
                }
            }
        });
        context.setPacketHandled(true);
    }
}
