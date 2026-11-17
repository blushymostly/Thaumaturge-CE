package com.leclowndu93150.thaumaturge.client.screen;

import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.screen.FocalCanvas.Bounds;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorBlockEntity;
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorMenu;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell.Part;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundFocalDraftPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final int LABEL = 0xFFCEBA80;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int DIM = 0xFF707070;
    private static final int ERROR = 0xFFFF8070;
    private static final int SYNC_DELAY = 4;
    private static final int SYNC_GRACE = 40;
    private static final List<String> CATEGORIES = List.of("flow", "delivery", "effect", "modifier");

    private final FocalCanvas canvas = new FocalCanvas();
    private FocusSpell draft = FocusSpell.empty();
    private String name = "";
    private String search = "";
    private EditBox nameBox;
    private EditBox searchBox;
    private int focusVersion;
    private int flushIn = -1;
    private int syncGrace;
    private boolean initialized;
    private boolean styleOpen;
    private int category = 1;
    private Bounds graph;
    private int right;
    private int inspectorWidth;
    private int paletteWidth;

    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        boolean compact = width < 370;
        paletteWidth = compact ? 80 : 108;
        inspectorWidth = compact ? 96 : 124;
        right = width - 18 - inspectorWidth;
        int graphX = 96 + paletteWidth;
        graph = new Bounds(graphX, 42, right - 6 - graphX, height - 60);

        if (!initialized) {
            focusVersion = menu.focusVersion();
            draft = menu.draft();
            name = menu.draftName();
            initialized = true;
        }
        nameBox = new EditBox(font, 117, 23, Math.max(20, width - 170), 12, text("name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(FocalManipulatorBlockEntity.MAX_NAME_LENGTH);
        nameBox.setHint(text("name"));
        nameBox.setValue(name);
        nameBox.setResponder(value -> {
            if (!name.equals(value) && editable()) {
                name = value;
                flushIn = SYNC_DELAY;
            }
        });
        addRenderableWidget(nameBox);
        searchBox = new EditBox(font, 95, 68, paletteWidth - 10, 12, text("search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(40);
        searchBox.setHint(text("search"));
        searchBox.setValue(search);
        searchBox.setResponder(value -> search = value);
        addRenderableWidget(searchBox);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        nameBox.tick();
        searchBox.tick();
        boolean slotChanged = focusVersion != menu.focusVersion();
        if (slotChanged) {
            focusVersion = menu.focusVersion();
            flushIn = -1;
            syncGrace = 0;
            canvas.reset();
        }
        if (syncGrace > 0)
            syncGrace--;
        if (draft.equals(menu.draft()) && name.equals(menu.draftName()))
            syncGrace = 0;
        if (slotChanged || flushIn < 0 && syncGrace == 0) {
            draft = menu.draft();
            name = menu.draftName();
            if (!nameBox.getValue().equals(name))
                nameBox.setValue(name);
        }
        if (flushIn > 0 && --flushIn == 0)
            flush(false);
        nameBox.setEditable(editable());
    }

    private boolean editable() {
        return menu.budget() > 0 && !menu.busy();
    }

    private void edit(FocusSpell next) {
        if (editable() && !next.equals(draft)) {
            draft = next;
            flushIn = SYNC_DELAY;
        }
    }

    private boolean canSubmit() {
        return editable() && draft.complete() && draft.complexity() <= menu.budget() && (flushIn >= 0 || syncGrace > 0 || menu.status() == FocalManipulatorMenu.Status.READY);
    }

    private void flush(boolean inscribe) {
        if (!editable() || focusVersion != menu.focusVersion() || flushIn < 0 && !inscribe)
            return;
        TTNetwork.sendToServer(new ServerboundFocalDraftPacket(menu.containerId, focusVersion, name, draft, inscribe));
        flushIn = -1;
        syncGrace = SYNC_GRACE;
    }

    @Override
    public void onClose() {
        flush(false);
        super.onClose();
    }

    @Override
    public void removed() {
        flush(false);
        super.removed();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        FocalTextures.cabinet(graphics);
        FocalTextures.frame(graphics, 72, 0, width - 72, height);
        FocalTextures.sprite(graphics, "slot", 90, 18, 18, 18);
        FocalTextures.field(graphics, 112, 20, width - 160, 14);
        drawIconButton(graphics, confirm(), "glyph_check", 10, mouseX, mouseY, menu.busy(), canSubmit() || menu.busy());
        drawPalette(graphics, mouseX, mouseY);
        canvas.render(graphics, draft, graph, mouseX, mouseY, font);
        drawInspector(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (styleOpen)
            drawStyleOptions(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        Component tooltip = editorTooltip(mouseX, mouseY);
        if (tooltip != null)
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private Bounds confirm() {
        return new Bounds(width - 42, 19, 24, 16);
    }

    private void drawPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = 0; index < CATEGORIES.size(); index++) {
            var tab = tab(index);
            String selected = index == category ? "_selected" : "";
            String face = index == category ? "tab_selected" : tab.contains(mouseX, mouseY) ? "tab_hover" : "tab_normal";
            FocalTextures.sprite(graphics, face, tab.x(), tab.y(), tab.width(), tab.height(), 26, 20);
            FocalTextures.sprite(graphics, "icon_" + CATEGORIES.get(index) + selected, tab.x() + (tab.width() - 12) / 2, tab.y() + 4, 12, 12);
        }
        FocalTextures.field(graphics, 90, 65, paletteWidth, 14);
        FocalTextures.well(graphics, 90, 82, paletteWidth, height - 100);
        var parts = visibleParts();
        for (int index = 0; index < parts.size(); index++) {
            Part part = parts.get(index);
            var cell = partCell(index);
            FocalTextures.sprite(graphics, "cell", cell.x(), cell.y(), 22, 22);
            FocalTextures.paletteGlyph(graphics, part, cell.x() + 11, cell.y() + 11);
            if (!editable() || !canvas.canAdd(draft, part))
                graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0x500D0810);
            else if (cell.contains(mouseX, mouseY))
                graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0x30FFFFFF);
        }
    }

    private List<Part> visibleParts() {
        String query = search.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(Part.values()).filter(part -> part != Part.ORIGIN && part.unlocked(ClientKnowledge.get()))
                .filter(part -> query.isEmpty() ? part.category().equals(CATEGORIES.get(category)) : partName(part).getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private Bounds tab(int index) {
        int pitch = paletteWidth / 4;
        return new Bounds(90 + index * pitch, 42, pitch - 1, 20);
    }

    private Bounds partCell(int index) {
        int columns = Math.max(1, (paletteWidth - 4) / 24);
        return new Bounds(93 + index % columns * 24, 85 + index / columns * 24, 22, 22);
    }

    private void drawInspector(GuiGraphics graphics, int mouseX, int mouseY) {
        draw(graphics, text("selected"), right, 42, LABEL);
        FocalTextures.field(graphics, right, 53, inspectorWidth, 30);
        var selected = canvas.selected(draft);
        if (selected == null) {
            draw(graphics, text("empty"), right + 5, 64, GREY);
        } else {
            FocalTextures.paletteGlyph(graphics, selected.kind(), right + 13, 68);
            drawClipped(graphics, partName(selected.kind()), right + 25, 58, inspectorWidth - 30, WHITE);
            drawClipped(graphics, text(selected.kind().category()), right + 25, 69, inspectorWidth - 30, GREY);
        }
        if (selected != null && selected.kind().isEffect()) {
            drawSetting(graphics, "power", selected.power(), 86, mouseX, mouseY);
            drawSetting(graphics, "duration", selected.duration(), 100, mouseX, mouseY);
        }
        int top = summaryY();
        if (selected != null) {
            draw(graphics, text("node_cost"), right, top - 17, GREY);
            String cost = Integer.toString(selected.complexity());
            graphics.drawString(font, cost, right + inspectorWidth - font.width(cost), top - 17, WHITE, false);
        }
        FocalTextures.sprite(graphics, "divider", right, top - 6, inspectorWidth, 2, 32, 2);
        draw(graphics, text("spell"), right, top, LABEL);
        var style = styleBounds();
        FocalTextures.field(graphics, style.x(), style.y(), style.width(), style.height());
        draw(graphics, text("cast"), right, style.y() + 3, GREY);
        drawClipped(graphics, text("instant"), style.x() + 5, style.y() + 3, style.width() - 18, WHITE);
        FocalTextures.sprite(graphics, "glyph_down", style.right() - 12, style.y() + 3, 8, 8);

        int budget = menu.budget();
        int complexity = draft.complexity();
        drawClipped(graphics, text("complexity", complexity, budget), right, top + 30, inspectorWidth, complexity > budget ? ERROR : WHITE);
        FocalTextures.field(graphics, right, top + 42, inspectorWidth, 7);
        int fill = budget == 0 ? 0 : Math.min(inspectorWidth - 4, (inspectorWidth - 4) * complexity / budget);
        if (fill > 0)
            FocalTextures.sprite(graphics, complexity > budget ? "bar_fill_over" : "bar_fill", right + 2, top + 43, fill, 4, 16, 4);

        if (complexity > 0 && budget > 0) {
            drawClipped(graphics, text("levels_vis", draft.inscriptionLevels(), draft.inscriptionVis(budget)), right, top + 53, inspectorWidth, GREY);
            int x = right;
            for (var cost : draft.crystals().entrySet()) {
                var stack = EssentiaCrystalFactory.of(cost.getKey(), cost.getValue());
                graphics.renderItem(stack, x, top + 64);
                graphics.renderItemDecorations(font, stack, x, top + 64);
                x += 16;
            }
        }
        FocalManipulatorMenu.Status status = menu.status();
        if (menu.busy()) {
            draw(graphics, text("progress", menu.progressPercent()), right, top + 82, 0xFF55FFFF);
        } else if (status != FocalManipulatorMenu.Status.READY) {
            int lineY = top + 82;
            if (lineY + 9 < height - 14) {
                FocalTextures.sprite(graphics, "warn", right, lineY, 9, 9);
                for (var line : font.split(statusText(), inspectorWidth - 12)) {
                    if (lineY + 9 >= height - 14)
                        break;
                    graphics.drawString(font, line, right + 12, lineY, GREY, false);
                    lineY += 11;
                }
            }
        }
    }

    private void drawSetting(GuiGraphics graphics, String id, int value, int y, int mouseX, int mouseY) {
        drawClipped(graphics, text(id), right, y + 2, inspectorWidth - 51, GREY);
        var minus = stepButton(y, -1);
        var plus = stepButton(y, 1);
        drawIconButton(graphics, minus, "glyph_left", 8, mouseX, mouseY, false, editable());
        drawIconButton(graphics, plus, "glyph_right", 8, mouseX, mouseY, false, editable());
        int x = minus.right() + 2;
        FocalTextures.field(graphics, x, y, plus.x() - x - 2, 12);
        graphics.drawCenteredString(font, Integer.toString(value), (x + plus.x() - 2) / 2, y + 2, WHITE);
    }

    private Bounds stepButton(int y, int delta) {
        return new Bounds(right + inspectorWidth - (delta < 0 ? 47 : 12), y, 12, 12);
    }

    private int summaryY() {
        var selected = canvas.selected(draft);
        return selected == null ? 90 : selected.kind().isEffect() ? 132 : 103;
    }

    private Bounds styleBounds() {
        int labelWidth = font.width(text("cast")) + 5;
        return new Bounds(right + labelWidth, summaryY() + 13, inspectorWidth - labelWidth, 14);
    }

    private Bounds styleOption(int index) {
        var style = styleBounds();
        return new Bounds(style.x(), style.bottom() + 14 * index, style.width(), 14);
    }

    private void drawStyleOptions(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        for (int index = 0; index < 3; index++) {
            var option = styleOption(index);
            String key = switch (index) {
                case 0 -> "instant";
                case 1 -> "charged";
                default -> "channeled";
            };
            FocalTextures.field(graphics, option.x(), option.y(), option.width(), option.height());
            drawClipped(graphics, text(key), option.x() + 5, option.y() + 3, option.width() - 9, index == 0 ? LABEL : DIM);
        }
        graphics.pose().popPose();
    }

    private void drawIconButton(GuiGraphics graphics, Bounds button, String glyph, int iconSize, int mouseX, int mouseY, boolean pressed, boolean enabled) {
        FocalTextures.button(graphics, button.x(), button.y(), button.width(), button.height(), enabled && button.contains(mouseX, mouseY), pressed);
        FocalTextures.sprite(graphics, glyph, button.x() + (button.width() - iconSize) / 2, button.y() + (button.height() - iconSize) / 2, iconSize, iconSize);
        if (!enabled)
            graphics.fill(button.x(), button.y(), button.right(), button.bottom(), 0x600B0810);
    }

    private void draw(GuiGraphics graphics, Component label, int x, int y, int color) {
        graphics.drawString(font, label, x, y, color, false);
    }

    private void drawClipped(GuiGraphics graphics, Component label, int x, int y, int available, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(label.getString(), Math.max(0, available)), x, y, color, false);
    }

    private Component statusText() {
        return text(menu.status().name().toLowerCase(Locale.ROOT));
    }

    private Component editorTooltip(int x, int y) {
        if (styleOpen) {
            if (styleOption(0).contains(x, y))
                return text("instant");
            if (styleOption(1).contains(x, y))
                return text("charged").copy().append(" · ").append(text("style_unavailable"));
            if (styleOption(2).contains(x, y))
                return text("channeled").copy().append(" · ").append(text("style_unavailable"));
            return null;
        }
        if (confirm().contains(x, y))
            return canSubmit() ? text("inscribe") : statusText();
        if (styleBounds().contains(x, y))
            return text("style");
        for (int index = 0; index < CATEGORIES.size(); index++) {
            if (tab(index).contains(x, y))
                return text(CATEGORIES.get(index));
        }
        var parts = visibleParts();
        for (int index = 0; index < parts.size(); index++) {
            if (partCell(index).contains(x, y))
                return partName(parts.get(index));
        }
        return canvas.tooltip(graph, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!nameBox.isMouseOver(x, y) && !searchBox.isMouseOver(x, y))
            setFocused(null);
        if (styleOpen) {
            styleOpen = false;
            return true;
        }
        if (button == 0 && styleBounds().contains(x, y)) {
            styleOpen = true;
            return true;
        }
        if (confirm().contains(x, y)) {
            if (button == 0 && canSubmit())
                flush(true);
            return true;
        }
        if (graph.contains(x, y)) {
            if (canvas.select(graph, x, y, button) && button == 1 && editable())
                edit(canvas.remove(draft));
            return true;
        }
        if (button == 0) {
            for (int index = 0; index < CATEGORIES.size(); index++) {
                if (tab(index).contains(x, y)) {
                    category = index;
                    searchBox.setValue("");
                    return true;
                }
            }
            var parts = visibleParts();
            for (int index = 0; index < parts.size(); index++) {
                if (partCell(index).contains(x, y)) {
                    if (editable())
                        edit(canvas.add(draft, parts.get(index)));
                    return true;
                }
            }
            for (int delta : new int[]{-1, 1}) {
                if (stepButton(86, delta).contains(x, y)) {
                    if (editable())
                        edit(canvas.adjust(draft, "power", delta));
                    return true;
                }
                if (stepButton(100, delta).contains(x, y)) {
                    if (editable())
                        edit(canvas.adjust(draft, "duration", delta));
                    return true;
                }
            }
        }
        if (new Bounds(90, 18, 18, 18).contains(x, y))
            flush(false);
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        if (graph.contains(x, y) && !styleOpen) {
            canvas.zoom((int) Math.signum(amount));
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return canvas.drag(dx, dy) || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        canvas.release();
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && styleOpen) {
            styleOpen = false;
            return true;
        }
        boolean typing = nameBox.isFocused() || searchBox.isFocused();
        if (!typing && (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) && editable()) {
            edit(canvas.remove(draft));
            return true;
        }
        if (!typing)
            flush(false);
        return super.keyPressed(key, scanCode, modifiers);
    }

    static Component text(String key, Object... arguments) {
        return Component.translatable("gui.thaumaturge.focal_manipulator." + key, arguments);
    }

    static Component partName(Part part) {
        return text(part.key());
    }
}
