package com.leclowndu93150.thaumaturge.content.device;

import com.leclowndu93150.thaumaturge.client.render.item.HungryChestItemBewlr;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Inventory/hand model uses the hungry chest BER instead of the cube block model. */
public final class HungryChestItem extends BlockItem {
    public HungryChestItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return HungryChestItemBewlr.getInstance();
            }
        });
    }
}
