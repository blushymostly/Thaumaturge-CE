package com.leclowndu93150.thaumaturge.content.workbench;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayNetwork;
import com.leclowndu93150.thaumaturge.content.item.WandCharge;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import java.util.ArrayList;
import net.minecraft.world.level.ChunkPos;
import com.leclowndu93150.thaumaturge.content.item.VisCrystalItem;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgressionEvents;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTMenus;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    public static final List<ResourceLocation> PRIMAL_ORDER = List.of(
            TTAspects.AER, TTAspects.IGNIS, TTAspects.AQUA, TTAspects.TERRA, TTAspects.ORDO, TTAspects.PERDITIO);

    public static final int RESULT_SLOT = 0;
    public static final int GRID_START = 1;
    public static final int GRID_END = GRID_START + WorkbenchContainer.CRAFTING_SLOTS;
    public static final int CRYSTAL_MENU_START = GRID_END;
    public static final int CRYSTAL_MENU_END = CRYSTAL_MENU_START + WorkbenchContainer.CRYSTAL_SLOTS;
    public static final int WAND_MENU_SLOT = CRYSTAL_MENU_END;
    public static final int PLAYER_START = WAND_MENU_SLOT + 1;
    public static final int PLAYER_END = PLAYER_START + 36;

    public static final int GRID_ORIGIN = 41;
    public static final int GRID_SPACING = 23;
    public static final int RESULT_X = 160;
    public static final int RESULT_Y = 64;
    public static final int WAND_X = 160;
    public static final int WAND_Y = 100;
    public static final int[] CRYSTAL_X = {64, 16, 112, 16, 112, 64};
    public static final int[] CRYSTAL_Y = {13, 35, 35, 94, 94, 116};
    public static final int INVENTORY_X = 16;
    public static final int INVENTORY_Y = 151;
    public static final int HOTBAR_Y = 209;
    public static final int SLOT_SPACING = 18;

    private static final int AURA_DATA = 0;
    private static final int REQUIRED_VIS_DATA = 1;

    private final ArcaneWorkbenchBlockEntity workbench;
    private final ContainerLevelAccess access;
    private final WorkbenchContainer craftInventory;
    private final WorkbenchCraftingContainer craftingGrid;
    private final ResultContainer resultContainer = new ResultContainer();
    private final SimpleContainerData data = new SimpleContainerData(2);
    private final Player player;
    private @Nullable ArcaneCraftingRecipe selectedRecipe;

    public ArcaneWorkbenchMenu(int id, Inventory inv, ArcaneWorkbenchBlockEntity workbench) {
        super(TTMenus.ARCANE_WORKBENCH.get(), id);
        this.workbench = workbench;
        this.player = inv.player;
        this.access = ContainerLevelAccess.create(workbench.getLevel(), workbench.getBlockPos());
        this.craftInventory = new WorkbenchContainer(workbench);
        this.craftingGrid = new WorkbenchCraftingContainer(craftInventory);
        this.craftInventory.setChangeListener(() -> slotsChanged(craftInventory));

        addSlot(new ArcaneWorkbenchResultSlot(resultContainer, this, RESULT_X, RESULT_Y));
        for (int cell = 0; cell < WorkbenchContainer.CRAFTING_SLOTS; cell++) {
            addSlot(new Slot(craftInventory, cell,
                    GRID_ORIGIN + (cell % 3) * GRID_SPACING,
                    GRID_ORIGIN + (cell / 3) * GRID_SPACING));
        }
        for (int i = 0; i < WorkbenchContainer.CRYSTAL_SLOTS; i++) {
            addSlot(new SlotCrystalEssentia(craftInventory, WorkbenchContainer.CRAFTING_SLOTS + i,
                    CRYSTAL_X[i], CRYSTAL_Y[i], PRIMAL_ORDER.get(i)));
        }
        addSlot(new SlotWorkbenchWand(craftInventory, WorkbenchContainer.WAND_SLOT, WAND_X, WAND_Y));

        for (int cell = 0; cell < 27; cell++) {
            addSlot(new Slot(inv, 9 + cell,
                    INVENTORY_X + (cell % 9) * SLOT_SPACING,
                    INVENTORY_Y + (cell / 9) * SLOT_SPACING));
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INVENTORY_X + col * SLOT_SPACING, HOTBAR_Y));
        }
        addDataSlots(data);
        if (craftsOnServer()) {
            refreshResult();
        }
    }

    public static ArcaneWorkbenchMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        ArcaneWorkbenchBlockEntity workbench = be instanceof ArcaneWorkbenchBlockEntity arcane
                ? arcane
                : new ArcaneWorkbenchBlockEntity(pos, TTBlocks.ARCANE_WORKBENCH.get().defaultBlockState());
        return new ArcaneWorkbenchMenu(id, inv, workbench);
    }

    public WorkbenchContainer inventory() {
        return craftInventory;
    }

    public @Nullable ArcaneCraftingRecipe selectedRecipe() {
        return selectedRecipe;
    }

    boolean craftsOnServer() {
        return workbench.getLevel() instanceof ServerLevel && !workbench.getLevel().isClientSide;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftInventory && craftsOnServer()) {
            refreshResult();
        }
        super.slotsChanged(container);
    }

    private void refreshResult() {
        selectedRecipe = null;
        data.set(REQUIRED_VIS_DATA, 0);
        resultContainer.setItem(0, ItemStack.EMPTY);
        Level level = workbench.getLevel();
        if (level == null) {
            return;
        }
        Optional<ArcaneCraftingRecipe> match = level.getRecipeManager()
                .getRecipeFor(TTRecipeTypes.ARCANE_CRAFTING.get(), craftingGrid, level);
        if (match.isEmpty()) {
            return;
        }
        ArcaneCraftingRecipe recipe = match.get();
        if (!researchGateOpen(recipe)) {
            return;
        }
        selectedRecipe = recipe;
        updatePayment((ServerLevel) level);
    }

    private void updatePayment(ServerLevel level) {
        var recipe = selectedRecipe;
        var anchors = auraAnchors(level);
        float available = availableAura(level, anchors);
        data.set(AURA_DATA, (int) available);
        if (recipe == null) return;
        var payment = payment(level, recipe);
        data.set(REQUIRED_VIS_DATA, payment.auraVis());
        resultContainer.setItem(0, available + 0.001F >= payment.auraVis() && hasRequiredCrystals(payment.crystals())
                ? recipe.assemble(craftingGrid, level.registryAccess()) : ItemStack.EMPTY);
    }

    ItemStack takeCraft() {
        if (!craftsOnServer() || selectedRecipe == null) return ItemStack.EMPTY;
        ServerLevel level = (ServerLevel) workbench.getLevel();
        ArcaneCraftingRecipe recipe = selectedRecipe;
        var payment = payment(level, recipe);
        var anchors = auraAnchors(level);
        if (availableAura(level, anchors) + 0.001F < payment.auraVis() || !hasRequiredCrystals(payment.crystals())
                || !researchGateOpen(recipe) || !recipe.matches(craftingGrid, level)) {
            refreshResult();
            return ItemStack.EMPTY;
        }
        ItemStack output = recipe.assemble(craftingGrid, level.registryAccess());
        if (output.isEmpty()) return ItemStack.EMPTY;

        spendAura(level, anchors, payment.auraVis());
        ItemStack wand = craftInventory.getItem(WorkbenchContainer.WAND_SLOT);
        for (var cost : payment.wand().entries()) WandCharge.spendExact(wand, cost.aspectId(), cost.amount());
        for (var cost : payment.relay().entries()) payment.source().drain(level, cost.aspectId(), cost.amount());
        recipe.consumeItems(craftingGrid);
        consumeCrystals(payment.crystals());
        workbench.setChanged();
        if (player instanceof ServerPlayer serverPlayer) ResearchProgressionEvents.recordCrafted(serverPlayer, output);
        refreshResult();
        return output;
    }

    private Payment payment(ServerLevel level, ArcaneCraftingRecipe recipe) {
        ItemStack wand = craftInventory.getItem(WorkbenchContainer.WAND_SLOT);
        boolean hasWand = wand.getItem() instanceof WandItem;
        var source = recipe.crystals().isEmpty() ? null
                : VisRelayNetwork.nearest(level, workbench.getBlockPos());
        AspectList wandCost = AspectList.EMPTY;
        AspectList relayCost = AspectList.EMPTY;
        AspectList crystals = AspectList.EMPTY;
        for (var crystal : recipe.crystals().entries()) {
            int amount = crystal.amount() * 2 * WandCharge.CENTIVIS_PER_VIS;
            var cost = new AspectInstance(crystal.aspectId(), amount);
            if (hasWand && WandCharge.stored(wand, crystal.aspectId()) >= amount) wandCost = wandCost.add(cost);
            else if (source != null && source.available(crystal.aspectId()) >= amount) relayCost = relayCost.add(cost);
            else crystals = crystals.add(crystal);
        }
        // Forge currently ships iron caps for both rods.
        float discount = com.leclowndu93150.thaumaturge.api.items.GogglesAccess.wearsRevealingGear(player) ? 0.05F : 0;
        var exhaust = player.getEffect(com.leclowndu93150.thaumaturge.registry.TTMobEffects.VIS_EXHAUST.get());
        if (exhaust != null) discount -= 0.1F * (exhaust.getAmplifier() + 1);
        float gearCost = Math.max(0.1F, 1 - discount);
        float modifier = !crystals.isEmpty() ? 1.25F * gearCost
                : hasWand && relayCost.isEmpty() ? Math.max(0.1F, 1.1F - discount) : gearCost;
        return new Payment((int) Math.ceil(recipe.visCost() * modifier), wandCost, relayCost, crystals, source);
    }

    private record Payment(int auraVis, AspectList wand, AspectList relay, AspectList crystals,
            @Nullable VisRelayNetwork.Connection source) {}

    private static void spendAura(ServerLevel level, List<BlockPos> anchors, int vis) {
        if (vis == 0) return;
        float share = Math.max(1, (float) vis / anchors.size());
        float remaining = vis;
        while (remaining > 0.001F) {
            float before = remaining;
            for (BlockPos anchor : anchors) {
                remaining -= AuraHelper.drainVis(level, anchor, Math.min(remaining, share), false);
                if (remaining <= 0.001F) return;
            }
            if (before - remaining < 0.001F) return;
        }
    }

    private boolean hasRequiredCrystals(AspectList crystals) {
        if (crystals.isEmpty()) {
            return true;
        }
        for (AspectInstance need : crystals.entries()) {
            int index = PRIMAL_ORDER.indexOf(need.aspectId());
            if (index < 0) {
                return false;
            }
            ItemStack stack = craftInventory.getItem(WorkbenchContainer.CRAFTING_SLOTS + index);
            AspectInstance held = TTItemData.getCrystalAspect(stack);
            if (!(stack.getItem() instanceof VisCrystalItem) || held == null
                    || !held.aspectId().equals(need.aspectId())
                    || stack.getCount() < need.amount()) {
                return false;
            }
        }
        return true;
    }

    private void consumeCrystals(AspectList crystals) {
        for (AspectInstance need : crystals.entries()) {
            int index = PRIMAL_ORDER.indexOf(need.aspectId());
            if (index < 0) {
                continue;
            }
            ItemStack stack = craftInventory.getItem(WorkbenchContainer.CRAFTING_SLOTS + index);
            stack.shrink(need.amount());
            craftInventory.setChanged();
        }
    }

    private List<BlockPos> auraAnchors(Level level) {
        BlockPos pos = workbench.getBlockPos();
        if (!level.getBlockState(pos.above()).is(TTBulkContent.BLOCKS.get("arcane_workbench_charger").get())) {
            return List.of(pos);
        }
        ChunkPos origin = new ChunkPos(pos);
        List<BlockPos> anchors = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos anchor = new ChunkPos(origin.x + dx, origin.z + dz).getMiddleBlockPosition(pos.getY());
                if (level.hasChunkAt(anchor)) anchors.add(anchor);
            }
        }
        anchors.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        return anchors;
    }

    private static float availableAura(Level level, List<BlockPos> anchors) {
        float total = 0;
        for (BlockPos anchor : anchors) total += AuraHelper.getVis(level, anchor);
        return total;
    }

    @Override
    public void broadcastChanges() {
        if (craftsOnServer()) updatePayment((ServerLevel) workbench.getLevel());
        super.broadcastChanges();
    }

    public int getCachedVis() {
        return data.get(AURA_DATA);
    }

    public int getRequiredVis() {
        return data.get(REQUIRED_VIS_DATA);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();
        if (index == RESULT_SLOT) {
            return quickMoveCraftResult();
        } else if (index >= PLAYER_START) {
            if (!moveFromPlayer(stack)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof VisCrystalItem) {
            AspectInstance aspect = TTItemData.getCrystalAspect(stack);
            if (aspect != null) {
                int index = PRIMAL_ORDER.indexOf(aspect.aspectId());
                if (index >= 0) {
                    int menuIndex = CRYSTAL_MENU_START + index;
                    if (moveItemStackTo(stack, menuIndex, menuIndex + 1, false)) {
                        return true;
                    }
                }
            }
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        if (path.contains("wand") || path.contains("staff") || path.contains("scepter")) {
            if (moveItemStackTo(stack, WAND_MENU_SLOT, WAND_MENU_SLOT + 1, false)) {
                return true;
            }
        }
        return moveItemStackTo(stack, GRID_START, GRID_END, false);
    }

    private ItemStack quickMoveCraftResult() {
        ItemStack first = ItemStack.EMPTY;
        while (true) {
            ItemStack preview = resultContainer.getItem(0);
            if (preview.isEmpty() || !hasRoomForResult(preview)) return first;
            ItemStack crafted;
            if (craftsOnServer()) {
                crafted = takeCraft();
            } else {
                crafted = preview.copy();
                resultContainer.setItem(0, ItemStack.EMPTY);
            }
            if (crafted.isEmpty()) return first;
            if (first.isEmpty()) first = crafted.copy();
            moveItemStackTo(crafted, PLAYER_START, PLAYER_END, true);
        }
    }

    private boolean hasRoomForResult(ItemStack result) {
        int remaining = result.getCount();
        for (int i = PLAYER_START; i < PLAYER_END; i++) {
            Slot slot = slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty() || ItemStack.isSameItemSameTags(existing, result)) {
                remaining -= Math.max(0, slot.getMaxStackSize(result) - existing.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TTBlocks.ARCANE_WORKBENCH.get());
    }

    private boolean researchGateOpen(ArcaneCraftingRecipe recipe) {
        if (player.level().isClientSide) {
            return true;
        }
        return ResearchGate.isOpen(TTAttachments.knowledge(player), recipe.research());
    }
}
