package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Combine two aspects at the research table into their compound. */
public final class ServerboundTableCombinePacket {
    private final BlockPos pos;
    private final ResourceLocation first;
    private final ResourceLocation second;
    private final boolean bonusFirst;
    private final boolean bonusSecond;

    public ServerboundTableCombinePacket(BlockPos pos, ResourceLocation first, ResourceLocation second, boolean bonusFirst, boolean bonusSecond) {
        this.pos = pos;
        this.first = first;
        this.second = second;
        this.bonusFirst = bonusFirst;
        this.bonusSecond = bonusSecond;
    }

    public static void encode(ServerboundTableCombinePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeResourceLocation(packet.first);
        buf.writeResourceLocation(packet.second);
        buf.writeBoolean(packet.bonusFirst);
        buf.writeBoolean(packet.bonusSecond);
    }

    public static ServerboundTableCombinePacket decode(FriendlyByteBuf buf) {
        return new ServerboundTableCombinePacket(buf.readBlockPos(), buf.readResourceLocation(), buf.readResourceLocation(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ServerboundTableCombinePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            BlockEntityResearchTable table = MenuResearchTable.target(player, packet.pos);
            if (table != null && TTAspects.get(packet.first) != null && TTAspects.get(packet.second) != null) {
                table.combineAspects(player, packet.first, packet.second, packet.bonusFirst, packet.bonusSecond);
            }
        });
        context.setPacketHandled(true);
    }
}
