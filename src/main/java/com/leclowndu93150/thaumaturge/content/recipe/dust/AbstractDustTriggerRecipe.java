package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.leclowndu93150.thaumaturge.api.recipe.DustGate;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerPlacement;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgressionEvents;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/** Shared plumbing for single-block dust triggers (simple block / block tag target). */
public abstract class AbstractDustTriggerRecipe implements DustTrigger {
    public static final int SWAP_DELAY_TICKS = 50;

    private final ResourceLocation id;
    private final ItemStack result;
    private final Optional<DustGate> research;

    protected AbstractDustTriggerRecipe(ResourceLocation id, ItemStack result, Optional<DustGate> research) {
        this.id = id;
        this.result = result;
        this.research = research;
    }

    public ItemStack result() {
        return this.result.copy();
    }

    @Override
    public Optional<DustGate> researchGate() {
        return this.research;
    }

    @Override
    public ItemStack assemble(DustTriggerInput input, RegistryAccess access) {
        return this.result.copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return this.result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeType<?> getType() {
        return TTRecipeTypes.DUST_TRIGGER.get();
    }

    @Override
    public void execute(DustTriggerInput input, Player player, @Nullable DustTriggerPlacement placement, Direction face) {
        if (!(input.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack stack = this.result.copy();
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchProgressionEvents.recordCrafted(serverPlayer, stack);
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            DustTriggerSwapQueue.enqueuePlace(serverLevel, input.pos(), input.clicked(), blockItem.getBlock().defaultBlockState(), SWAP_DELAY_TICKS);
        } else {
            DustTriggerSwapQueue.enqueueDrop(serverLevel, input.pos(), input.clicked(), stack, SWAP_DELAY_TICKS);
        }
    }

    protected static ItemStack readResult(JsonElement json) {
        JsonObject obj = GsonHelper.convertToJsonObject(json, "result");
        String raw = obj.has("item") ? GsonHelper.getAsString(obj, "item") : GsonHelper.getAsString(obj, "id");
        ResourceLocation itemId = ResourceLocation.tryParse(raw);
        Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            throw new JsonSyntaxException("Unknown dust trigger result item: " + raw);
        }
        return new ItemStack(item, GsonHelper.getAsInt(obj, "count", 1));
    }

    protected static Optional<DustGate> readGate(JsonObject json) {
        return json.has("research") ? Optional.of(DustGate.fromJson(json.get("research"))) : Optional.empty();
    }

    protected static void writeGate(FriendlyByteBuf buf, Optional<DustGate> gate) {
        buf.writeBoolean(gate.isPresent());
        gate.ifPresent(g -> g.toNetwork(buf));
    }

    protected static Optional<DustGate> readGate(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(DustGate.fromNetwork(buf)) : Optional.empty();
    }
}
