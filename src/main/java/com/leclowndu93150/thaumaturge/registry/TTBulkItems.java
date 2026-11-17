package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.content.item.AmuletVisItem;
import com.leclowndu93150.thaumaturge.content.item.CelestialNotesItem;
import com.leclowndu93150.thaumaturge.content.taint.CausalityCollapser;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import com.leclowndu93150.thaumaturge.content.item.SalvagedItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/** Auto-generated placeholders for CE26 item IDs. */
public final class TTBulkItems {
    public static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    static {
        register("curio_preserved");
        register("amber");
        register("amulet_fancy");
        register("amulet_mundane");
        ITEMS.put("amulet_vis", TTItems.ITEMS.register("amulet_vis", () -> new AmuletVisItem(new Item.Properties(), 100)));
        ITEMS.put("amulet_vis_crafted", TTItems.ITEMS.register("amulet_vis_crafted", () -> new AmuletVisItem(new Item.Properties(), 20)));
        register("arcane_armor_trim_smithing_template");
        register("arcane_key_gold");
        register("arcane_key_iron");
        register("bath_salts");
        register("bottle_taint");
        register("brain");
        register("rare_earth");
        ITEMS.put("causality_collapser", TTItems.ITEMS.register("causality_collapser", () -> new CausalityCollapser.ChargeItem(new Item.Properties())));
        register("chunk_beef");
        register("chunk_chicken");
        register("chunk_pork");
        register("chunk_mutton");
        register("chunk_rabbit");
        register("chunk_fish");
        register("charm_undying");
        register("cloth_boots");
        register("cloth_chest");
        register("cloth_legs");
        register("cloud_ring");
        register("creative_flux_sponge");
        register("crimson_blade");
        register("crimson_boots");
        register("crimson_captain_chest");
        register("crimson_captain_helm");
        register("crimson_plate_chest");
        register("crimson_plate_helm");
        register("crimson_plate_legs");
        register("crimson_praetor_chest");
        register("crimson_praetor_helm");
        register("crimson_praetor_legs");
        register("crimson_ranger_chest");
        register("crimson_ranger_helm");
        register("crimson_ranger_legs");
        register("crimson_robe_chest");
        register("crimson_robe_helm");
        register("crimson_robe_legs");
        register("curiosity_band");
        register("curio_rites");
        register("eldritch_eye");
        register("elemental_axe");
        register("elemental_hoe");
        register("elemental_pickaxe");
        register("elemental_shovel");
        register("elemental_spear");
        register("elemental_sword");
        ITEMS.put("flux_goo_bucket", TTItems.ITEMS.register("flux_goo_bucket", () -> new BucketItem(TTFluids.GOO_SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))));
        registerFocus("focus_1");
        registerFocus("focus_2");
        registerFocus("focus_3");
        register("focus_pouch");
        register("fortress_chest");
        register("fortress_helm");
        register("fortress_legs");
        register("girdle_fancy");
        register("girdle_mundane");
        register("golem_bell");
        register("grapple_gun");
        register("hand_mirror");
        register("jar_brace");
        register("label");
        register("liquid_death_bucket");
        register("loot_bag_common");
        register("loot_bag_rare");
        register("loot_bag_uncommon");
        register("mana_bean");
        register("mirror_shield");
        register("occult_armor_trim_smithing_template");
        register("pech_wand");
        register("primal_charm");
        register("primal_crusher");
        register("purifying_bucket");
        register("resonator");
        register("ring_apprentice");
        register("ring_fancy");
        register("ring_mundane");
        register("runed_tablet");
        register("sanity_checker");
        register("sanity_soap");
        register("staff_rod_blaze");
        register("staff_rod_bone");
        register("staff_rod_greatwood");
        register("staff_rod_ice");
        register("staff_rod_obsidian");
        register("staff_rod_primal");
        register("staff_rod_quartz");
        register("staff_rod_reed");
        register("staff_rod_silverwood");
        register("tainted_egg");
        register("thaumium_axe");
        register("thaumium_boots");
        register("thaumium_chest");
        register("thaumium_helm");
        register("thaumium_hoe");
        register("thaumium_legs");
        register("thaumium_pickaxe");
        // thaumium_shield registered in TTItems (Wave 4)
        register("thaumium_shovel");
        register("thaumium_spear");
        register("thaumium_sword");
        register("thaumonomicon_cheat");
        register("thaumonomicon_linking");
        register("thaumonomicon_sharing");
        register("thaumostatic_harness");
        register("traveller_boots");
        ITEMS.put("triple_meat_treat", TTItems.ITEMS.register("triple_meat_treat", () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6)
                .saturationMod(0.8F).alwaysEat().effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100), 0.66F).build()))));
        register("turret_advanced");
        register("turret_basic");
        register("verdant_charm");
        register("void_axe");
        register("void_boots");
        register("void_chest");
        register("void_helm");
        register("void_hoe");
        register("void_legs");
        register("void_pickaxe");
        register("void_robe_chest");
        register("void_robe_helm");
        register("void_robe_legs");
        register("void_seed");
        register("void_shield");
        register("void_shovel");
        register("void_spear");
        register("void_sword");
        register("voidseer_charm");
        // wand registered as WandItem in TTItems (Wave 2)
        register("wand_cap_copper");
        register("wand_cap_gold");
        // wand_cap_iron registered in TTItems (Wave 2)
        register("wand_cap_silver");
        register("wand_cap_silver_inert");
        register("wand_cap_thaumium_inert");
        register("wand_cap_void_inert");
        register("nugget_copper");
        // Keep the legacy silver items readable in existing Forge saves.
        register("ingot_silver");
        register("nugget_silver");
        register("nugget_void");
        register("wand_cap_thaumium");
        register("wand_cap_void");
        register("wand_rod_blaze");
        register("wand_rod_bone");
        register("wand_rod_greatwood");
        register("wand_rod_ice");
        register("wand_rod_obsidian");
        register("wand_rod_quartz");
        register("wand_rod_reed");
        register("wand_rod_silverwood");
        registerCelestial("moon_1", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_2", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_3", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_4", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_5", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_6", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_7", CelestialNotesItem.Kind.MOON);
        registerCelestial("moon_8", CelestialNotesItem.Kind.MOON);
        register("celestial_notes_sheet");
        registerCelestial("stars_1", CelestialNotesItem.Kind.STARS);
        registerCelestial("stars_2", CelestialNotesItem.Kind.STARS);
        registerCelestial("stars_3", CelestialNotesItem.Kind.STARS);
        registerCelestial("stars_4", CelestialNotesItem.Kind.STARS);
        registerCelestial("sun", CelestialNotesItem.Kind.SUN);

    }
    private static void register(String name) {
        ITEMS.put(name, TTItems.ITEMS.register(name, () -> new SalvagedItem(new Item.Properties())));
    }
    private static void registerFocus(String name) {
        ITEMS.put(name, TTItems.ITEMS.register(name, () -> new com.leclowndu93150.thaumaturge.content.item.StarterFocusItem(new Item.Properties().stacksTo(1))));
    }

    private static void registerCelestial(String variant, CelestialNotesItem.Kind kind) {
        String name = "celestial_notes_" + variant;
        ITEMS.put(name, TTItems.ITEMS.register(name, () -> new CelestialNotesItem(new Item.Properties(), kind)));
    }

    public static void bootstrap() {}
    private TTBulkItems() {}
}
