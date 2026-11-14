package com.leclowndu93150.thaumaturge.data.model;

import com.leclowndu93150.thaumaturge.TTIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Core non-block item models. Jar uses builtin/entity + BEWLR (hand-authored JSON).
 */
public final class TTItemModelProvider extends ItemModelProvider {
    public TTItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TTIds.MODID, helper);
    }

    @Override
    protected void registerModels() {
        // thaumometer uses hand-authored mesh model (scanner.ttmesh) — do not flatten
        textured("thaumonomicon", "thaumonomicon");
        textured("salis_mundus", "salis_mundus");
        textured("essentia_crystal", "essentia_crystal");
        textured("phial", "phial");
        textured("quicksilver", "quicksilver");
        textured("ingot_thaumium", "ingot_thaumium");
        textured("ingot_void", "ingot_void");
        textured("ingot_brass", "ingot_brass");
        textured("primordial_pearl", "primordial_pearl");

        getBuilder("crucible").parent(new ModelFile.UncheckedModelFile(modLoc("block/crucible")));
        getBuilder("arcane_workbench").parent(new ModelFile.UncheckedModelFile(modLoc("block/arcane_workbench")));
    }

    private void textured(String item, String texture) {
        getBuilder(item)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", new ResourceLocation(TTIds.MODID, "item/" + texture));
    }
}
