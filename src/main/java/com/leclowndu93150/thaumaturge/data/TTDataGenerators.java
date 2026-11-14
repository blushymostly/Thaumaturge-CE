package com.leclowndu93150.thaumaturge.data;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.data.model.TTBlockStateProvider;
import com.leclowndu93150.thaumaturge.data.model.TTItemModelProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TTDataGenerators {
    private TTDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        if (event.includeClient()) {
            generator.addProvider(true, new TTBlockStateProvider(output, helper));
            generator.addProvider(true, new TTItemModelProvider(output, helper));
        }
    }
}
