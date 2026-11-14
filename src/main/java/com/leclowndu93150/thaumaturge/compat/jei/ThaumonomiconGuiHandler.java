package com.leclowndu93150.thaumaturge.compat.jei;

import com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** JEI click-through from Thaumonomicon recipe inserts. */
public final class ThaumonomiconGuiHandler implements IGlobalGuiHandler {
    private static @Nullable IIngredientManager ingredients;

    public static void bind(IIngredientManager manager) {
        ingredients = manager;
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(double mouseX, double mouseY) {
        if (ingredients == null || !(Minecraft.getInstance().screen instanceof EntryDetailScreen screen)) {
            return Optional.empty();
        }
        EntryDetailScreen.HoveredItem hovered = screen.itemUnderMouse(mouseX, mouseY);
        if (hovered == null || hovered.stack().isEmpty()) {
            return Optional.empty();
        }
        Optional<ITypedIngredient<ItemStack>> typed = ingredients.createTypedIngredient(hovered.stack());
        if (typed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(clickable(typed.get(), new Rect2i(hovered.x(), hovered.y(), 16, 16)));
    }

    private static <T> IClickableIngredient<T> clickable(ITypedIngredient<T> ingredient, Rect2i area) {
        return new IClickableIngredient<>() {
            @Override
            public ITypedIngredient<T> getTypedIngredient() {
                return ingredient;
            }

            @Override
            public Rect2i getArea() {
                return area;
            }
        };
    }
}
