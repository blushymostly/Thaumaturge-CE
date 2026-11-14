package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

public final class NodeJarRitual {
    private static final int COST_PER_PRIMAL = 70 * WandCharge.CENTIVIS_PER_VIS;

    private NodeJarRitual() {}

    public static boolean use(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (!level.getBlockState(clicked).is(Tags.Blocks.GLASS)) return false;
        BlockPos target = null;
        for (BlockPos candidate : BlockPos.betweenClosed(clicked.offset(-1, -1, -1), clicked.offset(1, 1, 1))) {
            if (!level.hasChunkAt(candidate) || !level.getBlockState(candidate).is(TTBlocks.NODE.get())) continue;
            boolean complete = fitsStructure(level, candidate);
            if (target == null || complete) target = candidate.immutable();
            if (complete) break;
        }
        if (target == null) return false;
        Player player = context.getPlayer();
        if (level instanceof ServerLevel server && player != null) {
            capture(server, target, player, context.getItemInHand());
        }
        return true;
    }

    public static boolean capture(ServerLevel level, BlockPos pos, Player player, ItemStack wand) {
        if (!level.getBlockState(pos).is(TTBlocks.NODE.get())
                || !(level.getBlockEntity(pos) instanceof BlockEntityNode node) || node.isEnergized()) return false;
        if (!TTAttachments.knowledge(player).isComplete(TTIds.rl("node_jar"))) return reject(player, "no_research");
        if (!fitsStructure(level, pos)) return reject(player, "structure");
        List<BlockPos> shell = structurePositions(pos);
        if (!level.mayInteract(player, pos) || shell.stream().anyMatch(cell -> !level.mayInteract(player, cell))) return false;
        if (WandCharge.PRIMALS.stream().anyMatch(aspect -> WandCharge.stored(wand, aspect) < COST_PER_PRIMAL)) {
            return reject(player, "vis");
        }

        NodeModifier modifier = node.trait();
        if (level.random.nextFloat() < 0.75F) modifier = degrade(modifier);
        var data = new NodeData(node.kind(), Optional.ofNullable(modifier), node.getAspects(), node.capacity());
        Block jarBlock = TTBulkContent.BLOCKS.get("jar_node").get();
        if (!level.setBlock(pos, jarBlock.defaultBlockState(), Block.UPDATE_ALL)) return false;
        if (!(level.getBlockEntity(pos) instanceof JarNodeBlock.Entity jar)) {
            throw new IllegalStateException("Node jar block entity is missing after capture");
        }
        jar.applyNodeData(data);
        WandCharge.PRIMALS.forEach(aspect -> WandCharge.spendExact(wand, aspect, COST_PER_PRIMAL));
        for (BlockPos cell : shell) level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        level.levelEvent(2001, pos, Block.getId(Blocks.GLASS.defaultBlockState()));
        return true;
    }

    private static boolean reject(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.thaumaturge.node_jar." + reason, 70), true);
        return false;
    }

    private static @Nullable NodeModifier degrade(@Nullable NodeModifier modifier) {
        if (modifier == null) return NodeModifier.PALE;
        return switch (modifier) {
            case BRIGHT -> null;
            case PALE -> NodeModifier.FADING;
            default -> modifier;
        };
    }

    public static boolean fitsStructure(Level level, BlockPos pos) {
        return structurePositions(pos).stream().allMatch(cell -> level.hasChunkAt(cell)
                && level.getBlockState(cell).is(cell.getY() == pos.getY() + 2 ? BlockTags.WOODEN_SLABS : Tags.Blocks.GLASS));
    }

    public static List<BlockPos> structurePositions(BlockPos pos) {
        List<BlockPos> cells = new ArrayList<>(35);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            if (!cell.equals(pos)) cells.add(cell.immutable());
        }
        return cells;
    }
}
