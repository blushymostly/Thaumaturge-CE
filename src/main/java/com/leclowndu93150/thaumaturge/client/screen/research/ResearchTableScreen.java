package com.leclowndu93150.thaumaturge.client.screen.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.content.research.note.HexGrid;
import com.leclowndu93150.thaumaturge.content.research.note.NoteRules;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTableCombinePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTableDuplicatePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundTablePlaceAspectPacket;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Forge 1.20.1 port of the CE research table screen: aspect palette, combine tray, hex note sheet. */
public final class ResearchTableScreen extends AbstractContainerScreen<MenuResearchTable> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/guiresearchtable2.png");
    private static final ResourceLocation PARCHMENT = TTIds.rl("textures/misc/parchment3.png");
    private static final ResourceLocation HEX_IDLE = TTIds.rl("textures/gui/hex1.png");
    private static final ResourceLocation HEX_HOVER = TTIds.rl("textures/gui/hex2.png");
    private static final ResourceLocation UNKNOWN_ASPECT = TTIds.rl("textures/aspects/_unknown.png");
    private static final int LINE_HALF_WIDTH = 1;
    private static final int LINE_COLOR = (153 << 24) | (0 << 16) | (153 << 8) | 204;

    private static final int GUI_SIZE = 255;
    private static final int MAIN_PANE_H = 167;
    private static final int LOWER_PANEL_X = 40;
    private static final int LOWER_PANEL_V = 166;
    private static final int LOWER_PANEL_W = 184;
    private static final int LOWER_PANEL_H = 88;

    private static final int PALETTE_X = 10;
    private static final int PALETTE_Y = 40;
    private static final int PALETTE_CELL = 16;
    private static final int PALETTE_ROWS = 5;
    private static final int PALETTE_SLOTS = 25;
    private static final int PALETTE_W = 80;
    private static final int SEARCH_Y = 31;
    private static final int SEARCH_MAX_LENGTH = 32;
    private static final int PALETTE_H = 80;
    private static final int PAGE_STEP = 5;

    private static final int ARROW_PREV_X = 27;
    private static final int ARROW_NEXT_X = 51;
    private static final int ARROW_Y = 119;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 8;
    private static final int ARROW_PREV_U = 184;
    private static final int ARROW_NEXT_U = 208;
    private static final int ARROW_V = 208;
    private static final int PALETTE_SCROLL_H = ARROW_Y + ARROW_H - PALETTE_Y;
    private static final float PAGE_SOUND_VOLUME = 0.3F;

    private static final int SELECT1_CENTER_X = 21;
    private static final int SELECT2_CENTER_X = 79;
    private static final int SELECT_CENTER_Y = 147;
    private static final float SELECT_SCALE = 1.5F;
    private static final int SELECT1_HIT_X = 11;
    private static final int SELECT2_HIT_X = 71;
    private static final int SELECT_HIT_Y = 137;
    private static final int SELECT_SIZE = 16;

    private static final int COMBINE_X = 35;
    private static final int COMBINE_Y = 139;
    private static final int COMBINE_W = 32;
    private static final int COMBINE_H = 16;
    private static final int COMBINE_U = 184;
    private static final int COMBINE_V = 184;
    private static final int COMBINE_PRESSED_V = 168;
    private static final long COMBINE_COOLDOWN_MS = 200L;

    private static final int DUPE_X = 37;
    private static final int DUPE_Y = 5;
    private static final int DUPE_SIZE = 24;
    private static final int DUPE_U = 232;
    private static final int DUPE_V = 200;

    private static final int SHEET_X = 94;
    private static final int SHEET_Y = 8;
    private static final int SHEET_SIZE = 150;
    private static final int HEX_ORIGIN_X = 169;
    private static final int HEX_ORIGIN_Y = 83;
    private static final float HEX_SIZE = 9.0F;
    private static final int HEX_TILE_HALF = 8;
    private static final int ORB_OFFSET = -8;

    private static final int INK_WARN_X = 157;
    private static final int INK_WARN_Y = 84;

    private static final int HELPER_X = 41;
    private static final int HELPER_Y = 9;
    private static final int HELPER_SIZE = 16;
    private static final int HELPER_ROWS = 7;
    private static final int HELPER_ROW_START_Y = 10;
    private static final int HELPER_ROW_STRIDE = 17;
    private static final int HELPER_ARROW_Y = 144;
    private static final int HELPER_PAGE_HALF_GAP = 18;
    private static final int HELPER_ROW_WIDTH = 72;
    private static final double ASPECT_CLICK_DISTANCE_SQ = 4.0;

    private boolean helperOpen;
    private int helperPage;

    private @Nullable ResourceLocation draggedAspect;
    private double aspectDragStartX;
    private double aspectDragStartY;
    private @Nullable ResourceLocation select1;
    private @Nullable ResourceLocation select2;
    private int page;
    private @Nullable EditBox aspectSearch;
    private long combineCooldownUntil;

    public ResearchTableScreen(MenuResearchTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_SIZE;
        imageHeight = GUI_SIZE;
    }

    @Override
    protected void init() {
        super.init();
        aspectSearch = new EditBox(font, leftPos + PALETTE_X, topPos + SEARCH_Y, PALETTE_W, font.lineHeight, Component.translatable("gui.thaumaturge.thaumonomicon.search"));
        aspectSearch.setMaxLength(SEARCH_MAX_LENGTH);
        aspectSearch.setHint(Component.translatable("gui.thaumaturge.thaumonomicon.search"));
        aspectSearch.setResponder(ignored -> page = 0);
        addRenderableWidget(aspectSearch);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (aspectSearch != null && aspectSearch.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE && (aspectSearch.keyPressed(keyCode, scanCode, modifiers) || aspectSearch.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawDragged(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_SIZE, MAIN_PANE_H, 256, 256);
        graphics.blit(TEXTURE, leftPos + LOWER_PANEL_X, topPos + MAIN_PANE_H, 0, LOWER_PANEL_V, LOWER_PANEL_W, LOWER_PANEL_H, 256, 256);
        drawPalette(graphics, mouseX, mouseY);
        drawCombineTray(graphics, mouseX, mouseY);
        drawSheet(graphics, mouseX, mouseY);
        drawDuplicateButton(graphics, mouseX, mouseY);
        drawHelperButton(graphics, mouseX, mouseY);
        drawSlotHints(graphics, mouseX, mouseY);
    }

    // ---- data helpers -------------------------------------------------------------------------

    private @Nullable BlockEntityResearchTable table() {
        return menu.blockEntity();
    }

    private @Nullable ResearchNoteData noteData() {
        return ResearchNotes.dataOf(menu.slots.get(BlockEntityResearchTable.SLOT_NOTE).getItem());
    }

    private boolean hasInkReady() {
        ItemStack tools = menu.slots.get(BlockEntityResearchTable.SLOT_SCRIBE_TOOLS).getItem();
        return !tools.isEmpty() && tools.isDamageableItem() && tools.getDamageValue() < tools.getMaxDamage();
    }

    private boolean canDuplicate(@Nullable ResearchNoteData data) {
        return data != null && data.complete() && ClientKnowledge.get().isComplete(BlockEntityResearchTable.RESEARCH_DUPLICATION);
    }

    private boolean discovered(ResourceLocation aspect) {
        return minecraft != null && minecraft.player != null && AspectPools.isDiscovered(minecraft.player, aspect);
    }

    private int poolAmount(ResourceLocation aspect) {
        return ClientKnowledge.get().aspectAmount(aspect);
    }

    private int bonusAmount(ResourceLocation aspect) {
        BlockEntityResearchTable table = table();
        return table == null ? 0 : table.bonusAspects().amountOf(aspect);
    }

    private int availableOf(ResourceLocation aspect) {
        return poolAmount(aspect) + bonusAmount(aspect);
    }

    private static Component nameOf(ResourceLocation aspect) {
        String key = Util.makeDescriptionId("aspect", aspect);
        if (I18n.exists(key)) {
            return AspectNames.name(aspect);
        }
        String path = aspect.getPath();
        return Component.literal(path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1));
    }

    private List<ResourceLocation> discoveredAspects() {
        Set<ResourceLocation> ids = new HashSet<>(ClientKnowledge.get().poolAspectIds());
        TTAspects.all().forEach((id, aspect) -> {
            if (aspect.isPrimal()) {
                ids.add(id);
            }
        });
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : ids) {
            if (TTAspects.get(id) != null && discovered(id)) {
                result.add(id);
            }
        }
        result.sort(Comparator.<ResourceLocation, Boolean>comparing(id -> !TTAspects.get(id).isPrimal()).thenComparing(ResourceLocation::compareTo));
        if (aspectSearch != null && !aspectSearch.getValue().isBlank()) {
            String query = aspectSearch.getValue().toLowerCase(Locale.ROOT);
            result.removeIf(id -> !nameOf(id).getString().toLowerCase(Locale.ROOT).contains(query) && !id.toString().contains(query));
        }
        return result;
    }

    private List<ResourceLocation> discoveredCompounds() {
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : discoveredAspects()) {
            if (TTAspects.get(id).componentIds().size() == 2) {
                result.add(id);
            }
        }
        return result;
    }

    private void drawTag(GuiGraphics graphics, ResourceLocation aspect, int x, int y, float amount, float alpha) {
        IAspect value = TTAspects.get(aspect);
        if (value != null) {
            AspectTagRenderer.render(graphics, font, x, y, value, amount, alpha, false);
        }
    }

    private List<Component> aspectTooltip(ResourceLocation aspect) {
        List<Component> lines = new ArrayList<>();
        lines.add(nameOf(aspect));
        IAspect value = TTAspects.get(aspect);
        if (value != null && !value.isPrimal()) {
            lines.add(AspectNames.composition(aspect).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    // ---- palette ------------------------------------------------------------------------------

    private void drawPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ResourceLocation> aspects = discoveredAspects();
        int start = page * PAGE_STEP;
        int drawn = 0;
        ResourceLocation hovered = null;
        for (int i = start; i < aspects.size() && drawn < PALETTE_SLOTS; i++, drawn++) {
            ResourceLocation aspect = aspects.get(i);
            int x = leftPos + PALETTE_X + (drawn / PALETTE_ROWS) * PALETTE_CELL;
            int y = topPos + PALETTE_Y + (drawn % PALETTE_ROWS) * PALETTE_CELL;
            float alpha = availableOf(aspect) > 0 ? 1.0F : 0.33F;
            drawTag(graphics, aspect, x, y, availableOf(aspect), alpha);
            if (inRect(mouseX, mouseY, x, y, PALETTE_CELL, PALETTE_CELL)) {
                hovered = aspect;
            }
        }
        int lastPage = lastPage(aspects.size());
        if (page > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_PREV_X, topPos + ARROW_Y, ARROW_PREV_U, ARROW_V, ARROW_W, ARROW_H, 256, 256);
            if (inRect(mouseX, mouseY, leftPos + ARROW_PREV_X, topPos + ARROW_Y, ARROW_W, ARROW_H)) {
                graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.page_prev"), mouseX, mouseY);
            }
        }
        if (page < lastPage) {
            graphics.blit(TEXTURE, leftPos + ARROW_NEXT_X, topPos + ARROW_Y, ARROW_NEXT_U, ARROW_V, ARROW_W, ARROW_H, 256, 256);
            if (inRect(mouseX, mouseY, leftPos + ARROW_NEXT_X, topPos + ARROW_Y, ARROW_W, ARROW_H)) {
                graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.page_next"), mouseX, mouseY);
            }
        }
        if (hovered != null && draggedAspect == null) {
            graphics.renderComponentTooltip(font, aspectTooltip(hovered), mouseX, mouseY);
        }
    }

    private static int lastPage(int count) {
        return Math.max(0, (count - (PALETTE_SLOTS - PAGE_STEP)) / PAGE_STEP);
    }

    private @Nullable ResourceLocation paletteAspectAt(double mouseX, double mouseY) {
        if (mouseX < leftPos + PALETTE_X || mouseX >= leftPos + PALETTE_X + PALETTE_W || mouseY < topPos + PALETTE_Y || mouseY >= topPos + PALETTE_Y + PALETTE_H) {
            return null;
        }
        int col = (int) ((mouseX - leftPos - PALETTE_X) / PALETTE_CELL);
        int row = (int) ((mouseY - topPos - PALETTE_Y) / PALETTE_CELL);
        int index = page * PAGE_STEP + col * PALETTE_ROWS + row;
        List<ResourceLocation> aspects = discoveredAspects();
        return index >= 0 && index < aspects.size() ? aspects.get(index) : null;
    }

    // ---- combine tray -------------------------------------------------------------------------

    private void clearDepletedSelections() {
        if (select1 != null && availableOf(select1) <= 0) {
            select1 = null;
        }
        if (select2 != null && availableOf(select2) <= 0) {
            select2 = null;
        }
    }

    private void drawCombineTray(GuiGraphics graphics, int mouseX, int mouseY) {
        clearDepletedSelections();
        drawSelectTag(graphics, select1, leftPos + SELECT1_CENTER_X, topPos + SELECT_CENTER_Y);
        drawSelectTag(graphics, select2, leftPos + SELECT2_CENTER_X, topPos + SELECT_CENTER_Y);
        if (select1 != null && select2 != null) {
            graphics.blit(TEXTURE, leftPos + COMBINE_X, topPos + COMBINE_Y, COMBINE_U, COMBINE_V, COMBINE_W, COMBINE_H, 256, 256);
            if (System.currentTimeMillis() < combineCooldownUntil) {
                graphics.blit(TEXTURE, leftPos + COMBINE_X, topPos + COMBINE_Y, COMBINE_U, COMBINE_PRESSED_V, COMBINE_W, COMBINE_H, 256, 256);
            }
            if (inRect(mouseX, mouseY, leftPos + COMBINE_X, topPos + COMBINE_Y, COMBINE_W, COMBINE_H)) {
                graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.combine"), mouseX, mouseY);
            }
        }
        if (inRect(mouseX, mouseY, leftPos + SELECT1_HIT_X, topPos + SELECT_HIT_Y, SELECT_SIZE, SELECT_SIZE)) {
            graphics.renderTooltip(font, select1 != null ? nameOf(select1) : Component.translatable("gui.thaumaturge.research_table.select"), mouseX, mouseY);
        } else if (inRect(mouseX, mouseY, leftPos + SELECT2_HIT_X, topPos + SELECT_HIT_Y, SELECT_SIZE, SELECT_SIZE)) {
            graphics.renderTooltip(font, select2 != null ? nameOf(select2) : Component.translatable("gui.thaumaturge.research_table.select"), mouseX, mouseY);
        }
    }

    private void drawSelectTag(GuiGraphics graphics, @Nullable ResourceLocation aspect, int centerX, int centerY) {
        if (aspect == null) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(SELECT_SCALE, SELECT_SCALE, 1.0F);
        drawTag(graphics, aspect, -8, -8, 0.0F, 1.0F);
        graphics.pose().popPose();
    }

    private boolean hasAvailableComponents(ResourceLocation first, ResourceLocation second) {
        if (first.equals(second)) {
            return availableOf(first) >= 2;
        }
        return availableOf(first) > 0 && availableOf(second) > 0;
    }

    private void combineSelectedAspects() {
        if (select1 == null || select2 == null || System.currentTimeMillis() < combineCooldownUntil) {
            return;
        }
        combineCooldownUntil = System.currentTimeMillis() + COMBINE_COOLDOWN_MS;
        boolean bonus1 = poolAmount(select1) <= 0 && bonusAmount(select1) > 0;
        int secondPoolAmount = poolAmount(select2) - (select1.equals(select2) && !bonus1 ? 1 : 0);
        boolean bonus2 = secondPoolAmount <= 0 && bonusAmount(select2) > 0;
        TTNetwork.sendToServer(new ServerboundTableCombinePacket(menu.pos(), select1, select2, bonus1, bonus2));
        playSound(TTSounds.HHON.get(), 0.3F, 1.0F);
    }

    // ---- duplicate / helper -------------------------------------------------------------------

    private void drawDuplicateButton(GuiGraphics graphics, int mouseX, int mouseY) {
        BlockEntityResearchTable table = table();
        ResearchNoteData data = noteData();
        if (table == null || !canDuplicate(data)) {
            return;
        }
        graphics.blit(TEXTURE, leftPos + DUPE_X, topPos + DUPE_Y, DUPE_U, DUPE_V, DUPE_SIZE, DUPE_SIZE, 256, 256);
        if (inRect(mouseX, mouseY, leftPos + DUPE_X, topPos + DUPE_Y, DUPE_SIZE, DUPE_SIZE)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.thaumaturge.research_table.copy"));
            AspectList cost = table.duplicationCost(data);
            if (cost != null) {
                for (AspectInstance instance : cost.entries()) {
                    lines.add(Component.literal(instance.amount() + "x ").append(nameOf(instance.aspectId())));
                }
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void drawHelperButton(GuiGraphics graphics, int mouseX, int mouseY) {
        if (canDuplicate(noteData())) {
            return;
        }
        graphics.renderItem(new ItemStack(TTItems.THAUMONOMICON.get()), leftPos + HELPER_X, topPos + HELPER_Y);
        if (inRect(mouseX, mouseY, leftPos + HELPER_X, topPos + HELPER_Y, HELPER_SIZE, HELPER_SIZE)) {
            graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.helper"), mouseX, mouseY);
        }
    }

    private void drawHelper(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ResourceLocation> compounds = discoveredCompounds();
        int start = helperPage * HELPER_ROWS;
        for (int i = start, row = 0; i < compounds.size() && row < HELPER_ROWS; i++, row++) {
            ResourceLocation compound = compounds.get(i);
            List<ResourceLocation> parts = TTAspects.get(compound).componentIds();
            int y = topPos + SHEET_Y + HELPER_ROW_START_Y + row * HELPER_ROW_STRIDE;
            int x = leftPos + SHEET_X + (SHEET_SIZE - HELPER_ROW_WIDTH) / 2;
            drawHelperTag(graphics, parts.get(0), x, y, mouseX, mouseY);
            graphics.drawString(font, "+", x + 20, y + 4, 0xFF3A2A1A, false);
            drawHelperTag(graphics, parts.get(1), x + 28, y, mouseX, mouseY);
            graphics.drawString(font, "=", x + 48, y + 4, 0xFF3A2A1A, false);
            drawHelperTag(graphics, compound, x + 56, y, mouseX, mouseY);
        }
        int lastPage = Math.max(0, (compounds.size() - 1) / HELPER_ROWS);
        int center = leftPos + SHEET_X + SHEET_SIZE / 2;
        if (lastPage > 0) {
            String label = (helperPage + 1) + "/" + (lastPage + 1);
            graphics.drawString(font, label, center - font.width(label) / 2, topPos + HELPER_ARROW_Y, 0xFF3A2A1A, false);
        }
        if (helperPage > 0) {
            int x = center - HELPER_PAGE_HALF_GAP - ARROW_W;
            graphics.blit(TEXTURE, x, topPos + HELPER_ARROW_Y, ARROW_PREV_U, ARROW_V, ARROW_W, ARROW_H, 256, 256);
        }
        if (helperPage < lastPage) {
            int x = center + HELPER_PAGE_HALF_GAP;
            graphics.blit(TEXTURE, x, topPos + HELPER_ARROW_Y, ARROW_NEXT_U, ARROW_V, ARROW_W, ARROW_H, 256, 256);
        }
    }

    private void drawHelperTag(GuiGraphics graphics, ResourceLocation aspect, int x, int y, int mouseX, int mouseY) {
        if (discovered(aspect)) {
            drawTag(graphics, aspect, x, y, 0.0F, 1.0F);
            if (inRect(mouseX, mouseY, x, y, 16, 16)) {
                graphics.renderTooltip(font, nameOf(aspect), mouseX, mouseY);
            }
        } else {
            AspectTagRenderer.renderUnknown(graphics, x, y, null);
        }
    }

    // ---- note sheet ---------------------------------------------------------------------------

    private void drawSheet(GuiGraphics graphics, int mouseX, int mouseY) {
        // parchment3.png is 256x256 with art only in the top-left 150x150 (CE atlas crop).
        graphics.blit(PARCHMENT, leftPos + SHEET_X, topPos + SHEET_Y, 0, 0, SHEET_SIZE, SHEET_SIZE, 256, 256);
        if (helperOpen) {
            drawHelper(graphics, mouseX, mouseY);
            return;
        }
        ResearchNoteData data = noteData();
        if (data == null) {
            if (!hasInkReady()) {
                drawInkWarning(graphics);
            }
            return;
        }
        HexGrid.Hex hoveredHex = hexAt(mouseX, mouseY);
        Map<HexGrid.Hex, ResearchNoteData.Cell> cells = new HashMap<>(data.cellMap());
        drawConnections(graphics, data, cells);
        for (ResearchNoteData.Cell cell : data.cells()) {
            int cx = leftPos + HEX_ORIGIN_X + Math.round(cell.hex().pixelX(HEX_SIZE));
            int cy = topPos + HEX_ORIGIN_Y + Math.round(cell.hex().pixelY(HEX_SIZE));
            boolean hover = cell.hex().equals(hoveredHex);
            if (!data.complete() && cell.type() != ResearchNoteData.TYPE_ROOT) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float alpha = hover ? 1.0F : 0.25F;
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                graphics.blit(hover ? HEX_HOVER : HEX_IDLE, cx - HEX_TILE_HALF, cy - HEX_TILE_HALF, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            ResourceLocation aspect = cell.aspectOrNull();
            if (aspect != null) {
                if (!discovered(aspect)) {
                    AspectTagRenderer.renderUnknown(graphics, cx + ORB_OFFSET, cy + ORB_OFFSET, null);
                    if (hover) {
                        graphics.renderTooltip(font, Component.translatable("tooltip.thaumaturge.aspect.unknown"), mouseX, mouseY);
                    }
                } else {
                    drawTag(graphics, aspect, cx + ORB_OFFSET, cy + ORB_OFFSET, 0.0F, 1.0F);
                    if (hover && draggedAspect == null) {
                        graphics.renderComponentTooltip(font, aspectTooltip(aspect), mouseX, mouseY);
                    }
                }
            }
        }
        if (!hasInkReady()) {
            drawInkWarning(graphics);
        }
    }

    private void drawConnections(GuiGraphics graphics, ResearchNoteData data, Map<HexGrid.Hex, ResearchNoteData.Cell> cells) {
        for (ResearchNoteData.Cell cell : data.cells()) {
            if (!cell.active()) {
                continue;
            }
            for (int dir = 0; dir < 3; dir++) {
                HexGrid.Hex neighbour = cell.hex().neighbour(dir);
                ResearchNoteData.Cell other = cells.get(neighbour);
                if (other == null || !other.active()) {
                    continue;
                }
                if (NoteRules.connects(cell.aspectOrNull(), other.aspectOrNull(), this::discovered)) {
                    int x1 = leftPos + HEX_ORIGIN_X + Math.round(cell.hex().pixelX(HEX_SIZE));
                    int y1 = topPos + HEX_ORIGIN_Y + Math.round(cell.hex().pixelY(HEX_SIZE));
                    int x2 = leftPos + HEX_ORIGIN_X + Math.round(neighbour.pixelX(HEX_SIZE));
                    int y2 = topPos + HEX_ORIGIN_Y + Math.round(neighbour.pixelY(HEX_SIZE));
                    drawLine(graphics, x1, y1, x2, y2);
                }
            }
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int length = Math.round((float) Math.sqrt(dx * dx + dy * dy));
        graphics.pose().pushPose();
        graphics.pose().translate(x1, y1, 0.0F);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        graphics.fill(0, -LINE_HALF_WIDTH, length, LINE_HALF_WIDTH, LINE_COLOR);
        graphics.pose().popPose();
    }

    private void drawInkWarning(GuiGraphics graphics) {
        Component line0 = Component.translatable("gui.thaumaturge.research_table.no_ink.0");
        Component line1 = Component.translatable("gui.thaumaturge.research_table.no_ink.1");
        int x = leftPos + SHEET_X + SHEET_SIZE / 2;
        int y = topPos + INK_WARN_Y;
        graphics.drawString(font, line0, x - font.width(line0) / 2, y - font.lineHeight, 0xFFFF5555, false);
        graphics.drawString(font, line1, x - font.width(line1) / 2, y, 0xFFFF5555, false);
    }

    private void drawSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && draggedAspect == null) {
            if (menu.slots.get(BlockEntityResearchTable.SLOT_SCRIBE_TOOLS).getItem().isEmpty()
                    && inRect(mouseX, mouseY, leftPos + MenuResearchTable.SCRIBE_TOOLS_X, topPos + MenuResearchTable.SCRIBE_TOOLS_Y, 16, 16)) {
                graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.slot_tools"), mouseX, mouseY);
            } else if (menu.slots.get(BlockEntityResearchTable.SLOT_NOTE).getItem().isEmpty()
                    && inRect(mouseX, mouseY, leftPos + MenuResearchTable.NOTE_X, topPos + MenuResearchTable.NOTE_Y, 16, 16)) {
                graphics.renderTooltip(font, Component.translatable("gui.thaumaturge.research_table.slot_note"), mouseX, mouseY);
            }
        }
    }

    private void drawDragged(GuiGraphics graphics, int mouseX, int mouseY) {
        if (draggedAspect != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            drawTag(graphics, draggedAspect, mouseX - 8, mouseY - 8, 0.0F, 1.0F);
            graphics.pose().popPose();
        }
    }

    private HexGrid.@Nullable Hex hexAt(double mouseX, double mouseY) {
        if (mouseX < leftPos + SHEET_X || mouseX >= leftPos + SHEET_X + SHEET_SIZE || mouseY < topPos + SHEET_Y || mouseY >= topPos + SHEET_Y + SHEET_SIZE) {
            return null;
        }
        float relX = (float) (mouseX - leftPos - HEX_ORIGIN_X);
        float relY = (float) (mouseY - topPos - HEX_ORIGIN_Y);
        return HexGrid.pixelToHex(relX, relY, HEX_SIZE);
    }

    // ---- input --------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (!canDuplicate(noteData())) {
                if (inRect(mx, my, leftPos + HELPER_X, topPos + HELPER_Y, HELPER_SIZE, HELPER_SIZE)) {
                    helperOpen = !helperOpen;
                    playSound(TTSounds.KEY.get(), 0.3F, 1.0F);
                    return true;
                }
                if (helperOpen && handleHelperArrows(mx, my)) {
                    return true;
                }
            }
            if (handleArrows(mx, my) || handleCombineButton(mx, my) || handleSelectRemove(mx, my) || handleDuplicate(mx, my)) {
                return true;
            }
            ResourceLocation palette = paletteAspectAt(mx, my);
            if (palette != null) {
                if (hasShiftDown() && !TTAspects.get(palette).isPrimal() && ClientKnowledge.get().isComplete(BlockEntityResearchTable.RESEARCH_MASTERY)) {
                    List<ResourceLocation> components = TTAspects.get(palette).componentIds();
                    if (components.size() == 2) {
                        select1 = components.get(0);
                        select2 = components.get(1);
                        if (hasAvailableComponents(select1, select2)) {
                            combineSelectedAspects();
                        } else {
                            playSound(TTSounds.HHON.get(), 0.2F, 1.0F);
                        }
                        return true;
                    }
                }
                if (availableOf(palette) > 0) {
                    draggedAspect = palette;
                    aspectDragStartX = mx;
                    aspectDragStartY = my;
                    playSound(TTSounds.HHOFF.get(), 0.2F, 1.0F);
                }
                return true;
            }
            HexGrid.Hex hex = hexAt(mx, my);
            if (hex != null && draggedAspect == null && !helperOpen) {
                ResearchNoteData data = noteData();
                if (data != null && !data.complete()) {
                    ResearchNoteData.Cell cell = data.cellAt(hex);
                    if (cell != null && cell.type() == ResearchNoteData.TYPE_PLACED) {
                        TTNetwork.sendToServer(new ServerboundTablePlaceAspectPacket(menu.pos(), hex.q(), hex.r(), null));
                        playSound(TTSounds.ERASE.get(), 0.2F, 1.0F);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedAspect != null) {
            HexGrid.Hex hex = hexAt(mx, my);
            ResearchNoteData data = noteData();
            if (isAspectClick(mx, my)) {
                selectPaletteAspect(draggedAspect);
            } else if (hex != null && !helperOpen && data != null && !data.complete()) {
                ResearchNoteData.Cell cell = data.cellAt(hex);
                if (cell != null && cell.type() == ResearchNoteData.TYPE_BLANK) {
                    TTNetwork.sendToServer(new ServerboundTablePlaceAspectPacket(menu.pos(), hex.q(), hex.r(), draggedAspect));
                    playSound(TTSounds.WRITE.get(), 0.2F, 1.0F);
                }
            } else if (inRect(mx, my, leftPos + SELECT1_HIT_X - 8, topPos + SELECT_HIT_Y - 8, SELECT_SIZE * 2, SELECT_SIZE * 2)) {
                select1 = draggedAspect;
            } else if (inRect(mx, my, leftPos + SELECT2_HIT_X - 8, topPos + SELECT_HIT_Y - 8, SELECT_SIZE * 2, SELECT_SIZE * 2)) {
                select2 = draggedAspect;
            }
            draggedAspect = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private boolean isAspectClick(double mouseX, double mouseY) {
        double dx = mouseX - aspectDragStartX;
        double dy = mouseY - aspectDragStartY;
        return dx * dx + dy * dy <= ASPECT_CLICK_DISTANCE_SQ;
    }

    private void selectPaletteAspect(ResourceLocation aspect) {
        if (select1 == null) {
            select1 = aspect;
        } else {
            select2 = aspect;
        }
    }

    private boolean handleHelperArrows(double mx, double my) {
        int center = leftPos + SHEET_X + SHEET_SIZE / 2;
        if (inRect(mx, my, center - HELPER_PAGE_HALF_GAP - ARROW_W, topPos + HELPER_ARROW_Y, ARROW_W, ARROW_H)) {
            return stepHelperPage(-1);
        }
        if (inRect(mx, my, center + HELPER_PAGE_HALF_GAP, topPos + HELPER_ARROW_Y, ARROW_W, ARROW_H)) {
            return stepHelperPage(1);
        }
        return false;
    }

    private boolean handleArrows(double mx, double my) {
        if (inRect(mx, my, leftPos + ARROW_PREV_X, topPos + ARROW_Y, ARROW_W, ARROW_H)) {
            return stepPalettePage(-1);
        }
        if (inRect(mx, my, leftPos + ARROW_NEXT_X, topPos + ARROW_Y, ARROW_W, ARROW_H)) {
            return stepPalettePage(1);
        }
        return false;
    }

    private boolean stepHelperPage(int delta) {
        int target = helperPage + delta;
        if (target < 0 || target > Math.max(0, (discoveredCompounds().size() - 1) / HELPER_ROWS)) {
            return false;
        }
        helperPage = target;
        playSound(TTSounds.KEY.get(), PAGE_SOUND_VOLUME, 1.0F);
        return true;
    }

    private boolean stepPalettePage(int delta) {
        int target = page + delta;
        if (target < 0 || target > lastPage(discoveredAspects().size())) {
            return false;
        }
        page = target;
        playSound(TTSounds.KEY.get(), PAGE_SOUND_VOLUME, 1.0F);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0) {
            int step = delta < 0.0 ? 1 : -1;
            if (helperOpen && inRect(mouseX, mouseY, leftPos + SHEET_X, topPos + SHEET_Y, SHEET_SIZE, SHEET_SIZE)) {
                stepHelperPage(step);
                return true;
            }
            if (inRect(mouseX, mouseY, leftPos + PALETTE_X, topPos + PALETTE_Y, PALETTE_W, PALETTE_SCROLL_H)) {
                stepPalettePage(step);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean handleCombineButton(double mx, double my) {
        if (select1 == null || select2 == null || !inRect(mx, my, leftPos + COMBINE_X, topPos + COMBINE_Y, COMBINE_W, COMBINE_H)) {
            return false;
        }
        combineSelectedAspects();
        return true;
    }

    private boolean handleSelectRemove(double mx, double my) {
        if (select1 != null && inRect(mx, my, leftPos + SELECT1_HIT_X, topPos + SELECT_HIT_Y, SELECT_SIZE, SELECT_SIZE)) {
            select1 = null;
            playSound(TTSounds.HHOFF.get(), 0.2F, 1.0F);
            return true;
        }
        if (select2 != null && inRect(mx, my, leftPos + SELECT2_HIT_X, topPos + SELECT_HIT_Y, SELECT_SIZE, SELECT_SIZE)) {
            select2 = null;
            playSound(TTSounds.HHOFF.get(), 0.2F, 1.0F);
            return true;
        }
        return false;
    }

    private boolean handleDuplicate(double mx, double my) {
        if (table() == null || !inRect(mx, my, leftPos + DUPE_X, topPos + DUPE_Y, DUPE_SIZE, DUPE_SIZE) || !canDuplicate(noteData())) {
            return false;
        }
        TTNetwork.sendToServer(new ServerboundTableDuplicatePacket(menu.pos()));
        playSound(TTSounds.CLACK.get(), 0.4F, 1.0F);
        return true;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(sound, volume, pitch);
        }
    }
}
