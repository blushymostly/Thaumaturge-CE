package com.leclowndu93150.thaumaturge.compat.curios;

import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class GogglesCurioRenderer implements ICurioRenderer {
    private HumanoidModel<LivingEntity> armor;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack poses,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float headYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)
                || GogglesAccess.isRevealing(slot.entity().getItemBySlot(EquipmentSlot.HEAD))) {
            return;
        }
        if (armor == null) {
            armor = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            armor.setAllVisible(false);
            armor.head.visible = true;
            armor.hat.visible = true;
        }
        armor.head.copyFrom(humanoid.head);
        armor.hat.copyFrom(humanoid.hat);
        armor.young = humanoid.young;
        String path = stack.getItem().getArmorTexture(stack, slot.entity(), EquipmentSlot.HEAD, null);
        var vertices = buffers.getBuffer(RenderType.armorCutoutNoCull(ResourceLocation.tryParse(path)));
        armor.renderToBuffer(poses, vertices, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }
}
