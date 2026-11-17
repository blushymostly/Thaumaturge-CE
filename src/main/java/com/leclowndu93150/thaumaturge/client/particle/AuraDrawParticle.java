package com.leclowndu93150.thaumaturge.client.particle;

import com.leclowndu93150.thaumaturge.client.render.item.WandTipTracker;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AuraDrawParticle extends TTParticle {
    private static final ParticleSheet SHEET = ParticleSheet.sheet("sparkle");

    private final Player player;
    private final InteractionHand hand;
    private final Vec3 source;
    private final Vec3 bend;
    private final Vec3 initialTip;
    private final float size;

    public AuraDrawParticle(ClientLevel level, Player player, InteractionHand hand, Vec3 source, Vec3 bend, Vec3 tip, int color) {
        super(level, source.x, source.y, source.z, 0, 0, 0, SHEET);
        this.player = player;
        this.hand = hand;
        this.source = source;
        this.bend = bend;
        this.initialTip = tip;
        this.lifetime = 8 + random.nextInt(5);
        this.size = 0.013F + random.nextFloat() * 0.009F;
        setColor(color);
        rCol = Mth.lerp(0.35F, rCol, 0.82F);
        gCol = Mth.lerp(0.35F, gCol, 0.82F);
        bCol = Mth.lerp(0.35F, bCol, 0.82F);
    }

    @Override
    protected void update() {
        float t = progress();
        Vec3 position = onArc(initialTip, t);
        setPos(position.x, position.y, position.z);
        frame(16 + (int) (t * 15));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float t = Mth.clamp((age + partialTick - 1) / lifetime, 0, 1);
        float fade = Mth.sin(t * Mth.PI);
        alpha = 0.43F * fade;
        quadSize = size * (0.5F + 0.5F * fade);

        Vec3 tip = player.isUsingItem() ? WandTipTracker.tip(player, hand) : null;
        // Follow small wand movements while keeping each sparkle's start fixed in the air.
        Vec3 target = tip != null && tip.distanceToSqr(initialTip) <= 1 ? tip : initialTip;
        Vec3 position = onArc(target, t);
        renderQuad(buffer, camera, camera.rotation(), (float) (position.x - Mth.lerp(partialTick, xo, x)), (float) (position.y - Mth.lerp(partialTick, yo, y)),
                (float) (position.z - Mth.lerp(partialTick, zo, z)), partialTick);
    }

    private Vec3 onArc(Vec3 tip, float t) {
        return source.lerp(tip, t * t).add(bend.scale(4 * t * (1 - t)));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }
}
