package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Counts down and lands queued dust swaps; re-arms queues stored in chunks on load. */
@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DustTriggerTickHandler {
    private DustTriggerTickHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        DustTriggerSwapQueue queue = TTAttachments.dustQueue(chunk);
        if (queue.isEmpty()) {
            return;
        }
        DustTriggerSwapQueue.markChunkActive(level, chunk.getPos());
        for (PendingSwap entry : queue.entries()) {
            DustTriggerSwapQueue.markBlocked(level, entry.pos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DustTriggerSwapQueue.resetSession();
    }

    private static void tickLevel(ServerLevel level) {
        Set<ChunkPos> active = DustTriggerSwapQueue.activeChunks(level);
        if (active.isEmpty()) {
            return;
        }
        for (ChunkPos cp : active) {
            if (!level.hasChunk(cp.x, cp.z)) {
                continue;
            }
            LevelChunk chunk = level.getChunk(cp.x, cp.z);
            DustTriggerSwapQueue queue = TTAttachments.dustQueue(chunk);
            if (queue.isEmpty()) {
                DustTriggerSwapQueue.markChunkClear(level, cp);
                continue;
            }
            tickChunk(level, chunk, queue);
        }
    }

    private static void tickChunk(ServerLevel level, LevelChunk chunk, DustTriggerSwapQueue queue) {
        List<PendingSwap> entries = queue.entries();
        List<PendingSwap> snapshot = new ArrayList<>(entries);
        List<PendingSwap> next = new ArrayList<>(snapshot.size());
        List<PendingSwap> done = new ArrayList<>();
        for (PendingSwap entry : snapshot) {
            int remaining = entry.ticksRemaining() - 1;
            if (remaining <= 0) {
                done.add(entry);
            } else {
                next.add(entry.withTicks(remaining));
            }
        }
        entries.clear();
        entries.addAll(next);
        chunk.setUnsaved(true);
        for (PendingSwap entry : done) {
            complete(level, entry);
        }
        if (queue.isEmpty()) {
            DustTriggerSwapQueue.markChunkClear(level, chunk.getPos());
        }
    }

    private static void complete(ServerLevel level, PendingSwap entry) {
        BlockPos pos = entry.pos();
        if (!level.getBlockState(pos).is(entry.originalState().getBlock())) {
            DustTriggerSwapQueue.clearBlocked(level, pos);
            return;
        }
        BlockState resultState = entry.stateResult();
        ItemStack resultStack = entry.stackResult();
        if (resultState != null) {
            level.setBlock(pos, resultState, 3);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (resultStack != null && !resultStack.isEmpty()) {
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, resultStack.copy());
                drop.setDeltaMovement(0.0, 0.0, 0.0);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
        }
        DustTriggerFx.emitSwapBamf(level, pos);
        DustTriggerSwapQueue.clearBlocked(level, pos);
    }
}
