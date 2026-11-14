package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.content.device.BlockEntityHungryChest;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Inventory/hand: reuse the hungry chest BER via {@link BlockEntityRenderDispatcher#renderItem}. */
public final class HungryChestItemBewlr extends BlockEntityWithoutLevelRenderer {
    private static HungryChestItemBewlr instance;
    private static BlockEntityHungryChest dummy;

    private HungryChestItemBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static HungryChestItemBewlr getInstance() {
        if (instance == null) {
            instance = new HungryChestItemBewlr();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        dispatcher.renderItem(dummyChest(), poseStack, buffer, packedLight, packedOverlay);
    }

    private static BlockEntityHungryChest dummyChest() {
        if (dummy == null) {
            BlockState state = TTBlocks.HUNGRY_CHEST.get().defaultBlockState();
            dummy = new BlockEntityHungryChest(BlockPos.ZERO, state);
        }
        return dummy;
    }
}
