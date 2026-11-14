package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.note.HexGrid;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

/** Place an aspect on a note hex, or rub one out when {@code aspect} is null. */
public final class ServerboundTablePlaceAspectPacket {
    private final BlockPos pos;
    private final int q;
    private final int r;
    private final @Nullable ResourceLocation aspect;

    public ServerboundTablePlaceAspectPacket(BlockPos pos, int q, int r, @Nullable ResourceLocation aspect) {
        this.pos = pos;
        this.q = q;
        this.r = r;
        this.aspect = aspect;
    }

    public static void encode(ServerboundTablePlaceAspectPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.q);
        buf.writeVarInt(packet.r);
        buf.writeBoolean(packet.aspect != null);
        if (packet.aspect != null) {
            buf.writeResourceLocation(packet.aspect);
        }
    }

    public static ServerboundTablePlaceAspectPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int q = buf.readVarInt();
        int r = buf.readVarInt();
        ResourceLocation aspect = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new ServerboundTablePlaceAspectPacket(pos, q, r, aspect);
    }

    public static void handle(ServerboundTablePlaceAspectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            BlockEntityResearchTable table = MenuResearchTable.target(player, packet.pos);
            if (table == null || packet.aspect != null && TTAspects.get(packet.aspect) == null) {
                return;
            }
            table.placeAspect(player, new HexGrid.Hex(packet.q, packet.r), packet.aspect);
        });
        context.setPacketHandled(true);
    }
}
