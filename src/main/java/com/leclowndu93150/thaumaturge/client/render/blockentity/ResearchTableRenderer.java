package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshCache;
import com.leclowndu93150.thaumaturge.client.model.mesh.TTMeshPart;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.BlockResearchTable;
import com.leclowndu93150.thaumaturge.registry.TTItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Forge port of CE ResearchTableRenderer: inkwell + quill when scribing tools are installed. */
public final class ResearchTableRenderer implements BlockEntityRenderer<BlockEntityResearchTable> {
    private static final ResourceLocation INKWELL_MESH = TTIds.rl("models/mesh/research_inkwell.ttmesh");
    private static final ResourceLocation INKWELL_TEXTURE = TTIds.rl("textures/entity/research_inkwell.png");
    private static final ResourceLocation QUILL_TEXTURE = TTIds.rl("textures/entity/tablequill.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final float HALF_TURN = 180.0F;
    private static final float INKWELL_X = -0.28125F;
    private static final float INKWELL_Z = 0.28125F;
    private static final float QUILL_X = -0.226F;
    private static final float QUILL_Y = -0.095F;
    private static final float QUILL_Z = 0.076F;
    private static final float QUILL_YAW = -105.0F;
    private static final float QUILL_SCALE = 0.5F;
    private static final float QUILL_THICKNESS = 0.025F;
    private static final int QUILL_TEXELS = 16;

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlockEntityResearchTable table, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack tools = table.stackInSlot(BlockEntityResearchTable.SLOT_SCRIBE_TOOLS);
        boolean hasTools = tools.is(TTItemTags.SCRIBING_TOOLS);
        BlockState state = table.getBlockState();
        Direction facing = state.hasProperty(BlockResearchTable.FACING) ? state.getValue(BlockResearchTable.FACING) : Direction.NORTH;
        poseStack.pushPose();
        ResearchTableTop.pose(poseStack, facing);
        if (hasTools) {
            submitInkwell(poseStack, buffer, packedLight);
            submitQuill(poseStack, buffer, packedLight);
        }
        ResearchTableTop.submitSheets(poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void submitInkwell(PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(INKWELL_X, 0.0F, INKWELL_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(HALF_TURN));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(INKWELL_TEXTURE));
        for (TTMeshPart part : TTMeshCache.get(INKWELL_MESH).parts()) {
            TTMeshCache.renderPart(part, poseStack.last(), consumer, light, WHITE);
        }
        poseStack.popPose();
    }

    private static void submitQuill(PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(QUILL_X, QUILL_Y, QUILL_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(QUILL_YAW));
        poseStack.mulPose(Axis.XP.rotationDegrees(HALF_TURN));
        poseStack.scale(QUILL_SCALE, QUILL_SCALE, QUILL_SCALE);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(QUILL_TEXTURE));
        quill(poseStack.last(), consumer, light);
        poseStack.popPose();
    }

    private static void quill(PoseStack.Pose pose, VertexConsumer buffer, int light) {
        float back = -QUILL_THICKNESS;
        ResearchTableTop.vertex(pose, buffer, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, 1.0F, back, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, 1.0F, back, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, 0.0F, back, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, 0.0F, back, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, light);
        for (int texel = 0; texel < QUILL_TEXELS; texel++) {
            columnWalls(pose, buffer, texel, back, light);
            rowWalls(pose, buffer, texel, back, light);
        }
    }

    private static void columnWalls(PoseStack.Pose pose, VertexConsumer buffer, int column, float back, int light) {
        float left = (float) column / QUILL_TEXELS;
        float right = (float) (column + 1) / QUILL_TEXELS;
        float u = 1.0F - left - 0.5F / QUILL_TEXELS;
        ResearchTableTop.vertex(pose, buffer, left, 0.0F, back, u, 1.0F, -1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, left, 0.0F, 0.0F, u, 1.0F, -1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, left, 1.0F, 0.0F, u, 0.0F, -1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, left, 1.0F, back, u, 0.0F, -1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, right, 1.0F, back, u, 0.0F, 1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, right, 1.0F, 0.0F, u, 0.0F, 1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, right, 0.0F, 0.0F, u, 1.0F, 1.0F, 0.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, right, 0.0F, back, u, 1.0F, 1.0F, 0.0F, 0.0F, light);
    }

    private static void rowWalls(PoseStack.Pose pose, VertexConsumer buffer, int row, float back, int light) {
        float bottom = (float) row / QUILL_TEXELS;
        float top = (float) (row + 1) / QUILL_TEXELS;
        float v = 1.0F - bottom - 0.5F / QUILL_TEXELS;
        ResearchTableTop.vertex(pose, buffer, 0.0F, top, 0.0F, 1.0F, v, 0.0F, 1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, top, 0.0F, 0.0F, v, 0.0F, 1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, top, back, 0.0F, v, 0.0F, 1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, top, back, 1.0F, v, 0.0F, 1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, bottom, 0.0F, 0.0F, v, 0.0F, -1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, bottom, 0.0F, 1.0F, v, 0.0F, -1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 0.0F, bottom, back, 1.0F, v, 0.0F, -1.0F, 0.0F, light);
        ResearchTableTop.vertex(pose, buffer, 1.0F, bottom, back, 0.0F, v, 0.0F, -1.0F, 0.0F, light);
    }
}
