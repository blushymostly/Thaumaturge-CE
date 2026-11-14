package com.leclowndu93150.thaumaturge.content.crucible;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgressionEvents;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTBlockTags;
import com.leclowndu93150.thaumaturge.registry.TTCapabilities;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.leclowndu93150.thaumaturge.content.essentia.BellowsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrucibleBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 1000;
    public static final int MAX_ASPECT = 100;
    public static final int BOIL_HEAT = 150;
    public static final int MAX_HEAT = 200;
    public static final int CRAFT_WATER_COST = 50;

    private static final float FLUID_BASE = 0.3F;
    private static final float FLUID_TANK_SPAN = 0.5F;
    private static final float FLUID_OVERFLOW_HEIGHT = 1.001F;
    private static final float FLUID_EXACT_HEIGHT = 0.9999F;

    private final EssentiaTank aspectTank = new EssentiaTank(250).acceptsInput(false).onChange(this::syncToClient);
    private LazyOptional<EssentiaTank> aspectTankOpt = LazyOptional.of(() -> aspectTank);
    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            syncToClient();
        }
    };
    private LazyOptional<IFluidHandler> waterTankOpt = LazyOptional.of(() -> waterTank);

    private int heat;
    private int leakCounter = -100;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public EssentiaTank tank() {
        return aspectTank;
    }

    public AspectList aspects() {
        return aspectTank.getEssentia();
    }

    public void setAspects(AspectList aspects) {
        aspectTank.setContents(aspects);
        setChanged();
    }

    public int getHeat() {
        return heat;
    }

    public void setHeat(int heat) {
        this.heat = Math.max(0, Math.min(MAX_HEAT, heat));
        setChanged();
    }

    public FluidTank waterTank() {
        return waterTank;
    }

    public boolean hasWater() {
        return waterTank.getFluidAmount() > 0;
    }

    public boolean isBoiling() {
        return heat > BOIL_HEAT && hasWater();
    }

    public boolean isOverfilled() {
        return aspects().entries().stream().mapToInt(e -> e.amount()).sum() > MAX_ASPECT;
    }

    /** CE surfaceLevel — water fill + aspect foam height. */
    public float surfaceLevel() {
        float filled = (float) waterTank.getFluidAmount() / TANK_CAPACITY;
        float floor = FLUID_BASE + FLUID_TANK_SPAN * filled;
        int aspectTotal = aspects().entries().stream().mapToInt(e -> e.amount()).sum();
        float aspectShare = (float) aspectTotal / MAX_ASPECT;
        float height = floor + aspectShare * (1.0F - floor);
        int order = Float.compare(height, 1.0F);
        if (order > 0) {
            return FLUID_OVERFLOW_HEIGHT;
        }
        return order == 0 ? FLUID_EXACT_HEIGHT : height;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        if (level instanceof ServerLevel serverLevel) {
            crucible.tickServer(serverLevel);
        }
    }

    private void tickServer(ServerLevel level) {
        updateHeat(level);
        emitParticles(level);
        if (aspects().isEmpty()) return;
        leakCounter++;
        if (isOverfilled() && leakCounter % 5 == 0) leak(level, true);
        if (leakCounter >= 100) {
            leakCounter = 0;
            leak(level, false);
        }
        setChanged();
    }

    private void leak(ServerLevel level, boolean overflow) {
        var entries = aspects().entries();
        if (entries.isEmpty()) return;
        var random = level.getRandom();
        AspectInstance selected = entries.get(random.nextInt(entries.size()));
        aspectTank.setContents(aspects().remove(selected.aspectId(), 1));
        boolean spill = random.nextInt(4) < (overflow ? 1 : 3);
        if (!spill || !PhysicalFlux.spill(level, worldPosition, random)) {
            AuraHelper.polluteAura(level, worldPosition, overflow ? 1.0F : 0.25F, true);
        }
    }

    private void emitParticles(ServerLevel level) {
        if (!hasWater()) {
            return;
        }
        float height = surfaceLevel();
        if (heat > BOIL_HEAT && level.getGameTime() % 4 == 0) {
            CrucibleFx.froth(level, worldPosition, height, isOverfilled());
        }
        CrucibleFx.bubble(level, worldPosition, height, aspects());
    }

    private void updateHeat(ServerLevel level) {
        if (!hasWater()) {
            if (heat > 0) {
                heat--;
                syncToClient();
            }
            return;
        }
        // CE: thaumaturge:crucible_heat_sources (lava/fire/campfire/magma + all nitors under the cauldron).
        boolean heated = level.getBlockState(worldPosition.below()).is(TTBlockTags.CRUCIBLE_HEAT_SOURCES);
        int previous = heat;
        int airflow = 0;
        if (heated && heat < MAX_HEAT) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (BellowsHelper.blowsToward(level, worldPosition.relative(side), side)) airflow++;
            }
        }
        heat = heated ? Math.min(heat + 1 + airflow, MAX_HEAT) : Math.max(heat - 1, 0);
        // Sync when boiling state flips so clients see froth / allow insert feedback.
        if (heat != previous && (previous <= BOIL_HEAT) != (heat <= BOIL_HEAT)) {
            syncToClient();
        } else if (heat != previous) {
            setChanged();
        }
    }

    /** CE emptyOut: dump water and aspects (aspects become flux). */
    public void emptyOut() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int points = aspects().entries().stream().mapToInt(AspectInstance::amount).sum();
        if (points <= 0 && !hasWater()) {
            return;
        }
        AspectList spilled = aspects();
        float height = surfaceLevel();
        waterTank.drain(waterTank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        aspectTank.setContents(AspectList.EMPTY);
        int lost = points;
        for (int i = 0; i < points; i++) {
            if (serverLevel.random.nextInt(4) == 0 && PhysicalFlux.spill(serverLevel, worldPosition, serverLevel.random)) lost--;
        }
        if (lost > 0) AuraHelper.polluteAura(serverLevel, worldPosition, lost, true);
        leakCounter = -100;
        CrucibleFx.boilBurst(serverLevel, worldPosition, height, spilled.isEmpty() ? AspectList.EMPTY : spilled, Math.max(1, points / 5));
        serverLevel.playSound(null, worldPosition, TTSounds.SPILL.get(), SoundSource.BLOCKS, 0.45F, 0.7F);
        syncToClient();
    }

    public void absorbItem(ItemEntity itemEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !isBoiling() || itemEntity.getItem().isEmpty()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        Player credited = throwerPlayer(itemEntity);
        // World drops made by the crucible have no thrower and must not dissolve on the way out.
        if (credited == null || !credited.isAlive()) return;
        int budget = (stack.getCount() + 1) / 2;
        for (int i = 0; i < budget && smeltOne(serverLevel, stack.copyWithCount(1), credited); i++) {
            stack.shrink(1);
        }
        if (stack.isEmpty()) itemEntity.discard();
        else itemEntity.setItem(stack);
    }

    public boolean tryInsertFromPlayer(Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || !isBoiling() || stack.isEmpty()) {
            return false;
        }
        if (!smeltOne(serverLevel, stack.copyWithCount(1), player)) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    /** CE smeltOne: craft if a recipe matches, otherwise dissolve the item into aspects. */
    private boolean smeltOne(ServerLevel level, ItemStack single, @Nullable Player player) {
        CrucibleRecipe recipe = findRecipe(level, single, player);
        boolean consumed = recipe == null ? dissolve(level, single) : craft(level, recipe, player);
        if (consumed) {
            syncToClient();
        }
        return consumed;
    }

    private boolean craft(ServerLevel level, CrucibleRecipe recipe, @Nullable Player player) {
        if (waterTank.getFluidAmount() < CRAFT_WATER_COST) {
            return false;
        }
        leakCounter = -250;
        aspectTank.setContents(recipe.removeMatching(aspects()));
        waterTank.drain(CRAFT_WATER_COST, IFluidHandler.FluidAction.EXECUTE);
        CrucibleFx.boilBurst(level, worldPosition, surfaceLevel(), aspects(), 1);
        CrucibleFx.craftComplete(level, worldPosition);
        ItemStack result = recipe.assemble(null, level.registryAccess());
        spawnResult(level, result);
        float poofPitch = 0.9F + level.getRandom().nextFloat() * 0.2F;
        level.playSound(null, worldPosition, TTSounds.POOF.get(), SoundSource.BLOCKS, 0.4F, poofPitch);
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchProgressionEvents.recordCrafted(serverPlayer, result);
        }
        return true;
    }

    private static @Nullable Player throwerPlayer(ItemEntity itemEntity) {
        Entity owner = itemEntity.getOwner();
        return owner instanceof Player player && player.isAlive() ? player : null;
    }

    private boolean dissolve(ServerLevel level, ItemStack single) {
        AspectList gained = AspectIndex.of(single);
        if (gained.isEmpty()) {
            return false;
        }
        AspectList next = aspects();
        for (AspectInstance entry : gained.entries()) {
            next = next.add(entry);
        }
        leakCounter = -150;
        aspectTank.setContents(next);
        CrucibleFx.boilBurst(level, worldPosition, surfaceLevel(), aspects(), 1);
        float pitch = 0.7F + level.getRandom().nextFloat() * 0.3F;
        level.playSound(null, worldPosition, TTSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.35F, pitch);
        return true;
    }

    @Nullable
    private CrucibleRecipe findRecipe(ServerLevel level, ItemStack catalyst, @Nullable Player player) {
        List<CrucibleRecipe> recipes = level.getRecipeManager().getAllRecipesFor(TTRecipeTypes.CRUCIBLE.get());
        AspectList available = aspects();
        CrucibleRecipe best = null;
        int bestCost = -1;
        for (CrucibleRecipe recipe : recipes) {
            if (recipe.matchesCatalyst(catalyst) && recipe.matchesAspects(available) && recipe.researchAllows(player)) {
                int cost = recipe.aspects().entries().stream().mapToInt(AspectInstance::amount).sum();
                if (cost > bestCost || cost == bestCost && best != null && recipe.getId().compareTo(best.getId()) < 0) {
                    best = recipe;
                    bestCost = cost;
                }
            }
        }
        return best;
    }

    private void spawnResult(ServerLevel level, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(worldPosition).add(0.0, 0.75, 0.0);
        ItemEntity entity = new ItemEntity(level, center.x, center.y, center.z, result);
        entity.setDeltaMovement(0.0, 0.2, 0.0);
        level.addFreshEntity(entity);
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveClientVisible(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadClientVisible(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        loadClientVisible(tag);
    }

    private void saveClientVisible(CompoundTag tag) {
        tag.put("Tank", aspectTank.save());
        tag.put("Water", waterTank.writeToNBT(new CompoundTag()));
        tag.putInt("Heat", heat);
    }

    private void loadClientVisible(CompoundTag tag) {
        aspectTank.load(tag.getCompound("Tank"));
        if (tag.contains("Water")) {
            waterTank.readFromNBT(tag.getCompound("Water"));
        }
        heat = tag.getInt("Heat");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LeakCounter", leakCounter);
        saveClientVisible(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        leakCounter = tag.contains("LeakCounter") ? tag.getInt("LeakCounter") : -100;
        loadClientVisible(tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.ESSENTIA) {
            return aspectTankOpt.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return waterTankOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        aspectTankOpt = LazyOptional.of(() -> aspectTank);
        waterTankOpt = LazyOptional.of(() -> waterTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        aspectTankOpt.invalidate();
        waterTankOpt.invalidate();
    }
}
