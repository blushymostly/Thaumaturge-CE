package com.leclowndu93150.thaumaturge.network.packet;

import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorBlockEntity;
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorMenu;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundFocalDraftPacket(int menuId, int focusVersion, String name, FocusSpell draft, boolean inscribe) {
    public static void encode(ServerboundFocalDraftPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.menuId());
        buffer.writeVarInt(packet.focusVersion());
        buffer.writeUtf(packet.name(), FocalManipulatorBlockEntity.MAX_NAME_LENGTH);
        packet.draft().writeDraft(buffer);
        buffer.writeBoolean(packet.inscribe());
    }

    public static ServerboundFocalDraftPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundFocalDraftPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(FocalManipulatorBlockEntity.MAX_NAME_LENGTH), FocusSpell.readDraft(buffer),
                buffer.readBoolean());
    }

    public static void handle(ServerboundFocalDraftPacket packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null && player.containerMenu instanceof FocalManipulatorMenu menu && menu.containerId == packet.menuId()) {
            menu.submitDraft(player, packet.focusVersion(), packet.draft(), packet.name(), packet.inscribe());
        }
    }
}
