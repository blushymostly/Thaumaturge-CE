package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerFx;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = TTIds.MODID)
public final class NodeJarRitual {
    private static final int COST_PER_PRIMAL = 70 * WandCharge.CENTIVIS_PER_VIS;
    private static final int SHELL_MARGIN = 20;
    private static final int SETTLE_TICKS = 15;

    private NodeJarRitual() {}

    public static boolean use(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (!level.getBlockState(clicked).is(Tags.Blocks.GLASS))
            return false;
        BlockPos target = null;
        for (BlockPos candidate : BlockPos.betweenClosed(clicked.offset(-1, -1, -1), clicked.offset(1, 1, 1))) {
            if (!level.hasChunkAt(candidate) || !level.getBlockState(candidate).is(TTBlocks.NODE.get()))
                continue;
            boolean complete = fitsStructure(level, candidate);
            if (target == null || complete)
                target = candidate.immutable();
            if (complete)
                break;
        }
        if (target == null)
            return false;
        Player player = context.getPlayer();
        if (level instanceof ServerLevel server && player != null) {
            capture(server, target, player, context.getHand(), context.getItemInHand());
        }
        return true;
    }

    private static boolean capture(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack wand) {
        if (!level.getBlockState(pos).is(TTBlocks.NODE.get()) || !(level.getBlockEntity(pos) instanceof BlockEntityNode node) || node.isEnergized() || node.isCapturing())
            return false;
        if (!TTAttachments.knowledge(player).isComplete(TTIds.rl("node_jar")))
            return reject(player, "no_research");
        if (!fitsStructure(level, pos))
            return reject(player, "structure");
        List<BlockPos> shell = structurePositions(pos);
        if (!level.mayInteract(player, pos) || shell.stream().anyMatch(cell -> !level.mayInteract(player, cell)))
            return false;
        if (WandCharge.PRIMALS.stream().anyMatch(aspect -> WandCharge.stored(wand, aspect) < COST_PER_PRIMAL)) {
            return reject(player, "vis");
        }

        Vec3 origin = player instanceof ServerPlayer caster ? DustTriggerFx.posToHand(caster, hand) : Vec3.atCenterOf(pos);
        List<ShellBlock> pending = new ArrayList<>();
        for (BlockPos cell : shell) {
            int delay = (int) (origin.distanceTo(Vec3.atCenterOf(cell)) * DustTriggerFx.SPARKLE_TICKS_PER_BLOCK);
            pending.add(new ShellBlock(cell, level.getBlockState(cell), SHELL_MARGIN + Mth.clamp(delay, 0, 160)));
        }
        WandCharge.PRIMALS.forEach(aspect -> WandCharge.spendExact(wand, aspect, COST_PER_PRIMAL));
        node.beginCapture(new Capture(pending, SETTLE_TICKS));
        level.playSound(null, pos, TTSounds.WAND.get(), SoundSource.BLOCKS, 1, 1);
        shell.forEach(cell -> DustTriggerFx.emitBlockSparkles(level, cell, origin));
        return true;
    }

    private static void finish(ServerLevel level, BlockEntityNode node) {
        BlockPos pos = node.getBlockPos();
        if (level.getBlockEntity(pos) != node)
            return;
        NodeModifier modifier = node.trait();
        if (level.random.nextFloat() < 0.75F)
            modifier = degrade(modifier);
        var data = new NodeData(node.kind(), Optional.ofNullable(modifier), node.getAspects(), node.capacity());
        Block jarBlock = TTBulkContent.BLOCKS.get("jar_node").get();
        if (!level.setBlock(pos, jarBlock.defaultBlockState(), Block.UPDATE_ALL))
            return;
        if (!(level.getBlockEntity(pos) instanceof JarNodeBlock.Entity jar)) {
            throw new IllegalStateException("Node jar block entity is missing after capture");
        }
        jar.applyNodeData(data);
        DustTriggerFx.emitSwapBamf(level, pos);
    }

