package com.leclowndu93150.thaumaturge.client.render.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.WandParts;
import com.leclowndu93150.thaumaturge.client.render.TTFlatRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class WandRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float FOCUS_SCALE = 0.5F;
    private static final float STAFF_Y = 0.2F;
    private static final float STAFF_FOCUS_Y = -0.0475F;
    private static final float STAFF_FOCUS_SCALE_Y = 0.5525F;
    private static final ResourceLocation FOCUS = texture("wand");
    private static final RenderType RUNES = TTFlatRenderTypes.entityAdditiveWorld(TTIds.rl("textures/misc/script.png"));
    private static final Map<WandParts.Cap, ResourceLocation> CAP_TEXTURES = new EnumMap<>(WandParts.Cap.class);
    private static final Map<WandParts.Rod, ResourceLocation> ROD_TEXTURES = new EnumMap<>(WandParts.Rod.class);

    static {
        for (var cap : WandParts.Cap.values())
            CAP_TEXTURES.put(cap, texture("wand_cap_" + cap.id()));
        for (var rod : WandParts.Rod.values())
            ROD_TEXTURES.put(rod, texture("wand_rod_" + rod.material()));
    }

    private final ModelPart rod;
    private final ModelPart cap;
    private final ModelPart focus;

    public WandRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 8).addBox(-1, 1, -1, 2, 18, 2), PartPose.ZERO);
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
        var parts = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        rod = parts.getChild("rod");
        cap = parts.getChild("cap");
        focus = parts.getChild("focus");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        boolean staff = WandParts.rod(stack).staff();
        if (WandTipTracker.isThirdPerson(context)) {
            pose.mulPose(Axis.XP.rotationDegrees(135));
            pose.scale(0.625F, 0.625F, 0.625F);
            pose.translate(0, staff ? -1.4F : -15 / 16F, 0);
            WandTipTracker.capture(stack, context, pose);
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(180));
            if (staff) {
                boolean gui = context == ItemDisplayContext.GUI;
                float scale = gui ? 0.8F : 0.9F;
                pose.translate(0, gui ? -0.5F : -0.9F, 0);
                pose.scale(scale, scale, scale);
            }
        }
        renderParts(stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    static float tipY(ItemStack stack) {
        boolean staff = WandParts.rod(stack).staff();
        if (WandItem.focusColor(stack) != 0) {
            return staff ? STAFF_Y + STAFF_FOCUS_Y - 6 / 16F * STAFF_FOCUS_SCALE_Y : -6 / 16F * FOCUS_SCALE;
        }
        return staff ? STAFF_Y - 1.1F / 16F : -(WandParts.sceptre(stack) ? 1.3F : 1F) / 16F;
    }

    public void renderParts(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var core = WandParts.rod(stack);
        boolean staff = core.staff();
        boolean sceptre = WandParts.sceptre(stack);
        var minecraft = Minecraft.getInstance();
        float ticks = minecraft.level == null ? 0 : minecraft.level.getGameTime() % 72000 + minecraft.getFrameTime();

        pose.pushPose();
        if (staff)
            pose.translate(0, STAFF_Y, 0);
        pose.pushPose();
        if (staff) {
            pose.translate(0, -0.1F, 0);
            pose.scale(1.2F, 2, 1.2F);
        }
        int rodLight = core.glows() ? 205 + (int) (5 * Math.sin(Math.floor(ticks))) : light;
        rod.render(pose, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(ROD_TEXTURES.get(core)), false, stack.hasFoil()), rodLight, overlay);
        pose.popPose();

        pose.pushPose();
        pose.scale(staff ? 1.3F : 1.2F, staff ? 1.1F : 1, staff ? 1.3F : 1.2F);
        var metal = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(CAP_TEXTURES.get(WandParts.cap(stack))), false, stack.hasFoil());
        renderCap(pose, metal, light, overlay, 0, sceptre ? 1.3F : 1, sceptre ? 1.3F : 1);
        if (sceptre)
            renderCap(pose, metal, light, overlay, 0.3F, 1, 0.66F);
        if (staff)
            renderCap(pose, metal, light, overlay, 0.225F, 1, 0.66F);
        renderCap(pose, metal, light, overlay, 20 / 16F + (staff ? 0.875F : 0), 1, 1);
        pose.popPose();

        int tint = WandItem.focusColor(stack);
        if (tint != 0) {
            pose.pushPose();
            if (staff) {
                pose.translate(0, STAFF_FOCUS_Y, 0);
                pose.scale(0.525F, STAFF_FOCUS_SCALE_Y, 0.525F);
            } else {
                pose.scale(FOCUS_SCALE, FOCUS_SCALE, FOCUS_SCALE);
            }
            focus.render(pose, buffers.getBuffer(RenderType.entityTranslucent(FOCUS)), LightTexture.FULL_BRIGHT, overlay, (tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F,
                    0.95F);
            pose.popPose();
        }

        if (sceptre) {
            var runes = buffers.getBuffer(RUNES);
            for (int i = 0; i < 10; i++)
                drawRune(pose, runes, ticks + 36 * i, 0.16F, -0.125F, i, ticks);
        } else if (core == WandParts.Rod.PRIMAL_STAFF) {
            var runes = buffers.getBuffer(RUNES);
            for (int side = 0; side < 4; side++) {
                for (int step = 0; step < 14; step++) {
                    drawRune(pose, runes, 90 * (side + 1), 0.36F + 0.14F * step, -0.08F, (step + side * 3) % 16, ticks);
                }
            }
        }
        pose.popPose();
    }

    private void renderCap(PoseStack pose, VertexConsumer buffer, int light, int overlay, float y, float width, float height) {
        pose.pushPose();
        pose.translate(0, y, 0);
        pose.scale(width, height, width);
        cap.render(pose, buffer, light, overlay);
        pose.popPose();
    }

    private static void drawRune(PoseStack pose, VertexConsumer buffer, float yaw, float y, float depth, int glyph, float ticks) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        float phase = ticks + glyph * 5F;
        float wobble = 0.2F * (float) Math.sin(phase / 10);
        float half = 0.06F + wobble / 40;
        int red = Math.round(255 * (0.88F + 0.1F * (float) Math.sin(phase / 5)));
        int green = Math.round(255 * (0.63F + 0.1F * (float) Math.sin(phase / 7)));
        int alpha = Math.round(255 * (0.6F + wobble));
        float u = glyph / 16F;
        float nextU = (glyph + 1) / 16F;
        var matrix = pose.last();
        for (int corner = 0; corner < 4; corner++) {
            boolean left = corner == 0 || corner == 3;
            boolean lower = corner < 2;
            buffer.vertex(matrix.pose(), 0.01F + (left ? -half : half), y + (lower ? -half : half), depth).color(red, green, 51, alpha).uv(left ? nextU : u, lower ? 1 : 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(200).normal(matrix.normal(), 0, 0, 1).endVertex();
        }
        pose.popPose();
    }

    private static ResourceLocation texture(String name) {
        return TTIds.rl("textures/models/" + name + ".png");
    }
}
