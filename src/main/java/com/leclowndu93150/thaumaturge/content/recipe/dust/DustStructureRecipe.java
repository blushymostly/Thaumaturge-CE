package com.leclowndu93150.thaumaturge.content.recipe.dust;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionPillarBlock;
import com.google.gson.JsonSyntaxException;
import com.leclowndu93150.thaumaturge.api.recipe.DustGate;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerPlacement;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgressionEvents;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class DustStructureRecipe extends AbstractDustTriggerRecipe {
    private final List<Cell> cells;
    private final boolean rotate;

    public record Cell(BlockPos offset, Block source, @Nullable Block replacement) {}

    private DustStructureRecipe(ResourceLocation id, ItemStack result, Optional<DustGate> gate, List<Cell> cells, boolean rotate) {
        super(id, result, gate);
        this.cells = List.copyOf(cells);
        this.rotate = rotate;
    }

    public List<Cell> cells() {
        return cells;
    }

    @Override
    public boolean matches(DustTriggerInput input, Level level) {
        return findPlacement(input) != null;
    }

    @Override
    public @Nullable DustTriggerPlacement findPlacement(DustTriggerInput input) {
        if (!rotate) return validAt(input.level(), input.pos(), Direction.SOUTH) ? DustTriggerPlacement.origin() : null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos center = input.clicked().is(net.minecraft.world.level.block.Blocks.IRON_BARS)
                    ? input.pos().relative(side.getOpposite()) : input.pos();
            if (validAt(input.level(), center, side)) {
                BlockPos offset = center.subtract(input.pos());
                return new DustTriggerPlacement(offset.getX(), offset.getY(), offset.getZ(), side);
            }
        }
        return null;
    }

    private boolean validAt(Level level, BlockPos center, Direction side) {
        for (Cell cell : cells) {
            BlockPos at = center.offset(rotate(cell.offset(), side));
            if (!level.hasChunkAt(at) || !matchesSource(level.getBlockState(at), cell.source())) {
                return false;
            }
            if (cell.replacement() != null && !com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess.contentsOf(level, at).isEmpty()) {
                return false;
            }
        }
        return !(level.getBlockEntity(center) instanceof com.leclowndu93150.thaumaturge.content.essentia.smeltery.SmelterBlockEntity smelter)
                || smelter.isEmpty() && smelter.stock().isEmpty();
    }

    private static BlockPos rotate(BlockPos offset, @Nullable Direction side) {
        if (side == null) return offset;
        int x = offset.getX() * side.getStepZ() + offset.getZ() * side.getStepX();
        int z = offset.getZ() * side.getStepZ() - offset.getX() * side.getStepX();
        return new BlockPos(x, offset.getY(), z);
    }

    private static boolean matchesSource(net.minecraft.world.level.block.state.BlockState state, Block expected) {
        if (state.is(expected)) return true;
        // Both registry IDs exist in worlds saved before the arcane-stone alias was hidden.
        return BuiltInRegistries.BLOCK.getKey(expected).equals(com.leclowndu93150.thaumaturge.TTIds.rl("arcane_stone"))
                && state.is(com.leclowndu93150.thaumaturge.registry.TTBulkContent.BLOCKS.get("stone_arcane").get());
    }

    @Override
    public boolean isMultiblock() {
        return true;
    }

    @Override
    public List<BlockPos> sparkle(Level level, Player player, BlockPos clicked, DustTriggerPlacement placement) {
        BlockPos center = clicked.offset(placement.xOffset(), placement.yOffset(), placement.zOffset());
        return cells.stream().map(cell -> center.offset(rotate(cell.offset(), placement.facing()))).toList();
    }

    @Override
    public void execute(DustTriggerInput input, Player player, DustTriggerPlacement placement, Direction face) {
        if (placement == null) return;
        BlockPos center = input.pos().offset(placement.xOffset(), placement.yOffset(), placement.zOffset());
        for (Cell cell : cells) {
            if (cell.replacement() != null) {
                BlockPos at = center.offset(rotate(cell.offset(), placement.facing()));
                var old = input.level().getBlockState(at);
                var state = cell.replacement().defaultBlockState();
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && old.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, old.getValue(BlockStateProperties.HORIZONTAL_FACING));
                }
                if (rotate && at.equals(center) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, placement.facing().getOpposite());
                }
                if (state.getBlock() instanceof InfusionPillarBlock) {
                    state = InfusionPillarBlock.faceMatrix(state, at.subtract(center));
                }
                input.level().setBlock(at, state, Block.UPDATE_ALL);
            }
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchProgressionEvents.recordCrafted(serverPlayer, result());
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TTRecipeTypes.DUST_STRUCTURE_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<DustStructureRecipe> {
        @Override
        public DustStructureRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<Cell> cells = new ArrayList<>();
            for (var element : GsonHelper.getAsJsonArray(json, "cells")) {
                var cell = element.getAsJsonObject();
                var pos = new BlockPos(GsonHelper.getAsInt(cell, "x", 0), GsonHelper.getAsInt(cell, "y", 0), GsonHelper.getAsInt(cell, "z", 0));
                cells.add(new Cell(pos, block(cell, "source"), cell.has("replacement") ? block(cell, "replacement") : null));
            }
            if (cells.isEmpty() || cells.stream().noneMatch(cell -> cell.offset().equals(BlockPos.ZERO))
                    || cells.stream().map(Cell::offset).distinct().count() != cells.size()) {
                throw new JsonSyntaxException("Structure must contain the clicked block and unique offsets");
            }
            return new DustStructureRecipe(id, readResult(json.get("result")), readGate(json), cells, GsonHelper.getAsBoolean(json, "rotate", false));
        }

        private static Block block(JsonObject cell, String key) {
            var id = ResourceLocation.tryParse(GsonHelper.getAsString(cell, key));
            if (id == null) {
                throw new JsonSyntaxException("Invalid structure block " + key);
            }
            return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new JsonSyntaxException("Unknown structure block " + id));
        }

        @Override
        public DustStructureRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ItemStack result = buf.readItem();
            Optional<DustGate> gate = readGate(buf);
            List<Cell> cells = buf.readList(packet -> new Cell(packet.readBlockPos(),
                    BuiltInRegistries.BLOCK.get(packet.readResourceLocation()),
                    packet.readBoolean() ? BuiltInRegistries.BLOCK.get(packet.readResourceLocation()) : null));
            return new DustStructureRecipe(id, result, gate, cells, buf.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DustStructureRecipe recipe) {
            buf.writeItem(recipe.result());
            writeGate(buf, recipe.researchGate());
            buf.writeCollection(recipe.cells, (packet, cell) -> {
                packet.writeBlockPos(cell.offset());
                packet.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(cell.source()));
                packet.writeBoolean(cell.replacement() != null);
                if (cell.replacement() != null) {
                    packet.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(cell.replacement()));
                }
            });
            buf.writeBoolean(recipe.rotate);
        }
    }
}
