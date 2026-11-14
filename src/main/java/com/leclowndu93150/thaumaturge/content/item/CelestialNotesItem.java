package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CelestialNotesItem extends Item {
    private static final int STUDY_POINTS = 2;

    public enum Kind {
        SUN(List.of(TTAspects.LUX, TTAspects.IGNIS)),
        MOON(List.of(TTAspects.LUX, TTAspects.TENEBRAE)),
        STARS(List.of(TTAspects.LUX, TTAspects.VACUOS, TTAspects.ALIENIS));

        private final List<ResourceLocation> aspects;

        Kind(List<ResourceLocation> aspects) {
            this.aspects = aspects;
        }
    }

    private final Kind kind;

    public CelestialNotesItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            for (ResourceLocation aspect : kind.aspects) {
                AspectPools.grant(server, aspect, STUDY_POINTS);
            }
            server.sendSystemMessage(Component.translatable("message.thaumaturge.celestial.studied")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
