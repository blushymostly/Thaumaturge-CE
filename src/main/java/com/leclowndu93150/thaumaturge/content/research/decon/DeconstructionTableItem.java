package com.leclowndu93150.thaumaturge.content.research.decon;

import com.leclowndu93150.thaumaturge.client.render.blockentity.DeconstructionTableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class DeconstructionTableItem extends BlockItem {
    public DeconstructionTableItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft client = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(client.getBlockEntityRenderDispatcher(), client.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                MultiBufferSource buffers, int light, int overlay) {
                            var model = client.getEntityModels().bakeLayer(DeconstructionTableRenderer.LAYER);
                            DeconstructionTableRenderer.renderTable(model, pose, buffers, light, overlay);
                        }
                    };
                }
                return renderer;
            }
        });
    }
}
