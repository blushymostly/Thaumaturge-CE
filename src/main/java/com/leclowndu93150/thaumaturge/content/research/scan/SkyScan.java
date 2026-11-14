package com.leclowndu93150.thaumaturge.content.research.scan;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class SkyScan {
    private static final ResourceLocation NODES = TTIds.rl("nodes");
    private static final int TICKS_PER_DAY = 24000;
    private static final int HEADING_TOLERANCE = 10;
    private static final int ELEVATION_TOLERANCE = 7;

    private SkyScan() {}

    public static boolean canScan(Player player) {
        return observationFor(player) != null;
    }

    public static void scan(ServerPlayer player) {
        Observation observation = observationFor(player);
        if (observation == null) return;

        var knowledge = TTAttachments.knowledge(player);
        long day = player.level().getGameTime() / TICKS_PER_DAY;
        var key = ScanKeys.celestial(day, observation.subject());
        if (knowledge.isKnown(key)) {
            message(player, "message.thaumaturge.celestial.already_studied");
            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack paper = firstOf(inventory, Items.PAPER);
        if (paper.isEmpty() || firstOf(inventory, TTItems.SCRIBING_TOOLS.get()).isEmpty()) {
            message(player, "message.thaumaturge.celestial.cannot_note");
            return;
        }

        paper.shrink(1);
        var note = new ItemStack(TTBulkItems.ITEMS.get("celestial_notes_" + observation.variant()).get());
        if (!inventory.add(note)) player.drop(note, false);
        for (var id : List.copyOf(knowledge.completed())) {
            if (ScanKeys.isOldCelestial(id, day)) knowledge.forgetResearch(id);
        }
        knowledge.complete(key);
        TTAttachments.syncKnowledge(player);
    }

    private static @Nullable Observation observationFor(Player player) {
        Level level = player.level();
        if (player.getXRot() > 0 || !level.dimension().equals(Level.OVERWORLD)
                || !level.canSeeSky(player.blockPosition().above()) || !AspectPools.of(player).isComplete(NODES)) {
            return null;
        }

        int sunAngle = (int) (player.level().getSunAngle(1.0F) * Mth.RAD_TO_DEG);
        int cycle = Math.floorMod(sunAngle + 90, 360);
        boolean night = cycle > 180;
        int arc = night ? cycle - 180 : cycle;
        int heading = arc > 90 ? 180 : 0;
        int elevation = arc > 90 ? 180 - arc : arc;
        boolean aligned = Math.abs(Mth.wrapDegrees((int) (player.getYRot() + 90) - heading)) < HEADING_TOLERANCE
                && Math.abs((int) Math.abs(player.getXRot()) - elevation) < ELEVATION_TOLERANCE;
        if (!night) return aligned ? new Observation("sun", "sun") : null;
        if (aligned) {
            int phase = player.level().getMoonPhase();
            return new Observation("moon" + phase, "moon_" + (phase + 1));
        }
        int quadrant = switch (player.getDirection()) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
        return new Observation("star" + quadrant, "stars_" + (quadrant + 1));
    }

    private static ItemStack firstOf(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    private record Observation(String subject, String variant) {}
}
