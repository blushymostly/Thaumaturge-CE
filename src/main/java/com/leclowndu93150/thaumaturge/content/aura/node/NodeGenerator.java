package com.leclowndu93150.thaumaturge.content.aura.node;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.BiomeAuraModifiers;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Rolls type, modifier and aspect budget for a natural aura node. */
public final class NodeGenerator {
    public static final int DEFAULT_SPECIAL_RARITY = 18;
    public static final int DEFAULT_BASE_AURA = 100;

    private static final double PERCENT_TOTAL = 100.0;
    private static final double CHANCE_DARK = 5.0 / 3.0;
    private static final double CHANCE_UNSTABLE = 5.0 / 3.0;
    private static final double CHANCE_PURE = 5.0 / 3.0;
    private static final double CHANCE_TAINTED = 10.0 / 9.0;
    private static final double CHANCE_HUNGRY = 5.0 / 9.0;
    private static final int MODIFIER_RARITY_DIVISOR = 2;
    private static final float TAINTED_BIOME_FACTOR = 1.5F;
    private static final float SMALL_NODE_DIVISOR = 4.0F;
    private static final int MINIMUM_AURA = 8;
    private static final int BONUS_ASPECT_ROLLS = 3;
    private static final double BONUS_NONE_WEIGHT = 0.5;
    private static final int[] FACTORIALS = {1, 1, 2, 6};
    private static final float WEIGHT_FLOOR = 0.05F;
    private static final NodeModifier[] MODIFIERS = NodeModifier.values();
    private static final List<ResourceLocation> DARK_ASPECTS = List.of(TTAspects.MORTUUS, TTAspects.EXANIMIS, TTAspects.PERDITIO, TTAspects.TENEBRAE);

    private NodeGenerator() {}

    public static boolean createRandomNodeAt(ServerLevelAccessor level, BlockPos pos, RandomSource random, boolean silverwood, boolean eerie, boolean small, int specialRarity, int baseAura) {
        NodeData data = rollRandomNodeData(level, pos, random, silverwood, eerie, small, specialRarity, baseAura);
        return createNodeAt(level, pos, data);
    }

    public static boolean createLandmarkNodeAt(ServerLevelAccessor level, BlockPos pos, RandomSource random, NodeType type) {
        var data = rollNodeData(level, pos, random, false, false, false, DEFAULT_SPECIAL_RARITY, DEFAULT_BASE_AURA, type);
        return createNodeAt(level, pos, data);
    }

    public static boolean createNodeAt(ServerLevelAccessor level, BlockPos pos, NodeData data) {
        BlockEntity existing = level.getBlockEntity(pos);
        BlockEntityNode target = existing instanceof BlockEntityNode node ? node : placeNodeBlock(level, pos);
        if (target == null) {
            return false;
        }
        target.applyNodeData(data);
        target.setChanged();
        return true;
    }

