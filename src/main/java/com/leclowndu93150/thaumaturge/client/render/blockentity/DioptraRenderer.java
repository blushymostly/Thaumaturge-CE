package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityDioptra;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.LinkedHashMap;
import java.util.Map;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class DioptraRenderer implements BlockEntityRenderer<BlockEntityDioptra> {
    private static final ResourceLocation GRID = TTIds.rl("textures/misc/gridblock.png");
    private static final ResourceLocation SIDE = TTIds.rl("textures/entity/dioptra_side.png");
    private static final RenderType GRID_TYPE = TTFlatRenderTypes.entityTranslucentFlatWorld(GRID);
    private static final RenderType WALL_TYPE = TTFlatRenderTypes.entityTranslucentFlatWorld(SIDE);
    private static final MultiBufferSource.BufferSource HOLOGRAMS = MultiBufferSource.immediateWithBuffers(
            new LinkedHashMap<>(Map.of(GRID_TYPE, new BufferBuilder(256), WALL_TYPE, new BufferBuilder(256))),
            new BufferBuilder(256));
    private static final int SIZE = BlockEntityDioptra.GRID_SIZE;
    private static final int CELLS = SIZE - 1;
    private static final float BASE = 1.001F;
    private static final int[][] CORNERS = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final int[][] PERIMETER = {{0, 0, 1, 0}, {1, 0, 1, 1}, {1, 1, 0, 1}, {0, 1, 0, 0}};

    public DioptraRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void drawHolograms(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            // Chest and other opaque block entities have populated depth by this point.
            HOLOGRAMS.endBatch(GRID_TYPE);
            HOLOGRAMS.endBatch(WALL_TYPE);
        }
    }

    @Override
    public void render(BlockEntityDioptra dioptra, float partialTick, PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
        float time = (dioptra.getLevel() == null ? 0 : dioptra.getLevel().getGameTime() % 100000L) + partialTick;
        boolean vis = dioptra.getBlockState().getValue(BlockStateProperties.ENABLED);
        float red = 0.85F + 0.05F * Mth.sin(time / 12);
        float green = (vis ? 0.9F : 0.45F) + 0.05F * Mth.sin(time / 11);
        float blue = 0.95F + 0.05F * Mth.sin(time / 10);
        PoseStack.Pose pose = stack.last();
        VertexConsumer grid = HOLOGRAMS.getBuffer(GRID_TYPE);
        for (int row = 0; row < CELLS; row++) {
            for (int column = 0; column < CELLS; column++) {
                float distance = Mth.sqrt(Mth.square(column - CELLS / 2.0F) + Mth.square(row - CELLS / 2.0F));
                float glow = (200 + 15 * Mth.sin((time - 10 * distance) / 8)) / 255;
                for (int[] corner : CORNERS) {
                    int x = column + corner[0];
                    int z = row + corner[1];
                    float height = height(dioptra, x, z);
                    vertex(grid, pose, position(x), height, position(z), corner[0], 1 - corner[1],
                            0.8F * red * glow, green * glow, blue * glow, 0.9F, overlay);
                }
            }
        }
        for (int[] edge : PERIMETER) {
            for (int i = 0; i < CELLS; i++) {
                int x = edge[0] * CELLS + i * (edge[2] - edge[0]);
                int z = edge[1] * CELLS + i * (edge[3] - edge[1]);
                int nextX = x + edge[2] - edge[0];
                int nextZ = z + edge[3] - edge[1];
                vertex(grid, pose, position(x), height(dioptra, x, z), position(z), 1, 0, red, green, blue, 0.72F, overlay);
                vertex(grid, pose, position(nextX), height(dioptra, nextX, nextZ), position(nextZ), 1, 1, red, green, blue, 0.72F, overlay);
                vertex(grid, pose, position(nextX), BASE, position(nextZ), 0, 1, red, green, blue, 0, overlay);
                vertex(grid, pose, position(x), BASE, position(z), 0, 0, red, green, blue, 0, overlay);
            }
        }
        VertexConsumer walls = HOLOGRAMS.getBuffer(WALL_TYPE);
        for (int[] edge : PERIMETER) {
            vertex(walls, pose, edge[0], 1, edge[1], 0, 0, red, green, blue, 0.8F, overlay);
            vertex(walls, pose, edge[0], 2, edge[1], 1, 0, red, green, blue, 0.8F, overlay);
            vertex(walls, pose, edge[2], 2, edge[3], 1, 1, red, green, blue, 0.8F, overlay);
            vertex(walls, pose, edge[2], 1, edge[3], 0, 1, red, green, blue, 0.8F, overlay);
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static float height(BlockEntityDioptra dioptra, int x, int z) {
        return BASE + dioptra.gridValue(z * SIZE + x) / 96.0F;
    }

    private static float position(int index) {
        return 0.005F + 0.99F * index / CELLS;
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            float red, float green, float blue, float alpha, int overlay) {
        buffer.vertex(pose.pose(), x, y, z).color(red, green, blue, alpha).uv(u, v).overlayCoords(overlay)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
    }
}
