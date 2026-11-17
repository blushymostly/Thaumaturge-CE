package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.content.taint.EtherealBloom;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.aura.BlockEntityRechargePedestal;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityDioptra;
import com.leclowndu93150.thaumaturge.content.essentia.jar.EssentiaJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.jar.BrainJarBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.AlembicBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlockEntity;
import com.leclowndu93150.thaumaturge.content.essentia.tube.TubeBlockEntity;
import com.leclowndu93150.thaumaturge.content.crucible.CrucibleBlockEntity;
import com.leclowndu93150.thaumaturge.content.misc.nitor.BlockEntityNitor;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityArcaneEar;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityHungryChest;
import com.leclowndu93150.thaumaturge.content.device.levitator.BlockEntityLevitator;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchBlockEntity;
import net.minecraft.world.level.block.Block;
import com.leclowndu93150.thaumaturge.content.essentia.bellows.BlockEntityBellows;
import com.leclowndu93150.thaumaturge.content.item.FocalManipulatorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TTBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TTIds.MODID);

    public static final RegistryObject<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE = BLOCK_ENTITIES.register("crucible",
            () -> BlockEntityType.Builder.of(CrucibleBlockEntity::new, TTBlocks.CRUCIBLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<EssentiaJarBlockEntity>> ESSENTIA_JAR = BLOCK_ENTITIES.register("jar_normal",
            () -> BlockEntityType.Builder.of(EssentiaJarBlockEntity::new, TTBlocks.JAR_NORMAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<BrainJarBlockEntity>> BRAIN_JAR = BLOCK_ENTITIES.register("jar_brain",
            () -> BlockEntityType.Builder.of(BrainJarBlockEntity::new, TTBulkContent.BLOCKS.get("jar_brain").get()).build(null));
    public static final RegistryObject<BlockEntityType<TubeBlockEntity>> TUBE = BLOCK_ENTITIES.register("tube",
            () -> BlockEntityType.Builder.of(TubeBlockEntity::new, TTBlocks.TUBE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SmelterBlockEntity>> SMELTER = BLOCK_ENTITIES.register("smelter_basic", () -> BlockEntityType.Builder
            .of(SmelterBlockEntity::new, TTBlocks.SMELTER_BASIC.get(), TTBulkContent.BLOCKS.get("smelter_thaumium").get(), TTBulkContent.BLOCKS.get("smelter_void").get()).build(null));
    public static final RegistryObject<BlockEntityType<AlembicBlockEntity>> ALEMBIC = BLOCK_ENTITIES.register("alembic",
            () -> BlockEntityType.Builder.of(AlembicBlockEntity::new, TTBlocks.ALEMBIC.get()).build(null));
    public static final RegistryObject<BlockEntityType<ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH = BLOCK_ENTITIES.register("arcane_workbench",
            () -> BlockEntityType.Builder.of(ArcaneWorkbenchBlockEntity::new, TTBlocks.ARCANE_WORKBENCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<BlockEntityResearchTable>> RESEARCH_TABLE = BLOCK_ENTITIES.register("research_table",
            () -> BlockEntityType.Builder.of(BlockEntityResearchTable::new, TTBlocks.RESEARCH_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BlockEntityNitor>> NITOR = BLOCK_ENTITIES.register("nitor",
            () -> BlockEntityType.Builder.of(BlockEntityNitor::new, TTBlocks.NITORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.aura.node.JarNodeBlock.Entity>> JAR_NODE = BLOCK_ENTITIES.register("jar_node",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.aura.node.JarNodeBlock.Entity::new, TTBulkContent.BLOCKS.get("jar_node").get()).build(null));

    public static final RegistryObject<BlockEntityType<BlockEntityNode>> NODE = BLOCK_ENTITIES.register("node",
            () -> BlockEntityType.Builder.of(BlockEntityNode::new, TTBlocks.NODE.get(), TTBlocks.SILVERWOOD_NODE_LOG.get(), TTBulkContent.BLOCKS.get("obsidian_totem_charged").get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityDioptra>> DIOPTRA = BLOCK_ENTITIES.register("dioptra",
            () -> BlockEntityType.Builder.of(BlockEntityDioptra::new, TTBlocks.DIOPTRA.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityBellows>> BELLOWS = BLOCK_ENTITIES.register("bellows",
            () -> BlockEntityType.Builder.of(BlockEntityBellows::new, TTBulkContent.BLOCKS.get("bellows").get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityRechargePedestal>> RECHARGE_PEDESTAL = BLOCK_ENTITIES.register("recharge_pedestal",
            () -> BlockEntityType.Builder.of(BlockEntityRechargePedestal::new, TTBlocks.RECHARGE_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityDeconstructionTable>> DECONSTRUCTION_TABLE = BLOCK_ENTITIES.register("deconstruction_table",
            () -> BlockEntityType.Builder.of(BlockEntityDeconstructionTable::new, TTBlocks.DECONSTRUCTION_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityHungryChest>> HUNGRY_CHEST = BLOCK_ENTITIES.register("hungry_chest",
            () -> BlockEntityType.Builder.of(BlockEntityHungryChest::new, TTBlocks.HUNGRY_CHEST.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityLevitator>> LEVITATOR = BLOCK_ENTITIES.register("levitator",
            () -> BlockEntityType.Builder.of(BlockEntityLevitator::new, TTBlocks.LEVITATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityArcaneEar>> ARCANE_EAR = BLOCK_ENTITIES.register("arcane_ear",
            () -> BlockEntityType.Builder.of(BlockEntityArcaneEar::new, TTBlocks.ARCANE_EAR.get(), TTBlocks.ARCANE_EAR_TOGGLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix>> INFUSION_MATRIX = BLOCK_ENTITIES.register("infusion_matrix",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.infusion.InfusionMatrix::new, TTBlocks.INFUSION_MATRIX.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestal>> INFUSION_PEDESTAL = BLOCK_ENTITIES.register("infusion_pedestal",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.infusion.InfusionPedestal::new, TTBulkContent.BLOCKS.get("pedestal_arcane").get(),
                    TTBulkContent.BLOCKS.get("pedestal_ancient").get(), TTBulkContent.BLOCKS.get("pedestal_eldritch").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.AdvancedFurnace>> ADVANCED_FURNACE = BLOCK_ENTITIES
            .register("advanced_alchemical_furnace", () -> BlockEntityType.Builder
                    .of(com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.AdvancedFurnace::new, TTBulkContent.BLOCKS.get("advanced_alchemical_furnace").get()).build(null));
    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnaceNozzle>> FURNACE_NOZZLE = BLOCK_ENTITIES
            .register("advanced_alchemical_furnace_nozzle", () -> BlockEntityType.Builder
                    .of(com.leclowndu93150.thaumaturge.content.essentia.advancedfurnace.FurnaceNozzle::new, TTBulkContent.BLOCKS.get("advanced_alchemical_furnace_nozzle").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalFurnace>> INFERNAL_FURNACE = BLOCK_ENTITIES.register("infernal_furnace",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalFurnace::new, TTBulkContent.BLOCKS.get("infernal_furnace").get()).build(null));

    public static final RegistryObject<BlockEntityType<EtherealBloom>> ETHEREAL_BLOOM = BLOCK_ENTITIES.register("ethereal_bloom",
            () -> BlockEntityType.Builder.of(EtherealBloom::new, TTBulkContent.BLOCKS.get("ethereal_bloom").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.infusion.InfusionStabilizer.BlockEntity>> INFUSION_STABILIZER = BLOCK_ENTITIES.register("stabilizer",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.infusion.InfusionStabilizer.BlockEntity::new, TTBulkContent.BLOCKS.get("stabilizer").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.aura.node.NodeStabilizer.Device>> NODE_STABILIZER = BLOCK_ENTITIES.register("node_stabilizer",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.aura.node.NodeStabilizer.Device::new, TTBulkContent.BLOCKS.get("node_stabilizer").get(),
                    TTBulkContent.BLOCKS.get("node_stabilizer_advanced").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.aura.node.NodeTransducer.BlockEntity>> NODE_TRANSDUCER = BLOCK_ENTITIES.register("node_transducer",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.aura.node.NodeTransducer.BlockEntity::new, TTBulkContent.BLOCKS.get("node_transducer").get()).build(null));
    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayBlockEntity>> VIS_RELAY = BLOCK_ENTITIES.register("vis_relay",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.aura.relay.VisRelayBlockEntity::new, TTBulkContent.BLOCKS.get("vis_relay").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.device.EverfullUrnBlockEntity>> EVERFULL_URN = BLOCK_ENTITIES.register("everfull_urn",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.device.EverfullUrnBlockEntity::new, TTBulkContent.BLOCKS.get("everfull_urn").get()).build(null));

    public static final RegistryObject<BlockEntityType<com.leclowndu93150.thaumaturge.content.device.BlockEntityVisGenerator>> VIS_GENERATOR = BLOCK_ENTITIES.register("vis_generator",
            () -> BlockEntityType.Builder.of(com.leclowndu93150.thaumaturge.content.device.BlockEntityVisGenerator::new, TTBulkContent.BLOCKS.get("vis_generator").get()).build(null));

    public static final RegistryObject<BlockEntityType<FocalManipulatorBlockEntity>> FOCAL_MANIPULATOR = BLOCK_ENTITIES.register("focal_manipulator",
            () -> BlockEntityType.Builder.of(FocalManipulatorBlockEntity::new, TTBulkContent.BLOCKS.get("focal_manipulator").get()).build(null));

    private TTBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
