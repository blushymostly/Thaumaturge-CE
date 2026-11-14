package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanBindings;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.NetworkEvent;

/** Inventory scan: the player holds a thaumometer on the cursor over a slot (or their own portrait). CE ServerboundScanSlotPayload. */
public final class ServerboundScanSlotPacket {
    public static final int SELF = -1;
    public static final int CHANNEL_TICKS = 25;
    private static final int LATENCY_ALLOWANCE_TICKS = 5;
    private static final Map<UUID, Long> LAST_SCAN = new ConcurrentHashMap<>();

    private final int slot;

    public ServerboundScanSlotPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(ServerboundScanSlotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot + 1);
    }

    public static ServerboundScanSlotPacket decode(FriendlyByteBuf buf) {
        return new ServerboundScanSlotPacket(buf.readVarInt() - 1);
    }

    public static void handle(ServerboundScanSlotPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            AbstractContainerMenu menu = player.containerMenu;
            long now = player.level().getGameTime();
            long last = LAST_SCAN.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (!(menu.getCarried().getItem() instanceof ThaumometerItem) || now - last < CHANNEL_TICKS - LATENCY_ALLOWANCE_TICKS) {
                return;
            }
            LAST_SCAN.put(player.getUUID(), now);
            if (packet.slot == SELF) {
                ScanBindings.scanEntity(player, player);
                return;
            }
            if (packet.slot < 0 || packet.slot >= menu.slots.size()) {
                return;
            }
            Slot slot = menu.getSlot(packet.slot);
            if (slot.hasItem() && slot.mayPickup(player) && !(slot instanceof ResultSlot)) {
                ScanBindings.scanItem(player, slot.getItem());
            }
        });
        context.setPacketHandled(true);
    }
}
