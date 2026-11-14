package com.leclowndu93150.thaumaturge.client.render.blockentity;

import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Forge 1.20.1 port of CE CrucibleRenderer — water surface tinted by dissolved aspects. */
public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    private static final float TARGET_RED = 0.25F;
    private static final float TARGET_GREEN = 0.0F;
    private static final float TARGET_BLUE = 0.75F;
    private static final float RAMP_OFFSET = 0.5F;
    private static final float RAMP_RISE = 0.5F;
    private static final float CHANNEL = 255.0F;
    private static final float[][] CORNERS = {{0.0F, 0.0F}, {0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}};

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CrucibleBlockEntity crucible, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FluidStack fluid = crucible.waterTank().getFluid();
        if (fluid.isEmpty() || fluid.getAmount() <= 0) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = spriteOf(extensions, fluid);
        if (sprite == null) {
            return;
        }
        float height = crucible.surfaceLevel();
        int color = tinted(extensions.getTintColor(fluid), crucible.aspects().entries().stream().mapToInt(e -> e.amount()).sum());
        float a = ((color >> 24) & 255) / CHANNEL;
        float r = ((color >> 16) & 255) / CHANNEL;
        float g = ((color >> 8) & 255) / CHANNEL;
        float b = (color & 255) / CHANNEL;
        if (a <= 0.0F) {
            a = 1.0F;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        for (float[] corner : CORNERS) {
            float u = corner[0] <= 0.0F ? u0 : u1;
            float v = corner[1] <= 0.0F ? v0 : v1;
            consumer.vertex(matrix, corner[0], height, corner[1])
                    .color(r, g, b, a)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
        }
    }

    private static TextureAtlasSprite spriteOf(IClientFluidTypeExtensions extensions, FluidStack fluid) {
        var still = extensions.getStillTexture(fluid);
        if (still == null) {
            still = IClientFluidTypeExtensions.of(Fluids.WATER).getStillTexture();
        }
        if (still == null) {
            return null;
        }
        return new Material(InventoryMenu.BLOCK_ATLAS, still).sprite();
    }

    private static int tinted(int base, int aspectTotal) {
        float factor = Math.min(1.0F, RAMP_OFFSET + RAMP_RISE * aspectTotal / (float) CrucibleBlockEntity.MAX_ASPECT);
        factor *= Mth.clamp(aspectTotal, 0, 1);
        float red = Mth.lerp(factor, ((base >> 16) & 255) / CHANNEL, TARGET_RED);
        float green = Mth.lerp(factor, ((base >> 8) & 255) / CHANNEL, TARGET_GREEN);
        float blue = Mth.lerp(factor, (base & 255) / CHANNEL, TARGET_BLUE);
        int alpha = (base >> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }
        return (alpha << 24)
                | (Math.round(red * CHANNEL) << 16)
                | (Math.round(green * CHANNEL) << 8)
                | Math.round(blue * CHANNEL);
    }
}
