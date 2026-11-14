package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.item.EssentiaCrystalFactory;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TTCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TTIds.MODID);
    public static final TagKey<Item> INTERNAL_ITEMS = TagKey.create(Registries.ITEM, TTIds.rl("internal_items"));

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumaturge"))
            .icon(() -> new ItemStack(TTItems.THAUMONOMICON.get()))
            .displayItems((params, output) -> {
                TTItems.ITEMS.getEntries().forEach(entry -> {
                    ItemStack stack = entry.get().getDefaultInstance();
                    if (!stack.is(TTItems.ESSENTIA_CRYSTAL.get()) && !stack.is(INTERNAL_ITEMS)) {
                        output.accept(stack);
                    }
                });
                output.accept(EssentiaCrystalFactory.aer());
                output.accept(EssentiaCrystalFactory.ignis());
                output.accept(EssentiaCrystalFactory.aqua());
                output.accept(EssentiaCrystalFactory.terra());
                output.accept(EssentiaCrystalFactory.ordo());
                output.accept(EssentiaCrystalFactory.perditio());
            })
            .build());

    private TTCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
