package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.node.CreativeNodePlacerItem;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarItem;
import com.leclowndu93150.thaumaturge.content.item.SalisMundusItem;
import com.leclowndu93150.thaumaturge.content.item.SalvagedItem;
import com.leclowndu93150.thaumaturge.content.item.ThaumometerItem;
import com.leclowndu93150.thaumaturge.content.item.ThaumonomiconItem;
import com.leclowndu93150.thaumaturge.content.item.VisCrystalItem;
import com.leclowndu93150.thaumaturge.content.item.VisResonatorItem;
import com.leclowndu93150.thaumaturge.content.item.WandItem;
import com.leclowndu93150.thaumaturge.content.item.equipment.GogglesItem;
import com.leclowndu93150.thaumaturge.content.misc.nitor.BlockNitor;
import com.leclowndu93150.thaumaturge.content.research.note.ItemResearchNote;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import com.leclowndu93150.thaumaturge.content.item.equipment.ThaumiumShieldItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TTIds.MODID);

    public static final RegistryObject<Item> THAUMONOMICON = register("thaumonomicon", ThaumonomiconItem::new);
    public static final RegistryObject<Item> THAUMOMETER = register("thaumometer", ThaumometerItem::new);
    public static final RegistryObject<Item> SALIS_MUNDUS = register("salis_mundus", () -> new SalisMundusItem(new Item.Properties()));
    public static final RegistryObject<Item> ESSENTIA_CRYSTAL = register("essentia_crystal", VisCrystalItem::new);
    public static final RegistryObject<Item> PHIAL = register("phial",
            () -> new com.leclowndu93150.thaumaturge.content.item.PhialItem(new Item.Properties()));
    public static final RegistryObject<Item> ALUMENTUM = register("alumentum", () -> new Item(new Item.Properties()) {
        @Override
        public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
            return 4800;
        }
    });
    public static final RegistryObject<Item> FILTER = register("filter", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> MORPHIC_RESONATOR = register("morphic_resonator", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> QUICKSILVER = register("quicksilver", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> RAW_CINNABAR = register("raw_cinnabar", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INGOT_THAUMIUM = register("ingot_thaumium", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> INGOT_VOID = register("ingot_void", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> INGOT_BRASS = register("ingot_brass", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_BRASS = register("plate_brass", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_VOID = register("plate_void", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_THAUMIUM = register("plate_thaumium", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> NUGGET_BRASS = register("nugget_brass", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> NUGGET_THAUMIUM = register("nugget_thaumium", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> NUGGET_QUARTZ = register("nugget_quartz", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> NUGGET_QUICKSILVER = register("nugget_quicksilver", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> TALLOW = register("tallow", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> WAND = register("wand", () -> new WandItem(new Item.Properties()));
    public static final RegistryObject<Item> WAND_CAP_IRON = register("wand_cap_iron", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> VIS_RESONATOR = register("vis_resonator", () -> new VisResonatorItem(new Item.Properties()));
    public static final RegistryObject<Item> GOGGLES_REVEALING = register("goggles_revealing",
            () -> new GogglesItem(new Item.Properties().stacksTo(1).durability(100).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MECHANISM_SIMPLE = register("mechanism_simple", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> MECHANISM_COMPLEX = register("mechanism_complex", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> MIRRORED_GLASS = register("mirrored_glass", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_IRON = register("plate_iron", () -> new SalvagedItem(new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_SHIELD = register("thaumium_shield",
            () -> new ThaumiumShieldItem(new Item.Properties().durability(336).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CREATIVE_NODE_PLACER = register("creative_node_placer",
            () -> new CreativeNodePlacerItem(new Item.Properties()));
    public static final RegistryObject<Item> PRIMORDIAL_PEARL = register("primordial_pearl", () -> new SalvagedItem(new Item.Properties().stacksTo(16)));

    public static final int SCRIBING_TOOLS_DURABILITY = 100;
    public static final RegistryObject<Item> SCRIBING_TOOLS = register("scribing_tools", () -> new Item(new Item.Properties().stacksTo(1).durability(SCRIBING_TOOLS_DURABILITY)));
    public static final RegistryObject<Item> RESEARCH_NOTE = register("research_note", () -> new ItemResearchNote(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LOG_GREATWOOD = blockItem("log_greatwood", TTBlocks.LOG_GREATWOOD);
    public static final RegistryObject<Item> LOG_SILVERWOOD = blockItem("log_silverwood", TTBlocks.LOG_SILVERWOOD);
    public static final RegistryObject<Item> SILVERWOOD_NODE_LOG = blockItem("silverwood_node_log", TTBlocks.SILVERWOOD_NODE_LOG);
    public static final RegistryObject<Item> PLANK_GREATWOOD = blockItem("plank_greatwood", TTBlocks.PLANK_GREATWOOD);
    public static final RegistryObject<Item> PLANK_SILVERWOOD = blockItem("plank_silverwood", TTBlocks.PLANK_SILVERWOOD);
    public static final RegistryObject<Item> ARCANE_STONE = blockItem("arcane_stone", TTBlocks.ARCANE_STONE);
    public static final RegistryObject<Item> ARCANE_STONE_BRICK = blockItem("arcane_stone_brick", TTBlocks.ARCANE_STONE_BRICK);
    public static final RegistryObject<Item> CRUCIBLE = blockItem("crucible", TTBlocks.CRUCIBLE);
    public static final RegistryObject<Item> ARCANE_WORKBENCH = blockItem("arcane_workbench", TTBlocks.ARCANE_WORKBENCH);
    public static final RegistryObject<Item> RESEARCH_TABLE = blockItem("research_table", TTBlocks.RESEARCH_TABLE);
    public static final RegistryObject<Item> INFUSION_MATRIX = blockItem("infusion_matrix", TTBlocks.INFUSION_MATRIX);
    public static final RegistryObject<Item> JAR_NORMAL = ITEMS.register("jar_normal",
            () -> new EssentiaJarItem(TTBlocks.JAR_NORMAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> TUBE = blockItem("tube", TTBlocks.TUBE);
    public static final RegistryObject<Item> SMELTER_BASIC = blockItem("smelter_basic", TTBlocks.SMELTER_BASIC);
    public static final RegistryObject<Item> ALEMBIC = blockItem("alembic", TTBlocks.ALEMBIC);
    public static final RegistryObject<Item> ORE_CINNABAR = blockItem("ore_cinnabar", TTBlocks.ORE_CINNABAR);
    public static final RegistryObject<Item> DEEPSLATE_ORE_CINNABAR = blockItem("deepslate_ore_cinnabar", TTBlocks.DEEPSLATE_ORE_CINNABAR);
    public static final RegistryObject<Item> TABLE = blockItem("table_wood", TTBlocks.TABLE);
    public static final RegistryObject<Item> NODE = blockItem("node", TTBlocks.NODE);
    public static final RegistryObject<Item> DIOPTRA = blockItem("dioptra", TTBlocks.DIOPTRA);
    public static final RegistryObject<Item> RECHARGE_PEDESTAL = blockItem("recharge_pedestal", TTBlocks.RECHARGE_PEDESTAL);
    public static final RegistryObject<Item> DECONSTRUCTION_TABLE = ITEMS.register("deconstruction_table",
            () -> new com.leclowndu93150.thaumaturge.content.research.decon.DeconstructionTableItem(TTBlocks.DECONSTRUCTION_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> HUNGRY_CHEST = ITEMS.register("hungry_chest",
            () -> new com.leclowndu93150.thaumaturge.content.device.HungryChestItem(TTBlocks.HUNGRY_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<Item> LAMP_ARCANE = blockItem("lamp_arcane", TTBlocks.LAMP_ARCANE);
    public static final RegistryObject<Item> ITEM_GRATE = blockItem("item_grate", TTBlocks.ITEM_GRATE);
    public static final RegistryObject<Item> REDSTONE_RELAY = blockItem("redstone_relay", TTBlocks.REDSTONE_RELAY);
    public static final RegistryObject<Item> LEVITATOR = blockItem("levitator", TTBlocks.LEVITATOR);
    public static final RegistryObject<Item> ARCANE_EAR = blockItem("arcane_ear", TTBlocks.ARCANE_EAR);
    public static final RegistryObject<Item> ARCANE_EAR_TOGGLE = blockItem("arcane_ear_toggle", TTBlocks.ARCANE_EAR_TOGGLE);

    public static final Map<DyeColor, RegistryObject<Item>> NITORS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            RegistryObject<BlockNitor> block = TTBlocks.NITORS.get(dye);
            NITORS.put(dye, ITEMS.register("nitor_" + dye.getName(), () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final RegistryObject<Item> CRYSTAL_AER = blockItem("crystal_aer", TTBlocks.CRYSTAL_AER);
    public static final RegistryObject<Item> CRYSTAL_IGNIS = blockItem("crystal_ignis", TTBlocks.CRYSTAL_IGNIS);
    public static final RegistryObject<Item> CRYSTAL_AQUA = blockItem("crystal_aqua", TTBlocks.CRYSTAL_AQUA);
    public static final RegistryObject<Item> CRYSTAL_TERRA = blockItem("crystal_terra", TTBlocks.CRYSTAL_TERRA);
    public static final RegistryObject<Item> CRYSTAL_ORDO = blockItem("crystal_ordo", TTBlocks.CRYSTAL_ORDO);
    public static final RegistryObject<Item> CRYSTAL_PERDITIO = blockItem("crystal_perditio", TTBlocks.CRYSTAL_PERDITIO);
    public static final RegistryObject<Item> CRYSTAL_VITIUM = blockItem("crystal_vitium", TTBlocks.CRYSTAL_VITIUM);

    public static final RegistryObject<Item> CLUSTER_IRON = register("cluster_iron", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_GOLD = register("cluster_gold", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_COPPER = register("cluster_copper", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_CINNABAR = register("cluster_cinnabar", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_QUARTZ = register("cluster_quartz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_TIN = register("cluster_tin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_SILVER = register("cluster_silver", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLUSTER_LEAD = register("cluster_lead", () -> new Item(new Item.Properties()));

    private TTItems() {}

    private static RegistryObject<Item> register(String name, Supplier<Item> item) {
        return ITEMS.register(name, item);
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
