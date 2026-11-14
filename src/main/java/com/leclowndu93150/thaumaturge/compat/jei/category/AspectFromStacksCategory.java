package com.leclowndu93150.thaumaturge.compat.jei.category;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndex;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.client.render.aspect.AspectTagRenderer;
import com.leclowndu93150.thaumaturge.compat.jei.drawables.AlphaDrawable;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Pages of items that contain a given aspect (CE AspectFromStacks). */
public final class AspectFromStacksCategory implements IRecipeCategory<AspectFromStacksCategory.Wrapper> {
    public static final RecipeType<Wrapper> RECIPE_TYPE = RecipeType.create(TTIds.MODID, "aspect_from_stacks", Wrapper.class);

    private static final int PAGE_SIZE = 36;
    private static final int COLUMNS = 9;
    private static final ResourceLocation TEXTURE = TTIds.rl("textures/gui/gui_researchbook_overlay.png");
    private static final ResourceLocation INNER = TTIds.rl("textures/gui/gui_inner.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable resultSlot;

    public AspectFromStacksCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(176, 109);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(TTItems.THAUMONOMICON.get()));
        this.resultSlot = new AlphaDrawable(TEXTURE, 40, 6, 32, 32);
    }

    public static List<Wrapper> all(IIngredientManager ingredients) {
        Map<ResourceLocation, List<ItemStack>> byAspect = new HashMap<>();
        Map<ItemStack, AspectList> index = new HashMap<>();
        for (ItemStack stack : ingredients.getAllIngredients(VanillaTypes.ITEM_STACK)) {
            AspectList list = AspectIndex.of(stack);
            if (list.isEmpty()) {
                continue;
            }
            index.put(stack, list);
            for (AspectInstance entry : list.entries()) {
                byAspect.computeIfAbsent(entry.aspectId(), ignored -> new ArrayList<>()).add(stack);
            }
        }

        List<Wrapper> pages = new ArrayList<>();
        List<ResourceLocation> order = new ArrayList<>(byAspect.keySet());
        order.sort(Comparator
                .comparing((ResourceLocation id) -> {
                    IAspect aspect = TTAspects.get(id);
                    return aspect == null || aspect.componentIds().isEmpty() ? 0 : 1;
                })
                .thenComparing(ResourceLocation::toString));

        for (ResourceLocation aspectId : order) {
            List<ItemStack> stacks = byAspect.get(aspectId).stream()
                    .map(stack -> {
                        int amount = index.get(stack).amountOf(aspectId);
                        ItemStack copy = stack.copy();
                        copy.setCount(Math.max(1, amount));
                        return copy;
                    })
                    .sorted(Comparator.comparingInt(ItemStack::getCount).reversed())
                    .toList();
            for (int start = 0; start < stacks.size(); start += PAGE_SIZE) {
                pages.add(new Wrapper(aspectId, start / PAGE_SIZE, stacks.subList(start, Math.min(start + PAGE_SIZE, stacks.size()))));
            }
        }
        return pages;
    }

    @Override
    public RecipeType<Wrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumaturge.category.aspect_from_stacks");
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 109;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Wrapper recipe, IFocusGroup focuses) {
        int slot = 0;
        for (ItemStack stack : recipe.stacks()) {
            builder.addSlot(RecipeIngredientRole.INPUT, (slot % COLUMNS) * 18 + 8, (slot / COLUMNS) * 18 + 32)
                    .addItemStack(stack);
            slot++;
        }
    }

    @Override
    public void draw(Wrapper recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        resultSlot.draw(graphics, 72, 0);
        IAspect aspect = TTAspects.get(recipe.aspectId());
        if (aspect != null) {
            AspectTagRenderer.render(graphics, Minecraft.getInstance().font, 80, 8, aspect, 1);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
        graphics.blit(INNER, 5, 30, 0, 0, 162, 74, 256, 256);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(Wrapper recipe) {
        return TTIds.rl("aspect_from_stacks/" + recipe.aspectId().getNamespace() + "/"
                + recipe.aspectId().getPath() + "/" + recipe.page());
    }

    public record Wrapper(ResourceLocation aspectId, int page, List<ItemStack> stacks) {}
}
