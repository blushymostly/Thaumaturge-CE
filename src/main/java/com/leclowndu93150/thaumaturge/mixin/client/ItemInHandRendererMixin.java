package com.leclowndu93150.thaumaturge.mixin.client;

import com.leclowndu93150.thaumaturge.client.render.item.WandTipTracker;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void thaumaturge$beginWandRender(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player player && WandTipTracker.isThirdPerson(context) && stack.getItem() instanceof WandItem) {
            WandTipTracker.begin(player, stack, context);
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void thaumaturge$endWandRender(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player && WandTipTracker.isThirdPerson(context) && stack.getItem() instanceof WandItem) {
            WandTipTracker.end();
        }
    }
}
