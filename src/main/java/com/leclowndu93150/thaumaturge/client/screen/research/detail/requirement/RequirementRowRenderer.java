package com.leclowndu93150.thaumaturge.client.screen.research.detail.requirement;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.ClientKnowledge;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.client.render.research.EntryIconRenderer;
import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.DetailFrame;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.EntryDetailModel;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.AspectTileDrawer;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookBlit;
import com.leclowndu93150.thaumaturge.client.screen.research.detail.draw.BookSprites;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeReward;
import com.leclowndu93150.thaumaturge.content.research.KnowledgeType;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchData;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchRequirement;
import com.leclowndu93150.thaumaturge.content.research.ResearchStage;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundAdvanceStagePacket;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundObtainNotePacket;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the research / obtain / craft / knowledge requirement rows at the bottom of a stage page,
 * plus the completion divider and "Complete" button.
 */
public final class RequirementRowRenderer {
    private static final int LABEL_U = 200;
    private static final int LABEL_WIDTH = 56;
    private static final int LABEL_HEIGHT = 16;
    private static final int LABEL_OFFSET_X = -12;
    private static final int LABEL_DRAW_Y_SHIFT = -1;
    private static final int LABEL_HOVER_DIVISOR = 4;
    private static final int LABEL_TINT = 0x40FFFFFF;

    private static final int PREREQ_ICON_SIZE = 16;
    private static final int PREREQ_TEXTURE_SIZE = 32;
    private static final int PREREQ_UNKNOWN_TINT = 0xFF80BFFF;
    private static final String PREREQ_MAP_PREFIX = "m_";
    private static final String PREREQ_CHEST_PREFIX = "c_";
    private static final String PREREQ_FLASK_PREFIX = "f_";
    private static final String SCANNED_ASPECT_PREFIX = "scanned/aspect/";

    private static final long ROTATION_MILLIS = 1000L;
    private static final long PULSE_PERIOD_MILLIS = 1000L;
    private static final float PULSE_AMPLITUDE = 0.25F;
    private static final float PULSE_FLOOR = 0.75F;
    private static final int OBSERVATION_UNKNOWN_TINT = 0xFF80BFFF;

    private static final int COMPLETE_DIVIDER_INSET = 4;
    private static final int COMPLETE_DIVIDER_Y_OFFSET = -2;
    private static final int COMPLETE_BUTTON_U = 84;
    private static final int COMPLETE_BUTTON_V = 216;
    private static final int COMPLETE_BUTTON_TINT_IDLE = 0xFFCCCCE6;
    private static final int COMPLETE_CAPTION_CENTER_X = 52;
    private static final int COMPLETE_CAPTION_Y_OFFSET = -4;

    private Rect completeHit = new Rect(0, 0, 0, 0);
    private final List<TheoryHit> theoryHits = new ArrayList<>();

    private record TheoryHit(Rect area, ResourceLocation entryId, int ordinal, ResourceLocation learnKey) {}

    public Rect completeButtonHit() {
        return completeHit;
    }

    public boolean tryObtainTheoryNote(double mouseX, double mouseY) {
        for (TheoryHit hit : theoryHits) {
            if (hit.area().contains((int) mouseX, (int) mouseY)) {
                TTNetwork.sendToServer(new ServerboundObtainNotePacket(hit.entryId(), hit.ordinal()));
                return true;
            }
        }
        return false;
    }

    public void render(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, ResearchStage stage, int displayedStage, int mouseX, int mouseY) {
        completeHit = new Rect(0, 0, 0, 0);
        theoryHits.clear();
        if (model.leftPage() > 0) {
            return;
        }
        Player player = frame.player();
        boolean completedStage = model.completedStageView(player);
        PlayerKnowledge knowledge = ClientKnowledge.get();
        RequirementLayout layout = RequirementLayout.of(stage, frame.top());
        boolean allMet = true;
        if (layout.hasResearch()) {
            drawRowLabel(graphics, frame, layout.researchY(), RequirementRowKind.RESEARCH, mouseX, mouseY);
            allMet &= drawResearchRow(graphics, frame, stage.requiredResearch(), knowledge, layout.researchY(), completedStage, mouseX, mouseY);
        }
        if (layout.hasObtain()) {
            drawRowLabel(graphics, frame, layout.obtainY(), RequirementRowKind.OBTAIN, mouseX, mouseY);
            allMet &= drawItemRow(graphics, frame, knowledge, stage.obtain(), layout.obtainY(), true, completedStage, mouseX, mouseY);
        }
        if (layout.hasCraft()) {
            drawRowLabel(graphics, frame, layout.craftY(), RequirementRowKind.CRAFT, mouseX, mouseY);
            allMet &= drawItemRow(graphics, frame, knowledge, stage.craft(), layout.craftY(), false, completedStage, mouseX, mouseY);
        }
        if (layout.hasKnowledge()) {
            drawRowLabel(graphics, frame, layout.knowledgeY(), RequirementRowKind.KNOWLEDGE, mouseX, mouseY);
            allMet &= drawKnowledgeRow(graphics, frame, model, stage, knowledge, layout.knowledgeY(), displayedStage, completedStage, mouseX, mouseY);
        }
        if (layout.any()) {
            BookBlit.sprite(graphics, frame.left() + COMPLETE_DIVIDER_INSET, layout.dividerY() + COMPLETE_DIVIDER_Y_OFFSET, BookSprites.DIVIDER_U, BookSprites.DIVIDER_V,
                    BookSprites.DIVIDER_WIDTH, BookSprites.DIVIDER_TALL_HEIGHT);
            if (allMet && !completedStage) {
                drawCompleteButton(graphics, frame, layout, mouseX, mouseY);
            } else if (allMet) {
                drawCaption(graphics, frame, layout, Component.translatable("gui.thaumaturge.thaumonomicon.stage_completed"));
            }
        } else if (!completedStage && model.claimAutoAdvance()) {
            // CE: stages with no requirements auto-advance once when the page is viewed.
            tryAdvance(model);
        }
    }

