package com.leclowndu93150.thaumaturge.client.render.research;

import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.client.model.data.ModelData;

public final class StructurePreview {
    private StructurePreview() {}

    public static Rect area(int cx, int cy) {
        return new Rect(cx - 48, cy - 62, 96, 100);
    }

    public static int layers(DustStructureRecipe recipe) {
        return bounds(recipe).getYSpan();
    }

    private static BoundingBox bounds(DustStructureRecipe recipe) {
        return BoundingBox.encapsulatingPositions(recipe.cells().stream().map(DustStructureRecipe.Cell::offset).toList()).orElseThrow();
    }

    public static Component layerLabel(int layer, int count) {
        return layer < 0 ? Component.translatable("gui.thaumaturge.thaumonomicon.preview.all_layers")
                : Component.translatable("gui.thaumaturge.thaumonomicon.preview.layers", layer + 1, count);
    }

    public static Rect layerButton(Font font, int cx, int cy, Component label, int step) {
        int half = font.width(label) / 2;
        return new Rect(step < 0 ? cx - half - 16 : cx + half + 6, cy + 48, 10, 10);
    }

    static void drawControls(GuiGraphics graphics, Font font, int cx, int cy, int layer, int count) {
        if (count <= 1) return;
        Component label = layerLabel(layer, count);
        graphics.drawString(font, label, cx - font.width(label) / 2, cy + 49, 0xFF505050, false);
        for (int step : new int[] {-1, 1}) {
            Rect button = layerButton(font, cx, cy, label, step);
            int x = button.x(), y = button.y();
            graphics.fill(x, y, x + 10, y + 10, 0x50775935);
            graphics.hLine(x + 2, x + 7, y + 4, 0xFF503F2E);
            if (step > 0) graphics.vLine(x + 4, y + 1, y + 7, 0xFF503F2E);
        }
    }

    static void render(GuiGraphics graphics, int cx, int cy, DustStructureRecipe recipe, float rotation, float tilt, int layer) {
        var cells = recipe.cells();
        var bounds = bounds(recipe);
        float width = (float) Math.hypot(bounds.getXSpan(), bounds.getZSpan());
        double tiltRadians = Math.toRadians(tilt);
        double height = bounds.getYSpan() * Math.cos(tiltRadians) + width * Math.sin(tiltRadians);
        float scale = (float) Math.min(16, Math.min(94 / width, 98 / height));

        graphics.flush();
        Rect area = area(cx, cy);
        graphics.enableScissor(area.x(), area.y(), area.x() + area.width(), area.y() + area.height());
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy - 12, 180);
        // Reflect Y to cancel the GUI projection's flipped winding and preserve the camera's depth.
        pose.last().pose().scale(scale, -scale, scale);
        pose.last().normal().scale(1, -1, 1);
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-(bounds.minX() + bounds.maxX() + 1) / 2F, -(bounds.minY() + bounds.maxY() + 1) / 2F,
                -(bounds.minZ() + bounds.maxZ() + 1) / 2F);
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        var minecraft = Minecraft.getInstance();
        var renderer = minecraft.getBlockRenderer();
        var visible = new LinkedHashMap<BlockPos, BlockState>();
        for (var cell : cells) {
            if (layer < 0 || cell.offset().getY() - bounds.minY() <= layer) {
                visible.put(cell.offset(), cell.source().defaultBlockState());
            }
        }
        var buffer = graphics.bufferSource().getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        var random = RandomSource.create();
        for (var entry : visible.entrySet()) {
            var pos = entry.getKey();
            var state = entry.getValue();
            if (state.is(Blocks.LAVA)) continue;
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            var model = renderer.getBlockModel(state);
            int tint = minecraft.getBlockColors().getColor(state, null, null, 0);
            for (Direction direction : Direction.values()) {
                var neighbor = pos.relative(direction);
                var neighborState = visible.get(neighbor);
                if (neighborState != null && neighborState.isSolidRender(EmptyBlockGetter.INSTANCE, neighbor)) continue;
                random.setSeed(42L);
                for (var quad : model.getQuads(state, direction, random, ModelData.EMPTY, null)) {
                    drawQuad(pose.last(), buffer, quad, tint);
                }
            }
            random.setSeed(42L);
            for (var quad : model.getQuads(state, null, random, ModelData.EMPTY, null)) {
                drawQuad(pose.last(), buffer, quad, tint);
            }
            pose.popPose();
        }
        for (var entry : visible.entrySet()) {
            if (!entry.getValue().is(Blocks.LAVA)) continue;
            var pos = entry.getKey();
            pose.pushPose();
            pose.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            minecraft.getItemRenderer().renderStatic(new ItemStack(Items.LAVA_BUCKET), ItemDisplayContext.NONE,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, graphics.bufferSource(), minecraft.level, 0);
            pose.popPose();
        }
        graphics.bufferSource().endBatch();
        pose.popPose();
        graphics.disableScissor();
    }

    private static void drawQuad(PoseStack.Pose pose, VertexConsumer buffer, BakedQuad quad, int tint) {
        int color = quad.isTinted() ? tint : 0xFFFFFF;
        buffer.putBulkData(pose, quad, (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
