package com.leclowndu93150.thaumaturge.client.effect;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.render.item.WandTipTracker;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAuraDrawPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class AuraDrawEffects {
    private static final List<ClientboundAuraDrawPacket> PULSES = new ArrayList<>();
    private static ClientLevel pulseLevel;

    private AuraDrawEffects() {}

    public static void enqueue(ClientboundAuraDrawPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level != pulseLevel) {
            PULSES.clear();
            pulseLevel = level;
        }
        if (level != null && PULSES.size() < 128) PULSES.add(packet);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || PULSES.isEmpty()) return;
        var level = Minecraft.getInstance().level;
        if (level != null && level == pulseLevel) {
            for (var pulse : PULSES) emit(level, pulse);
        }
        PULSES.clear();
        pulseLevel = null;
    }

    private static void emit(ClientLevel level, ClientboundAuraDrawPacket pulse) {
        if (!(level.getEntity(pulse.playerId()) instanceof Player player) || !player.isUsingItem()
                || player.getUsedItemHand() != pulse.hand() || !(player.getUseItem().getItem() instanceof WandItem)) return;
        Vec3 tip = WandTipTracker.tip(player, pulse.hand());
        if (tip == null) return;

        Vec3 towardsTip = tip.subtract(pulse.source());
        double distance = towardsTip.length();
        if (distance < 0.1) return;
        int count = Math.max(2, Math.min(6, (int) Math.ceil(distance * 3)));
        Vec3 velocity = towardsTip.scale(0.13 / distance);
        var particle = new SparkleParticleOptions(pulse.color(), 0.45F, 0, 0.91F, 0, 4, false);
        for (int i = 0; i < count; i++) {
            Vec3 point = pulse.source().lerp(tip, (double) i / count);
            level.addParticle(particle, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
