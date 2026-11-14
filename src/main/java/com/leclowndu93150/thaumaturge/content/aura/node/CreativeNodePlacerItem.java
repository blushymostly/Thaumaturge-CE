package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.registry.TTItemData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Creative-only item: places a random node, or the node whose data it carries (from pick-block). */
public final class CreativeNodePlacerItem extends Item {
    public CreativeNodePlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        NodeData data = TTItemData.getNodeData(context.getItemInHand());
        boolean placed = data != null
                ? NodeGenerator.createNodeAt(level, pos, data)
                : NodeGenerator.createRandomNodeAt(level, pos, level.getRandom(), false, false, false, NodeGenerator.DEFAULT_SPECIAL_RARITY, NodeGenerator.DEFAULT_BASE_AURA);
        return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thaumaturge.creative_only").withStyle(ChatFormatting.DARK_PURPLE));
        NodeData data = TTItemData.getNodeData(stack);
        if (data != null) {
            tooltip.add(Component.literal(data.type().getSerializedName() + (data.modifier().isPresent() ? " / " + data.modifier().get().getSerializedName() : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
