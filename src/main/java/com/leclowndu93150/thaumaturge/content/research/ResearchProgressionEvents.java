package com.leclowndu93150.thaumaturge.content.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.config.ThaumaturgeCommonConfig;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import com.leclowndu93150.thaumaturge.content.research.scan.DiscoveryScans;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Early progression: craft flags, crystal → dream journal → Salis Mundus path,
 * plus auromancy altitude milestones (CE {@code checkAltitude} parity).
 */
@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResearchProgressionEvents {
    private static final ResourceLocation BOOK = TTIds.rl("gotthaumonomicon");
    private static final ResourceLocation CRYSTALS = TTIds.rl("gotcrystals");
    private static final ResourceLocation DREAM = TTIds.rl("gotdream");
    private static final ResourceLocation UNLOCK_AUROMANCY = TTIds.rl("unlock_auromancy");
    private static final ResourceLocation DEEP_DOWN = TTIds.rl("m_deepdown");
    private static final ResourceLocation UP_HIGH = TTIds.rl("m_uphigh");
    private static final ResourceLocation HELL_AND_BACK = TTIds.rl("m_hellandback");

    private static final String CRYSTALS_MESSAGE = "message.thaumaturge.discovery.crystals";
    private static final String DREAM_MESSAGE = "message.thaumaturge.discovery.dream";
    private static final String DEEP_DOWN_MESSAGE = "message.thaumaturge.discovery.deep_down";
    private static final String UP_HIGH_MESSAGE = "message.thaumaturge.discovery.up_high";
    private static final String HELL_AND_BACK_MESSAGE = "message.thaumaturge.discovery.hell_and_back";
    private static final int BIOME_CHECK_INTERVAL = 200;
    private static final String BOOK_TITLE_KEY = "book.thaumaturge.start.title";
    private static final List<String> BOOK_PAGE_KEYS = List.of(
            "book.thaumaturge.start.1",
            "book.thaumaturge.start.2",
            "book.thaumaturge.start.3");

    /** CE ResearchProgressionEvents.DEEP_MARGIN_BLOCKS */
    private static final int DEEP_MARGIN_BLOCKS = 10;
    /** CE ResearchProgressionEvents.HIGH_HEIGHT_FACTOR */
    private static final double HIGH_HEIGHT_FACTOR = 0.4;

    private static final ResourceLocation[] EARLY_CRAFTS = {
            TTItems.SALIS_MUNDUS.getId(),
            TTItems.THAUMOMETER.getId(),
            TTItems.THAUMONOMICON.getId(),
            TTItems.ARCANE_WORKBENCH.getId(),
            TTItems.CRUCIBLE.getId(),
            TTItems.RESEARCH_TABLE.getId(),
            TTItems.PHIAL.getId()
    };

    /** Unlock → base pairs so old saves get category openers / base_* after Wave 5 sibling fix. */
    private static final ResourceLocation[][] UNLOCK_BASE_PAIRS = {
            { TTIds.rl("unlock_alchemy"), TTIds.rl("base_alchemy") },
            { TTIds.rl("unlock_artifice"), TTIds.rl("base_artifice") },
            { TTIds.rl("unlock_infusion"), TTIds.rl("base_infusion") },
            { TTIds.rl("unlock_golemancy"), TTIds.rl("base_golemancy") },
            { TTIds.rl("unlock_eldritch"), TTIds.rl("base_eldritch") }
    };

    private ResearchProgressionEvents() {}

    public static void bookOpened(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide) {
            PlayerKnowledge knowledge = TTAttachments.knowledge(serverPlayer);
            backfillCategoryBases(serverPlayer, knowledge);
            if (ResearchManager.complete(serverPlayer, BOOK)) {
                return;
            }
            if (!knowledge.isComplete(BOOK)) {
                knowledge.startResearch(BOOK);
                knowledge.markResearchComplete(BOOK, 1);
                TTAttachments.syncKnowledge(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        backfillCategoryBases(player, TTAttachments.knowledge(player));
        DiscoveryScans.restoreKnownScans(player);
    }

    /** If an unlock_* is complete but its base_* sibling was never granted (pre-Wave-5 saves), grant it now. */
    private static void backfillCategoryBases(ServerPlayer player, PlayerKnowledge knowledge) {
        boolean changed = false;
        for (ResourceLocation[] pair : UNLOCK_BASE_PAIRS) {
            if (knowledge.isComplete(pair[0]) && !knowledge.isComplete(pair[1])) {
                if (ResearchManager.complete(player, pair[1])) {
                    changed = true;
                }
            }
        }
        if (changed) {
            TTAttachments.syncKnowledge(player);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        recordCrafted(player, event.getCrafting());
    }

    /** CE also credits craft flags when the result is picked up (e.g. crucible eject). */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        ItemStack stack = event.getStack();
        recordCrafted(player, stack);
        if (stack.is(TTItems.THAUMONOMICON.get())) {
            ResearchManager.complete(player, BOOK);
        }
        if (stack.is(TTItems.ESSENTIA_CRYSTAL.get())) {
            crystalPickup(player);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (dreamUnlocked(player)) {
            grantDreamJournal(player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (knowledge.isKnown(TTIds.rl("base_auromancy")) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            ResearchManager.progressResearch(player, TTIds.rl("f_onfire"));
        }
        if (knowledge.isKnown(TTIds.rl("focus_projectile")) && event.getSource().getDirectEntity() != null) {
            var type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getSource().getDirectEntity().getType());
            String flag = switch (type.getPath()) {
                case "arrow", "spectral_arrow", "trident" -> "f_arrow";
                case "fireball", "small_fireball", "dragon_fireball", "wither_skull" -> "f_fireball";
                case "llama_spit" -> "f_spit";
                default -> null;
            };
            if (flag != null) {
                ResearchManager.progressResearch(player, TTIds.rl(flag));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        boolean auromancyPending = knowledge.isKnown(UNLOCK_AUROMANCY) && !knowledge.isComplete(UNLOCK_AUROMANCY);
        if (auromancyPending) {
            checkAltitude(player, knowledge);
        }
        if (player.tickCount % BIOME_CHECK_INTERVAL == 0) {
            checkBiomes(player, knowledge);
            com.leclowndu93150.thaumaturge.content.taint.FluxDiscovery.check(player);
        }
    }

    private static void checkAltitude(ServerPlayer player, PlayerKnowledge knowledge) {
        Level level = player.level();
        double y = player.getY();
        if (y < level.getMinBuildHeight() + DEEP_MARGIN_BLOCKS) {
            discover(player, knowledge, DEEP_DOWN, DEEP_DOWN_MESSAGE);
        }
        if (y > level.getMaxBuildHeight() * HIGH_HEIGHT_FACTOR) {
            discover(player, knowledge, UP_HIGH, UP_HIGH_MESSAGE);
        }
    }

    private static void checkBiomes(ServerPlayer player, PlayerKnowledge knowledge) {
        if (player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_NETHER)) {
            discover(player, knowledge, HELL_AND_BACK, HELL_AND_BACK_MESSAGE);
        }
    }

    private static void discover(ServerPlayer player, PlayerKnowledge knowledge, ResourceLocation research, String messageKey) {
        if (knowledge.isComplete(research)) {
            return;
        }
        if (ResearchManager.progressResearch(player, research)) {
            player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    /** Records a crafted stack toward research progress; shared by vanilla crafting, crucible, and dust crafting. */
    public static void recordCrafted(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation crafted = ResearchManager.craftedKey(itemId);
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        if (knowledge.complete(crafted)) {
            TTAttachments.syncKnowledge(player);
        }
        for (ResourceLocation early : EARLY_CRAFTS) {
            if (early.equals(itemId)) {
                ResearchManager.advanceStage(player, TTIds.rl("first_steps"));
                break;
            }
        }
        ResearchManager.advanceStage(player, TTIds.rl("unlock_alchemy"));
        ResearchManager.advanceStage(player, TTIds.rl("base_alchemy"));
        ResearchManager.advanceStage(player, UNLOCK_AUROMANCY);
        ResearchManager.advanceStage(player, TTIds.rl("base_auromancy"));
    }

    private static void crystalPickup(ServerPlayer player) {
        if (awardIfNew(player, CRYSTALS)) {
            tell(player, CRYSTALS_MESSAGE);
        }
        if (ThaumaturgeCommonConfig.NO_SLEEP.get() && dreamUnlocked(player)) {
            grantDreamJournal(player);
        }
    }

    private static boolean dreamUnlocked(ServerPlayer player) {
        PlayerKnowledge knowledge = TTAttachments.knowledge(player);
        return knowledge.isKnown(CRYSTALS) && !knowledge.isKnown(DREAM);
    }

    private static void grantDreamJournal(ServerPlayer player) {
        ResearchManager.complete(player, DREAM);
        ItemStack book = writeDreamBook(player);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        tell(player, DREAM_MESSAGE);
    }

    private static ItemStack writeDreamBook(ServerPlayer player) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.getOrCreateTag().putString("title", Component.translatable(BOOK_TITLE_KEY).getString());
        book.getOrCreateTag().putString("author", player.getGameProfile().getName());
        ListTag pages = new ListTag();
        for (String key : BOOK_PAGE_KEYS) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(key))));
        }
        book.getOrCreateTag().put("pages", pages);
        return book;
    }

    private static boolean awardIfNew(ServerPlayer player, ResourceLocation research) {
        if (TTAttachments.knowledge(player).isKnown(research)) {
            return false;
        }
        return ResearchManager.complete(player, research);
    }

    private static void tell(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
