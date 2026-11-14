package com.leclowndu93150.thaumaturge.client.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.research.scan.ScanKeys;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundScanSlotPacket;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/** Hold a thaumometer on the cursor over an inventory slot (or your portrait) to scan it (CE InventoryScanHandler). */
@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class InventoryScanHandler {
    private static final int SOUND_INTERVAL = 2;
    private static final float SOUND_VOLUME = 0.2F;
    private static final float SOUND_PITCH = 0.45F;
    private static final float SOUND_PITCH_SPREAD = 0.1F;
    private static final int PLAYER_PANEL_X = 26;
    private static final int PLAYER_PANEL_Y = 8;
    private static final int PLAYER_PANEL_WIDTH = 52;
    private static final int PLAYER_PANEL_HEIGHT = 70;
    private static final int CURSOR_TOP_OFFSET = -36;
    private static final int TAG_TOP_OFFSET = -44;
    private static final int TAG_SPACING = 18;
    private static final int SCAN_DOTS = 3;
    private static final int PROGRESS_COLOR = 0xFFFFAA00;
    private static final int HINT_COLOR = 0xFFAA00AA;
    /** Above vanilla item tooltips so study hints stay readable. */
    private static final float OVERLAY_Z = 500.0F;
    private static final int NO_TARGET = Integer.MIN_VALUE;

    private static int target = NO_TARGET;
    private static int ticks;

    private InventoryScanHandler() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen
                || !(player.containerMenu.getCarried().getItem() instanceof ThaumometerItem)) {
            target = NO_TARGET;
            ticks = 0;
            return;
        }
        int hovered = targetUnderMouse(screen, player, event.getMouseX(), event.getMouseY());
        if (hovered != target) {
            target = hovered;
            ticks = 0;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Slot slot = hoveredSlot(player);
        if (slot != null) {
            // Vanilla hides item tooltips while the cursor carries a stack; the scan target still needs one.
            graphics.renderTooltip(minecraft.font, slot.getItem(), event.getMouseX(), event.getMouseY());
        }
        int top = event.getMouseY() + CURSOR_TOP_OFFSET;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, OVERLAY_Z);
        if (ticks > 0) {
            Component progress = scanning(ticks / (float) ServerboundScanSlotPacket.CHANNEL_TICKS);
            graphics.drawString(minecraft.font, progress, event.getMouseX() - minecraft.font.width(progress) / 2, top, PROGRESS_COLOR, true);
            graphics.pose().popPose();
            return;
        }
        if (slot != null) {
            renderKnownAspects(graphics, minecraft, player, slot.getItem(), event.getMouseX(), event.getMouseY() + TAG_TOP_OFFSET);
        }
        Component hint = studyHint(player, targetAspects(player));
        if (hint != null) {
            graphics.drawString(minecraft.font, hint, event.getMouseX() - minecraft.font.width(hint) / 2, top, HINT_COLOR, true);
        }
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || target == NO_TARGET || !(minecraft.screen instanceof AbstractContainerScreen<?>) || !(player.containerMenu.getCarried().getItem() instanceof ThaumometerItem)) {
            target = NO_TARGET;
            ticks = 0;
            return;
        }
        boolean scannable = target == ServerboundScanSlotPacket.SELF || hoveredSlot(player) != null;
        if (!scannable || studyHint(player, targetAspects(player)) != null || !isStillScannable(player)) {
            ticks = 0;
            return;
        }
        ticks++;
        if (ticks % SOUND_INTERVAL == 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TTSounds.CAMERA_TICKS.get(), SOUND_PITCH + player.getRandom().nextFloat() * SOUND_PITCH_SPREAD, SOUND_VOLUME));
        }
        if (ticks >= ServerboundScanSlotPacket.CHANNEL_TICKS) {
            TTNetwork.CHANNEL.sendToServer(new ServerboundScanSlotPacket(target));
            ticks = 0;
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof ThaumometerItem) {
            event.getToolTip().add(Component.translatable("tooltip.thaumaturge.thaumometer.inventory_scan").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (event.getItemStack().is(TTItems.TABLE.get())) {
            event.getToolTip().add(Component.translatable("tooltip.thaumaturge.table.research").withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component scanning(float progress) {
        int dots = Mth.clamp((int) (progress * SCAN_DOTS) + 1, 1, SCAN_DOTS);
        return Component.translatable("gui.thaumaturge.scan.scanning").append(".".repeat(dots)).withStyle(ChatFormatting.GOLD);
    }

    private static @Nullable Component studyHint(LocalPlayer player, AspectList aspects) {
        for (AspectInstance entry : aspects.entries()) {
            if (!AspectPools.hasDiscoveredComponents(player, entry.aspectId())) {
                return AspectPools.missingComponentHint(player, entry.aspectId());
            }
        }
        return null;
    }

    private static boolean isStillScannable(LocalPlayer player) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        if (target == ServerboundScanSlotPacket.SELF) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(player.getType());
            if (!knowledge.isScanned(id) || !knowledge.isKnown(ScanKeys.entity(player.getType()))) {
                return true;
            }
            return hasUndiscoveredAspects(player, EntityAspects.of(player));
        }
        Slot slot = hoveredSlot(player);
        if (slot == null) {
            return false;
        }
        ItemStack stack = slot.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!knowledge.isScanned(itemId) || !knowledge.isKnown(ScanKeys.item(stack.getItem()))) {
            return true;
        }
        return hasUndiscoveredAspects(player, AspectIndex.of(stack));
    }

    private static boolean hasUndiscoveredAspects(LocalPlayer player, AspectList aspects) {
        for (AspectInstance entry : aspects.entries()) {
            if (AspectPools.hasDiscoveredComponents(player, entry.aspectId()) && !AspectPools.isDiscovered(player, entry.aspectId())) {
                return true;
            }
        }
        return false;
    }

    private static void renderKnownAspects(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, ItemStack stack, int centreX, int y) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!ClientKnowledge.get().isScanned(itemId) && !ClientKnowledge.get().isKnown(ScanKeys.item(stack.getItem()))) {
            return;
        }
        AspectList aspects = AspectIndex.of(stack);
        if (aspects.isEmpty()) {
            return;
        }
        int x = centreX - (aspects.entries().size() * TAG_SPACING - (TAG_SPACING - AspectTagRenderer.TAG_SIZE)) / 2;
        for (AspectInstance entry : aspects.entries()) {
            IAspect aspect = TTAspects.get(entry.aspectId());
            if (aspect != null && AspectPools.isDiscovered(player, entry.aspectId())) {
                AspectTagRenderer.render(graphics, minecraft.font, x, y, aspect, entry.amount());
            }
            x += TAG_SPACING;
        }
    }

    private static int targetUnderMouse(AbstractContainerScreen<?> screen, LocalPlayer player, int mouseX, int mouseY) {
        if (screen instanceof InventoryScreen && overPlayerPanel(screen, mouseX, mouseY)) {
            return ServerboundScanSlotPacket.SELF;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem() || !slot.mayPickup(player) || slot instanceof ResultSlot) {
            return NO_TARGET;
        }
        return slot.index;
    }

    private static boolean overPlayerPanel(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        int left = screen.getGuiLeft() + PLAYER_PANEL_X;
        int top = screen.getGuiTop() + PLAYER_PANEL_Y;
        return mouseX >= left && mouseX < left + PLAYER_PANEL_WIDTH && mouseY >= top && mouseY < top + PLAYER_PANEL_HEIGHT;
    }

    private static @Nullable Slot hoveredSlot(LocalPlayer player) {
        if (target == NO_TARGET || target == ServerboundScanSlotPacket.SELF || target < 0 || target >= player.containerMenu.slots.size()) {
            return null;
        }
        Slot slot = player.containerMenu.getSlot(target);
        return slot.hasItem() ? slot : null;
    }

    private static AspectList targetAspects(LocalPlayer player) {
        if (target == ServerboundScanSlotPacket.SELF) {
            return EntityAspects.of(player);
        }
        Slot slot = hoveredSlot(player);
        return slot == null ? AspectList.EMPTY : AspectIndex.of(slot.getItem());
    }
}
