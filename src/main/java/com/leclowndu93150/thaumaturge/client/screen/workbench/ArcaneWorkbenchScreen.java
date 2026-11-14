package com.leclowndu93150.thaumaturge.client.screen.workbench;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;
import com.leclowndu93150.thaumaturge.content.workbench.WorkbenchContainer;
import com.leclowndu93150.thaumaturge.content.workbench.WorkbenchCraftingContainer;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/arcane_workbench.png");
    private static final ResourceLocation WAND_SLOT_TEXTURE = TTIds.rl("textures/gui/workbench_wand_slot.png");
    private static final int TEX_SIZE = 256;
    private static final int WAND_SLOT_TEX_W = 38;
    private static final int WAND_SLOT_TEX_H = 34;
    private static final int WAND_SLOT_TEX_OFFSET_X = 10;
    private static final int WAND_SLOT_TEX_OFFSET_Y = 6;
    private static final float CRYSTAL_SPACING_DEGREES = 60.0F;
    private static final float CRYSTAL_SPIN_TICKS_PER_RADIAN = 75.0F;
    private static final int VIS_TEXT_CENTER_X = 168;
    private static final int VIS_AVAILABLE_Y = 46;
    private static final int VIS_REQUIRED_Y = 38;
    private static final float VIS_TEXT_SCALE = 0.5F;
    private static final int VIS_AVAILABLE_COLOR = 0xFF6E6EEE;
    private static final int VIS_SHORT_COLOR = 0xFFEE6E6E;
    private static final int VIS_REQUIRED_COLOR = 0xFFC0FFFF;
    private static final int SLOT_SIZE = 16;
    private static final int CRYSTAL_SUBSTITUTE_VIS = 2;
    private static final int WAND_COST_Y_OFFSET = 118;
    private static final int WAND_COST_HOVER_MIN_HALF_WIDTH = 12;

    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 190;
        imageHeight = 234;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE);
        graphics.blit(WAND_SLOT_TEXTURE,
                x + ArcaneWorkbenchMenu.WAND_X - WAND_SLOT_TEX_OFFSET_X,
                y + ArcaneWorkbenchMenu.WAND_Y - WAND_SLOT_TEX_OFFSET_Y,
                0, 0, WAND_SLOT_TEX_W, WAND_SLOT_TEX_H, WAND_SLOT_TEX_W, WAND_SLOT_TEX_H);
        renderCrystalOverlays(graphics, x, y);
    }

    private void renderCrystalOverlays(GuiGraphics graphics, int left, int top) {
        List<OverlayCrystal> overlays = overlayCrystals();
        if (overlays.isEmpty()) {
            return;
        }
        float spin = (minecraft != null ? minecraft.level.getGameTime() : 0) / CRYSTAL_SPIN_TICKS_PER_RADIAN;
        PoseStack pose = graphics.pose();
        for (OverlayCrystal overlay : overlays) {
            int index = ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(overlay.aspectId());
            if (index < 0) {
                continue;
            }
            int slotX = left + ArcaneWorkbenchMenu.CRYSTAL_X[index];
            int slotY = top + ArcaneWorkbenchMenu.CRYSTAL_Y[index];
            int color = aspectColor(overlay.aspectId());
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            pose.pushPose();
            pose.translate(slotX + 7.5F, slotY + 8.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotation(index * CRYSTAL_SPACING_DEGREES * Mth.DEG_TO_RAD + spin));
            pose.scale(0.5F, 0.5F, 1.0F);
            pose.translate(-32.0F, -32.0F, 0.0F);
            // Untinted GuiGraphics.blit skips blend; setShaderColor alone paints opaque rings.
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(r, g, b, 0.5F);
            graphics.blit(TEXTURE, 0, 0, 192, 0, 64, 64, TEX_SIZE, TEX_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    private List<OverlayCrystal> overlayCrystals() {
        List<OverlayCrystal> list = new ArrayList<>();
        AspectList crystals = clientRecipeCrystals();
        if (!crystals.isEmpty()) {
            for (AspectInstance entry : crystals.entries()) {
                list.add(new OverlayCrystal(entry.aspectId()));
            }
            return list;
        }
        for (int i = 0; i < WorkbenchContainer.CRYSTAL_SLOTS; i++) {
            ItemStack stack = menu.inventory().getItem(WorkbenchContainer.CRAFTING_SLOTS + i);
            AspectInstance aspect = TTItemData.getCrystalAspect(stack);
            if (aspect != null) {
                list.add(new OverlayCrystal(aspect.aspectId()));
            }
        }
        return list;
    }

    private AspectList clientRecipeCrystals() {
        Level level = minecraft != null ? minecraft.level : null;
        if (level == null) {
            return AspectList.EMPTY;
        }
        WorkbenchCraftingContainer grid = new WorkbenchCraftingContainer(menu.inventory());
        Optional<ArcaneCraftingRecipe> match = level.getRecipeManager()
                .getRecipeFor(TTRecipeTypes.ARCANE_CRAFTING.get(), grid, level);
        return match.map(ArcaneCraftingRecipe::crystals).orElse(AspectList.EMPTY);
    }

    private static int aspectColor(ResourceLocation id) {
        IAspect aspect = TTAspects.get(id);
        return aspect != null ? aspect.color() : 0xFFFFFF;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        int available = menu.getCachedVis();
        int required = menu.getRequiredVis();
        drawCenteredScaled(graphics, available + " vis", VIS_TEXT_CENTER_X, VIS_AVAILABLE_Y, VIS_AVAILABLE_COLOR);
        if (required > 0) {
            int color = required > available ? VIS_SHORT_COLOR : VIS_REQUIRED_COLOR;
            drawCenteredScaled(graphics, required + " cost", VIS_TEXT_CENTER_X, VIS_REQUIRED_Y, color);
        }
    }

    private void drawCenteredScaled(GuiGraphics graphics, String text, int centerX, int y, int color) {
        PoseStack pose = graphics.pose();
        float width = font.width(text) * VIS_TEXT_SCALE;
        pose.pushPose();
        pose.translate(centerX - width / 2.0F, y, 0.0F);
        pose.scale(VIS_TEXT_SCALE, VIS_TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderWandCostRow(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        renderWorkbenchTooltips(graphics, mouseX, mouseY);
    }

    private void renderWandCostRow(GuiGraphics graphics, int mouseX, int mouseY) {
        AspectList crystals = clientRecipeCrystals();
        if (crystals.isEmpty()) {
            return;
        }
        ItemStack wand = menu.inventory().getItem(WorkbenchContainer.WAND_SLOT);
        if (wand.isEmpty()) {
            return;
        }
        MutableComponent amounts = null;
        for (AspectInstance entry : crystals.entries()) {
            IAspect aspect = TTAspects.get(entry.aspectId());
            int color = aspect != null ? aspect.color() : 0xFFFFFF;
            Component chunk = Component.literal(Integer.toString(entry.amount() * CRYSTAL_SUBSTITUTE_VIS))
                    .withStyle(style -> style.withColor(color));
            if (amounts == null) {
                amounts = Component.empty().append(chunk);
            } else {
                amounts.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY)).append(chunk);
            }
        }
        if (amounts == null) {
            return;
        }
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        int lineCenterX = left + ArcaneWorkbenchMenu.WAND_X + 8;
        int lineY = top + WAND_COST_Y_OFFSET;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(lineCenterX, lineY, 0.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        int amountsWidth = font.width(amounts);
        graphics.drawString(font, amounts, -amountsWidth / 2, 0, 0xFFFFFFFF, false);
        pose.popPose();
        int hoverHalfWidth = Math.max(amountsWidth / 4, WAND_COST_HOVER_MIN_HALF_WIDTH);
        if (overText(mouseX, mouseY, lineCenterX, lineY, hoverHalfWidth)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.thaumaturge.arcane_workbench.wand_pay.tooltip.title").withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.thaumaturge.arcane_workbench.wand_pay.tooltip", CRYSTAL_SUBSTITUTE_VIS).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private void renderWorkbenchTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        AspectList crystals = clientRecipeCrystals();
        for (AspectInstance entry : crystals.entries()) {
            int index = ArcaneWorkbenchMenu.PRIMAL_ORDER.indexOf(entry.aspectId());
            if (index < 0) {
                continue;
            }
            int slotX = left + ArcaneWorkbenchMenu.CRYSTAL_X[index];
            int slotY = top + ArcaneWorkbenchMenu.CRYSTAL_Y[index];
            if (mouseX < slotX || mouseX >= slotX + SLOT_SIZE || mouseY < slotY || mouseY >= slotY + SLOT_SIZE) {
                continue;
            }
            Component hint = Component.translatable("gui.thaumaturge.arcane_workbench.crystal_needed.tooltip", CRYSTAL_SUBSTITUTE_VIS);
            ItemStack crystal = menu.inventory().getItem(WorkbenchContainer.CRAFTING_SLOTS + index);
            if (crystal.isEmpty()) {
                graphics.renderTooltip(font, hint, mouseX, mouseY);
            } else {
                graphics.renderComponentTooltip(font, List.of(crystal.getHoverName(), hint), mouseX, mouseY);
            }
            return;
        }

        int available = menu.getCachedVis();
        int required = menu.getRequiredVis();
        boolean lacking = required > available;
        if (overScaledCenter(mouseX, mouseY, left + VIS_TEXT_CENTER_X, top + VIS_AVAILABLE_Y, Integer.toString(available).length() + 4)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.thaumaturge.arcane_workbench.vis_available.tooltip.title").withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("gui.thaumaturge.arcane_workbench.vis_available.tooltip").withStyle(ChatFormatting.GRAY));
            if (lacking) {
                lines.add(Component.translatable("gui.thaumaturge.arcane_workbench.vis_available.tooltip.short").withStyle(ChatFormatting.RED));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (required > 0 && overScaledCenter(mouseX, mouseY, left + VIS_TEXT_CENTER_X, top + VIS_REQUIRED_Y, Integer.toString(required).length() + 6)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.thaumaturge.arcane_workbench.required_vis.tooltip.title").withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("gui.thaumaturge.arcane_workbench.required_vis.tooltip").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private boolean overScaledCenter(int mouseX, int mouseY, int centerX, int top, int approxChars) {
        int half = Math.max(8, Math.round(approxChars * 3 * VIS_TEXT_SCALE));
        return overText(mouseX, mouseY, centerX, top, half);
    }

    private static boolean overText(int mouseX, int mouseY, int centerX, int top, int halfWidth) {
        return mouseX >= centerX - halfWidth && mouseX < centerX + halfWidth && mouseY >= top && mouseY < top + 8;
    }

    private record OverlayCrystal(ResourceLocation aspectId) {}
}
