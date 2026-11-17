package com.leclowndu93150.thaumaturge.client.effect;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.particle.AuraDrawParticle;
import com.leclowndu93150.thaumaturge.client.render.item.WandTipTracker;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.network.packet.ClientboundAuraDrawPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
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
        if (level != null && PULSES.size() < 128)
            PULSES.add(packet);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || PULSES.isEmpty())
            return;
        var level = Minecraft.getInstance().level;
        if (level != null && level == pulseLevel) {
            for (var pulse : PULSES)
                emit(level, pulse);
        }
        PULSES.clear();
        pulseLevel = null;
    }

    private static void emit(ClientLevel level, ClientboundAuraDrawPacket pulse) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.options.particles().get() == ParticleStatus.MINIMAL)
            return;
        if (!(level.getEntity(pulse.playerId()) instanceof Player player) || !player.isUsingItem() || player.getUsedItemHand() != pulse.hand() || !(player.getUseItem().getItem() instanceof WandItem))
            return;
        var tip = WandTipTracker.tip(player, pulse.hand());
        if (tip == null)
            return;

        var random = level.getRandom();
        var forward = player.getLookAngle();
        var right = Vec3.directionFromRotation(0, player.getYRot() + 90);
        var up = right.cross(forward);
        double side = (random.nextBoolean() ? 1 : -1) * (0.18 + random.nextDouble() * 0.30);
        var source = tip.add(forward.scale(0.18 + random.nextDouble() * 0.36)).add(right.scale(side)).add(up.scale((random.nextDouble() - 0.45) * 0.40));
        if (level.clip(new ClipContext(tip, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS)
            return;
        var bend = right.scale(side * 0.28).add(up.scale(0.09 + random.nextDouble() * 0.13));
        minecraft.particleEngine.add(new AuraDrawParticle(level, player, pulse.hand(), source, bend, tip, pulse.color()));
    }
}
