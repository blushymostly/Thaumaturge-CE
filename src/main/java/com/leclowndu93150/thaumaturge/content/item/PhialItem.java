package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.AlembicBlockEntity;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhialItem extends Item {
    public static final int AMOUNT = 10;

    public PhialItem(Properties properties) {
        super(properties);
    }

    public static int tintColor(ItemStack stack, int layer) {
        AspectList contents = TTItemData.getAspects(stack);
        if (layer != 1 || contents.isEmpty()) {
            return 0xFFFFFF;
        }
        var aspect = TTAspects.get(contents.entries().get(0).aspectId());
        return aspect == null ? 0xFFFFFF : aspect.color();
    }

    @Override
    public Component getName(ItemStack stack) {
        AspectList contents = TTItemData.getAspects(stack);
        if (contents.isEmpty()) {
            return super.getName(stack);
        }
        ResourceLocation aspect = contents.entries().get(0).aspectId();
        return Component.translatable(getDescriptionId() + ".filled", AspectNames.name(aspect));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
        boolean jar = target instanceof EssentiaJarBlockEntity;
        if (!jar && !(target instanceof AlembicBlockEntity)) {
            return InteractionResult.PASS;
        }
        IEssentiaHandler handler = EssentiaAccess.handler(context.getLevel(), context.getClickedPos(), Direction.UP);
        if (handler == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = context.getItemInHand();
        AspectList contents = TTItemData.getAspects(held);
        boolean filling = contents.isEmpty();
        ResourceLocation aspect = filling ? EssentiaAccess.firstAvailable(handler, null) : contents.entries().get(0).aspectId();
        int amount = filling ? AMOUNT : contents.amountOf(aspect);
        if (aspect == null || (!filling && (!jar || contents.size() != 1 || amount != AMOUNT))) {
            return InteractionResult.PASS;
        }

        int possible = filling ? handler.extract(aspect, amount, true) : handler.insert(aspect, amount, true);
        if (possible != amount) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            if (filling) {
                handler.extract(aspect, amount, false);
            } else {
                handler.insert(aspect, amount, false);
            }
            context.getLevel().playSound(null, context.getClickedPos(), TTSounds.JAR.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            boolean creative = player.getAbilities().instabuild;
            if (filling || !creative) {
                ItemStack result = new ItemStack(this);
                if (filling) {
                    TTItemData.setAspects(result, AspectList.EMPTY.add(new AspectInstance(aspect, amount)));
                }
                if (!creative) {
                    held.shrink(1);
                }
                if (held.isEmpty()) {
                    player.setItemInHand(context.getHand(), result);
                } else if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
