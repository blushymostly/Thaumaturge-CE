package com.leclowndu93150.thaumaturge.client.render.research;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import java.util.List;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class PreviewDepthProbe {
    // Set TT_PREVIEW_DEPTH_TEST=1 before runClient to sample the actual rendered depth.
    private static final boolean ENABLED = "1".equals(System.getenv("TT_PREVIEW_DEPTH_TEST"));
    private static boolean done;

    @SubscribeEvent
    public static void open(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (ENABLED && event.phase == TickEvent.Phase.END && !done && mc.getOverlay() == null && mc.screen != null
                && !(mc.screen instanceof ProbeScreen)) mc.setScreen(new ProbeScreen());
    }

    @SubscribeEvent
    public static void preview(ScreenEvent.Render.Post event) throws Exception {
        if (!ENABLED || done || !(event.getScreen() instanceof ProbeScreen)) return;
        done = true;
        var mc = Minecraft.getInstance();
        checkPillarLighting(mc);
        var graphics = event.getGuiGraphics();
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int cy = h / 3;
        int[] centers = {w / 4, w / 2, 3 * w / 4};
        graphics.fill(0, 0, w, h, 0xFFECE6D6);
        StructurePreview.render(graphics, centers[0], cy, colored(false), 0, 16, -1);
        StructurePreview.render(graphics, centers[1], cy, colored(true), 0, 16, -1);
        StructurePreview.render(graphics, centers[2], cy, colored(false), 180, 16, -1);
        graphics.drawCenteredString(mc.font, "Front first", centers[0], cy + 18, 0x333333);
        graphics.drawCenteredString(mc.font, "Rear first", centers[1], cy + 18, 0x333333);
        graphics.drawCenteredString(mc.font, "Rotated 180 degrees", centers[2], cy + 18, 0x333333);
        var id = TTIds.rl("dust_trigger/infusion_altar");
        var path = "/data/thaumaturge/recipes/" + id.getPath() + ".json";
        try (var reader = new InputStreamReader(PreviewDepthProbe.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            var altar = new DustStructureRecipe.Serializer().fromJson(id, JsonParser.parseReader(reader).getAsJsonObject());
            StructurePreview.render(graphics, w / 2, 3 * h / 4, altar, 22.5F, 32, -1);
        }
        graphics.flush();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for (int i = 0; i < centers.length; i++) {
                int x = centers[i] * image.getWidth() / w;
                int y = (cy - 12) * image.getHeight() / h;
                int color = image.getPixelRGBA(x, y);
                int red = color & 255;
                int blue = color >> 16 & 255;
                System.out.printf("Preview overlap %d: RGB #%02x%02x%02x%n", i, red, color >> 8 & 255, blue);
                if (i < 2 ? red <= blue : blue <= red) {
                    throw new IllegalStateException("Rear block occluded the front block at preview " + i);
                }
            }
        }
        System.out.println("Preview depth passed with reversed recipe order and reversed camera");
        Screenshot.grab(mc.gameDirectory, "ce-preview-depth.png", mc.getMainRenderTarget(), message -> {
            System.out.println(message.getString());
            mc.execute(mc::stop);
        });
    }

    private static void checkPillarLighting(Minecraft mc) {
        var random = RandomSource.create(42);
        for (String id : List.of("pillar_arcane", "pillar_ancient", "pillar_eldritch")) {
            var block = TTBulkContent.BLOCKS.get(id).get();
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var model = mc.getBlockRenderer().getBlockModel(state);
                checkUnlitVertices(id, model.getQuads(state, null, random));
            }
            var itemModel = mc.getItemRenderer().getModel(new ItemStack(block), null, null, 0);
            checkUnlitVertices(id + " item", itemModel.getQuads(null, null, random));
        }
        System.out.println("Pillar lighting passed for all three materials, block orientations and item models");
    }

    private static void checkUnlitVertices(String model, List<BakedQuad> quads) {
        var format = DefaultVertexFormat.BLOCK;
        int lightOffset = format.getOffset(format.getElements().indexOf(DefaultVertexFormat.ELEMENT_UV2)) / Integer.BYTES;
        if (quads.isEmpty()) throw new IllegalStateException("No baked quads for " + model);
        for (BakedQuad quad : quads) {
            if (!quad.isShade()) throw new IllegalStateException("No shading for " + model);
            for (int index = lightOffset; index < quad.getVertices().length; index += format.getIntegerSize()) {
                if (quad.getVertices()[index] != 0) throw new IllegalStateException("Baked light in " + model);
            }
        }
    }

    private static DustStructureRecipe colored(boolean reverse) {
        String front = "{\"source\":\"minecraft:red_concrete\"}";
        String rear = "{\"z\":-2,\"source\":\"minecraft:blue_concrete\"}";
        String cells = reverse ? rear + "," + front : front + "," + rear;
        String json = "{\"result\":{\"item\":\"minecraft:red_concrete\"},\"cells\":[" + cells + "]}";
        return new DustStructureRecipe.Serializer().fromJson(TTIds.rl("test/depth"), JsonParser.parseString(json).getAsJsonObject());
    }

    private static final class ProbeScreen extends Screen {
        ProbeScreen() { super(Component.literal("Preview depth test")); }
    }
}