    @Nullable
    private static BlockEntityNode placeNodeBlock(ServerLevelAccessor level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        boolean free = !current.getFluidState().is(FluidTags.WATER) && (current.isAir() || current.canBeReplaced() || current.is(BlockTags.LEAVES));
        if (!free || !level.setBlock(pos, TTBlocks.NODE.get().defaultBlockState(), Block.UPDATE_ALL)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof BlockEntityNode placed ? placed : null;
    }

    public static NodeData rollRandomNodeData(ServerLevelAccessor level, BlockPos pos, RandomSource random, boolean silverwood, boolean eerie, boolean small, int specialRarity, int baseAura) {
        return rollNodeData(level, pos, random, silverwood, eerie, small, specialRarity, baseAura, null);
    }

    private static NodeData rollNodeData(ServerLevelAccessor level, BlockPos pos, RandomSource random, boolean silverwood,
            boolean eerie, boolean small, int specialRarity, int baseAura, @Nullable NodeType forcedType) {
        ResourceLocation biomeId = level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
        float biomeStrength = baseAura * (biomeId == null ? BiomeAuraModifiers.DEFAULT_MODIFIER : BiomeAuraModifiers.get(biomeId));

        NodeType type = forcedType;
        if (type == null) type = silverwood ? NodeType.PURE : eerie ? NodeType.DARK : rollType(random, specialRarity);
        NodeModifier modifier = random.nextInt(Math.max(1, specialRarity / MODIFIER_RARITY_DIVISOR)) == 0 ? MODIFIERS[random.nextInt(MODIFIERS.length)] : null;

        float strength = biomeStrength;
        if (type != NodeType.PURE && biomeId != null && BiomeAuraModifiers.isTainted(biomeId)) {
            strength *= TAINTED_BIOME_FACTOR;
            if (forcedType == NodeType.TAINTED || forcedType == null && random.nextBoolean()) {
                type = NodeType.TAINTED;
                strength *= TAINTED_BIOME_FACTOR;
            }
        }
        if (silverwood || small) {
            strength /= SMALL_NODE_DIVISOR;
        }
        int aura = Math.max(MINIMUM_AURA, (int) strength);
        int floor = aura / 2;
        int budget = floor + random.nextInt(Math.max(1, aura - floor));

        Map<ResourceLocation, Integer> tally = new LinkedHashMap<>();
        List<ResourceLocation> primals = new ArrayList<>();
        List<ResourceLocation> compounds = new ArrayList<>();
        for (Map.Entry<ResourceLocation, IAspect> entry : TTAspects.all().entrySet()) {
            (entry.getValue().isPrimal() ? primals : compounds).add(entry.getKey());
        }
        tally(tally, pick(primals, random), 1);
        tally(tally, pick(compounds, random), 1);
        int[] bonus = drawBonus(random, specialRarity);
        for (int i = 0; i < bonus[0]; i++) {
            tally(tally, pick(primals, random), 1);
        }
        for (int i = 0; i < bonus[1]; i++) {
            tally(tally, pick(compounds, random), 1);
        }
        typeFlavour(tally, type, random);
        terrainFlavour(level, pos, tally);
        allocateBudget(tally, budget, random);

        List<AspectInstance> entries = new ArrayList<>();
        tally.forEach((id, amount) -> {
            if (amount > 0) {
                entries.add(new AspectInstance(id, amount));
            }
        });
        AspectList list = AspectList.ofEntries(entries);
        return new NodeData(type, Optional.ofNullable(modifier), list, list);
    }

    private static void terrainFlavour(ServerLevelAccessor level, BlockPos origin, Map<ResourceLocation, Integer> tally) {
        int radius = 5;
        int water = 0;
        int lava = 0;
        int stone = 0;
        int leaves = 0;
        for (BlockPos cell : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!level.hasChunk(cell.getX() >> 4, cell.getZ() >> 4) || level.isOutsideBuildHeight(cell)) {
                continue;
            }
            BlockState state = level.getBlockState(cell);
            if (state.getFluidState().is(FluidTags.WATER)) {
                water++;
            } else if (state.getFluidState().is(FluidTags.LAVA)) {
                lava++;
            } else if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) {
                stone++;
            } else if (state.is(BlockTags.LEAVES)) {
                leaves++;
            }
        }
        if (water > 100) {
            tally(tally, TTAspects.AQUA, 1);
        }
        if (lava > 100) {
            tally(tally, TTAspects.IGNIS, 1);
            tally(tally, TTAspects.TERRA, 1);
        }
        if (stone > 500) {
            tally(tally, TTAspects.TERRA, 1);
        }
        if (leaves > 100) {
            tally(tally, TTAspects.HERBA, 1);
        }
    }

    private static NodeType rollType(RandomSource random, int specialRarity) {
        double scale = (double) DEFAULT_SPECIAL_RARITY / specialRarity;
        double dark = CHANCE_DARK * scale;
        double unstable = CHANCE_UNSTABLE * scale;
        double pure = CHANCE_PURE * scale;
        double tainted = CHANCE_TAINTED * scale;
        double hungry = CHANCE_HUNGRY * scale;
        double roll = random.nextDouble() * Math.max(PERCENT_TOTAL, dark + unstable + pure + tainted + hungry);
        if (roll < dark) {
            return NodeType.DARK;
        }
        roll -= dark;
        if (roll < unstable) {
            return NodeType.UNSTABLE;
        }
        roll -= unstable;
        if (roll < pure) {
            return NodeType.PURE;
        }
        roll -= pure;
        if (roll < tainted) {
            return NodeType.TAINTED;
        }
        roll -= tainted;
        return roll < hungry ? NodeType.HUNGRY : NodeType.NORMAL;
    }

    /** @return {primalCount, compoundCount} */
    private static int[] drawBonus(RandomSource random, int specialRarity) {
        double none = BONUS_NONE_WEIGHT;
        double compound = BONUS_NONE_WEIGHT / Math.max(1, specialRarity);
        double primal = BONUS_NONE_WEIGHT - compound;
        double roll = random.nextDouble();
        int[] last = {0, 0};
        for (int compounds = 0; compounds <= BONUS_ASPECT_ROLLS; compounds++) {
            for (int primals = 0; primals + compounds <= BONUS_ASPECT_ROLLS; primals++) {
                int empty = BONUS_ASPECT_ROLLS - primals - compounds;
                double arrangements = FACTORIALS[BONUS_ASPECT_ROLLS] / (FACTORIALS[primals] * FACTORIALS[compounds] * FACTORIALS[empty]);
                double weight = arrangements * Math.pow(primal, primals) * Math.pow(compound, compounds) * Math.pow(none, empty);
                last = new int[] {primals, compounds};
                if (roll < weight) {
                    return last;
                }
                roll -= weight;
            }
        }
        return last;
    }

    private static void typeFlavour(Map<ResourceLocation, Integer> tally, NodeType type, RandomSource random) {
        switch (type) {
            case HUNGRY -> {
                tally(tally, TTAspects.DESIDERIUM, 2);
                if (random.nextBoolean()) {
                    tally(tally, TTAspects.VACUOS, 1);
                }
            }
            case PURE -> tally(tally, random.nextBoolean() ? TTAspects.VICTUS : TTAspects.ORDO, 2);
            case DARK -> {
                for (ResourceLocation id : DARK_ASPECTS) {
                    if (random.nextBoolean()) {
                        tally(tally, id, 1);
                    }
                }
            }
            case TAINTED -> tally(tally, TTAspects.VITIUM, 2);
            default -> {
            }
        }
    }

    private static ResourceLocation pick(List<ResourceLocation> pool, RandomSource random) {
        return pool.get(random.nextInt(pool.size()));
    }

    private static void tally(Map<ResourceLocation, Integer> target, ResourceLocation aspect, int amount) {
        target.merge(aspect, amount, Integer::sum);
    }

    private static void allocateBudget(Map<ResourceLocation, Integer> target, int budget, RandomSource random) {
        int committed = target.values().stream().mapToInt(Integer::intValue).sum();
        int extra = Math.max(0, budget - committed);
        List<ResourceLocation> keys = new ArrayList<>(target.keySet());
        int count = keys.size();
        if (count == 0 || extra == 0) {
            return;
        }
        double[] weights = new double[count];
        double weightSum = 0.0;
        for (int slot = 0; slot < count; slot++) {
            weights[slot] = random.nextFloat() + WEIGHT_FLOOR;
            weightSum += weights[slot];
        }
        int[] shares = new int[count];
        double[] remainders = new double[count];
        int assigned = 0;
        for (int slot = 0; slot < count; slot++) {
            double quota = extra * weights[slot] / weightSum;
            shares[slot] = (int) quota;
            remainders[slot] = quota - shares[slot];
            assigned += shares[slot];
        }
        List<Integer> byRemainder = new ArrayList<>(count);
        for (int slot = 0; slot < count; slot++) {
            byRemainder.add(slot);
        }
        byRemainder.sort((a, b) -> Double.compare(remainders[b], remainders[a]));
        int leftover = extra - assigned;
        for (int rank = 0; rank < leftover; rank++) {
            shares[byRemainder.get(rank % count)]++;
        }
        for (int slot = 0; slot < count; slot++) {
            target.merge(keys.get(slot), shares[slot], Integer::sum);
        }
    }
}
