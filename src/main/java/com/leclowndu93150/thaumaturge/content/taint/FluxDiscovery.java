package com.leclowndu93150.thaumaturge.content.taint;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Flux exposure and alternate discovery sources for content still being ported. */
@Mod.EventBusSubscriber(modid = TTIds.MODID)
public final class FluxDiscovery {
    private FluxDiscovery() {}

    public static void check(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        int base = AuraHelper.getAuraBase(level, pos);
        if (base <= 0 || AuraHelper.getFlux(level, pos) < base * 0.75F) {
            return;
        }
        ResearchManager.progressResearch(player, TTIds.rl("f_toomuchflux"));
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        var mob = event.getEntity();
        if (mob instanceof TaintSeedEntity) {
            drop(event, new ItemStack(TTBulkItems.ITEMS.get("void_seed").get()));
            if (mob.getRandom().nextInt(5) == 0) drop(event, new ItemStack(TTItems.PRIMORDIAL_PEARL.get()));
        } else if (mob.getType() == EntityType.EVOKER) {
            drop(event, new ItemStack(TTBulkItems.ITEMS.get("curio_rites").get()));
        } else if (mob.getPersistentData().getBoolean("ThaumaturgeNodeGuard")) {
            drop(event, new ItemStack(TTBulkItems.ITEMS.get("brain").get()));
        }
    }

    private static void drop(LivingDropsEvent event, ItemStack stack) {
        var mob = event.getEntity();
        event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), stack));
    }
}
