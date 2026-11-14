package com.leclowndu93150.thaumaturge.data.model;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionPillarBlock;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import java.util.Map;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge 1.20.1 stand-in for CE26 TTModelProvider (1.21 ModelProvider API).
 * Generates CE-shaped models for core registered blocks + mesh loaders.
 * Hand-authored geometry (crucible, workbench, jars, infusion) stays in main resources.
 */
public final class TTBlockStateProvider extends BlockStateProvider {
    private static final Map<String, MeshSpec> MESH_BLOCKS = Map.of(
            "pillar_arcane", new MeshSpec("models/mesh/pillar.ttmesh", "block/pillar_normal", true),
            "pillar_ancient", new MeshSpec("models/mesh/pillar.ttmesh", "block/pillar_ancient", true),
            "pillar_eldritch", new MeshSpec("models/mesh/pillar.ttmesh", "block/pillar_eldritch", true),
            "node_stabilizer", new MeshSpec("models/mesh/node_stabilizer.ttmesh", "block/node_stabilizer", false),
            "node_stabilizer_advanced", new MeshSpec("models/mesh/node_stabilizer.ttmesh", "block/node_stabilizer", false),
            "node_transducer", new MeshSpec("models/mesh/node_stabilizer.ttmesh", "block/node_converter", false),
            "eldritch_crab_spawner", new MeshSpec("models/mesh/eldritch_crab_spawner.ttmesh", "block/eldritch_crab_spawner", true)
    );

    private static final Map<String, String> CUBE_TEXTURES = Map.of(
            "arcane_stone", "arcane_stone_1",
            "tube_buffer", "tube_fittings",
            "tube_filter", "tube_fittings",
            "tube_oneway", "tube_fittings",
            "tube_restrict", "tube_fittings",
            "tube_valve", "tube_fittings"
    );

    public TTBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TTIds.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        axisBlock((RotatedPillarBlock) TTBlocks.LOG_GREATWOOD.get(),
                modLoc("block/log_greatwood"), modLoc("block/log_greatwood_top"));
        simpleBlockItem(TTBlocks.LOG_GREATWOOD.get(), models().getExistingFile(modLoc("block/log_greatwood")));

        axisBlock((RotatedPillarBlock) TTBlocks.LOG_SILVERWOOD.get(),
                modLoc("block/log_silverwood"), modLoc("block/log_silverwood_top"));
        simpleBlockItem(TTBlocks.LOG_SILVERWOOD.get(), models().getExistingFile(modLoc("block/log_silverwood")));

        for (Map.Entry<String, String> entry : CUBE_TEXTURES.entrySet()) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(TTIds.MODID, entry.getKey()));
            if (block == null || block.defaultBlockState().isAir()) {
                continue;
            }
            cubeAllNamed(block, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, MeshSpec> entry : MESH_BLOCKS.entrySet()) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(TTIds.MODID, entry.getKey()));
            if (block == null || block.defaultBlockState().isAir()) {
                continue;
            }
            registerMesh(block, entry.getKey(), entry.getValue());
        }
    }

    private void cubeAllNamed(Block block, String path, String texture) {
        BlockModelBuilder model = models().cubeAll(path, modLoc("block/" + texture));
        getVariantBuilder(block).partialState().setModels(new ConfiguredModel(model));
        itemModels().getBuilder(path).parent(model);
    }

    private void registerMesh(Block block, String path, MeshSpec spec) {
        BlockModelBuilder builder = models().getBuilder(path)
                .parent(new ModelFile.UncheckedModelFile("block/block"))
                .texture("particle", modLoc(spec.texture()))
                .texture("texture", modLoc(spec.texture()))
                .ao(false);
        builder.customLoader(TTMeshLoaderBuilder::new)
                .mesh(modLoc(spec.mesh()))
                .flipV(true)
                .cornerSpace(spec.cornerSpace())
                .worldLight(block instanceof InfusionPillarBlock)
                .end();
        simpleBlock(block, builder);
        itemModels().getBuilder(path).parent(builder);
    }

    private record MeshSpec(String mesh, String texture, boolean cornerSpace) {}
}
