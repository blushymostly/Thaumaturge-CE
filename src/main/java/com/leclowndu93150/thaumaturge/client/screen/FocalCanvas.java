package com.leclowndu93150.thaumaturge.client.screen;

import com.leclowndu93150.thaumaturge.content.item.FocusSpell;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell.Node;
import com.leclowndu93150.thaumaturge.content.item.FocusSpell.Part;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class FocalCanvas {
    private static final int COLUMN = 24;
    private static final int ROW = 32;
    private static final float[] ZOOMS = {0.5F, 0.75F, 1, 1.25F, 1.5F, 2};
    private static final int DEFAULT_ZOOM = 2;
    private static final int NODE_SIZE = 22;

    private List<Integer> selection = List.of(0);
    private List<Placed> placed = List.of();
    private int zoom = DEFAULT_ZOOM;
    private double panX;
    private double panY;
    private boolean panning;

    void reset() {
        selection = List.of(0);
        zoom = DEFAULT_ZOOM;
        panX = 0;
        panY = 0;
    }

    Node selected(FocusSpell spell) {
        return at(spell.root(), selection);
    }

    boolean canAdd(FocusSpell spell, Part kind) {
        Node selected = selected(spell);
        if (selected == null) {
            Node parent = at(spell.root(), parentPath());
            return parent != null && !selection.isEmpty() && parent.children().size() == selection.get(selection.size() - 1) && parent.children().size() < parent.kind().capacity()
                    && parent.kind().accepts(kind);
        }
        if (selected.kind() == Part.ORIGIN && kind == Part.BRANCH && selected.children().size() == 1) {
            return kind.accepts(selected.children().get(0).kind());
        }
        return selected.children().size() < selected.kind().capacity() && selected.kind().accepts(kind);
    }

    FocusSpell add(FocusSpell spell, Part kind) {
        if (!canAdd(spell, kind))
            return spell;
        Node selected = selected(spell);
        if (selected == null) {
            return new FocusSpell(replace(spell.root(), selection, 0, kind.node()));
        }
        if (selected.kind() == Part.ORIGIN && !selected.children().isEmpty()) {
            Node branch = kind.node().withChildren(selected.children());
            selection = List.of(0);
            return new FocusSpell(selected.withChildren(List.of(branch)));
        }
        selection = childPath(selection, selected.children().size());
        return new FocusSpell(replace(spell.root(), selection, 0, kind.node()));
    }

    FocusSpell remove(FocusSpell spell) {
        Node node = selected(spell);
        if (selection.isEmpty() || node == null)
            return spell;
        var parentPath = parentPath();
        Node parent = at(spell.root(), parentPath);
        int index = selection.get(selection.size() - 1);
        var siblings = new ArrayList<>(parent.children());
        siblings.remove(index);
        if (siblings.size() + node.children().size() <= parent.kind().capacity() && node.children().stream().allMatch(child -> parent.kind().accepts(child.kind()))) {
            siblings.addAll(index, node.children());
        }
        selection = parentPath;
        return new FocusSpell(replace(spell.root(), parentPath, 0, parent.withChildren(siblings)));
    }

    FocusSpell adjust(FocusSpell spell, String setting, int change) {
        Node node = selected(spell);
        if (node == null || !node.kind().isEffect())
            return spell;
        boolean power = setting.equals("power");
        int before = power ? node.power() : node.duration();
        int low = power ? 1 : node.kind().minDuration();
        int high = power ? 5 : node.kind().maxDuration();
        int after = Math.max(low, Math.min(high, before + change));
        return before == after ? spell : new FocusSpell(replace(spell.root(), selection, 0, node.withSetting(setting, after)));
    }

    private static Node at(Node node, List<Integer> path) {
        for (int index : path) {
            if (index >= node.children().size())
                return null;
            node = node.children().get(index);
        }
        return node;
    }

    private static Node replace(Node node, List<Integer> path, int depth, Node replacement) {
        if (depth == path.size())
            return replacement;
        int index = path.get(depth);
        var children = new ArrayList<>(node.children());
        if (index == children.size())
            children.add(replacement);
        else
            children.set(index, replace(children.get(index), path, depth + 1, replacement));
        return node.withChildren(children);
    }

    private List<Integer> parentPath() {
        return selection.isEmpty() ? selection : selection.subList(0, selection.size() - 1);
    }

    private static List<Integer> childPath(List<Integer> parent, int index) {
        var path = new ArrayList<>(parent);
        path.add(index);
        return List.copyOf(path);
    }

    void render(GuiGraphics graphics, FocusSpell spell, Bounds area, int mouseX, int mouseY, net.minecraft.client.gui.Font font) {
        FocalTextures.well(graphics, area.x(), area.y(), area.width(), area.height());
        graphics.enableScissor(area.x() + 3, area.y() + 3, area.right() - 3, area.bottom() - 3);
        FocalTextures.sprite(graphics, "sigil", area.x() + area.width() / 2 - 64, area.y() + area.height() / 2 - 64, 128, 128);
        var nodes = new ArrayList<Placed>();
        layout(nodes, spell.root(), List.of(), 0, 0, null);
        placed = nodes;

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(area.x() + area.width() / 2 + panX, area.y() + 24 + panY, 0);
        float scale = ZOOMS[zoom];
        pose.scale(scale, scale, 1);
        for (Placed node : placed) {
            if (node.parent() != null)
                drawLink(graphics, node);
        }
        for (Placed node : placed) {
            if (node.node() == null) {
                FocalTextures.sprite(graphics, "cell", node.x() - 11, node.y() - 11, 22, 22);
            } else {
                FocalTextures.node(graphics, node.node().kind(), node.x(), node.y());
            }
            if (selection.equals(node.path())) {
                FocalTextures.sprite(graphics, "selection", node.x() - 12, node.y() - 12, 24, 24);
            }
        }
        pose.popPose();
        graphics.disableScissor();

        int controlsY = area.y() + 5;
        for (int step : new int[]{-1, 1}) {
            int x = area.right() - (step < 0 ? 31 : 17);
            var button = new Bounds(x, controlsY, 12, 12);
            FocalTextures.button(graphics, x, controlsY, 12, 12, button.contains(mouseX, mouseY), false);
            FocalTextures.sprite(graphics, step < 0 ? "glyph_minus" : "glyph_plus", x + 2, controlsY + 2, 8, 8);
        }
        String value = Math.round(scale * 100) + "%";
        if (area.width() >= 65)
            graphics.drawString(font, value, area.right() - 35 - font.width(value), controlsY + 2, 0xFFAAAAAA, false);
    }

    private static void drawLink(GuiGraphics graphics, Placed node) {
        int x = node.x();
        int y = node.y();
        int parentX = node.parent().x();
        FocalTextures.strip(graphics, x - 6, y - 22, 54, 232, 12, 12);
        if (x == parentX)
            return;
        boolean left = x < parentX;
        FocalTextures.strip(graphics, x - (left ? 4 : 12), y - 36, left ? 8 : 24, 240, 16, 16);
        for (int between = Math.min(x, parentX) + COLUMN; between < Math.max(x, parentX); between += COLUMN) {
            FocalTextures.strip(graphics, between - 12, y - 36, 72, 240, 24, 16);
        }
    }

    private void layout(List<Placed> nodes, Node node, List<Integer> path, int x, int row, Placed parent) {
        var current = new Placed(node, path, x, row * ROW, parent);
        nodes.add(current);
        if (node == null)
            return;
        int children = node.children().size();
        int required = node.kind() == Part.BRANCH ? 2 : node.kind().capacity();
        boolean showEmpty = children < node.kind().capacity() && (children < required || selection.equals(path));
        int displayed = children + (showEmpty ? 1 : 0);
        for (int index = 0; index < displayed; index++) {
            int offset = index - displayed / 2;
            if (displayed % 2 == 0 && offset >= 0)
                offset++;
            int column = x + offset * COLUMN;
            Node child = index < children ? node.children().get(index) : null;
            layout(nodes, child, childPath(path, index), column, row + 1, current);
        }
    }

    boolean select(Bounds area, double x, double y, int button) {
        if (!area.contains(x, y))
            return false;
        if (button == 0 && y >= area.y() + 5 && y < area.y() + 17 && x >= area.right() - 31 && x < area.right() - 5) {
            zoom(x < area.right() - 18 ? -1 : 1);
            return false;
        }
        Placed hit = hit(area, x, y);
        if (hit != null) {
            selection = hit.path();
            return true;
        }
        panning = button == 0;
        return false;
    }

    Component tooltip(Bounds area, int x, int y) {
        if (!area.contains(x, y))
            return null;
        Placed hit = hit(area, x, y);
        if (hit == null)
            return null;
        return hit.node() == null ? FocalManipulatorScreen.text("empty") : FocalManipulatorScreen.partName(hit.node().kind());
    }

    private Placed hit(Bounds area, double x, double y) {
        float scale = ZOOMS[zoom];
        double localX = (x - area.x() - area.width() / 2 - panX) / scale;
        double localY = (y - area.y() - 24 - panY) / scale;
        for (Placed node : placed) {
            if (Math.abs(localX - node.x()) <= NODE_SIZE / 2.0 && Math.abs(localY - node.y()) <= NODE_SIZE / 2.0)
                return node;
        }
        return null;
    }

    void zoom(int direction) {
        int next = Math.max(0, Math.min(ZOOMS.length - 1, zoom + direction));
        panX *= ZOOMS[next] / ZOOMS[zoom];
        panY *= ZOOMS[next] / ZOOMS[zoom];
        zoom = next;
    }

    boolean drag(double dx, double dy) {
        if (!panning)
            return false;
        panX += dx;
        panY += dy;
        return true;
    }

    void release() {
        panning = false;
    }

    record Bounds(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }
        int bottom() {
            return y + height;
        }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record Placed(Node node, List<Integer> path, int x, int y, Placed parent) {
    }
}
