package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.leclowndu93150.thaumaturge.api.recipe.DustGate;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Dust trigger targeting one specific block: {@code {"target": "minecraft:cauldron", "result": {...}, "research": {...}}}. */
public final class DustTriggerSimpleRecipe extends AbstractDustTriggerRecipe {
    private final Block target;

    public DustTriggerSimpleRecipe(ResourceLocation id, Block target, ItemStack result, Optional<DustGate> research) {
        super(id, result, research);
        this.target = target;
    }

    public Block target() {
        return this.target;
    }

    @Override
    public boolean matches(DustTriggerInput input, Level level) {
        return input.clicked().getBlock() == this.target;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TTRecipeTypes.DUST_TRIGGER_SIMPLE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<DustTriggerSimpleRecipe> {
        @Override
        public DustTriggerSimpleRecipe fromJson(ResourceLocation id, JsonObject json) {
            String raw = GsonHelper.getAsString(json, "target");
            ResourceLocation blockId = ResourceLocation.tryParse(raw);
            Block block = blockId == null ? null : BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            if (block == null) {
                throw new JsonSyntaxException("Unknown dust trigger target block: " + raw);
            }
            return new DustTriggerSimpleRecipe(id, block, readResult(json.get("result")), readGate(json));
        }

        @Override
        public DustTriggerSimpleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Block block = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
            ItemStack result = buf.readItem();
            return new DustTriggerSimpleRecipe(id, block, result, readGate(buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DustTriggerSimpleRecipe recipe) {
            buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(recipe.target));
            buf.writeItem(recipe.result());
            writeGate(buf, recipe.researchGate());
        }
    }
}
