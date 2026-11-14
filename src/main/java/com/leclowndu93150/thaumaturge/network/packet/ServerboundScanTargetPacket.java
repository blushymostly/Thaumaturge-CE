package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.research.scan.ScanBindings;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public final class ServerboundScanTargetPacket {
    public enum TargetType {
        BLOCK,
        ENTITY,
        ITEM
    }

    private final TargetType type;
    private final BlockPos blockPos;
    private final int entityId;

    private ServerboundScanTargetPacket(TargetType type, BlockPos blockPos, int entityId) {
        this.type = type;
        this.blockPos = blockPos;
        this.entityId = entityId;
    }

    public static ServerboundScanTargetPacket block(BlockPos pos) {
        return new ServerboundScanTargetPacket(TargetType.BLOCK, pos, -1);
    }

    public static ServerboundScanTargetPacket entity(int id) {
        return new ServerboundScanTargetPacket(TargetType.ENTITY, BlockPos.ZERO, id);
    }

    public static void encode(ServerboundScanTargetPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeBlockPos(packet.blockPos);
        buf.writeVarInt(packet.entityId);
    }

    public static ServerboundScanTargetPacket decode(FriendlyByteBuf buf) {
        return new ServerboundScanTargetPacket(buf.readEnum(TargetType.class), buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ServerboundScanTargetPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            switch (packet.type) {
                case BLOCK -> ScanBindings.scanBlock(player, packet.blockPos);
                case ENTITY -> {
                    Entity entity = player.level().getEntity(packet.entityId);
                    if (entity != null) {
                        ScanBindings.scanEntity(player, entity);
                    }
                }
                case ITEM -> ScanBindings.scanHeld(player);
            }
        });
        context.setPacketHandled(true);
    }
}
