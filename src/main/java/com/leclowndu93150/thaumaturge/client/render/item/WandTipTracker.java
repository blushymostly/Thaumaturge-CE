package com.leclowndu93150.thaumaturge.client.render.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class WandTipTracker {
    private static final long MAX_AGE_MS = 250;
    private static final Deque<HeldWand> RENDERING = new ArrayDeque<>();
    private static final Map<Player, Tip> TIPS = new WeakHashMap<>();

    private WandTipTracker() {}

    public static boolean isThirdPerson(ItemDisplayContext context) {
        return context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    public static void begin(Player player, ItemStack stack, ItemDisplayContext context) {
        RENDERING.push(new HeldWand(player, stack, context));
    }

    public static void end() {
        RENDERING.pop();
    }

    static void capture(ItemStack stack, ItemDisplayContext context, PoseStack pose) {
        HeldWand held = RENDERING.peek();
        if (held == null || held.stack() != stack || held.context() != context) return;
        Player player = held.player();
        HumanoidArm arm = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!player.isUsingItem() || player.getUsedItemHand() != hand) return;
        Vector3f offset = pose.last().pose().transformPosition(new Vector3f(0, WandRenderer.tipY(stack), 0));
        RenderSystem.getInverseViewRotationMatrix().transform(offset);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        TIPS.put(player, new Tip(hand, camera.add(offset.x, offset.y, offset.z), Util.getMillis()));
    }

    public static @Nullable Vec3 tip(Player player, InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return WandHandRenderer.tip(hand);
        }
        Tip tip = TIPS.get(player);
        return tip != null && tip.hand() == hand && Util.getMillis() - tip.time() <= MAX_AGE_MS ? tip.position() : null;
    }

    private record HeldWand(Player player, ItemStack stack, ItemDisplayContext context) {}
    private record Tip(InteractionHand hand, Vec3 position, long time) {}
}
