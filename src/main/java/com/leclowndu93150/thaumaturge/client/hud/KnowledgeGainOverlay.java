package com.leclowndu93150.thaumaturge.client.hud;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lightweight Forge stand-in for CE KnowledgeGainOverlay: floating aspect chips after scan grants.
 * Overlay is registered once from {@link com.leclowndu93150.thaumaturge.client.ThaumaturgeClient}.
 */
@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class KnowledgeGainOverlay {
    private static final int BASE_DURATION_TICKS = 60;
    private static final int EXTRA_DURATION_SPREAD = 20;
    private static final int MAX_ICONS_PER_GAIN = 5;
    private static final int CHIP = AspectTagRenderer.TAG_SIZE;
    private static final int STRIDE = CHIP + 4;
    private static final int MARGIN = 24;
    private static final float RISE_PER_TICK = 0.6F;

    private static final List<Tracker> TRACKERS = new ArrayList<>();

    private KnowledgeGainOverlay() {}

    public static void onAspectGain(ResourceLocation aspectId, int amount) {
        IAspect aspect = TTAspects.get(aspectId);
        if (aspect == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int icons = Math.max(1, Math.min(MAX_ICONS_PER_GAIN, Math.max(amount, 1)));
        for (int i = 0; i < icons; i++) {
            int life = BASE_DURATION_TICKS + mc.level.getRandom().nextInt(EXTRA_DURATION_SPREAD);
            float drift = (mc.level.getRandom().nextFloat() - 0.5F) * 24.0F;
            TRACKERS.add(new Tracker(aspect, life, life, drift));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            TRACKERS.clear();
            return;
        }
        Iterator<Tracker> it = TRACKERS.iterator();
        while (it.hasNext()) {
            Tracker tracker = it.next();
            tracker.remaining--;
            if (tracker.remaining <= 0) {
                it.remove();
            }
        }
    }

    public static void render(GuiGraphics graphics, float partialTick, int width, int height) {
        if (TRACKERS.isEmpty()) {
            return;
        }
        int baseX = width - MARGIN - CHIP;
        int baseY = height - MARGIN - CHIP;
        int index = 0;
        for (Tracker tracker : TRACKERS) {
            float life = (tracker.remaining - partialTick) / (float) tracker.life;
            float alpha = Math.min(1.0F, Math.max(0.0F, life * 2.0F));
            float rise = (tracker.life - tracker.remaining + partialTick) * RISE_PER_TICK;
            int x = Math.round(baseX + tracker.driftX - index * (STRIDE * 0.35F));
            int y = Math.round(baseY - rise - index * 2.0F);
            AspectTagRenderer.render(graphics, x, y, tracker.aspect, alpha, false);
            index++;
        }
    }

    private static final class Tracker {
        private final IAspect aspect;
        private final int life;
        private int remaining;
        private final float driftX;

        private Tracker(IAspect aspect, int life, int remaining, float driftX) {
            this.aspect = aspect;
            this.life = life;
            this.remaining = remaining;
            this.driftX = driftX;
        }
    }
}
