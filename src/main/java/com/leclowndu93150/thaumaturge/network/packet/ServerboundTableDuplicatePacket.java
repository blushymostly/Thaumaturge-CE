package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Duplicate the completed note in the research table. */
public final class ServerboundTableDuplicatePacket {
    private final BlockPos pos;

    public ServerboundTableDuplicatePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ServerboundTableDuplicatePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static ServerboundTableDuplicatePacket decode(FriendlyByteBuf buf) {
        return new ServerboundTableDuplicatePacket(buf.readBlockPos());
    }

    public static void handle(ServerboundTableDuplicatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            BlockEntityResearchTable table = MenuResearchTable.target(player, packet.pos);
            if (table != null) {
                table.duplicateNote(player);
            }
        });
        context.setPacketHandled(true);
    }
}
