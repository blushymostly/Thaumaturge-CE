package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.decon.MenuDeconstructionTable;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ServerboundDeconCollectPacket {
    private final BlockPos pos;

    public ServerboundDeconCollectPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ServerboundDeconCollectPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static ServerboundDeconCollectPacket decode(FriendlyByteBuf buf) {
        return new ServerboundDeconCollectPacket(buf.readBlockPos());
    }

    public static void handle(ServerboundDeconCollectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            BlockEntityDeconstructionTable table = MenuDeconstructionTable.target(player, packet.pos);
            if (table != null) {
                table.collect(player);
            }
        });
        context.setPacketHandled(true);
    }
}