    @SubscribeEvent
    public static void protectNodeDuringCapture(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof BlockEntityNode node && node.isCapturing()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void protectNodeDuringExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> event.getLevel().getBlockEntity(pos) instanceof BlockEntityNode node && node.isCapturing());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preserveReplacedShellBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            multi.getReplacedBlockSnapshots().forEach(snapshot -> preservePlacement(level, snapshot.getPos()));
        } else {
            preservePlacement(level, event.getPos());
        }
    }

    private static void preservePlacement(ServerLevel level, BlockPos pos) {
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-1, -2, -1), pos.offset(1, 1, 1))) {
            if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof BlockEntityNode node && node.isCapturing()) {
                node.preserveCaptureShellBlock(pos);
            }
        }
    }

    private record ShellBlock(BlockPos pos, BlockState state, int delay) {
    }

    static final class Capture {
        private final List<ShellBlock> pending;
        private int settle;
        private int elapsed;

        private Capture(List<ShellBlock> pending, int settle) {
            this.pending = pending;
            this.settle = settle;
        }

        void tick(ServerLevel level, BlockEntityNode node) {
            if (pending.isEmpty()) {
                if (settle > 0)
                    settle--;
                if (settle == 0)
                    finish(level, node);
                if (!node.isRemoved())
                    node.setChanged();
                return;
            }
            elapsed = Math.min(180, elapsed + 1);
            pending.removeIf(cell -> {
                if (cell.delay() > elapsed || !level.hasChunkAt(cell.pos()))
                    return false;
                if (level.getBlockState(cell.pos()) == cell.state()) {
                    level.levelEvent(2001, cell.pos(), Block.getId(cell.state()));
                    level.setBlockAndUpdate(cell.pos(), Blocks.AIR.defaultBlockState());
                }
                return true;
            });
            node.setChanged();
        }

        boolean preserve(BlockPos pos) {
            return pending.removeIf(cell -> cell.pos().equals(pos));
        }

        CompoundTag save() {
            var list = new ListTag();
            for (ShellBlock cell : pending) {
                var tag = new CompoundTag();
                tag.putLong("Pos", cell.pos().asLong());
                tag.put("State", NbtUtils.writeBlockState(cell.state()));
                tag.putInt("Delay", cell.delay());
                list.add(tag);
            }
            var tag = new CompoundTag();
            tag.put("Shell", list);
            tag.putInt("Elapsed", elapsed);
            tag.putInt("Settle", settle);
            return tag;
        }

        static Capture load(CompoundTag tag, BlockPos center) {
            List<ShellBlock> pending = new ArrayList<>();
            var shell = structurePositions(center);
            for (Tag entry : tag.getList("Shell", Tag.TAG_COMPOUND)) {
                CompoundTag cell = (CompoundTag) entry;
                BlockPos pos = BlockPos.of(cell.getLong("Pos"));
                if (shell.contains(pos)) {
                    var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), cell.getCompound("State"));
                    pending.add(new ShellBlock(pos, state, Mth.clamp(cell.getInt("Delay"), 0, 180)));
                }
            }
            Capture capture = new Capture(pending, Mth.clamp(tag.getInt("Settle"), 0, SETTLE_TICKS));
            capture.elapsed = Mth.clamp(tag.getInt("Elapsed"), 0, 180);
            return capture;
        }
    }

    private static boolean reject(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.thaumaturge.node_jar." + reason, 70), true);
        return false;
    }

    private static @Nullable NodeModifier degrade(@Nullable NodeModifier modifier) {
        if (modifier == null)
            return NodeModifier.PALE;
        return switch (modifier) {
            case BRIGHT -> null;
            case PALE -> NodeModifier.FADING;
            default -> modifier;
        };
    }

    public static boolean fitsStructure(Level level, BlockPos pos) {
        return structurePositions(pos).stream().allMatch(cell -> level.hasChunkAt(cell) && level.getBlockState(cell).is(cell.getY() == pos.getY() + 2 ? BlockTags.WOODEN_SLABS : Tags.Blocks.GLASS));
    }

    public static List<BlockPos> structurePositions(BlockPos pos) {
        List<BlockPos> cells = new ArrayList<>(35);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            if (!cell.equals(pos))
                cells.add(cell.immutable());
        }
        return cells;
    }
}