    public void tryAdvance(EntryDetailModel model) {
        TTNetwork.sendToServer(new ServerboundAdvanceStagePacket(model.entryId()));
    }

    private void drawCompleteButton(GuiGraphics graphics, DetailFrame frame, RequirementLayout layout, int mouseX, int mouseY) {
        Rect button = layout.completeButton(frame.left());
        completeHit = button;
        int tint = button.contains(mouseX, mouseY) ? BookSprites.WHITE : COMPLETE_BUTTON_TINT_IDLE;
        BookBlit.tintedSprite(graphics, button.x(), button.y(), COMPLETE_BUTTON_U, COMPLETE_BUTTON_V, button.width(), button.height(), tint);
        drawCaption(graphics, frame, layout, Component.translatable("gui.thaumaturge.thaumonomicon.complete_stage"));
    }

    private void drawCaption(GuiGraphics graphics, DetailFrame frame, RequirementLayout layout, Component caption) {
        graphics.drawString(frame.font(), caption, frame.left() + COMPLETE_CAPTION_CENTER_X - frame.font().width(caption) / 2, layout.dividerY() + COMPLETE_CAPTION_Y_OFFSET,
                BookSprites.WHITE, true);
    }

    private void drawRowLabel(GuiGraphics graphics, DetailFrame frame, int rowY, RequirementRowKind kind, int mouseX, int mouseY) {
        int labelX = frame.left() + LABEL_OFFSET_X;
        BookBlit.tintedSprite(graphics, labelX, rowY + LABEL_DRAW_Y_SHIFT, LABEL_U, kind.labelV(), LABEL_WIDTH, LABEL_HEIGHT, LABEL_TINT);
        if (Rect.inside(labelX, rowY, LABEL_WIDTH / LABEL_HOVER_DIVISOR, LABEL_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(frame.font(), Component.translatable(kind.tooltipKey()), mouseX, mouseY);
        }
    }

    private boolean drawResearchRow(GuiGraphics graphics, DetailFrame frame, List<ResourceLocation> prerequisites, PlayerKnowledge knowledge, int rowY, boolean completedStage,
            int mouseX, int mouseY) {
        int spacing = RequirementLayout.spacingFor(prerequisites.size());
        boolean allMet = true;
        for (int index = 0; index < prerequisites.size(); index++) {
            ResourceLocation prerequisite = prerequisites.get(index);
            int slotX = RequirementLayout.slotX(frame.left(), index, spacing);
            drawPrerequisiteIcon(graphics, frame, slotX, rowY, prerequisite);
            boolean met = completedStage || knowledge.isComplete(prerequisite);
            allMet &= met;
            if (met) {
                BookBlit.checkmark(graphics, slotX, rowY);
            }
            if (Rect.inside(slotX, rowY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE, mouseX, mouseY)) {
                Component name = prerequisiteName(prerequisite);
                String hint = "research." + prerequisite.getNamespace() + "." + prerequisite.getPath() + ".title.hint";
                if (net.minecraft.client.resources.language.I18n.exists(hint)) {
                    graphics.renderComponentTooltip(frame.font(), List.of(name, Component.translatable(hint)), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(frame.font(), name, mouseX, mouseY);
                }
            }
        }
        return allMet;
    }

    private static Component prerequisiteName(ResourceLocation prerequisite) {
        ResearchEntry entry = ResearchData.entry(prerequisite);
        if (entry != null) {
            return Component.translatable(entry.nameKey());
        }
        IAspect aspect = scannedAspect(prerequisite);
        if (aspect != null) {
            return Component.literal(aspect.tag());
        }
        String key = "research." + prerequisite.getNamespace() + "." + prerequisite.getPath() + ".title";
        return net.minecraft.client.resources.language.I18n.exists(key) ? Component.translatable(key) : Component.literal(prerequisite.toString());
    }

    private void drawPrerequisiteIcon(GuiGraphics graphics, DetailFrame frame, int x, int y, ResourceLocation prerequisite) {
        ResearchEntry linked = ResearchData.entry(prerequisite);
        if (linked != null) {
            EntryIconRenderer.drawResearchIcon(graphics, x, y, EntryIconRenderer.resolveIcon(linked, frame.tick()), false);
            return;
        }
        ResourceLocation flagIcon = flagIconFor(prerequisite.getPath());
        if (flagIcon != null) {
            graphics.blit(flagIcon, x, y, PREREQ_ICON_SIZE, PREREQ_ICON_SIZE, 0.0F, 0.0F, PREREQ_TEXTURE_SIZE, PREREQ_TEXTURE_SIZE, PREREQ_TEXTURE_SIZE, PREREQ_TEXTURE_SIZE);
            return;
        }
        IAspect aspect = scannedAspect(prerequisite);
        if (aspect != null) {
            AspectTileDrawer.known(graphics, x, y, aspect);
            return;
        }
        BookBlit.iconTile(graphics, AspectTileDrawer.UNKNOWN_TILE, x, y, PREREQ_UNKNOWN_TINT);
    }

    private static @Nullable IAspect scannedAspect(ResourceLocation prerequisite) {
        String path = prerequisite.getPath();
        if (!path.startsWith(SCANNED_ASPECT_PREFIX)) {
            return null;
        }
        String rest = path.substring(SCANNED_ASPECT_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        ResourceLocation aspectId = ResourceLocation.tryParse(rest.substring(0, slash) + ":" + rest.substring(slash + 1));
        return aspectId == null ? null : TTAspects.get(aspectId);
    }

    private static @Nullable ResourceLocation flagIconFor(String path) {
        if (path.startsWith(PREREQ_MAP_PREFIX)) {
            return TTScreenTextures.RESEARCH_PREREQ_MAP;
        }
        if (path.startsWith(PREREQ_CHEST_PREFIX)) {
            return TTScreenTextures.RESEARCH_PREREQ_CHEST;
        }
        if (path.startsWith(PREREQ_FLASK_PREFIX)) {
            return TTScreenTextures.RESEARCH_PREREQ_FLASK;
        }
        return null;
    }

    private boolean drawItemRow(GuiGraphics graphics, DetailFrame frame, PlayerKnowledge knowledge, List<ResearchRequirement> requirements, int rowY, boolean obtainRow,
            boolean completedStage, int mouseX, int mouseY) {
        Player player = frame.player();
        Font font = frame.font();
        int spacing = RequirementLayout.spacingFor(requirements.size());
        boolean allMet = true;
        for (int index = 0; index < requirements.size(); index++) {
            ResearchRequirement requirement = requirements.get(index);
            int slotX = RequirementLayout.slotX(frame.left(), index, spacing);
            ItemStack shown = rotatingStack(requirement, index);
            if (!shown.isEmpty()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.renderItem(shown, slotX, rowY);
                graphics.renderItemDecorations(font, shown, slotX, rowY);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            boolean met = completedStage || (obtainRow ? StageRequirementCheck.obtainMet(player, requirement) : StageRequirementCheck.craftMet(knowledge, requirement));
            allMet &= met;
            if (met) {
                BookBlit.checkmark(graphics, slotX, rowY);
            }
            if (Rect.inside(slotX, rowY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE, mouseX, mouseY)) {
                if (shown.isEmpty()) {
                    graphics.renderTooltip(font, Component.translatable(obtainRow ? RequirementRowKind.OBTAIN.tooltipKey() : RequirementRowKind.CRAFT.tooltipKey()), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, shown, mouseX, mouseY);
                }
            }
        }
        return allMet;
    }

    private static ItemStack rotatingStack(ResearchRequirement requirement, int slotIndex) {
        ItemStack stack = requirement.stackAt(slotIndex + System.currentTimeMillis() / ROTATION_MILLIS);
        if (!stack.isEmpty()) {
            stack.setCount(Math.max(1, Math.min(requirement.amount(), stack.getMaxStackSize())));
        }
        return stack;
    }

    private boolean drawKnowledgeRow(GuiGraphics graphics, DetailFrame frame, EntryDetailModel model, ResearchStage stage, PlayerKnowledge knowledge, int rowY,
            int displayedStage, boolean completedStage, int mouseX, int mouseY) {
        Player player = frame.player();
        Font font = frame.font();
        List<KnowledgeReward> rewards = stage.requiredKnowledge();
        AspectList observationCost = ResearchNotes.stageObservationCost(model.research(), stage);
        int spacing = RequirementLayout.spacingFor(Math.max(rewards.size(), observationCost.entries().size()));
        int[] slotXs = RequirementLayout.knowledgeSlotXs(frame.left(), rewards, spacing, observationCost.entries().size());
        boolean observationAffordable = completedStage || observationCost.isEmpty() || AspectPools.canAfford(player, observationCost);
        boolean observationDrawn = false;
        boolean allMet = true;
        int theoryOrdinal = ResearchNotes.theoryRowsBefore(model.research(), displayedStage);
        for (int index = 0; index < rewards.size(); index++) {
            KnowledgeReward reward = rewards.get(index);
            int slotX = slotXs[index];
            if (reward.type() == KnowledgeType.THEORY) {
                int ordinal = theoryOrdinal++;
                ResourceLocation learnKey = ResearchNoteData.learnKey(model.entryId(), ordinal);
                boolean met = completedStage || knowledge.isKnown(learnKey);
                allMet &= met;
                ItemStack noteIcon = new ItemStack(TTItems.RESEARCH_NOTE.get());
                graphics.renderItem(noteIcon, slotX, rowY);
                graphics.renderItemDecorations(font, noteIcon, slotX, rowY);
                theoryHits.add(new TheoryHit(new Rect(slotX, rowY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE), model.entryId(), ordinal, learnKey));
                if (met) {
                    BookBlit.checkmark(graphics, slotX, rowY);
                }
                if (Rect.inside(slotX, rowY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE, mouseX, mouseY)) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("tooltip.thaumaturge.research_note.theory", Component.translatable(model.research().nameKey())));
                    if (!met) {
                        String hint = ResearchNotes.hasNoteFor(frame.player(), learnKey)
                                ? "tooltip.thaumaturge.research_note.table"
                                : "gui.thaumaturge.thaumonomicon.note_click";
                        lines.add(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
                    }
                    graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                }
                continue;
            }
            allMet &= observationAffordable;
            if (!observationDrawn) {
                observationDrawn = true;
                drawObservationChips(graphics, frame, observationCost, slotX, rowY, spacing, completedStage, mouseX, mouseY);
            }
        }
        return allMet;
    }

    private void drawObservationChips(GuiGraphics graphics, DetailFrame frame, AspectList cost, int firstX, int rowY, int spacing, boolean completedStage, int mouseX, int mouseY) {
        Player player = frame.player();
        Font font = frame.font();
        List<AspectInstance> chips = cost.entries();
        for (int index = 0; index < chips.size(); index++) {
            AspectInstance chip = chips.get(index);
            int chipX = firstX + index * spacing;
            boolean discovered = AspectPools.isDiscovered(player, chip.aspectId());
            boolean hovered = Rect.inside(chipX, rowY, BookSprites.SLOT_SIZE, BookSprites.SLOT_SIZE, mouseX, mouseY);
            IAspect aspect = TTAspects.get(chip.aspectId());
            if (discovered && aspect != null) {
                int have = AspectPools.amount(player, chip.aspectId());
                float alpha = have < chip.amount()
                        ? Mth.sin(System.currentTimeMillis() % PULSE_PERIOD_MILLIS / (float) PULSE_PERIOD_MILLIS * Mth.TWO_PI) * PULSE_AMPLITUDE + PULSE_FLOOR
                        : 1.0F;
                AspectTagRenderer.render(graphics, font, chipX, rowY, aspect, chip.amount(), alpha, false);
                if (hovered) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("gui.thaumaturge.thaumonomicon.research_cost"));
                    lines.add(Component.translatable("tooltip.thaumaturge.amount_needed", AspectNames.name(chip.aspectId()), have, chip.amount())
                            .withStyle(have >= chip.amount() ? ChatFormatting.GREEN : ChatFormatting.RED));
                    graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                }
            } else {
                BookBlit.iconTile(graphics, AspectTileDrawer.UNKNOWN_TILE, chipX, rowY, OBSERVATION_UNKNOWN_TINT);
                if (hovered) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("tooltip.thaumaturge.aspect.unknown"));
                    lines.add(Component.translatable("message.thaumaturge.research.discovery_error", AspectNames.help(chip.aspectId())).withStyle(ChatFormatting.GRAY));
                    graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                }
            }
            if (completedStage || discovered && AspectPools.amount(player, chip.aspectId()) >= chip.amount()) {
                BookBlit.checkmark(graphics, chipX, rowY);
            }
        }
    }
}
