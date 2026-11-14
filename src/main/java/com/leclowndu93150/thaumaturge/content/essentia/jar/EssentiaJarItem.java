package com.leclowndu93150.thaumaturge.content.essentia.jar;

import com.leclowndu93150.thaumaturge.client.render.item.JarItemBewlr;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.AlembicBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class EssentiaJarItem extends BlockItem {
    public EssentiaJarItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof AlembicBlockEntity)) {
            return super.useOn(context);
        }
        IEssentiaHandler source = EssentiaAccess.handler(level, context.getClickedPos(), Direction.UP);
        if (source == null) {
            return super.useOn(context);
        }
        ItemStack held = context.getItemInHand();
        EssentiaTank tank = new EssentiaTank(EssentiaJarBlockEntity.CAPACITY);
        CompoundTag existing = held.getTagElement("BlockEntityTag");
        if (existing != null) {
            tank.load(existing.getCompound("Tank"));
        }
        var aspect = EssentiaAccess.firstAvailable(source, tank.getSuctionAspect(Direction.UP));
        if (aspect == null || EssentiaAccess.transfer(source, Direction.UP, tank, Direction.UP, aspect,
                tank.getCapacity(), level.isClientSide) == 0) {
            return super.useOn(context);
        }
        if (!level.isClientSide) {
            ItemStack filled = held.copyWithCount(1);
            filled.getOrCreateTagElement("BlockEntityTag").put("Tank", tank.save());
            if (held.getCount() == 1) {
                player.setItemInHand(context.getHand(), filled);
            } else {
                held.shrink(1);
                if (!player.getInventory().add(filled)) {
                    player.drop(filled, false);
                }
            }
            level.playSound(null, context.getClickedPos(), TTSounds.JAR.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return JarItemBewlr.getInstance();
            }
        });
    }
}
