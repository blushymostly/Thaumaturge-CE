package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.content.taint.EtherealBloom;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.decor.BlockObsidianTotem;
import com.leclowndu93150.thaumaturge.content.world.tree.BlockSilverwoodNodeLog;
import com.leclowndu93150.thaumaturge.content.world.TTBiomeKeys;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BlockEntityNode extends AbstractSyncedBlockEntity {
    public static final String NBT_NODE = "Node";

    private static final float RAW_PER_POINT = 3.0F;
    private static final float REFILL_SUCCESS_THRESHOLD = 2.99F;
    private static final int DEGRADE_AFTER_FAILURES = 10;
    private static final int DECAY_INTERVAL = 1200;
    private static final int DRAIN_LINGER = 12;
    private static final int DISCHARGE_RANGE = 4;

    private NodeType type = NodeType.NORMAL;
    @Nullable
    private NodeModifier modifier;
    private AspectList held = AspectList.EMPTY;
    private AspectList aspectsBase = AspectList.EMPTY;

    private @Nullable AspectList originalCapacity;
    private final Map<ResourceLocation, Integer> relayAllowance = new HashMap<>();
    private final Map<ResourceLocation, Integer> relayCredit = new HashMap<>();

    private int tickCounter;
    private int starvation;
    private int refillWait;
    private int taintPurifyTicks;
    private @Nullable DrainEffect drainEffect;
    private @Nullable NodeJarRitual.Capture capture;

    public boolean isCapturing() {
        return capture != null;
    }

    void beginCapture(NodeJarRitual.Capture ritual) {
        capture = ritual;
        drainEffect = null;
        setChangedAndSync();
    }

    void preserveCaptureShellBlock(BlockPos pos) {
        if (capture != null && capture.preserve(pos))
            setChanged();
    }

    public record DrainEffect(UUID player, int color, long expiresAt) {
    }

    public @Nullable DrainEffect drainEffect() {
        return drainEffect != null && level != null && level.getGameTime() < drainEffect.expiresAt() ? drainEffect : null;
    }

    public void recordDrain(Player player, int color) {
        if (level instanceof ServerLevel server) {
            drainEffect = new DrainEffect(player.getUUID(), color, server.getGameTime() + DRAIN_LINGER);
            syncToClient();
        }
    }

    public BlockEntityNode(BlockPos pos, BlockState state) {
        this(TTBlockEntities.NODE.get(), pos, state);
    }

    protected BlockEntityNode(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public NodeType kind() {
        return type;
    }

    @Nullable
    public NodeModifier trait() {
        return modifier;
    }

    public AspectList getAspects() {
        return held;
    }

    public AspectList capacity() {
        return aspectsBase;
    }

    public int drain(ResourceLocation aspect, int amount) {
        if (isCapturing())
            return 0;
        int taken = Math.min(Math.max(0, amount), held.amountOf(aspect));
        if (taken > 0) {
            held = held.remove(aspect, taken);
            setChangedAndSync();
        }
        return taken;
    }

    public boolean isEnergized() {
        return originalCapacity != null;
    }

    void setEnergized(boolean energized) {
        if (energized == isEnergized())
            return;
        relayAllowance.clear();
        relayCredit.clear();
        if (energized) {
            originalCapacity = aspectsBase;
            Map<ResourceLocation, Integer> primals = new java.util.LinkedHashMap<>();
            for (AspectInstance aspect : aspectsBase.entries()) {
                collectPrimals(aspect.aspectId(), aspect.amount(), primals, new java.util.HashSet<>());
            }
            aspectsBase = AspectList.EMPTY;
            for (var aspect : primals.entrySet()) {
                aspectsBase = aspectsBase.add(new AspectInstance(aspect.getKey(), aspect.getValue()));
            }
            held = aspectsBase;
        } else {
            aspectsBase = originalCapacity;
            originalCapacity = null;
            held = AspectList.ofEntries(held.entries().stream().filter(entry -> aspectsBase.amountOf(entry.aspectId()) > 0)
                    .map(entry -> entry.withAmount(Math.min(entry.amount(), aspectsBase.amountOf(entry.aspectId())))).toList());
        }
        setChangedAndSync();
    }

    public int availableCentivis(ResourceLocation aspect) {
        if (!isEnergized() || !WandCharge.PRIMALS.contains(aspect))
            return 0;
        return Math.min(relayAllowance.getOrDefault(aspect, 0), held.amountOf(aspect) * WandCharge.CENTIVIS_PER_VIS + relayCredit.getOrDefault(aspect, 0));
    }

    public int drainCentivis(ResourceLocation aspect, int request) {
        int taken = Math.min(availableCentivis(aspect), Math.max(0, request));
        if (taken == 0)
            return 0;
        int credit = relayCredit.getOrDefault(aspect, 0);
        int unit = WandCharge.CENTIVIS_PER_VIS;
        int points = Math.max(0, (taken - credit + unit - 1) / unit);
        relayAllowance.put(aspect, relayAllowance.get(aspect) - taken);
        relayCredit.put(aspect, credit + points * unit - taken);
        if (points == 0)
            setChanged();
        else
            drain(aspect, points);
        return taken;
    }

    private void tickEnergized(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.above()).is(TTBulkContent.BLOCKS.get("node_transducer").get())) {
            setEnergized(false);
            return;
        }
        boolean refilled = false;
        for (AspectInstance entry : aspectsBase.entries()) {
            ResourceLocation aspect = entry.aspectId();
            int rate = entry.amount();
            relayAllowance.compute(aspect, (id, allowance) -> Math.min(rate * 200, (allowance == null ? 0 : allowance) + rate));
            if (level.getGameTime() % 20 != 0)
                continue;
            for (int attempt = 0; attempt < Math.max(1, rate / 20) && held.amountOf(aspect) < rate; attempt++) {
                boolean flux = type == NodeType.TAINTED;
                float cost = ThaumaturgeCommonConfig.ENERGIZED_NODE_VIS_PER_POINT.get().floatValue();
                float available = flux ? AuraHelper.getFlux(level, pos) : AuraHelper.getVis(level, pos);
                if (available < cost)
                    break;
                if (flux)
                    AuraHelper.drainFlux(level, pos, cost, false);
                else
                    AuraHelper.drainVis(level, pos, cost, false);
                held = held.add(new AspectInstance(aspect, 1));
                refilled = true;
            }
        }
        if (refilled)
            setChangedAndSync();
    }

    public NodeData nodeData() {
        return new NodeData(type, Optional.ofNullable(modifier), held, aspectsBase);
    }

    public void applyNodeData(NodeData data) {
        originalCapacity = null;
        drainEffect = null;
        relayAllowance.clear();
        relayCredit.clear();
        changeType(data.type());
        modifier = data.modifier().orElse(null);
        held = data.aspects();
        aspectsBase = data.aspectsBase();
        starvation = 0;
        refillWait = 0;
        taintPurifyTicks = 0;
        setChangedAndSync();
    }

    /**
     * CE {@code burstIntoOrbs} Make-it-Work: grant held aspects the breaker can receive,
     * drop essentia crystals for the rest, then clear. No EntityAspectOrb yet.
     */
    public void burstAndClear(@Nullable Player breaker) {
        if (!(level instanceof ServerLevel server)) {
            held = AspectList.EMPTY;
            return;
        }
        if (!held.isEmpty()) {
            ServerPlayer recipient = nearestPlayer(server, breaker);
            AspectList remaining = AspectList.EMPTY;
            for (AspectInstance entry : held.entries()) {
                if (recipient != null && AspectPools.hasDiscoveredComponents(recipient, entry.aspectId())) {
                    AspectPools.grant(recipient, entry.aspectId(), entry.amount());
                } else {
                    remaining = remaining.add(entry);
                }
            }
            for (AspectInstance entry : remaining.entries()) {
                int count = Math.min(Math.max(entry.amount(), 1), 8);
                Block.popResource(server, worldPosition, EssentiaCrystalFactory.of(entry.aspectId(), count));
            }
            emitBurstFx(server);
        }
        held = AspectList.EMPTY;
        setChanged();
    }

    @Nullable
    private ServerPlayer nearestPlayer(ServerLevel level, @Nullable Player preferred) {
        if (preferred instanceof ServerPlayer serverPlayer && serverPlayer.level() == level) {
            return serverPlayer;
        }
        AABB box = new AABB(worldPosition).inflate(8.0);
        ServerPlayer closest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            double dist = player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            if (dist < best) {
                best = dist;
                closest = player;
            }
        }
        return closest;
    }

    private void emitBurstFx(ServerLevel level) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        for (int i = 0; i < 12; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 0.35;
            double vy = level.random.nextDouble() * 0.25;
            double vz = (level.random.nextDouble() - 0.5) * 0.35;
            int color = 0xFF00FFFF | (level.random.nextInt(0x40) << 16);
            level.sendParticles(new SparkleParticleOptions(color, 0.35F, 0, 0.9F, 0.02F, 20, true), x, y, z, 0, vx, vy, vz, 1.0);
        }
    }

    public void serverTick(Level tickLevel, BlockPos pos) {
        if (!(tickLevel instanceof ServerLevel serverLevel)) {
            return;
        }
        if (capture != null) {
            capture.tick(serverLevel, this);
            return;
        }
        if (isEnergized()) {
            tickEnergized(serverLevel, pos);
            return;
        }
        tickCounter++;
        if (this instanceof JarNodeBlock.Entity) {
            tickRefill(serverLevel, pos, 2);
            tickDecay(serverLevel, pos);
            return;
        }
        int tier = NodeTransducer.stabilizerTier(tickLevel, pos.below());
        boolean protectedFromTaint = type == NodeType.TAINTED && tickTaintProtection(serverLevel, pos);
        if (!protectedFromTaint)
            tickRefill(serverLevel, pos, refillFactor(tier));
        tickDischarge(serverLevel, pos, tier);
        if (tickDecay(serverLevel, pos) || protectedFromTaint)
            return;
        if (type == NodeType.HUNGRY) {
            NodeTypeEffects.attract(this, serverLevel, pos);
        }
        if (tier > 0 && tickCounter % 100 == 0) {
            if (type == NodeType.UNSTABLE && serverLevel.random.nextBoolean() && serverLevel.random.nextInt(tier == 2 ? 5000 : 10000) == 0) {
                changeType(NodeType.NORMAL);
                setChangedAndSync();
            }
            if (modifier == NodeModifier.FADING && serverLevel.random.nextInt(tier == 2 ? 6250 : 12500) == 0) {
                modifier = NodeModifier.PALE;
                setChangedAndSync();
            }
        }
        if ((tier == 0 || type != NodeType.UNSTABLE) && tickCounter % 50 == 0) {
            NodeTypeEffects.tick(this, serverLevel, pos, tickCounter);
        }
        NodeBiomeSpread.tick(serverLevel, type, pos, tickCounter);
    }

    private void changeType(NodeType next) {
        NodeType previous = type;
        type = next;
        if (previous != next && !(this instanceof JarNodeBlock.Entity) && level instanceof ServerLevel server) {
            NodeBiomeSpread.revert(server, worldPosition, previous);
        }
    }

    private static int refillFactor(int tier) {
        return tier == 2 ? 20 : tier == 1 ? 2 : 1;
    }

    private void tickRefill(ServerLevel level, BlockPos pos, int factor) {
        if (refillWait > 0)
            refillWait--;
        int interval = NodeRules.baseRefillInterval(modifier) * factor;
        if (interval <= 0 || tickCounter % interval != 0 || refillWait > 0)
            return;
        adaptToAura(level, pos);
        refill(level, pos);
    }

    private void adaptToAura(ServerLevel level, BlockPos pos) {
        int base = AuraHelper.getAuraBase(level, pos);
        if (base <= 0)
            return;
        float flux = AuraHelper.getFlux(level, pos);
        RandomSource random = level.getRandom();
        if (type != NodeType.TAINTED && type != NodeType.PURE && flux > 0.5F * base && random.nextInt(20) == 0 && !EtherealBloom.protects(level, pos)) {
            changeType(NodeType.TAINTED);
            setChangedAndSync();
        }
        if (modifier != NodeModifier.BRIGHT && flux < 0.1F * base && AuraHelper.getVis(level, pos) >= 0.9F * base && level.getBiome(pos).is(TTBiomeKeys.MAGICAL_FOREST) && random.nextInt(50) == 0) {
            modifier = NodeRules.improve(modifier);
            setChangedAndSync();
        }
    }

    private void tickDischarge(ServerLevel level, BlockPos pos, int tier) {
        if (tier == 1 || modifier == NodeModifier.FADING)
            return;
        RandomSource random = level.getRandom();
        int interval = modifier == NodeModifier.BRIGHT || type == NodeType.HUNGRY && modifier != null ? 1 : modifier == NodeModifier.PALE ? 3 : 2;
        if (tickCounter % interval != 0 || modifier == NodeModifier.PALE && random.nextBoolean())
            return;

        BlockPos donorPos = pos.offset(dischargeOffset(random), dischargeOffset(random), dischargeOffset(random));
        if (donorPos.equals(pos) || !level.hasChunkAt(donorPos) || !(level.getBlockEntity(donorPos) instanceof BlockEntityNode donor) || donor instanceof JarNodeBlock.Entity || donor.isCapturing()
                || donor.held.isEmpty() || NodeTransducer.stabilizerTier(level, donorPos.below()) != 0 || donor.averageContent() >= averageContent())
            return;

        AspectInstance offered = donor.held.entries().get(random.nextInt(donor.held.size()));
        ResourceLocation aspect = offered.aspectId();
        donor.held = donor.held.remove(aspect, 1);
        int base = aspectsBase.amountOf(aspect);
        if (held.amountOf(aspect) < base) {
            held = held.add(new AspectInstance(aspect, 1));
        } else if (base < Integer.MAX_VALUE) {
            boolean elevated = type == NodeType.HUNGRY || modifier == NodeModifier.BRIGHT;
            int divisor = 1 + (elevated ? (int) (base / 1.5) : base);
            if (random.nextInt(divisor) == 0) {
                aspectsBase = aspectsBase.add(new AspectInstance(aspect, 1));
                if (modifier == NodeModifier.PALE && random.nextInt(100) == 0)
                    modifier = null;
                if (random.nextInt(3) == 0)
                    donor.aspectsBase = donor.aspectsBase.remove(aspect, 1);
            }
        }

        if (donor.held.isEmpty() && !donor.isEnergized()) {
            if (donor.getBlockState().getBlock() instanceof BlockNode)
                level.destroyBlock(donorPos, true);
            else
                donor.removeDepleted(level, donorPos);
        } else {
            donor.refillWait = NodeRules.baseRefillInterval(donor.modifier) / 2;
            donor.setChangedAndSync();
        }
        setChangedAndSync();
        level.playSound(null, donorPos, TTSounds.ZAP.get(), SoundSource.BLOCKS, 0.1F, 1 + random.nextFloat() * 0.2F);
        BoltParticleOptions.strike(level, Vec3.atCenterOf(donorPos), Vec3.atCenterOf(pos), 0.3F);
    }

    private static int dischargeOffset(RandomSource random) {
        return random.nextInt(DISCHARGE_RANGE + 1) - random.nextInt(DISCHARGE_RANGE + 1);
    }

    private long averageContent() {
        long stored = held.entries().stream().mapToLong(AspectInstance::amount).sum();
        long capacity = aspectsBase.entries().stream().mapToLong(AspectInstance::amount).sum();
        return (stored + capacity) / 2;
    }

    private boolean tickDecay(ServerLevel level, BlockPos pos) {
        if (tickCounter % DECAY_INTERVAL != 0)
            return false;
        for (AspectInstance entry : aspectsBase.entries()) {
            if (held.amountOf(entry.aspectId()) > 0)
                continue;
            boolean lost = entry.amount() == 1 || level.random.nextInt(20) == 0;
            aspectsBase = aspectsBase.remove(entry.aspectId(), lost ? entry.amount() : 1);
            if (lost) {
                if (level.random.nextInt(5) == 0 && modifier != NodeModifier.PALE && modifier != NodeModifier.FADING) {
                    modifier = NodeRules.degrade(modifier);
                }
                if (modifier == NodeModifier.PALE && level.random.nextInt(5) == 0)
                    modifier = NodeModifier.FADING;
            }
            if (aspectsBase.isEmpty()) {
                removeDepleted(level, pos);
                return true;
            }
            setChangedAndSync();
            break;
        }
        return false;
    }

    private boolean tickTaintProtection(ServerLevel level, BlockPos pos) {
        boolean protectedByBloom = EtherealBloom.protects(level, pos);
        if (tickCounter % 20 == 0) {
            if (protectedByBloom) {
                taintPurifyTicks += 20;
                if (taintPurifyTicks >= ThaumaturgeCommonConfig.TAINT_STARVE_TICKS.get()) {
                    applyNodeData(new NodeData(NodeType.NORMAL, Optional.ofNullable(modifier), held, aspectsBase));
                }
                setChanged();
            } else if (taintPurifyTicks != 0) {
                taintPurifyTicks = 0;
                setChanged();
            }
        }
        return protectedByBloom;
    }

    void feed(AspectList food, net.minecraft.util.RandomSource random) {
        if (isCapturing())
            return;
        java.util.Map<ResourceLocation, Integer> primals = new java.util.LinkedHashMap<>();
        for (AspectInstance aspect : food.entries()) {
            collectPrimals(aspect.aspectId(), aspect.amount(), primals, new java.util.HashSet<>());
        }
        if (primals.isEmpty())
            return;
        var chosen = new java.util.ArrayList<>(primals.entrySet()).get(random.nextInt(primals.size()));
        int capacity = aspectsBase.amountOf(chosen.getKey());
        if (held.amountOf(chosen.getKey()) < capacity) {
            held = held.add(new AspectInstance(chosen.getKey(), 1));
        } else if (random.nextFloat() < chosen.getValue() / (1.0F + capacity * 10)) {
            aspectsBase = aspectsBase.add(new AspectInstance(chosen.getKey(), 1));
        } else {
            return;
        }
        setChangedAndSync();
    }

    private static void collectPrimals(ResourceLocation id, int amount, java.util.Map<ResourceLocation, Integer> totals, java.util.Set<ResourceLocation> path) {
        var aspect = com.leclowndu93150.thaumaturge.api.aspect.TTAspects.get(id);
        if (aspect == null || !path.add(id))
            return;
        if (aspect.isPrimal())
            totals.merge(id, amount, Integer::sum);
        else
            for (ResourceLocation component : aspect.componentIds())
                collectPrimals(component, amount, totals, path);
        path.remove(id);
    }

    boolean erode(ServerLevel level, BlockPos pos) {
        if (!aspectsBase.isEmpty()) {
            var entries = aspectsBase.entries();
            var chosen = entries.get(level.random.nextInt(entries.size())).aspectId();
            aspectsBase = aspectsBase.remove(chosen, 1);
            int surplus = held.amountOf(chosen) - aspectsBase.amountOf(chosen);
            if (surplus > 0) {
                held = held.remove(chosen, surplus);
            }
            setChangedAndSync();
        }
        if (aspectsBase.isEmpty()) {
            removeDepleted(level, pos);
            return true;
        }
        return false;
    }

    private void removeDepleted(ServerLevel level, BlockPos pos) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof BlockSilverwoodNodeLog) {
            level.setBlockAndUpdate(pos, BlockSilverwoodNodeLog.depletedState(state));
        } else if (state.is(TTBulkContent.BLOCKS.get("obsidian_totem_charged").get())) {
            BlockState totem = TTBulkContent.BLOCKS.get("obsidian_totem").get().defaultBlockState();
            level.setBlockAndUpdate(pos, BlockObsidianTotem.shapedState(totem, level, pos));
        } else {
            level.removeBlock(pos, false);
        }
    }

    private void refill(ServerLevel level, BlockPos pos) {
        List<ResourceLocation> eligible = new ArrayList<>();
        for (AspectInstance entry : held.entries()) {
            if (entry.amount() < aspectsBase.amountOf(entry.aspectId())) {
                eligible.add(entry.aspectId());
            }
        }
        if (eligible.isEmpty()) {
            starvation = 0;
            return;
        }
        ResourceLocation chosen = eligible.get(level.getRandom().nextInt(eligible.size()));
        boolean flux = type == NodeType.TAINTED;
        float taken = flux ? AuraHelper.drainFlux(level, pos, RAW_PER_POINT, false) : AuraHelper.drainVis(level, pos, RAW_PER_POINT, false);
        if (taken >= REFILL_SUCCESS_THRESHOLD) {
            held = held.add(new AspectInstance(chosen, 1));
            starvation = 0;
            setChangedAndSync();
            return;
        }
        if (taken > 0.0F) {
            if (flux) {
                AuraHelper.addFlux(level, pos, taken);
            } else {
                AuraHelper.addVis(level, pos, taken);
            }
        }
        if (++starvation >= DEGRADE_AFTER_FAILURES) {
            starvation = 0;
            if (NodeTransducer.stabilizerTier(level, pos.below()) == 0)
                modifier = NodeRules.degrade(modifier);
            setChangedAndSync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_NODE, nodeData().toTag());
        if (capture != null)
            tag.put("JarCapture", capture.save());
        if (originalCapacity != null) {
            AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, originalCapacity).result().ifPresent(encoded -> tag.put("OriginalCapacity", encoded));
            CompoundTag credit = new CompoundTag();
            relayCredit.forEach((aspect, amount) -> {
                if (amount > 0)
                    credit.putInt(aspect.toString(), amount);
            });
            tag.put("RelayCredit", credit);
        }
        if (taintPurifyTicks > 0)
            tag.putInt("TaintPurifyTicks", taintPurifyTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        taintPurifyTicks = tag.getInt("TaintPurifyTicks");
        relayAllowance.clear();
        relayCredit.clear();
        CompoundTag credit = tag.getCompound("RelayCredit");
        for (String key : credit.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int amount = credit.getInt(key);
            if (id != null && WandCharge.PRIMALS.contains(id) && amount > 0 && amount < WandCharge.CENTIVIS_PER_VIS)
                relayCredit.put(id, amount);
        }
        readNodeFromTag(tag);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        readNodeFromTag(tag);
        drainEffect = tag.hasUUID("DrainPlayer") ? new DrainEffect(tag.getUUID("DrainPlayer"), tag.getInt("DrainColor"), tag.getLong("DrainUntil")) : null;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readClientData(tag);
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        super.writeClientData(tag);
        DrainEffect effect = drainEffect();
        if (effect != null) {
            tag.putUUID("DrainPlayer", effect.player());
            tag.putInt("DrainColor", effect.color());
            tag.putLong("DrainUntil", effect.expiresAt());
        }
    }

    private void readNodeFromTag(CompoundTag tag) {
        capture = tag.contains("JarCapture", net.minecraft.nbt.Tag.TAG_COMPOUND) ? NodeJarRitual.Capture.load(tag.getCompound("JarCapture"), worldPosition) : null;
        originalCapacity = tag.contains("OriginalCapacity") ? AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("OriginalCapacity")).result().orElse(null) : null;
        NodeData data = NodeData.fromTag(tag.getCompound(NBT_NODE));
        if (data != null) {
            type = data.type();
            modifier = data.modifier().orElse(null);
            held = data.aspects();
            aspectsBase = data.aspectsBase();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.3);
    }
}
