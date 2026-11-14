package com.leclowndu93150.thaumaturge.client.screen.research;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.content.aspect.AspectNames;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.decon.MenuDeconstructionTable;
import com.leclowndu93150.thaumaturge.network.TTNetwork;
import com.leclowndu93150.thaumaturge.network.packet.ServerboundDeconCollectPacket;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public final class DeconstructionTableScreen extends AbstractContainerScreen<MenuDeconstructionTable> {
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_decontable.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;
    private static final int BAR_X = 93;
    private static final int BAR_Y = 15;
    private static final int BAR_U = 176;
    private static final int BAR_W = 9;
    private static final int BAR_MAX_H = 46;
    private static final int RESULT_X = 63;
    private static final int RESULT_Y = 47;
    private static final int RESULT_SIZE = 16;

    public DeconstructionTableScreen(MenuDeconstructionTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int fill = BAR_MAX_H - menu.breakTime() * BAR_MAX_H / BlockEntityDeconstructionTable.BREAK_TIME_TICKS;
        fill = Math.max(0, Math.min(BAR_MAX_H, fill));
        if (fill > 0) {
            graphics.blit(TEXTURE, leftPos + BAR_X, topPos + BAR_Y + BAR_MAX_H - fill, BAR_U, BAR_MAX_H - fill, BAR_W, fill);
        }
        IAspect result = resultAspect();
        if (result != null) {
            AspectTagRenderer.render(graphics, font, leftPos + RESULT_X, topPos + RESULT_Y, result, 1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        ResourceLocation id = menu.blockEntity() == null ? null : menu.blockEntity().resultAspect();
        if (id != null && mouseX >= leftPos + RESULT_X && mouseX < leftPos + RESULT_X + RESULT_SIZE && mouseY >= topPos + RESULT_Y
                && mouseY < topPos + RESULT_Y + RESULT_SIZE) {
            graphics.renderTooltip(font, List.of(AspectNames.name(id), Component.translatable("gui.thaumaturge.deconstruction_table.collect")), Optional.empty(), mouseX, mouseY);
        }
    }

    private @Nullable IAspect resultAspect() {
        BlockEntityDeconstructionTable table = menu.blockEntity();
        if (table == null) {
            return null;
        }
        ResourceLocation id = table.resultAspect();
        return id == null ? null : TTAspects.get(id);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            BlockEntityDeconstructionTable table = menu.blockEntity();
            if (table != null && table.resultAspect() != null && mouseX >= leftPos + RESULT_X && mouseX < leftPos + RESULT_X + RESULT_SIZE && mouseY >= topPos + RESULT_Y
                    && mouseY < topPos + RESULT_Y + RESULT_SIZE) {
                TTNetwork.sendToServer(new ServerboundDeconCollectPacket(menu.pos()));
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(TTSounds.HHON.get(), 0.3F, 1.0F);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
