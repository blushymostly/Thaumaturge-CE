package com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark;

import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.research.ResearchRecipeResolver;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.knowledge.KnowledgeGridRenderer;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class BookmarkLayout {
    public static final int SIDE_WIDTH = 25;
    public static final int SIDE_HEIGHT = 16;

    private static final int SIDE_OFFSET_X = -48;
    private static final int ASPECT_Y = 9;
    private static final int KNOWLEDGE_Y = 32;
    private static final int RECIPE_OFFSET_X = 280;
    private static final int RECIPE_FIRST_Y = -8;
    private static final int RECIPE_MAX_STEP = 25;
    private static final int RECIPE_TOTAL_BUDGET = 200;
    private static final int RECIPE_WIDTH = 30;
    private static final int RECIPE_HEIGHT = 16;
    private static final int RECIPE_TOP_INSET = 1;

    private BookmarkLayout() {}

    public static List<BookmarkSlot> slots(DetailFrame frame, EntryDetailModel model, ResearchStage stage) {
        List<BookmarkSlot> slots = new ArrayList<>();
        if (ClientKnowledge.get().isComplete(EntryDetailModel.FIRST_STEPS) || model.isAspectsEntry()) {
            slots.add(sideSlot(BookmarkKind.ASPECTS, frame, ASPECT_Y));
        }
        boolean knowsKnowledge = ClientKnowledge.get().isComplete(EntryDetailModel.KNOWLEDGE_TYPES) || KnowledgeGridRenderer.hasAnyKnowledge(frame.player());
        if (knowsKnowledge && !model.isKnowledgeTypesEntry()) {
            slots.add(sideSlot(BookmarkKind.KNOWLEDGE, frame, KNOWLEDGE_Y));
        }
        List<ResourceLocation> recipes = stage.recipes().stream()
                .filter(id -> ResearchRecipeResolver.exists(frame.level(), id)).toList();
        if (recipes.isEmpty()) {
            return slots;
        }
        int step = Math.min(RECIPE_MAX_STEP, RECIPE_TOTAL_BUDGET / recipes.size());
        for (int ordinal = 0; ordinal < recipes.size(); ordinal++) {
            Rect area = new Rect(frame.left() + RECIPE_OFFSET_X, frame.top() + RECIPE_FIRST_Y + step * ordinal - RECIPE_TOP_INSET, RECIPE_WIDTH, RECIPE_HEIGHT);
            slots.add(new BookmarkSlot(BookmarkKind.RECIPE, recipes.get(ordinal), area));
        }
        return slots;
    }

    public static List<BookmarkSlot> ofKind(List<BookmarkSlot> slots, BookmarkKind kind) {
        return slots.stream().filter(slot -> slot.kind() == kind).toList();
    }

    private static BookmarkSlot sideSlot(BookmarkKind kind, DetailFrame frame, int hoverY) {
        return new BookmarkSlot(kind, null, new Rect(frame.left() + SIDE_OFFSET_X, frame.top() + hoverY, SIDE_WIDTH, SIDE_HEIGHT));
    }
}
