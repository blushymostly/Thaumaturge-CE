package com.leclowndu93150.thaumaturge.content.research.note;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeReward;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeType;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchData;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTItemTags;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class ResearchNotes {
    public static final String NOTE_KEY = "ResearchNote";

    private ResearchNotes() {}

    public static int theoryRowsBefore(ResearchEntry entry, int stageIndex) {
        int count = 0;
        for (int i = 0; i < stageIndex && i < entry.stages().size(); i++) {
            count += theoryRows(entry.stages().get(i));
        }
        return count;
    }

    private static boolean isReachableTheoryRow(PlayerKnowledge knowledge, ResourceLocation entryId, ResearchEntry entry, int ordinal) {
        if (!knowledge.isKnown(entryId) || knowledge.isComplete(entryId)) {
            return false;
        }
        int stage = Math.max(0, knowledge.stage(entryId));
        if (stage >= entry.stages().size()) {
            return false;
        }
        return ordinal >= 0 && ordinal < theoryRowsBefore(entry, stage + 1);
    }

    public static int theoryRows(ResearchStage stage) {
        int count = 0;
        for (KnowledgeReward reward : stage.requiredKnowledge()) {
            if (reward.type() == KnowledgeType.THEORY) {
                count++;
            }
        }
        return count;
    }

    public static AspectList anchors(ResearchEntry entry) {
        AspectList authored = entry.noteAspects();
        if (!authored.isEmpty()) {
            return authored;
        }
        AspectList fallback = AspectList.EMPTY;
        for (ResourceLocation primal : new ResourceLocation[] {TTAspects.AER, TTAspects.IGNIS, TTAspects.AQUA}) {
            fallback = fallback.add(new AspectInstance(primal, 1));
        }
        return fallback;
    }

    /** CE: observation stage cost = note_aspects × max(1, amount). */
    public static AspectList observationCost(ResearchEntry entry, int amount) {
        AspectList cost = AspectList.EMPTY;
        int multiplier = Math.max(1, amount);
        for (AspectInstance instance : entry.noteAspects().entries()) {
            cost = cost.add(new AspectInstance(instance.aspectId(), instance.amount() * multiplier));
        }
        return cost;
    }

    /** Max non-theory knowledge amount on the stage, or -1 if none. */
    public static int stageObservationAmount(ResearchStage stage) {
        int amount = -1;
        for (KnowledgeReward reward : stage.requiredKnowledge()) {
            if (reward.type() != KnowledgeType.THEORY) {
                amount = Math.max(amount, reward.amount());
            }
        }
        return amount;
    }

    public static AspectList stageObservationCost(ResearchEntry entry, ResearchStage stage) {
        int amount = stageObservationAmount(stage);
        return amount < 0 ? AspectList.EMPTY : observationCost(entry, amount);
    }

    public static @Nullable ResearchNoteData dataOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TTItemData.ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = tag.getCompound(TTItemData.ROOT);
        if (!root.contains(NOTE_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return ResearchNoteData.CODEC.parse(NbtOps.INSTANCE, root.get(NOTE_KEY)).result().orElse(null);
    }

    public static void write(ItemStack stack, ResearchNoteData data) {
        ResearchNoteData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(encoded -> TTItemData.root(stack).put(NOTE_KEY, encoded));
    }

    public static boolean hasNoteFor(Player player, ResourceLocation learnKey) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ResearchNoteData data = dataOf(inv.getItem(i));
            if (data != null && data.learnKey().equals(learnKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean obtainNote(ServerPlayer player, ResourceLocation entryId, int ordinal) {
        ResearchEntry entry = ResearchData.entry(entryId);
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (entry == null || !isReachableTheoryRow(knowledge, entryId, entry, ordinal)) {
            return false;
        }
        ResourceLocation learnKey = ResearchNoteData.learnKey(entryId, ordinal);
        if (knowledge.isKnown(learnKey) || hasNoteFor(player, learnKey)) {
            return false;
        }
        if ((!consumeInk(player, true) || !hasItem(player, Items.PAPER)) && !player.getAbilities().instabuild) {
            player.sendSystemMessage(Component.translatable("message.thaumaturge.research_note.missing_tools").withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }
        consumeInk(player, false);
        consumeItem(player, Items.PAPER);
        ResearchNoteData data = NoteGenerator.generate(entryId, ordinal, anchors(entry), entry.complexity(), player.getRandom());
        ItemStack note = new ItemStack(TTItems.RESEARCH_NOTE.get());
        write(note, data);
        if (!player.getInventory().add(note)) {
            player.drop(note, false);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.WRITE.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        return true;
    }

    public static boolean consumeInk(Player player, boolean simulate) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(TTItemTags.SCRIBING_TOOLS) && stack.getDamageValue() < stack.getMaxDamage()) {
                if (!simulate) {
                    stack.setDamageValue(stack.getDamageValue() + 1);
                }
                return true;
            }
        }
        if (player.containerMenu instanceof MenuResearchTable table && table.blockEntity() != null && table.blockEntity().hasInkReady()) {
            return simulate || table.blockEntity().consumeInk();
        }
        return false;
    }

    private static boolean hasItem(Player player, Item item) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeItem(Player player, Item item) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static Component entryName(ResourceLocation entryId) {
        ResearchEntry entry = ResearchData.entry(entryId);
        return entry == null ? Component.literal(entryId.toString()) : Component.translatable(entry.nameKey());
    }
}
