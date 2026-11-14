package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.client.effect.AuraDrawEffects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAuraDrawPacket(int playerId, InteractionHand hand, Vec3 source, int color) {
    public static void encode(ClientboundAuraDrawPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.playerId());
        buffer.writeEnum(packet.hand());
        buffer.writeDouble(packet.source().x);
        buffer.writeDouble(packet.source().y);
        buffer.writeDouble(packet.source().z);
        buffer.writeInt(packet.color());
    }

    public static ClientboundAuraDrawPacket decode(FriendlyByteBuf buffer) {
        int player = buffer.readVarInt();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        Vec3 source = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new ClientboundAuraDrawPacket(player, hand, source, buffer.readInt());
    }

    public static void handle(ClientboundAuraDrawPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AuraDrawEffects.enqueue(packet));
    }
}
