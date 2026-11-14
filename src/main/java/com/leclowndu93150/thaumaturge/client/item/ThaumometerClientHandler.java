package com.leclowndu93150.thaumaturge.client.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.effect.ClientEffects;
import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class ThaumometerClientHandler {
    private static final int HIGHLIGHT_INTERVAL_TICKS = 5;
    private static final int LOCK_WINDOW_TICKS = 2;
    private static final int PULSE_INTERVAL_TICKS = 2;
    private static final float SOUND_VOLUME = 0.2F;
    private static final float SOUND_PITCH_BASE = 0.45F;
    private static final float SOUND_PITCH_SPREAD = 0.1F;

    private static @Nullable Identity lockedTarget;

    private ThaumometerClientHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) {
            return;
        }
        tickScan(minecraft, player, level);
        tickHighlight(player, level);
    }

    private static boolean holdsThaumometer(LocalPlayer player) {
        Item thaumometer = TTItems.THAUMOMETER.get();
        return player.getMainHandItem().is(thaumometer) || player.getOffhandItem().is(thaumometer);
    }

    private static void tickHighlight(LocalPlayer player, ClientLevel level) {
        if (player.tickCount % HIGHLIGHT_INTERVAL_TICKS != 0 || !holdsThaumometer(player)) {
            return;
        }
        ThaumometerItem.ScanTarget target = ThaumometerItem.resolveTarget(level, player);
        if (target instanceof ThaumometerItem.ScanTarget.Block block) {
            ClientEffects.scanSparkles(level, block.pos());
        } else if (target instanceof ThaumometerItem.ScanTarget.Living living) {
            ClientEffects.scanSparkles(living.entity());
        }
    }

    private static void tickScan(Minecraft minecraft, LocalPlayer player, ClientLevel level) {
        if (!player.isUsingItem() || !player.getUseItem().is(TTItems.THAUMOMETER.get())) {
            lockedTarget = null;
            return;
        }
        int elapsed = player.getTicksUsingItem();
        ThaumometerItem.ScanTarget target = ThaumometerItem.resolveTarget(level, player);
        if (!keepsLock(elapsed, target)) {
            cancelScan(minecraft, player);
            return;
        }
        if (elapsed % PULSE_INTERVAL_TICKS == 0) {
            pulse(level, player, target);
        }
        if (elapsed >= ThaumometerItem.SCAN_COMPLETE_ELAPSED_TICKS) {
            cancelScan(minecraft, player);
        }
    }

    private static boolean keepsLock(int elapsed, ThaumometerItem.ScanTarget target) {
        Identity current = Identity.of(target);
        if (elapsed < LOCK_WINDOW_TICKS) {
            lockedTarget = current;
            return true;
        }
        return current.equals(lockedTarget);
    }

    private static void cancelScan(Minecraft minecraft, LocalPlayer player) {
        lockedTarget = null;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (gameMode != null) {
            gameMode.releaseUsingItem(player);
        }
    }

    private static void pulse(ClientLevel level, LocalPlayer player, ThaumometerItem.ScanTarget target) {
        RandomSource random = level.getRandom();
        float pitch = SOUND_PITCH_BASE + random.nextFloat() * SOUND_PITCH_SPREAD;
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), TTSounds.CAMERA_TICKS.get(), SoundSource.PLAYERS, SOUND_VOLUME, pitch, false);
        if (target instanceof ThaumometerItem.ScanTarget.Block block) {
            ClientEffects.runeGlyph(level, block.pos());
        } else if (target instanceof ThaumometerItem.ScanTarget.Living living) {
            ClientEffects.runeGlyph(living.entity());
        }
    }

    private record Identity(String key) {
        static Identity of(ThaumometerItem.ScanTarget target) {
            if (target instanceof ThaumometerItem.ScanTarget.Block block) {
                BlockPos pos = block.pos();
                return new Identity("b:" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            if (target instanceof ThaumometerItem.ScanTarget.Living living) {
                return new Identity("e:" + living.entity().getId());
            }
            return new Identity("sky");
        }
    }
}
