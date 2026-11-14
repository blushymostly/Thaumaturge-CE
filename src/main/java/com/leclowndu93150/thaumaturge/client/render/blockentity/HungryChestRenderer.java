package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityHungryChest;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Forge BER for hungry chest — uses entity/chest/hungry.png via the chest atlas. */
public final class HungryChestRenderer extends ChestRenderer<BlockEntityHungryChest> {
    private static final Material HUNGRY = new Material(Sheets.CHEST_SHEET, TTIds.rl("entity/chest/hungry"));

    public HungryChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Material getMaterial(BlockEntityHungryChest blockEntity, ChestType chestType) {
        return HUNGRY;
    }
}
