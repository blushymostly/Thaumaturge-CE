package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.content.taint.BlockEtherealBloom;
import com.leclowndu93150.thaumaturge.content.taint.flux.BlockFluxGas;
import com.leclowndu93150.thaumaturge.content.taint.flux.BlockFluxGoo;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionPillarBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.leclowndu93150.thaumaturge.content.world.tree.BlockSaplingTT;
import com.leclowndu93150.thaumaturge.content.world.tree.TTTreeGrowers;
import com.leclowndu93150.thaumaturge.content.decor.MagicCandleBlock;
import com.leclowndu93150.thaumaturge.content.device.BlockEffectGlimmer;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.item.DoubleHighBlockItem;
import java.util.function.BiFunction;
import net.minecraftforge.registries.RegistryObject;

/** Auto-generated placeholders for CE26 block IDs not yet given bespoke classes. */
public final class TTBulkContent {
    public static final Map<String, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> BLOCK_ITEMS = new LinkedHashMap<>();

    static {
        registerBlock("activator_rail");
        registerBlock("advanced_alchemical_construct");
        registerBlock("advanced_alchemical_furnace", com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.AdvancedFurnaceBlock::new);
        registerBlock("advanced_alchemical_furnace_advanced_construct_placeholder", () -> new com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnacePartBlock(false));
        registerBlock("advanced_alchemical_furnace_alembic_placeholder", () -> new com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnacePartBlock(false));
        registerBlock("advanced_alchemical_furnace_construct_placeholder", () -> new com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnacePartBlock(false));
        registerBlock("advanced_alchemical_furnace_nozzle", () -> new com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnacePartBlock(true));
        registerBlock("alchemical_construct");
        registerBlock("amber_block");
        registerBlock("amber_brick");
        registerBlock("arcane_bore");
        registerDoor("arcane_door", Blocks.IRON_DOOR, BlockSetType.IRON);
        // arcane_ear / arcane_ear_toggle registered in TTBlocks (Wave 5)
        registerBlock("arcane_grindstone");
        registerBlock("arcane_pressure_plate");
        registerBlock("arcane_workbench_charger", com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchChargerBlock::new);
        registerBlock("banner_crimson_cult");
        registerBlock("barrier");
        registerBlock("bellows", () -> new com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockBellows(
                BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1F, 20F).noOcclusion()));
        registerBlock("brain_box");
        registerBlock("button_greatwood");
        registerBlock("button_silverwood");
        registerBlock("centrifuge");
        registerBlock("cinderpearl", () -> new BushBlock(BlockBehaviour.Properties.copy(Blocks.FERN).lightLevel(state -> 8)) {
            @Override
            protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
                return soil.is(BlockTags.SAND) || soil.is(BlockTags.DIRT) || soil.is(BlockTags.TERRACOTTA);
            }
        });
        registerBlock("condenser");
        registerBlock("condenser_lattice");
        registerBlock("condenser_lattice_dirty");
        // deconstruction_table registered in TTBlocks (Wave 3.0)
        registerBlock("deepslate_ore_amber", () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(3).requiresCorrectToolForDrops(), net.minecraft.util.valueproviders.UniformInt.of(2, 5)));
        registerBlock("deepslate_ore_quartz", () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(3).requiresCorrectToolForDrops(), net.minecraft.util.valueproviders.UniformInt.of(2, 5)));
        // dioptra registered in TTBlocks (Wave 2b)
        registerDoor("door_greatwood", Blocks.DARK_OAK_DOOR, BlockSetType.DARK_OAK);
        registerDoor("door_silverwood", Blocks.BIRCH_DOOR, BlockSetType.BIRCH);
        registerBlock("effect_glimmer", BlockEffectGlimmer::new);
        registerBlock("effect_sap");
        registerBlock("effect_shock");
        registerBlock("eldritch_altar");
        registerBlock("eldritch_capstone");
        registerBlock("eldritch_crab_spawner");
        registerBlock("eldritch_crust");
        registerBlock("eldritch_crust_glowing");
        registerBlock("eldritch_door");
        registerBlock("eldritch_lock");
        registerBlock("eldritch_nothing");
        registerBlock("eldritch_nothing_dormant");
        registerBlock("eldritch_obelisk");
        registerBlock("eldritch_pedestal");
        registerBlock("eldritch_pillar");
        registerBlock("eldritch_portal");
        registerBlock("eldritch_reliquary");
        registerBlock("eldritch_rock");
        registerBlock("eldritch_stone");
        registerBlock("eldritch_stone_crystal");
        registerBlock("eldritch_stone_inert");
        registerBlock("eldritch_trap");
        registerBlock("essentia_crystalizer");
        registerBlock("essentia_input");
        registerBlock("essentia_output");
        registerBlock("essentia_reservoir");
        registerBlock("ethereal_bloom", () -> new BlockEtherealBloom(BlockBehaviour.Properties.copy(Blocks.POPPY).lightLevel(state -> 8)));
        registerBlock("everfull_urn", com.leclowndu93150.thaumaturge.content.device.EverfullUrnBlock::new);
        registerBlock("fence_gate_greatwood");
        registerBlock("fence_gate_silverwood");
        registerBlock("fence_greatwood");
        registerBlock("fence_silverwood");
        registerBlock("flesh_block");
        registerBlock("flux_gas", () -> new BlockFluxGas(BlockBehaviour.Properties.of().replaceable().noCollission().noOcclusion().noLootTable()));
        registerBlock("flux_goo", () -> new BlockFluxGoo(BlockBehaviour.Properties.copy(Blocks.WATER).randomTicks()));
        registerBlock("flux_scrubber");
        registerBlock("focal_manipulator", com.leclowndu93150.thaumaturge.content.item.StarterFocus::new);
        registerBlock("golem_builder");
        registerBlock("golem_fetter");
        registerBlock("grass_ambient", () -> new net.minecraft.world.level.block.GrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
        registerBlock("greatwood");
        registerBlock("hole");
        // hungry_chest registered in TTBlocks (Wave 4)

        registerBlock("infernal_furnace", com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalFurnaceBlock::new);
        registerBlock("inlay", com.leclowndu93150.thaumaturge.content.infusion.InfusionInlay::new);
        // item_grate registered in TTBlocks (Wave 4)

        registerBlock("jar_brain", com.leclowndu93150.thaumaturge.content.essentia.jar.BrainJarBlock::new, com.leclowndu93150.thaumaturge.content.essentia.jar.BrainJarItem::new);
        registerBlock("jar_creative");
        registerBlock("jar_node");
        registerBlock("jar_void");
        // lamp_arcane registered in TTBlocks (Wave 4)

        registerBlock("lamp_fertility");
        registerBlock("lamp_growth");
        registerBlock("leaves_greatwood", () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
        registerBlock("leaves_silverwood", () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
        // levitator registered in TTBlocks (Wave 5)
        registerBlock("liquid_death");
        registerBlock("mana_pod");
        registerBlock("matrix_cost");
        registerBlock("matrix_speed");
        registerBlock("metal_brass");
        registerBlock("metal_thaumium");
        registerBlock("metal_void");
        registerBlock("mirror");
        registerBlock("mirror_essentia");
        registerBlock("node_stabilizer", () -> new com.leclowndu93150.thaumaturge.content.aura.node.NodeStabilizer(1));
        registerBlock("node_stabilizer_advanced", () -> new com.leclowndu93150.thaumaturge.content.aura.node.NodeStabilizer(2));
        registerBlock("node_transducer", com.leclowndu93150.thaumaturge.content.aura.node.NodeTransducer::new);
        registerBlock("obsidian_tile", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
        registerBlock("obsidian_totem", () -> new com.leclowndu93150.thaumaturge.content.decor.BlockObsidianTotem(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
        registerBlock("obsidian_totem_charged", () -> new com.leclowndu93150.thaumaturge.content.decor.BlockObsidianTotemCharged(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
        registerBlock("ore_amber", () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3).requiresCorrectToolForDrops(), net.minecraft.util.valueproviders.UniformInt.of(2, 5)));
        registerBlock("ore_quartz", () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3).requiresCorrectToolForDrops(), net.minecraft.util.valueproviders.UniformInt.of(2, 5)));
        registerBlock("pattern_crafter");
        registerBlock("paving_stone_barrier");
        registerBlock("paving_stone_travel");
        registerBlock("pedestal_ancient", com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestalBlock::new);
        registerBlock("pedestal_arcane", com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestalBlock::new);
        registerBlock("pedestal_eldritch", com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestalBlock::new);
        registerBlock("pillar_ancient", () -> new InfusionPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));
        registerBlock("pillar_arcane", () -> new InfusionPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));
        registerBlock("pillar_eldritch", () -> new InfusionPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));
        registerBlock("placeholder_anvil");
        registerBlock("placeholder_cauldron");
        registerBlock("placeholder_iron_bars");
        registerBlock("placeholder_nether_bricks", () -> new com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalShellBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_BRICKS)));
        registerBlock("placeholder_obsidian", () -> new com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalShellBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
        registerBlock("placeholder_table");
        registerBlock("potion_sprayer");
        registerBlock("pressure_plate_greatwood");
        registerBlock("pressure_plate_silverwood");
        registerBlock("purifying_fluid");
        // recharge_pedestal registered in TTBlocks (Wave 2b)
        // redstone_relay registered in TTBlocks (Wave 4)

        registerBlock("sapling_greatwood", () -> new BlockSaplingTT(TTTreeGrowers.GREATWOOD, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
        registerBlock("sapling_silverwood", () -> new BlockSaplingTT(TTTreeGrowers.SILVERWOOD, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
        registerBlock("shimmerleaf", () -> new BushBlock(BlockBehaviour.Properties.copy(Blocks.FERN)));
        registerBlock("silverwood");
        // silverwood_node_log registered in TTBlocks (RotatedPillar + node BE)
        registerBlock("slab_ancient", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_ancient_rock", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_ancient_tile", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_arcane_brick", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_SLAB)));
        registerBlock("slab_arcane_stone", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_SLAB)));
        registerBlock("slab_eldritch", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_eldritch_rock", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_eldritch_stone", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("slab_greatwood", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS)));
        registerBlock("slab_silverwood", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
        registerBlock("smelter_aux");
        registerBlock("smelter_thaumium", () -> new com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlock(
                BlockBehaviour.Properties.copy(Blocks.FURNACE)));
        registerBlock("smelter_vent");
        registerBlock("smelter_void", () -> new com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlock(
                BlockBehaviour.Properties.copy(Blocks.FURNACE)));
        registerBlock("spa");
        registerBlock("stabilizer", com.leclowndu93150.thaumaturge.content.infusion.InfusionStabilizer::new);
        registerBlock("stairs_ancient", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_ancient_rock", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_ancient_tile", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_arcane", () -> new StairBlock(
                () -> TTBlocks.ARCANE_STONE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_STAIRS)));
        registerBlock("stairs_arcane_brick", () -> new StairBlock(
                () -> TTBlocks.ARCANE_STONE_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_STAIRS)));
        registerBlock("stairs_eldritch", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_eldritch_rock", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_eldritch_tile", () -> new StairBlock(Blocks.STONE_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        registerBlock("stairs_greatwood", () -> new StairBlock(Blocks.DARK_OAK_PLANKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS)));
        registerBlock("stairs_silverwood", () -> new StairBlock(Blocks.BIRCH_PLANKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
        registerBlock("stone_ancient");
        registerBlock("stone_ancient_doorway");
        registerBlock("stone_ancient_glyphed");
        registerBlock("stone_ancient_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)));
        registerBlock("stone_ancient_rock");
        registerBlock("stone_ancient_tile");
        registerBlock("stone_arcane");
        registerBlock("stone_arcane_brick");
        registerBlock("stone_arcane_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)));
        registerBlock("stone_eldritch_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)));
        registerBlock("stone_eldritch_tile");
        registerBlock("stone_porous");
        registerBlock("stripped_greatwood");
        registerBlock("stripped_log_greatwood");
        registerBlock("stripped_log_silverwood");
        registerBlock("stripped_silverwood");
        registerBlock("table_stone");
        registerBlock("taint_crust");
        registerBlock("taint_feature");
        registerBlock("taint_fibre", () -> new BushBlock(BlockBehaviour.Properties.copy(Blocks.FERN)) {
            @Override
            protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
                return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
            }
        });
        registerBlock("taint_geyser");
        registerBlock("taint_log");
        registerBlock("taint_rock");
        registerBlock("taint_soil");
        registerBlock("taint_spore_stalk");
        registerBlock("tallow_block");
        registerBlock("thaumatorium");
        registerBlock("thaumatorium_top");
        registerBlock("trapdoor_greatwood", () -> new TrapDoorBlock(
                BlockBehaviour.Properties.copy(Blocks.DARK_OAK_TRAPDOOR), BlockSetType.DARK_OAK));
        registerBlock("trapdoor_silverwood", () -> new TrapDoorBlock(
                BlockBehaviour.Properties.copy(Blocks.BIRCH_TRAPDOOR), BlockSetType.BIRCH));
        registerBlock("tube_buffer");
        registerBlock("tube_filter");
        registerBlock("tube_oneway");
        registerBlock("tube_restrict");
        registerBlock("tube_valve");
        registerBlock("vis_battery");
        registerBlock("vis_generator");
        registerBlock("vis_relay", com.leclowndu93150.thaumaturge.content.aura.relay.VisRelay::new);
        registerBlock("vishroom", () -> new BushBlock(BlockBehaviour.Properties.copy(Blocks.FERN)));
        registerBlock("void_siphon");
        registerBlock("wall_ancient", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_ancient_rock", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_ancient_tile", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_arcane_brick", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_arcane_stone", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_banner_crimson_cult");
        registerBlock("wall_eldritch_rock", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_eldritch_stone", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("wall_eldritch_tile", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_WALL)));
        registerBlock("warded_glass");
        registerBlock("banner_white");
        registerBlock("wall_banner_white");
        registerBlock("candle_white", MagicCandleBlock::new);
        registerBlock("banner_orange");
        registerBlock("wall_banner_orange");
        registerBlock("candle_orange", MagicCandleBlock::new);
        registerBlock("banner_magenta");
        registerBlock("wall_banner_magenta");
        registerBlock("candle_magenta", MagicCandleBlock::new);
        registerBlock("banner_light_blue");
        registerBlock("wall_banner_light_blue");
        registerBlock("candle_light_blue", MagicCandleBlock::new);
        registerBlock("banner_yellow");
        registerBlock("wall_banner_yellow");
        registerBlock("candle_yellow", MagicCandleBlock::new);
        registerBlock("banner_lime");
        registerBlock("wall_banner_lime");
        registerBlock("candle_lime", MagicCandleBlock::new);
        registerBlock("banner_pink");
        registerBlock("wall_banner_pink");
        registerBlock("candle_pink", MagicCandleBlock::new);
        registerBlock("banner_gray");
        registerBlock("wall_banner_gray");
        registerBlock("candle_gray", MagicCandleBlock::new);
        registerBlock("banner_light_gray");
        registerBlock("wall_banner_light_gray");
        registerBlock("candle_light_gray", MagicCandleBlock::new);
        registerBlock("banner_cyan");
        registerBlock("wall_banner_cyan");
        registerBlock("candle_cyan", MagicCandleBlock::new);
        registerBlock("banner_purple");
        registerBlock("wall_banner_purple");
        registerBlock("candle_purple", MagicCandleBlock::new);
        registerBlock("banner_blue");
        registerBlock("wall_banner_blue");
        registerBlock("candle_blue", MagicCandleBlock::new);
        registerBlock("banner_brown");
        registerBlock("wall_banner_brown");
        registerBlock("candle_brown", MagicCandleBlock::new);
        registerBlock("banner_green");
        registerBlock("wall_banner_green");
        registerBlock("candle_green", MagicCandleBlock::new);
        registerBlock("banner_red");
        registerBlock("wall_banner_red");
        registerBlock("candle_red", MagicCandleBlock::new);
        registerBlock("banner_black");
        registerBlock("wall_banner_black");
        registerBlock("candle_black", MagicCandleBlock::new);
        registerBlock("candle_holder_brass");
        registerBlock("candle_holder_thaumium");
        registerBlock("candle_holder_void");

    }

    private static void registerBlock(String name) {
        registerBlock(name, () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
    }

    private static void registerBlock(String name, Supplier<? extends Block> factory) {
        registerBlock(name, factory, BlockItem::new);
    }

    private static void registerDoor(String name, Block base, BlockSetType type) {
        registerBlock(name, () -> new DoorBlock(BlockBehaviour.Properties.copy(base), type), DoubleHighBlockItem::new);
    }

    private static void registerBlock(String name, Supplier<? extends Block> factory,
            BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        RegistryObject<Block> block = TTBlocks.BLOCKS.register(name, factory);
        BLOCKS.put(name, block);
        BLOCK_ITEMS.put(name, TTItems.ITEMS.register(name, () -> itemFactory.apply(block.get(), new Item.Properties())));
    }

    public static void bootstrap() {
        // static init registers entries
    }

    private TTBulkContent() {}
}
