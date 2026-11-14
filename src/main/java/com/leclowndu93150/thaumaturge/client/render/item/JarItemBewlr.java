package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.client.render.blockentity.JarRenderer;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Forge 1.20.1 stand-in for CE26 JarItemSpecialRenderer.
 * Renders the jar block model plus the essentia surface read from the stack's {@code BlockEntityTag.Tank}.
 */
public final class JarItemBewlr extends BlockEntityWithoutLevelRenderer {
    private static JarItemBewlr instance;

    private JarItemBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static JarItemBewlr getInstance() {
        if (instance == null) {
            instance = new JarItemBewlr();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = TTBlocks.JAR_NORMAL.get().defaultBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay, ModelData.EMPTY, null);
        EssentiaTank contents = contentsOf(stack);
        if (contents != null) {
            JarRenderer.renderTank(contents, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private static EssentiaTank contentsOf(ItemStack stack) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null || !blockEntityTag.contains("Tank")) {
            return null;
        }
        EssentiaTank tank = new EssentiaTank(EssentiaJarBlockEntity.CAPACITY);
        tank.load(blockEntityTag.getCompound("Tank"));
        return tank.stored() > 0 ? tank : null;
    }
}
