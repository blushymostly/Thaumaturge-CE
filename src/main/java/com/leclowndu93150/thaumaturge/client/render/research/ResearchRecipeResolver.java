package com.leclowndu93150.thaumaturge.client.render.research;

import com.leclowndu93150.thaumaturge.TTIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Resolve research bookmark ids: exact recipe key, then recipes sharing {@link Recipe#getGroup()}. */
@Mod.EventBusSubscriber(modid = TTIds.MODID, value = Dist.CLIENT)
public final class ResearchRecipeResolver {
    private static RecipeManager indexedManager;
    private static Map<String, List<Recipe<?>>> byGroup = Map.of();

    private ResearchRecipeResolver() {}

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        indexedManager = null;
        byGroup = Map.of();
    }

    public static List<Recipe<?>> variants(@Nullable Level level, @Nullable ResourceLocation id) {
        if (level == null || id == null) {
            return List.of();
        }
        RecipeManager recipes = level.getRecipeManager();
        Optional<? extends Recipe<?>> direct = recipes.byKey(id);
        if (direct.isPresent()) {
            return List.of(direct.get());
        }
        if (indexedManager != recipes) {
            Map<String, List<Recipe<?>>> groups = new HashMap<>();
            for (Recipe<?> recipe : recipes.getRecipes()) {
                String group = recipe.getGroup();
                if (!group.isEmpty()) {
                    groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(recipe);
                }
            }
            groups.replaceAll((group, entries) -> List.copyOf(entries));
            byGroup = Map.copyOf(groups);
            indexedManager = recipes;
        }
        return byGroup.getOrDefault(id.toString(), List.of());
    }

    public static boolean exists(@Nullable Level level, ResourceLocation id) {
        return !variants(level, id).isEmpty();
    }

    public static ItemStack icon(@Nullable Level level, @Nullable ResourceLocation id) {
        List<Recipe<?>> recipes = variants(level, id);
        if (recipes.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }
        return recipes.get(0).getResultItem(level.registryAccess());
    }

    public static @Nullable Recipe<?> first(@Nullable Level level, @Nullable ResourceLocation id) {
        List<Recipe<?>> recipes = variants(level, id);
        return recipes.isEmpty() ? null : recipes.get(0);
    }
}
