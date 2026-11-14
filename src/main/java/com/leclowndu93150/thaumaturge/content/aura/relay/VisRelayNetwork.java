package com.leclowndu93150.thaumaturge.content.aura.relay;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class VisRelayNetwork {
    private static final int RANGE = 8;
    static final int MAX_HOPS = 16;

    private VisRelayNetwork() {}

    public static List<Connection> connections(ServerLevel level, BlockPos consumer) {
        var seenSources = new HashSet<BlockPos>();
        List<Connection> result = new ArrayList<>();
        for (var relay : nearby(level, consumer, VisRelayBlockEntity.class)) {
            Connection connection = resolve(level, relay).connection();
            if (connection != null && seenSources.add(connection.node.getBlockPos())) result.add(connection);
        }
        return result;
    }

    public static @Nullable Connection nearest(ServerLevel level, BlockPos consumer) {
        for (var relay : nearby(level, consumer, VisRelayBlockEntity.class)) {
            Connection connection = resolve(level, relay).connection();
            if (connection != null) return connection;
        }
        return null;
    }

    public static int drainEverySourceNear(ServerLevel level, BlockPos consumer, ResourceLocation aspect, int amount) {
        int drained = 0;
        for (Connection source : connections(level, consumer)) {
            if (drained >= amount) break;
            drained += source.drain(level, aspect, amount - drained);
        }
        return drained;
    }

    public static int drainNodesNear(ServerLevel level, BlockPos consumer, ResourceLocation aspect, int amount) {
        int drained = 0;
        for (BlockEntityNode node : nearby(level, consumer, BlockEntityNode.class)) {
            if (drained >= amount) break;
            drained += node.drainCentivis(aspect, amount - drained);
        }
        return drained;
    }

    static void refreshLink(ServerLevel level, VisRelayBlockEntity relay) {
        Resolution current = resolve(level, relay);
        if (current.connection() != null || current.unloaded()) return;

        BlockPos origin = relay.getBlockPos();
        for (var node : nearby(level, origin, BlockEntityNode.class)) {
            if (node.isEnergized() && sightline(level, origin, node.getBlockPos()) == Sightline.CLEAR) {
                relay.setLink(node.getBlockPos(), 1);
                return;
            }
        }
        var upstream = nearby(level, origin, VisRelayBlockEntity.class).stream()
                .filter(other -> other.depth() > 0 && other.depth() < MAX_HOPS
                        && sightline(level, origin, other.getBlockPos()) == Sightline.CLEAR
                        && resolve(level, other).connection() != null)
                .min(Comparator.comparingInt(VisRelayBlockEntity::depth))
                .orElse(null);
        if (upstream == null) relay.setLink(null, 0);
        else relay.setLink(upstream.getBlockPos(), upstream.depth() + 1);
    }

    private static Resolution resolve(ServerLevel level, VisRelayBlockEntity relay) {
        Hop route = null;
        int depth = relay.depth();
        for (int remaining = depth; remaining > 0 && depth <= MAX_HOPS; remaining--) {
            BlockPos pos = relay.getBlockPos();
            BlockPos parent = relay.parent();
            if (parent == null || pos.equals(parent) || !inRange(pos, parent)) return Resolution.INVALID;
            if (!level.hasChunkAt(parent)) return Resolution.UNLOADED;
            Sightline sightline = sightline(level, pos, parent);
            if (sightline == Sightline.UNLOADED) return Resolution.UNLOADED;
            if (sightline == Sightline.BLOCKED) return Resolution.INVALID;

            route = new Hop(pos, route);
            BlockEntity next = level.getBlockEntity(parent);
            if (remaining == 1 && next instanceof BlockEntityNode node && node.isEnergized() && !node.isRemoved()) {
                return new Resolution(new Connection(node, route), false);
            }
            if (!(next instanceof VisRelayBlockEntity upstream) || upstream.isRemoved() || upstream.depth() != remaining - 1) {
                return Resolution.INVALID;
            }
            relay = upstream;
        }
        return Resolution.INVALID;
    }

    private static boolean inRange(BlockPos from, BlockPos to) {
        return Math.abs(from.getX() - to.getX()) <= RANGE
                && Math.abs(from.getY() - to.getY()) <= RANGE
                && Math.abs(from.getZ() - to.getZ()) <= RANGE;
    }

    private static <T extends BlockEntity> List<T> nearby(ServerLevel level, BlockPos origin, Class<T> type) {
        // Inspect block entities in loaded chunks without loading chunks to discover connections.
        List<T> found = new ArrayList<>();
        for (int x = (origin.getX() - RANGE) >> 4; x <= (origin.getX() + RANGE) >> 4; x++) {
            for (int z = (origin.getZ() - RANGE) >> 4; z <= (origin.getZ() + RANGE) >> 4; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (!entity.isRemoved() && !pos.equals(origin) && inRange(origin, pos) && type.isInstance(entity)) {
                        found.add(type.cast(entity));
                    }
                }
            }
        }
        found.sort(Comparator.<T>comparingDouble(entity -> entity.getBlockPos().distSqr(origin))
                .thenComparing(BlockEntity::getBlockPos));
        return found;
    }

    private static Sightline sightline(ServerLevel level, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        return BlockGetter.traverseBlocks(start, end, level, (world, point) -> {
            if (point.equals(from) || point.equals(to)) return null;
            if (!world.hasChunkAt(point)) return Sightline.UNLOADED;
            var shape = world.getBlockState(point).getCollisionShape(world, point);
            return shape.clip(start, end, point) == null ? null : Sightline.BLOCKED;
        }, world -> Sightline.CLEAR);
    }

    public static final class Connection {
        private final BlockEntityNode node;
        private final Hop route;

        private Connection(BlockEntityNode node, Hop route) {
            this.node = node;
            this.route = route;
        }

        public int available(ResourceLocation aspect) {
            return node.availableCentivis(aspect);
        }

        public int available() {
            return WandCharge.PRIMALS.stream().mapToInt(this::available).sum();
        }

        public int drain(ServerLevel level, ResourceLocation aspect, int amount) {
            int drawn = node.drainCentivis(aspect, amount);
            if (drawn > 0) {
                var entry = TTAspects.get(aspect);
                int color = entry == null ? 0xFFFFFF : entry.color();
                for (Hop hop = route; hop != null; hop = hop.previous()) {
                    if (level.getBlockEntity(hop.pos()) instanceof VisRelayBlockEntity relay) {
                        relay.pulse(level.getGameTime(), color);
                    }
                }
            }
            return drawn;
        }
    }

    private enum Sightline { CLEAR, BLOCKED, UNLOADED }

    private record Hop(BlockPos pos, @Nullable Hop previous) {}

    private record Resolution(@Nullable Connection connection, boolean unloaded) {
        private static final Resolution INVALID = new Resolution(null, false);
        private static final Resolution UNLOADED = new Resolution(null, true);
    }
}
