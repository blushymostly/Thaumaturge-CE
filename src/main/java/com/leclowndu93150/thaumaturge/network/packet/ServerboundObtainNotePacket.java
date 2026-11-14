package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Ask the server to write a blank theory research note for a stage's theory row. */
public final class ServerboundObtainNotePacket {
    private final ResourceLocation entry;
    private final int ordinal;

    public ServerboundObtainNotePacket(ResourceLocation entry, int ordinal) {
        this.entry = entry;
        this.ordinal = ordinal;
    }

    public static void encode(ServerboundObtainNotePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.entry);
        buf.writeVarInt(packet.ordinal);
    }

    public static ServerboundObtainNotePacket decode(FriendlyByteBuf buf) {
        return new ServerboundObtainNotePacket(buf.readResourceLocation(), buf.readVarInt());
    }

    public static void handle(ServerboundObtainNotePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ResearchNotes.obtainNote(player, packet.entry, packet.ordinal);
            }
        });
        context.setPacketHandled(true);
    }
}
