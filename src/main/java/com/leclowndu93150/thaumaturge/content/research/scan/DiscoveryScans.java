package com.leclowndu93150.thaumaturge.content.research.scan;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DiscoveryScans {
    private static final Map<String, List<String>> TARGETS = Map.ofEntries(
            Map.entry("scanned/orecrystal",
                    List.of("thaumaturge:crystal_aer", "thaumaturge:crystal_ignis", "thaumaturge:crystal_aqua", "thaumaturge:crystal_terra", "thaumaturge:crystal_ordo", "thaumaturge:crystal_perditio",
                            "thaumaturge:crystal_vitium")),
            Map.entry("f_teleport", List.of("nether_portal", "end_portal", "end_portal_frame", "ender_pearl", "enderman")),
            Map.entry("f_spider", List.of("spider", "cave_spider", "thaumaturge:mind_spider")), Map.entry("f_bat", List.of("bat")),
            Map.entry("f_fly", List.of("bat", "parrot", "ghast", "blaze", "thaumaturge:taint_swarm")), Map.entry("scanned/entity/thaumaturge/cultist", List.of("evoker")),
            Map.entry("f_dispenser", List.of("dispenser")), Map.entry("f_matclay", List.of("clay", "clay_ball", "terracotta")),
            Map.entry("f_matiron", List.of("iron_ore", "deepslate_iron_ore", "iron_ingot", "iron_block")), Map.entry("f_matbrass", List.of("thaumaturge:ingot_brass", "thaumaturge:metal_brass")),
            Map.entry("f_matthaumium", List.of("thaumaturge:ingot_thaumium", "thaumaturge:metal_thaumium", "thaumaturge:plate_thaumium")),
            Map.entry("f_matvoid", List.of("thaumaturge:ingot_void", "thaumaturge:metal_void", "thaumaturge:plate_void")),
            Map.entry("f_brain",
                    List.of("thaumaturge:brain", "thaumaturge:brainy_zombie", "thaumaturge:giant_brainy_zombie", "thaumaturge:brainy_drowned", "thaumaturge:brainy_husk", "zombie", "husk", "drowned")),
            Map.entry("f_golem", List.of("iron_golem", "snow_golem", "shulker", "thaumaturge:golem", "thaumaturge:arcane_bore")),
            Map.entry("f_arrow", List.of("arrow", "spectral_arrow", "trident", "thaumaturge:golem_dart")),
            Map.entry("f_fireball", List.of("fireball", "small_fireball", "dragon_fireball", "wither_skull")), Map.entry("f_spit", List.of("llama_spit")),
            Map.entry("f_voidseed", List.of("thaumaturge:void_seed")), Map.entry("primordial_pearl", List.of("thaumaturge:primordial_pearl")),
            Map.entry("scanned/fluxrift", List.of("thaumaturge:flux_rift")), Map.entry("f_toomuchflux", List.of("thaumaturge:flux_rift", "thaumaturge:taint_seed")));

    private DiscoveryScans() {}

    public static boolean record(ServerPlayer player, ResourceLocation target) {
        boolean changed = false;
        for (var discovery : TARGETS.entrySet()) {
            if (discovery.getValue().contains(target.toString()) || target.getNamespace().equals("minecraft") && discovery.getValue().contains(target.getPath())) {
                changed |= ResearchManager.progressResearch(player, TTIds.rl(discovery.getKey()));
            }
        }
        return changed;
    }

    public static void restoreKnownScans(ServerPlayer player) {
        for (ResourceLocation scanned : TTAttachments.knowledge(player).scanned()) {
            record(player, scanned);
        }
    }
}
