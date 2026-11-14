package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.leclowndu93150.thaumaturge.content.research.decon.MenuDeconstructionTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import com.leclowndu93150.thaumaturge.content.workbench.ArcaneWorkbenchMenu;

public final class TTMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TTIds.MODID);

    public static final RegistryObject<MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
            () -> IForgeMenuType.create(ArcaneWorkbenchMenu::fromNetwork));

    public static final RegistryObject<MenuType<MenuResearchTable>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> IForgeMenuType.create(MenuResearchTable::fromNetwork));

    public static final RegistryObject<MenuType<MenuDeconstructionTable>> DECONSTRUCTION_TABLE = MENUS.register("deconstruction_table",
            () -> IForgeMenuType.create(MenuDeconstructionTable::fromNetwork));
    public static final RegistryObject<MenuType<SmelterMenu>> SMELTER = MENUS.register("smelter",
            () -> new MenuType<>(SmelterMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private TTMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
