package com.leclowndu93150.thaumaturge.client.render.research;

import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustStructureRecipe;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class DeviceRenderProbe {
    private static final String CAPTURE = System.getenv("TT_DEVICE_CAPTURE");
    private static boolean done;

    @SubscribeEvent
    public static void open(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (CAPTURE != null && event.phase == TickEvent.Phase.END && !done && mc.getOverlay() == null && mc.screen != null
                && !(mc.screen instanceof Probe)) mc.setScreen(new Probe());
    }

    @SubscribeEvent
    public static void capture(ScreenEvent.Render.Post event) {
        if (CAPTURE == null || done || !(event.getScreen() instanceof Probe)) return;
        done = true;
        var mc = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFFB7B7B7);
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 3;
        for (int i = 0; i < 2; i++) {
            var id = i == 0 ? "jar_brain" : "arcane_workbench_charger";
            int center = x + (i == 0 ? -55 : 55);
            graphics.pose().pushPose();
            graphics.pose().translate(center - 24, y - 28, 0);
            graphics.pose().scale(3, 3, 1);
            graphics.renderItem(new ItemStack(TTBulkContent.BLOCK_ITEMS.get(id).get()), 0, 0);
            graphics.pose().popPose();
            graphics.drawCenteredString(mc.font, Component.translatable("block.thaumaturge." + id), center, y + 24, 0xFFFFFF);
        }
        var fixture = """
                {"result":{"item":"thaumaturge:arcane_workbench_charger"},"cells":[
                    {"source":"thaumaturge:arcane_workbench"},
                    {"y":1,"source":"thaumaturge:arcane_workbench_charger"}
                ]}
                """;
        var preview = new DustStructureRecipe.Serializer().fromJson(TTIds.rl("test/charger_model"),
                JsonParser.parseString(fixture).getAsJsonObject());
        StructurePreview.render(graphics, x, graphics.guiHeight() * 4 / 5, preview, 35, 24, -1);
        graphics.flush();
        Screenshot.grab(mc.gameDirectory, CAPTURE + ".png", mc.getMainRenderTarget(), message -> {
            System.out.println(message.getString());
            mc.execute(mc::stop);
        });
    }

    private static final class Probe extends Screen {
        Probe() { super(Component.literal("Device render check")); }
    }
}
