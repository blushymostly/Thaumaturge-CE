package com.leclowndu93150.thaumaturge.client.screen.research;

import com.leclowndu93150.thaumaturge.client.screen.AbstractTTScreen;
import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark.BookmarkJitter;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark.BookmarkKind;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark.BookmarkLayout;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark.BookmarkPainter;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark.BookmarkSlot;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.insert.AspectInsertRenderer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.insert.RecipeInsertRenderer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.knowledge.KnowledgeGridRenderer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.page.NavigationLayout;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.page.NavigationPainter;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.page.PageTextLayout;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.requirement.RequirementRowRenderer;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchFlag;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundClearResearchFlagsPacket;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class EntryDetailScreen extends AbstractTTScreen {
    private static final float BOOK_ZOOM = 1.3F;
    private static final int BOOK_TEX_W = 256;
    private static final int BOOK_TEX_H = 181;
    private static final float TURN_VOLUME = 0.66F;
    private static final float SELECT_VOLUME = 0.7F;
    private static final float SELECT_PITCH = 0.9F;
    private static final float DISMISS_VOLUME = 0.4F;
    private static final float DISMISS_PITCH = 1.1F;
    private static final float WRITE_VOLUME = 0.66F;
    private static final List<ResearchFlag> OPEN_CLEARED_FLAGS = List.of(ResearchFlag.RESEARCH, ResearchFlag.PAGE);

    private final EntryDetailModel model;
    private final @Nullable Screen parent;
    private final RequirementRowRenderer requirementRows = new RequirementRowRenderer();
    private int bookLeft;
    private int bookTop;

    public EntryDetailScreen(ResearchEntry entry, ResourceLocation entryId, @Nullable Screen parent) {
        super(Component.translatable(entry.nameKey()));
        this.parent = parent;
        this.model = new EntryDetailModel(entry, entryId);
        ThaumonomiconBrowserScreen.rememberEntry(entryId);
    }

    @Override
    protected void init() {
        super.init();
        bookLeft = (width - BOOK_TEX_W) / 2;
        bookTop = (height - BOOK_TEX_H) / 2;
        rebuildPages();
        model.restorePage();
        clearFlagsOnce();
    }

    private void rebuildPages() {
        Player player = localPlayer();
        int displayedStage = model.displayedStage(player);
        int progressStage = model.progressStage(player);
        ResearchStage shown = model.stages().get(Math.max(0, displayedStage));
        int knowledgeRows = KnowledgeGridRenderer.activeTypeCount(player);
        model.replacePages(PageTextLayout.compose(font, model.entryId(), shown, knowledgeRows, model.stages().size(), progressStage,
                model.completedStageView(player)));
        model.markBuilt(displayedStage, progressStage);
    }

    @Override
    public void tick() {
        super.tick();
        if (model.needsRebuild(localPlayer())) {
            rebuildPages();
            model.restorePage();
        }
    }

    private void clearFlagsOnce() {
        if (!model.claimFlagClear()) {
            return;
        }
        TTNetwork.sendToServer(new ServerboundClearResearchFlagsPacket(model.entryId(), OPEN_CLEARED_FLAGS));
    }

    private @Nullable Player localPlayer() {
        return minecraft == null ? null : minecraft.player;
    }

    private DetailFrame frameFor(Player player) {
        return new DetailFrame(font, player, minecraft == null ? null : minecraft.level, bookLeft, bookTop, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawPaneBackground(graphics);
        Player player = localPlayer();
        if (player != null) {
            DetailFrame frame = frameFor(player);
            int displayedStage = model.displayedStage(player);
            ResearchStage stage = model.stages().get(Math.max(0, displayedStage));
            if (model.insertOpen()) {
                if (model.insert() == EntryDetailModel.Insert.ASPECTS) {
                    AspectInsertRenderer.render(graphics, frame, model, mouseX, mouseY);
                } else if (model.insert() == EntryDetailModel.Insert.KNOWLEDGE) {
                    KnowledgeGridRenderer.drawInsert(graphics, frame, mouseX, mouseY);
                } else if (model.insert() == EntryDetailModel.Insert.RECIPE) {
                    RecipeInsertRenderer.render(graphics, frame, model, mouseX, mouseY);
                }
            } else {
                PageTextLayout.drawSpread(graphics, frame, getTitle(), model.pages(), model.leftPage());
                requirementRows.render(graphics, frame, model, stage, displayedStage, mouseX, mouseY);
            }
            List<BookmarkSlot> slots = BookmarkLayout.slots(frame, model, stage);
            BookmarkPainter.paint(graphics, frame, slots, minecraft == null ? null : minecraft.level,
                    BookmarkJitter.forEntry(model.entryId(), displayedStage), mouseX, mouseY);
            if (!model.insertOpen()) {
                NavigationPainter.paint(graphics, frame, model, mouseX, mouseY);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPaneBackground(GuiGraphics graphics) {
        float shiftX = (width - BOOK_TEX_W * BOOK_ZOOM) * 0.5F;
        float shiftY = (height - BOOK_TEX_H * BOOK_ZOOM) * 0.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(shiftX, shiftY, 0);
        graphics.pose().scale(BOOK_ZOOM, BOOK_ZOOM, 1.0F);
        graphics.blit(TTScreenTextures.RESEARCH_BOOK, 0, 0, 0, 0, BOOK_TEX_W, BOOK_TEX_H, TTScreenTextures.TEX_SIZE, TTScreenTextures.TEX_SIZE);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && model.rotateStructure(dx, -dy))
                || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && model.endStructureRotation())
                || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        Player player = localPlayer();
        return (player != null && RecipeInsertRenderer.scrollPreview(frameFor(player), model, x, y, delta))
                || super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Player player = localPlayer();
        if (player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        DetailFrame frame = frameFor(player);
        ResearchStage stage = model.stages().get(Math.max(0, model.displayedStage(player)));
        for (BookmarkSlot slot : BookmarkLayout.slots(frame, model, stage)) {
            if (!slot.hoverArea().contains(mouseX, mouseY)) {
                continue;
            }
            if (slot.kind() == BookmarkKind.ASPECTS) {
                if (model.insert() == EntryDetailModel.Insert.ASPECTS) {
                    model.closeInsert();
                    playCue(TTSounds.PAGE.get(), DISMISS_VOLUME, DISMISS_PITCH);
                } else {
                    model.openInsert(EntryDetailModel.Insert.ASPECTS);
                    playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
                }
                return true;
            }
            if (slot.kind() == BookmarkKind.KNOWLEDGE) {
                if (model.insert() == EntryDetailModel.Insert.KNOWLEDGE) {
                    model.closeInsert();
                    playCue(TTSounds.PAGE.get(), DISMISS_VOLUME, DISMISS_PITCH);
                } else {
                    model.openInsert(EntryDetailModel.Insert.KNOWLEDGE);
                    playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
                }
                return true;
            }
            if (slot.kind() == BookmarkKind.RECIPE && slot.recipeId() != null) {
                model.selectRecipe(slot.recipeId());
                playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
                return true;
            }
        }
        if (RecipeInsertRenderer.startPreviewDrag(frame, model, mouseX, mouseY)) return true;
        if (RecipeInsertRenderer.clickPreviewLayer(frame, model, mouseX, mouseY)) {
            playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
            return true;
        }
        if (model.insert() == EntryDetailModel.Insert.RECIPE && RecipeInsertRenderer.clickPager(model, frame, mouseX, mouseY)) {
            playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
            return true;
        }
        if (model.insert() == EntryDetailModel.Insert.ASPECTS) {
            int pages = AspectInsertRenderer.pageCount();
            if (mouseX < frame.paperLeft() + 50) {
                model.shiftAspectsPage(-1, pages);
                playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
                return true;
            }
            if (mouseX > frame.paperLeft() + 200) {
                model.shiftAspectsPage(1, pages);
                playCue(TTSounds.PAGE.get(), SELECT_VOLUME, SELECT_PITCH);
                return true;
            }
        }
        if (model.insertOpen()) {
            return true;
        }
        if (NavigationLayout.backButton(frame).contains(mouseX, mouseY)) {
            returnToBrowser();
            return true;
        }
        if (model.leftPage() > 0 && NavigationLayout.pageLeftHit(frame).contains(mouseX, mouseY) && model.turnSpreadBack()) {
            playCue(TTSounds.PAGE.get(), TURN_VOLUME, 1.0F);
            return true;
        }
        if (model.hasNextSpread() && NavigationLayout.pageRightHit(frame).contains(mouseX, mouseY) && model.turnSpreadForward()) {
            playCue(TTSounds.PAGE.get(), TURN_VOLUME, 1.0F);
            return true;
        }
        if (model.canNavigateStageHistory(player)) {
            int displayed = model.displayedStage(player);
            int progress = model.progressStage(player);
            if (displayed > 0 && NavigationLayout.stageLeftHit(frame).contains(mouseX, mouseY)) {
                model.pinStage(displayed - 1, progress);
                rebuildPages();
                playCue(TTSounds.PAGE.get(), TURN_VOLUME, 1.0F);
                return true;
            }
            if (displayed < progress && NavigationLayout.stageRightHit(frame).contains(mouseX, mouseY)) {
                model.pinStage(displayed + 1, progress);
                rebuildPages();
                playCue(TTSounds.PAGE.get(), TURN_VOLUME, 1.0F);
                return true;
            }
        }
        Rect complete = requirementRows.completeButtonHit();
        if (complete.width() > 0 && complete.contains(mouseX, mouseY)) {
            requirementRows.tryAdvance(model);
            playCue(TTSounds.WRITE.get(), WRITE_VOLUME, 1.0F);
            return true;
        }
        if (requirementRows.tryObtainTheoryNote(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (model.insertOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            model.closeInsert();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            returnToBrowser();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void returnToBrowser() {
        if (model.insertOpen()) {
            model.closeInsert();
            playCue(TTSounds.PAGE.get(), DISMISS_VOLUME, DISMISS_PITCH);
            return;
        }
        ThaumonomiconBrowserScreen.rememberEntry(null);
        if (minecraft != null) {
            minecraft.setScreen(parent != null ? parent : new ThaumonomiconBrowserScreen());
        }
    }

    private void playCue(SoundEvent sound, float volume, float pitch) {
        Player listener = localPlayer();
        if (listener != null) {
            listener.playSound(sound, volume, pitch);
        }
    }

    @Override
    public void onClose() {
        ThaumonomiconBrowserScreen.rememberEntry(null);
        super.onClose();
    }

    /** JEI / EMI: item under mouse on recipe insert (16×16 hit). */
    public @Nullable HoveredItem itemUnderMouse(double mouseX, double mouseY) {
        Player player = localPlayer();
        if (player == null || model.insert() != EntryDetailModel.Insert.RECIPE) {
            return null;
        }
        return RecipeInsertRenderer.hoveredItem(frameFor(player), model, mouseX, mouseY);
    }

    /** JEI aspect under mouse — Wave 0 returns null until aspect ingredient type lands. */
    public @Nullable HoveredAspect aspectUnderMouse(double mouseX, double mouseY) {
        return null;
    }

    public record HoveredItem(ItemStack stack, int x, int y) {}

    public record HoveredAspect(ResourceLocation aspect, int x, int y) {}
}
