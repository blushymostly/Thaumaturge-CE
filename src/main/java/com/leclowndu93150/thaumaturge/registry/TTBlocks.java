package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aura.BlockRechargePedestal;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockNode;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlock;
import com.leclowndu93150.thaumaturge.content.decor.BlockTable;
import com.leclowndu93150.thaumaturge.content.device.BlockDioptra;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlock;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.AlembicBlock;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlock;
import com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlock;
import com.leclowndu93150.thaumaturge.content.misc.nitor.BlockNitor;
import com.leclowndu93150.thaumaturge.content.device.BlockHungryChest;
import com.leclowndu93150.thaumaturge.content.device.BlockLampArcane;
import com.leclowndu93150.thaumaturge.content.device.BlockRedstoneRelay;
import com.leclowndu93150.thaumaturge.content.device.grate.BlockItemGrate;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.table.BlockResearchTable;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlock;
import com.leclowndu93150.thaumaturge.content.world.crystal.BlockCrystal;
import com.leclowndu93150.thaumaturge.content.world.tree.BlockSilverwoodNodeLog;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TTIds.MODID);

    public static final RegistryObject<Block> LOG_GREATWOOD = register("log_greatwood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()));
    public static final RegistryObject<Block> LOG_SILVERWOOD = register("log_silverwood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()));
    public static final RegistryObject<BlockSilverwoodNodeLog> SILVERWOOD_NODE_LOG = BLOCKS.register("silverwood_node_log",
            () -> new BlockSilverwoodNodeLog(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD).lightLevel(state -> 5).ignitedByLava()));
    public static final RegistryObject<Block> PLANK_GREATWOOD = register("plank_greatwood",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS)));
    public static final RegistryObject<Block> PLANK_SILVERWOOD = register("plank_silverwood",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
    public static final RegistryObject<Block> ARCANE_STONE = register("arcane_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));
    public static final RegistryObject<Block> ARCANE_STONE_BRICK = register("arcane_stone_brick",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));
    public static final RegistryObject<Block> CRUCIBLE = register("crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> ARCANE_WORKBENCH = register("arcane_workbench",
            () -> new ArcaneWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> RESEARCH_TABLE = register("research_table",
            () -> new BlockResearchTable(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> INFUSION_MATRIX = register("infusion_matrix",
            () -> new com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrixBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> JAR_NORMAL = register("jar_normal",
            () -> new EssentiaJarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).sound(TTSoundTypes.JAR).noOcclusion()));
    public static final RegistryObject<Block> TUBE = register("tube",
            () -> new TubeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> SMELTER_BASIC = register("smelter_basic",
            () -> new SmelterBlock(BlockBehaviour.Properties.copy(Blocks.FURNACE)));
    public static final RegistryObject<Block> ALEMBIC = register("alembic",
            () -> new AlembicBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> ORE_CINNABAR = register("ore_cinnabar",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DEEPSLATE_ORE_CINNABAR = register("deepslate_ore_cinnabar",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5F, 5.0F)
                    .sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> TABLE = register("table_wood",
            () -> new BlockTable(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<BlockNode> NODE = BLOCKS.register("node",
            () -> new BlockNode(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.0F, 3600000.0F).noOcclusion().noLootTable().isValidSpawn((state, level, pos, type) -> false)));
    public static final RegistryObject<Block> DIOPTRA = register("dioptra",
            () -> new BlockDioptra(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> RECHARGE_PEDESTAL = register("recharge_pedestal",
            () -> new BlockRechargePedestal(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> DECONSTRUCTION_TABLE = register("deconstruction_table",
            () -> new BlockDeconstructionTable(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> HUNGRY_CHEST = BLOCKS.register("hungry_chest", BlockHungryChest::new);
    public static final RegistryObject<Block> LAMP_ARCANE = BLOCKS.register("lamp_arcane", BlockLampArcane::new);
    public static final RegistryObject<Block> ITEM_GRATE = BLOCKS.register("item_grate", BlockItemGrate::new);
    public static final RegistryObject<Block> REDSTONE_RELAY = BLOCKS.register("redstone_relay", BlockRedstoneRelay::new);
    public static final RegistryObject<Block> LEVITATOR = BLOCKS.register("levitator",
            com.leclowndu93150.thaumaturge.content.device.levitator.BlockLevitator::new);
    public static final RegistryObject<Block> ARCANE_EAR = BLOCKS.register("arcane_ear",
            () -> new com.leclowndu93150.thaumaturge.content.device.BlockArcaneEar(false));
    public static final RegistryObject<Block> ARCANE_EAR_TOGGLE = BLOCKS.register("arcane_ear_toggle",
            () -> new com.leclowndu93150.thaumaturge.content.device.BlockArcaneEar(true));

    public static final Map<DyeColor, RegistryObject<BlockNitor>> NITORS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            NITORS.put(dye, BLOCKS.register("nitor_" + dye.getName(),
                    () -> new BlockNitor(dye, BlockBehaviour.Properties.of()
                            .mapColor(dye.getMapColor())
                            .strength(0.1F)
                            .sound(SoundType.WOOL)
                            .lightLevel(state -> 15)
                            .noOcclusion()
                            .noCollission()
                            .pushReaction(PushReaction.DESTROY))));
        }
    }

    public static final RegistryObject<BlockCrystal> CRYSTAL_AER = registerCrystal("crystal_aer", TTAspects.AER);
    public static final RegistryObject<BlockCrystal> CRYSTAL_IGNIS = registerCrystal("crystal_ignis", TTAspects.IGNIS);
    public static final RegistryObject<BlockCrystal> CRYSTAL_AQUA = registerCrystal("crystal_aqua", TTAspects.AQUA);
    public static final RegistryObject<BlockCrystal> CRYSTAL_TERRA = registerCrystal("crystal_terra", TTAspects.TERRA);
    public static final RegistryObject<BlockCrystal> CRYSTAL_ORDO = registerCrystal("crystal_ordo", TTAspects.ORDO);
    public static final RegistryObject<BlockCrystal> CRYSTAL_PERDITIO = registerCrystal("crystal_perditio", TTAspects.PERDITIO);
    public static final RegistryObject<BlockCrystal> CRYSTAL_VITIUM = registerCrystal("crystal_vitium", TTAspects.VITIUM);

    private TTBlocks() {}

    private static RegistryObject<Block> register(String name, Supplier<Block> block) {
        return BLOCKS.register(name, block);
    }

    private static RegistryObject<BlockCrystal> registerCrystal(String name, ResourceLocation aspect) {
        return BLOCKS.register(name, () -> new BlockCrystal(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.25F)
                .sound(TTSoundTypes.CRYSTAL)
                .lightLevel(state -> 1)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY), aspect));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
