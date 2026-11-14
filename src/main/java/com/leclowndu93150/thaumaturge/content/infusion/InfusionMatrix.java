package com.leclowndu93150.thaumaturge.content.infusion;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.content.blockentity.AbstractSyncedBlockEntity;
import com.leclowndu93150.thaumaturge.content.decor.MagicCandleBlock;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgressionEvents;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class InfusionMatrix extends AbstractSyncedBlockEntity {
    private static final int RADIUS = 8;
    private static final int WORK_INTERVAL = 10;
    private static final int ESSENTIA_RANGE = 12;
    private static final int SURVEY_INTERVAL = 100;
    private static final int ESSENTIA_RETRY_DELAY = 200;
    private static final int INGREDIENT_PULL_CYCLES = 5;
    private static final int FINISH_GRACE_CYCLES = 3;
    private static final float MAX_STABILITY = 25;
    private static final float MIN_STABILITY = -100;
    private static final ResourceLocation RESEARCH = TTIds.rl("infusion");
    private static final List<String> VARIANTS = List.of("arcane", "ancient", "eldritch");

    private boolean active;
    private boolean checkedPillarDirections;
    private float startup;
    private float previousStartup;
    private int clientCraftTicks;

    void clientTick() {
        previousStartup = startup;
        float change = Math.max(startup / 10, 0.001F);
        startup = active ? Math.min(1, startup + change) : Math.max(0, startup - change);
        clientCraftTicks = craftingOnClient ? Math.min(50, clientCraftTicks + 1) : Math.max(0, clientCraftTicks - 2);
    }

    public float startup(float partial) {
        return net.minecraft.util.Mth.lerp(partial, previousStartup, startup);
    }

    public int clientCraftTicks() {
        return clientCraftTicks;
    }

    private void repairPillarDirections(ServerLevel level) {
        BlockPos center = worldPosition.below(2);
        if (!level.hasChunksAt(center.offset(-1, 0, -1), center.offset(1, 0, 1))) return;
        var pillar = TTBulkContent.BLOCKS.get("pillar_" + variant()).get();
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                BlockPos at = center.offset(x, 0, z);
                if (!level.getBlockState(at).is(pillar)) return;
            }
        }
        checkedPillarDirections = true;
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                BlockPos at = center.offset(x, 0, z);
                var state = level.getBlockState(at);
                var facing = InfusionPillarBlock.faceMatrix(state, at.subtract(center));
                if (state != facing) level.setBlockAndUpdate(at, facing);
            }
        }
    }
    private @Nullable CraftJob job;
    private @Nullable ResourceLocation legacyRecipe;
    private @Nullable UUID legacyOwner;
    private @Nullable Environment environment;
    private long nextSurvey;
    private long retryEssentiaAfter;
    private float stability;
    private boolean craftingOnClient;

    private record Tuning(int interval, float cost, float stability) {}
    private record Environment(Tuning tuning, List<BlockPos> pedestals, float replenish) {}

    public InfusionMatrix(BlockPos pos, BlockState state) {
        super(TTBlockEntities.INFUSION_MATRIX.get(), pos, state);
    }

    public boolean active() {
        return active;
    }

    public String variant() {
        if (level != null) {
            for (String variant : VARIANTS) {
                if (level.getBlockState(worldPosition.below(2)).is(TTBulkContent.BLOCKS.get("pedestal_" + variant).get())) {
                    return variant;
                }
            }
        }
        return "arcane";
    }

    private @Nullable InfusionPedestal altar(Level level) {
        BlockPos center = worldPosition.below(2);
        if (!(level.getBlockEntity(center) instanceof InfusionPedestal pedestal)) {
            return null;
        }
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                BlockPos at = center.offset(x, 0, z);
                if (!level.hasChunkAt(at) || !(level.getBlockState(at).getBlock() instanceof InfusionPillarBlock)) {
                    return null;
                }
            }
        }
        return pedestal;
    }

    public boolean activate(ServerLevel level, ServerPlayer player) {
        InfusionPedestal center = altar(level);
        if (!TTAttachments.knowledge(player).isComplete(RESEARCH) || center == null) {
            player.displayClientMessage(Component.translatable("message.thaumaturge.infusion.altar_required"), true);
            return false;
        }
        if (!active) {
            active = true;
            setChangedAndSync();
            play(level, TTSounds.CRAFTSTART.get(), 1);
            return true;
        }
        if (job != null || legacyRecipe != null) return false;
        environment = null;
        Environment current = environment(level);
        var items = occupiedPedestals(level, current.pedestals());
        var candidates = level.getRecipeManager().getAllRecipesFor(TTRecipeTypes.INFUSION.get()).stream()
                .sorted(Comparator.comparing(InfusionRecipe::getId)).toList();
        for (InfusionRecipe recipe : candidates) {
            if (recipe.isUnlocked(player) && matches(recipe, center, items)) {
                start(level, recipe, player.getUUID(), center, items, current);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable("message.thaumaturge.infusion.no_matching_recipe"), true);
        return false;
    }

    private void start(ServerLevel level, InfusionRecipe recipe, @Nullable UUID owner, InfusionPedestal center,
            List<InfusionPedestal> items, Environment current) {
        float cost = current.tuning().cost();
        AspectList quote = AspectList.EMPTY;
        for (AspectInstance aspect : recipe.aspects().entries()) {
            int amount = (int) (aspect.amount() * cost);
            if (amount > 0) quote = quote.add(aspect.withAmount(amount));
        }
        ItemStack catalyst = center.item().copyWithCount(1);
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (catalyst.isDamageableItem() && catalyst.isDamaged() && result.isDamageableItem() && !result.isDamaged()) {
            result.setDamageValue((int) ((double) catalyst.getDamageValue() / catalyst.getMaxDamage() * result.getMaxDamage()));
        }
        var ingredients = items.stream().map(pedestal -> pedestal.item().copyWithCount(1)).toList();
        job = new CraftJob(ingredients, quote, quote, catalyst, result, recipe.instability(), owner);
        retryEssentiaAfter = 0;
        play(level, TTSounds.CRAFTSTART.get(), 1);
        setChangedAndSync();
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (!checkedPillarDirections && now % 20 == 0) repairPillarDirections(level);
        if (!active) return;
        // A partially loaded altar cannot prove its structure invalid or safely advance a legacy job.
        BlockPos centerPos = worldPosition.below(2);
        if (!level.hasChunksAt(centerPos.offset(-1, 0, -1), centerPos.offset(1, 0, 1))) return;
        InfusionPedestal center = altar(level);
        if (center == null) {
            active = false;
            abort(level);
            return;
        }
        Environment current = environment(level);
        if (legacyRecipe != null) {
            if (!level.hasChunksAt(worldPosition.offset(-RADIUS, 0, -RADIUS), worldPosition.offset(RADIUS, 0, RADIUS))) return;
            environment = null;
            current = environment(level);
            resumeLegacy(level, center, current);
        }
        int interval = Math.max(1, current.tuning().interval() / 2);
        if (job == null) {
            if (stability < MAX_STABILITY && now % Math.max(5, interval) == 0) {
                stability = Math.min(MAX_STABILITY, stability + Math.max(0.1F, current.replenish()));
                setChanged();
            }
            return;
        }
        if (now % interval != 0) return;

        if (!ItemStack.isSameItemSameTags(center.item(), job.catalyst)) {
            instability(level, current);
            abort(level);
            return;
        }
        int divisor = stability > 12.5F ? 5 : stability >= 0 ? 6 : stability > -25 ? 7 : 8;
        float loss = level.random.nextFloat() * job.instability / divisor;
        stability = Mth.clamp(stability - loss + current.replenish(), MIN_STABILITY, MAX_STABILITY);
        if (stability < 0 && level.random.nextInt(1500) <= Math.abs(stability)) {
            instability(level, current);
            stability += 5 + level.random.nextFloat() * 5;
        } else if (!job.remaining.isEmpty()) {
            drawEssentia(level);
        } else if (!job.ingredients.isEmpty()) {
            pullIngredient(level, current);
        } else if (++job.finishing >= FINISH_GRACE_CYCLES) {
            finish(level, center);
        }
        setChanged();
    }

    private void resumeLegacy(ServerLevel level, InfusionPedestal center, Environment current) {
        var recipe = level.getRecipeManager().byKey(legacyRecipe).orElse(null);
        UUID owner = legacyOwner;
        legacyRecipe = null;
        legacyOwner = null;
        var items = occupiedPedestals(level, current.pedestals());
        if (recipe instanceof InfusionRecipe infusion && matches(infusion, center, items)) {
            var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
            if (player == null || infusion.isUnlocked(player)) {
                start(level, infusion, owner, center, items, current);
                return;
            }
        }
        setChangedAndSync();
    }

    private Environment environment(ServerLevel level) {
        long now = level.getGameTime();
        if (environment == null || now >= nextSurvey) {
            List<BlockPos> pedestals = pedestalPositions(level);
            Tuning tuning = tuning(level, pedestals);
            environment = new Environment(tuning, pedestals, symmetryScore(level, worldPosition) + tuning.stability());
            nextSurvey = now + SURVEY_INTERVAL;
        }
        return environment;
    }

    private void drawEssentia(ServerLevel level) {
        long now = level.getGameTime();
        for (AspectInstance aspect : job.remaining.entries()) {
            if (now >= retryEssentiaAfter && drawOne(level, aspect)) {
                job.remaining = job.remaining.remove(aspect.aspectId(), 1);
                return;
            }
            stability = Math.max(MIN_STABILITY, stability - 0.25F);
        }
        if (now >= retryEssentiaAfter) retryEssentiaAfter = now + ESSENTIA_RETRY_DELAY;
    }

    private boolean drawOne(ServerLevel level, AspectInstance aspect) {
        List<BlockPos> sources = new ArrayList<>();
        int minX = worldPosition.getX() - ESSENTIA_RANGE;
        int maxX = worldPosition.getX() + ESSENTIA_RANGE;
        int minZ = worldPosition.getZ() - ESSENTIA_RANGE;
        int maxZ = worldPosition.getZ() + ESSENTIA_RANGE;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    var pos = entity.getBlockPos();
                    if (entity instanceof EssentiaJarBlockEntity && !entity.isRemoved()
                            && pos.getX() >= minX && pos.getX() <= maxX
                            && pos.getZ() >= minZ && pos.getZ() <= maxZ
                            && Math.abs(pos.getY() - worldPosition.getY()) <= ESSENTIA_RANGE) {
                        sources.add(pos);
                    }
                }
            }
        }
        sources.sort(Comparator.<BlockPos>comparingDouble(worldPosition::distSqr).thenComparingLong(BlockPos::asLong));
        for (BlockPos pos : sources) {
            var source = EssentiaAccess.handler(level, pos, null);
            if (source != null && source.canOutputTo(null) && source.extract(aspect.aspectId(), 1, false) == 1) {
                stream(level, pos);
                return true;
            }
        }
        return false;
    }

    private void pullIngredient(ServerLevel level, Environment current) {
        if (job.pullFrom != null) {
            if (!level.hasChunkAt(job.pullFrom)) return;
            var entity = level.getBlockEntity(job.pullFrom);
            var wanted = job.ingredients.get(job.pullIndex);
            if (entity instanceof InfusionPedestal pedestal && ItemStack.isSameItemSameTags(pedestal.item(), wanted)) {
                if (--job.pulling > 0) return;
                ItemStack held = pedestal.item();
                ItemStack remainder = held.getCraftingRemainingItem();
                if (held.getCount() > 1) {
                    pedestal.setItem(held.copyWithCount(held.getCount() - 1));
                    if (!remainder.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, job.pullFrom.above(), remainder);
                } else {
                    pedestal.setItem(remainder);
                }
                job.ingredients.remove(job.pullIndex);
                job.pullFrom = null;
                return;
            }
            job.pullFrom = null;
        }
        for (int i = 0; i < job.ingredients.size(); i++) {
            ItemStack wanted = job.ingredients.get(i);
            for (BlockPos pos : current.pedestals()) {
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof InfusionPedestal pedestal
                        && ItemStack.isSameItemSameTags(pedestal.item(), wanted)) {
                    job.pullFrom = pos;
                    job.pullIndex = i;
                    job.pulling = INGREDIENT_PULL_CYCLES;
                    stream(level, pos);
                    return;
                }
            }
            if (level.random.nextInt(i + 1) == 0) {
                var quote = job.quoted.entries();
                if (!quote.isEmpty()) job.remaining = job.remaining.add(quote.get(level.random.nextInt(quote.size())).withAmount(1));
                stability = Math.max(MIN_STABILITY, stability - 0.25F);
            }
        }
    }

    private void finish(ServerLevel level, InfusionPedestal center) {
        ItemStack result = job.result.copy();
        ServerPlayer crafter = job.owner == null ? null : level.getServer().getPlayerList().getPlayer(job.owner);
        center.setItem(result);
        job = null;
        if (crafter != null) {
            crafter.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), result.getCount());
            ResearchProgressionEvents.recordCrafted(crafter, result);
        }
        play(level, TTSounds.CRAFT.get(), 1);
        setChangedAndSync();
    }

    private void abort(ServerLevel level) {
        if (job != null) play(level, TTSounds.CRAFTFAIL.get(), 0.6F);
        job = null;
        legacyRecipe = null;
        legacyOwner = null;
        setChangedAndSync();
    }

    private void play(ServerLevel level, SoundEvent sound, float pitch) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, pitch);
    }

    private void stream(ServerLevel level, BlockPos source) {
        Vec3 start = Vec3.atCenterOf(source);
        Vec3 travel = Vec3.atCenterOf(worldPosition).subtract(start);
        for (int step = 1; step <= 5; step++) {
            var at = start.add(travel.scale(step / 6.0));
            level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 1, 0.12, 0.12, 0.12, 0);
        }
    }

    private Tuning tuning(ServerLevel level, List<BlockPos> pedestals) {
        var ancient = TTBulkContent.BLOCKS.get("pillar_ancient").get();
        var eldritch = TTBulkContent.BLOCKS.get("pillar_eldritch").get();
        boolean allAncient = true;
        boolean allEldritch = true;
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                var at = worldPosition.offset(x, -2, z);
                var state = level.hasChunkAt(at) ? level.getBlockState(at) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                allAncient &= state.is(ancient);
                allEldritch &= state.is(eldritch);
            }
        }
        int interval = WORK_INTERVAL;
        float cost = 1;
        float gain = 0;
        if (allAncient) {
            interval--;
            cost -= 0.10F;
            gain -= 0.1F;
        } else if (allEldritch) {
            interval -= 3;
            cost += 0.05F;
            gain += 0.2F;
        }
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                BlockPos at = worldPosition.offset(x, -5, z);
                if (!level.hasChunkAt(at)) continue;
                var state = level.getBlockState(at);
                if (state.is(TTBulkContent.BLOCKS.get("matrix_speed").get())) {
                    interval--;
                    cost += 0.01F;
                } else if (state.is(TTBulkContent.BLOCKS.get("matrix_cost").get())) {
                    interval++;
                    cost -= 0.02F;
                }
            }
        }
        var ancientPedestal = TTBulkContent.BLOCKS.get("pedestal_ancient").get();
        var eldritchPedestal = TTBulkContent.BLOCKS.get("pedestal_eldritch").get();
        for (BlockPos pos : pedestals) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ancientPedestal)) cost -= 0.01F;
            else if (state.is(eldritchPedestal)) cost += 0.0025F;
        }
        return new Tuning(Math.max(1, interval), Math.max(0.5F, cost), gain);
    }

    private void instability(ServerLevel level, Environment current) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(10))) {
            if (ResearchManager.progressResearch(player, TTIds.rl("instability"))) {
                player.displayClientMessage(Component.translatable("message.thaumaturge.infusion.instability"), true);
            }
        }
        List<InfusionPedestal> items = occupiedPedestals(level, current.pedestals());
        if (!items.isEmpty()) {
            InfusionPedestal target = items.get(level.random.nextInt(items.size()));
            var stabilizer = InfusionInlay.source(level, target.getBlockPos(), 5);
            if (stabilizer != null && stabilizer.mitigate(5 + level.random.nextInt(6))) return;
            ItemStack ejected = target.item();
            target.setItem(ItemStack.EMPTY);
            net.minecraft.world.level.block.Block.popResource(level, target.getBlockPos().above(), ejected);
        }
        AuraHelper.polluteAura(level, worldPosition, Math.max(1, -stability), true);
    }

    public static float symmetryScore(Level level, BlockPos matrix) {
        Map<net.minecraft.world.level.block.Block, Float> weights = new HashMap<>();
        float score = 0;
        for (BlockPos at : BlockPos.betweenClosed(matrix.offset(-8, -7, -8), matrix.offset(8, 3, 8))) {
            int x = at.getX() - matrix.getX();
            int z = at.getZ() - matrix.getZ();
            if (x < 0 || x == 0 && z <= 0) continue;
            BlockPos mirror = at.offset(-2 * x, 0, -2 * z);
            if (!level.hasChunkAt(at) || !level.hasChunkAt(mirror)) continue;
            var left = stabilizingBlock(level.getBlockState(at));
            var right = stabilizingBlock(level.getBlockState(mirror));
            if (left == null && right == null) continue;
            if (left == null || left != right) {
                score -= 0.1F;
                continue;
            }
            float weight = weights.getOrDefault(left, left instanceof InfusionInlay ? 0.025F : 0.1F);
            score += weight;
            weights.put(left, weight * 0.75F);
        }
        return score;
    }

    private static @Nullable net.minecraft.world.level.block.Block stabilizingBlock(BlockState state) {
        var block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.AbstractSkullBlock) return net.minecraft.world.level.block.Blocks.SKELETON_SKULL;
        return block instanceof MagicCandleBlock || block instanceof InfusionInlay || block instanceof InfusionStabilizer ? block : null;
    }

    private List<BlockPos> pedestalPositions(ServerLevel level) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (x == 0 && z == 0) continue;
                for (int y = 3; y >= -7; y--) {
                    BlockPos pos = worldPosition.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof InfusionPedestal) positions.add(pos);
                }
            }
        }
        return positions;
    }

    private static List<InfusionPedestal> occupiedPedestals(ServerLevel level, List<BlockPos> positions) {
        List<InfusionPedestal> items = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof InfusionPedestal pedestal && !pedestal.item().isEmpty()) {
                items.add(pedestal);
            }
        }
        return items;
    }

    private static boolean matches(InfusionRecipe recipe, InfusionPedestal center, List<InfusionPedestal> items) {
        return !items.isEmpty() && items.size() == recipe.components().size()
                && recipe.central().test(center.item()) && assign(recipe, items, new boolean[items.size()], 0);
    }

    private static boolean assign(InfusionRecipe recipe, List<InfusionPedestal> items, boolean[] used, int index) {
        if (index == recipe.components().size()) {
            return true;
        }
        var ingredient = recipe.components().get(index);
        for (int i = 0; i < items.size(); i++) {
            if (!used[i] && ingredient.test(items.get(i).item())) {
                used[i] = true;
                if (assign(recipe, items, used, index + 1)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Active", active);
        tag.putFloat("Stability", stability);
        if (job != null) {
            tag.put("Job", job.save());
        } else if (legacyRecipe != null) {
            tag.putString("Recipe", legacyRecipe.toString());
            if (legacyOwner != null) tag.putUUID("Owner", legacyOwner);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean("Active");
        stability = tag.getFloat("Stability");
        job = tag.contains("Job", Tag.TAG_COMPOUND) ? CraftJob.load(tag.getCompound("Job")) : null;
        legacyRecipe = tag.contains("Recipe", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        legacyOwner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        environment = null;
        retryEssentiaAfter = 0;
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putBoolean("Active", active);
        tag.putBoolean("Crafting", job != null);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        active = tag.getBoolean("Active");
        craftingOnClient = tag.getBoolean("Crafting");
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readClientData(tag);
    }

    private static final class CraftJob {
        private final List<ItemStack> ingredients;
        private final AspectList quoted;
        private final ItemStack catalyst;
        private final ItemStack result;
        private final int instability;
        private final @Nullable UUID owner;
        private AspectList remaining;
        private @Nullable BlockPos pullFrom;
        private int pullIndex;
        private int pulling;
        private int finishing;

        private CraftJob(List<ItemStack> ingredients, AspectList quoted, AspectList remaining, ItemStack catalyst,
                ItemStack result, int instability, @Nullable UUID owner) {
            this.ingredients = new ArrayList<>(ingredients);
            this.quoted = quoted;
            this.remaining = remaining;
            this.catalyst = catalyst;
            this.result = result;
            this.instability = instability;
            this.owner = owner;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag items = new ListTag();
            for (ItemStack stack : ingredients) items.add(stack.save(new CompoundTag()));
            tag.put("Ingredients", items);
            AspectList.CODEC.encodeStart(NbtOps.INSTANCE, quoted).result().ifPresent(list -> tag.put("Quote", list));
            AspectList.CODEC.encodeStart(NbtOps.INSTANCE, remaining).result().ifPresent(list -> tag.put("Remaining", list));
            tag.put("Catalyst", catalyst.save(new CompoundTag()));
            tag.put("Result", result.save(new CompoundTag()));
            tag.putInt("Instability", instability);
            if (owner != null) tag.putUUID("Owner", owner);
            return tag;
        }

        private static @Nullable CraftJob load(CompoundTag tag) {
            var catalyst = ItemStack.of(tag.getCompound("Catalyst"));
            var result = ItemStack.of(tag.getCompound("Result"));
            if (catalyst.isEmpty() || result.isEmpty()) return null;
            var items = new ArrayList<ItemStack>();
            ListTag saved = tag.getList("Ingredients", Tag.TAG_COMPOUND);
            for (int i = 0; i < saved.size(); i++) {
                ItemStack item = ItemStack.of(saved.getCompound(i));
                if (item.isEmpty()) return null;
                items.add(item);
            }
            var quote = AspectList.CODEC.parse(NbtOps.INSTANCE, tag.getList("Quote", Tag.TAG_COMPOUND)).result();
            var remaining = AspectList.CODEC.parse(NbtOps.INSTANCE, tag.getList("Remaining", Tag.TAG_COMPOUND)).result();
            if (quote.isEmpty() || remaining.isEmpty()) return null;
            UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
            return new CraftJob(items, quote.get(), remaining.get(), catalyst, result, tag.getInt("Instability"), owner);
        }
    }
}
