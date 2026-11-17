package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchChargerBlock;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class FocalManipulatorBlockEntity extends BlockEntity implements MenuProvider {
    private static final int VIS_PER_SECOND = 20;

    private final SimpleContainer focus = new SimpleContainer(1);
    public static final int MAX_NAME_LENGTH = 50;

    private FocusSpell draft = FocusSpell.empty();
    private String name = "";
    private int focusVersion;
    private boolean loading;
    private float remainingVis;
    private float totalVis;

    public FocalManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(TTBlockEntities.FOCAL_MANIPULATOR.get(), pos, state);
        focus.addListener(container -> {
            if (loading)
                return;
            ItemStack stack = container.getItem(0);
            FocusSpell saved = FocusSpell.read(stack);
            draft = saved == null ? FocusSpell.empty() : saved;
            name = stack.hasCustomHoverName() ? trimName(stack.getHoverName().getString()) : "";
            focusVersion++;
            remainingVis = 0;
            totalVis = 0;
            syncDraft();
        });
    }

    static boolean canOpen(Player player) {
        return TTAttachments.knowledge(player).isComplete(TTIds.rl("unlock_auromancy"));
    }

    public SimpleContainer focus() {
        return focus;
    }

    public FocusSpell draft() {
        return draft;
    }

    public String draftName() {
        return name;
    }

    int focusVersion() {
        return focusVersion;
    }

    void setDraft(FocusSpell spell, String name) {
        String boundedName = trimName(name);
        if (!busy() && (!draft.equals(spell) || !this.name.equals(boundedName))) {
            draft = spell;
            this.name = boundedName;
            syncDraft();
        }
    }

    private static String trimName(String value) {
        String clean = net.minecraft.SharedConstants.filterText(value);
        return clean.substring(0, Math.min(clean.length(), MAX_NAME_LENGTH));
    }

    private void syncDraft() {
        if (level instanceof ServerLevel server) {
            setChanged();
            var state = getBlockState();
            server.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public boolean busy() {
        return remainingVis > 0;
    }

    int progress() {
        return busy() ? Math.min(99, (int) (100 * (1 - remainingVis / totalVis))) : 0;
    }

    FocalManipulatorMenu.Status status(Player player) {
        ItemStack stack = focus.getItem(0);
        if (busy())
            return FocalManipulatorMenu.Status.WRITING;
        if (!StarterFocus.isFocus(stack))
            return FocalManipulatorMenu.Status.NO_FOCUS;
        if (!draft.complete())
            return FocalManipulatorMenu.Status.INCOMPLETE;
        if (!canOpen(player) || !StarterFocus.permitted(player, draft))
            return FocalManipulatorMenu.Status.RESEARCH;
        if (!StarterFocus.fits(stack, draft))
            return FocalManipulatorMenu.Status.TIER;
        if (!player.getAbilities().instabuild) {
            if (player.experienceLevel < draft.inscriptionLevels())
                return FocalManipulatorMenu.Status.LEVELS;
            if (!hasCrystals(player.getInventory(), draft.crystals()))
                return FocalManipulatorMenu.Status.CRYSTALS;
        }
        return FocalManipulatorMenu.Status.READY;
    }

    public boolean startInscribing(Player player) {
        if (!(level instanceof ServerLevel server) || status(player) != FocalManipulatorMenu.Status.READY)
            return false;
        if (!player.getAbilities().instabuild) {
            takeCrystals(player.getInventory(), draft.crystals());
            player.giveExperienceLevels(-draft.inscriptionLevels());
        }
        totalVis = draft.inscriptionVis(StarterFocus.budget(focus.getItem(0)));
        remainingVis = totalVis;
        server.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6F, 0.8F);
        setChanged();
        return true;
    }

    void tick() {
        if (!busy() || !(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0)
            return;
        float drawn = drainAura(server, Math.min(VIS_PER_SECOND, remainingVis));
        if (drawn <= 0)
            return;
        remainingVis -= drawn;
        if (!busy()) {
            ItemStack stack = focus.getItem(0);
            draft.write(stack);
            if (name.isBlank())
                stack.resetHoverName();
            else
                stack.setHoverName(Component.literal(name));
            totalVis = 0;
            syncDraft();
            server.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1, 1);
        }
        setChanged();
    }

    private float drainAura(ServerLevel server, float wanted) {
        var anchors = ArcaneWorkbenchChargerBlock.auraAnchors(server, worldPosition);
        if (anchors.isEmpty())
            return 0;
        if (anchors.size() == 1)
            return AuraHelper.drainVis(server, anchors.get(0), wanted, false);

        float share = Math.max(1, wanted / anchors.size());
        float owed = wanted;
        boolean progressed;
        do {
            progressed = false;
            for (BlockPos anchor : anchors) {
                if (owed <= 0)
                    break;
                float drained = AuraHelper.drainVis(server, anchor, Math.min(share, owed), false);
                progressed |= drained > 0.01F;
                owed -= drained;
            }
        } while (owed > 0.01F && progressed);
        return wanted - owed;
    }

    private static ResourceLocation crystalAspect(ItemStack stack) {
        if (!stack.is(TTItems.ESSENTIA_CRYSTAL.get()))
            return null;
        var aspect = TTItemData.getCrystalAspect(stack);
        return aspect != null && aspect.amount() == 1 ? aspect.aspectId() : null;
    }

    private static boolean hasCrystals(Inventory inventory, Map<ResourceLocation, Integer> costs) {
        Map<ResourceLocation, Integer> remaining = new HashMap<>(costs);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            remaining.computeIfPresent(crystalAspect(stack), (aspect, count) -> count - stack.getCount());
        }
        return remaining.values().stream().allMatch(count -> count <= 0);
    }

    private static void takeCrystals(Inventory inventory, Map<ResourceLocation, Integer> costs) {
        Map<ResourceLocation, Integer> remaining = new HashMap<>(costs);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ResourceLocation aspect = crystalAspect(stack);
            int taken = Math.min(stack.getCount(), remaining.getOrDefault(aspect, 0));
            if (taken == 0)
                continue;
            stack.shrink(taken);
            remaining.put(aspect, remaining.get(aspect) - taken);
        }
        inventory.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumaturge.focal_manipulator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FocalManipulatorMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Focus", focus.getItem(0).save(new CompoundTag()));
        tag.put("Draft", draft.save());
        tag.putString("Name", name);
        tag.putInt("FocusVersion", focusVersion);
        tag.putFloat("RemainingVis", remainingVis);
        tag.putFloat("TotalVis", totalVis);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loading = true;
        try {
            focus.setItem(0, ItemStack.of(tag.getCompound("Focus")));
        } finally {
            loading = false;
        }
        focusVersion = tag.getInt("FocusVersion");
        FocusSpell saved = FocusSpell.readDraft(tag.getCompound("Draft"));
        if (saved == null)
            saved = FocusSpell.read(focus.getItem(0));
        draft = saved == null ? FocusSpell.empty() : saved;
        ItemStack stack = focus.getItem(0);
        name = tag.contains("Name") ? trimName(tag.getString("Name")) : stack.hasCustomHoverName() ? trimName(stack.getHoverName().getString()) : "";
        totalVis = 0;
        remainingVis = 0;
        if (!draft.complete() || !StarterFocus.isFocus(focus.getItem(0)))
            return;
        float total = tag.getFloat("TotalVis");
        float remaining = tag.getFloat("RemainingVis");
        if (total > 0 && remaining > 0 && remaining <= total) {
            totalVis = total;
            remainingVis = remaining;
        }
    }
}
