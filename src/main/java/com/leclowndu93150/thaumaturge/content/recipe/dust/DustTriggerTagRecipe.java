package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.leclowndu93150.thaumaturge.api.recipe.DustGate;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Dust trigger targeting any block in a block tag: {@code {"target_tag": "forge:bookshelves", "result": {...}, "research": {...}}}. */
public final class DustTriggerTagRecipe extends AbstractDustTriggerRecipe {
    private final TagKey<Block> targetTag;

    public DustTriggerTagRecipe(ResourceLocation id, TagKey<Block> targetTag, ItemStack result, Optional<DustGate> research) {
        super(id, result, research);
        this.targetTag = targetTag;
    }

    public TagKey<Block> targetTag() {
        return this.targetTag;
    }

    @Override
    public boolean matches(DustTriggerInput input, Level level) {
        return input.clicked().is(this.targetTag);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TTRecipeTypes.DUST_TRIGGER_TAG_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<DustTriggerTagRecipe> {
        @Override
        public DustTriggerTagRecipe fromJson(ResourceLocation id, JsonObject json) {
            String raw = GsonHelper.getAsString(json, "target_tag");
            ResourceLocation tagId = ResourceLocation.tryParse(raw);
            if (tagId == null) {
                throw new JsonSyntaxException("Invalid dust trigger target_tag: " + raw);
            }
            return new DustTriggerTagRecipe(id, TagKey.create(Registries.BLOCK, tagId), readResult(json.get("result")), readGate(json));
        }

        @Override
        public DustTriggerTagRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, buf.readResourceLocation());
            ItemStack result = buf.readItem();
            return new DustTriggerTagRecipe(id, tag, result, readGate(buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DustTriggerTagRecipe recipe) {
            buf.writeResourceLocation(recipe.targetTag.location());
            buf.writeItem(recipe.result());
            writeGate(buf, recipe.researchGate());
        }
    }
}
