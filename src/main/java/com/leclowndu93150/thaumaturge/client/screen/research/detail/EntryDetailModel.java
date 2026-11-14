package com.leclowndu93150.thaumaturge.client.screen.research.detail;

import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.research.PageParser;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class EntryDetailModel {
    private static final int NONE = -1;
    public static final ResourceLocation FIRST_STEPS = ResourceLocation.tryParse("thaumaturge:first_steps");
    public static final ResourceLocation KNOWLEDGE_TYPES = ResourceLocation.tryParse("thaumaturge:knowledge_types");

    public enum Insert {
        NONE, ASPECTS, KNOWLEDGE, RECIPE
    }

    private final ResearchEntry entry;
    private final ResourceLocation entryId;
    private final List<PageParser.Page> pages = new ArrayList<>();
    private int leftPage;
    private int pinnedStage = NONE;
    private boolean flagsCleared;
    private boolean autoAdvanced;
    private int builtDisplayed = -1;
    private int builtProgress = -1;
    private Insert insert = Insert.NONE;
    private @Nullable ResourceLocation selectedRecipe;
    private int recipeVariant;
    private float rotationOffset;
    private float dragRotation = Float.NaN;
    private float structureTilt = 32;
    private int structureLayer = -1;

    public float structureRotation() {
        return Float.isNaN(dragRotation) ? automaticRotation() + rotationOffset : dragRotation;
    }

    public void beginStructureRotation() {
        dragRotation = structureRotation();
    }

    public float structureTilt() {
        return structureTilt;
    }

    public boolean rotateStructure(double delta, double tiltDelta) {
        if (Float.isNaN(dragRotation)) return false;
        dragRotation = (dragRotation + (float) delta) % 360;
        structureTilt = Mth.clamp(structureTilt + (float) tiltDelta, 10, 75);
        return true;
    }

    public boolean endStructureRotation() {
        if (Float.isNaN(dragRotation)) return false;
        rotationOffset = dragRotation - automaticRotation();
        dragRotation = Float.NaN;
        return true;
    }

    private static float automaticRotation() {
        return (net.minecraft.Util.getMillis() % 72000L) / 200F;
    }

    private void resetStructure() {
        structureLayer = -1;
        rotationOffset = 22.5F - automaticRotation();
        structureTilt = 32;
        dragRotation = Float.NaN;
    }

    public int structureLayer() {
        return structureLayer;
    }

    public void changeStructureLayer(int step, int layerCount) {
        structureLayer = Math.floorMod(structureLayer + 1 + step, layerCount + 1) - 1;
    }
    private int aspectsPageIndex;

    public EntryDetailModel(ResearchEntry entry, ResourceLocation entryId) {
        this.entry = entry;
        this.entryId = entryId;
    }

    public ResearchEntry research() {
        return entry;
    }

    public ResourceLocation entryId() {
        return entryId;
    }

    public List<ResearchStage> stages() {
        return entry.stages();
    }

    public List<PageParser.Page> pages() {
        return pages;
    }

    public void replacePages(List<PageParser.Page> next) {
        pages.clear();
        pages.addAll(next);
        leftPage = Mth.clamp(leftPage, 0, Math.max(0, pages.size() - 1));
        if ((leftPage & 1) != 0) {
            leftPage--;
        }
    }

    public void markBuilt(int displayed, int progress) {
        builtDisplayed = displayed;
        builtProgress = progress;
    }

    public boolean needsRebuild(@Nullable Player player) {
        return builtDisplayed != displayedStage(player) || builtProgress != progressStage(player);
    }

    public int leftPage() {
        return leftPage;
    }

    public void setLeftPage(int page) {
        leftPage = Math.max(0, page);
    }

    public void restorePage() {
        leftPage = Mth.clamp(leftPage, 0, Math.max(0, pages.size() - 1));
        if ((leftPage & 1) != 0) {
            leftPage = Math.max(0, leftPage - 1);
        }
    }

    public boolean turnSpreadForward() {
        if (leftPage + 2 < pages.size()) {
            leftPage += 2;
            return true;
        }
        return false;
    }

    public boolean turnSpreadBack() {
        if (leftPage >= 2) {
            leftPage -= 2;
            return true;
        }
        if (leftPage == 1) {
            leftPage = 0;
            return true;
        }
        return false;
    }

    public boolean hasNextSpread() {
        return leftPage + 2 < pages.size();
    }

    public int progressStage(@Nullable Player player) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        boolean complete = knowledge.isComplete(entryId);
        int stageCount = stages().size();
        int stored = knowledge.stage(entryId);
        int resolved = Math.max(stored, 0);
        if (complete || resolved >= stageCount) {
            resolved = stageCount - 1;
        }
        return Math.min(Math.max(0, resolved), Math.max(0, stageCount - 1));
    }

    public int displayedStage(@Nullable Player player) {
        int progress = progressStage(player);
        if (pinnedStage >= 0) {
            return Math.min(pinnedStage, progress);
        }
        return progress;
    }

    public void pinStage(int requested, int progress) {
        int clamped = Mth.clamp(requested, 0, progress);
        pinnedStage = clamped == progress ? NONE : clamped;
        leftPage = 0;
    }

    public boolean canNavigateStageHistory(@Nullable Player player) {
        return stages().size() > 1 && progressStage(player) > 0;
    }

    public boolean completedStageView(@Nullable Player player) {
        return displayedStage(player) < progressStage(player) || ClientKnowledge.get().isComplete(entryId);
    }

    public boolean claimFlagClear() {
        if (flagsCleared) {
            return false;
        }
        flagsCleared = true;
        return true;
    }

    public boolean claimAutoAdvance() {
        if (autoAdvanced) {
            return false;
        }
        autoAdvanced = true;
        return true;
    }

    public Insert insert() {
        return insert;
    }

    public boolean insertOpen() {
        return insert != Insert.NONE;
    }

    public void openInsert(Insert kind) {
        insert = kind;
        if (kind == Insert.ASPECTS) {
            aspectsPageIndex = 0;
        }
    }

    public void closeInsert() {
        insert = Insert.NONE;
        selectedRecipe = null;
        recipeVariant = 0;
        resetStructure();
    }

    public int aspectsPageIndex() {
        return aspectsPageIndex;
    }

    public void shiftAspectsPage(int delta, int pageCount) {
        if (pageCount <= 0) {
            aspectsPageIndex = 0;
            return;
        }
        aspectsPageIndex = Mth.clamp(aspectsPageIndex + delta, 0, pageCount - 1);
    }

    public void selectRecipe(ResourceLocation recipeId) {
        if (insert == Insert.RECIPE && recipeId.equals(selectedRecipe)) {
            closeInsert();
            return;
        }
        selectedRecipe = recipeId;
        recipeVariant = 0;
        resetStructure();
        insert = Insert.RECIPE;
    }

    @Nullable
    public ResourceLocation selectedRecipe() {
        return selectedRecipe;
    }

    public int recipeVariant() {
        return recipeVariant;
    }

    public void setRecipeVariant(int variant) {
        int next = Math.max(0, variant);
        if (recipeVariant != next) resetStructure();
        recipeVariant = next;
    }

    public boolean isAspectsEntry() {
        return FIRST_STEPS.equals(entryId) || entry.hasMeta("aspects");
    }

    public boolean isKnowledgeTypesEntry() {
        return KNOWLEDGE_TYPES.equals(entryId);
    }

    public static boolean knowsResearch(@Nullable Player player, ResourceLocation id) {
        return ClientKnowledge.get().isComplete(id);
    }
}
