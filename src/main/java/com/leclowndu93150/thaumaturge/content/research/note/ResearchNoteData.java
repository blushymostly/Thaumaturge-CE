package com.leclowndu93150.thaumaturge.content.research.note;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Research-note puzzle state. Stored in the item NBT via {@link #CODEC} (Forge stand-in for a CE data component). */
public record ResearchNoteData(ResourceLocation entry, int index, int color, boolean complete, int copies, List<Cell> cells) {
    public static final int TYPE_BLANK = 0;
    public static final int TYPE_ROOT = 1;
    public static final int TYPE_PLACED = 2;

    public record Cell(HexGrid.Hex hex, int type, Optional<ResourceLocation> aspect) {
        public static final Codec<Cell> CODEC = RecordCodecBuilder.create(instance -> instance.group(HexGrid.Hex.CODEC.fieldOf("hex").forGetter(Cell::hex),
                Codec.INT.fieldOf("type").forGetter(Cell::type), ResourceLocation.CODEC.optionalFieldOf("aspect").forGetter(Cell::aspect)).apply(instance, Cell::new));

        public @Nullable ResourceLocation aspectOrNull() {
            return aspect.orElse(null);
        }

        public boolean active() {
            return type >= TYPE_ROOT && aspect.isPresent();
        }
    }

    public static final Codec<ResearchNoteData> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("entry").forGetter(ResearchNoteData::entry), Codec.INT.fieldOf("index").forGetter(ResearchNoteData::index),
                    Codec.INT.fieldOf("color").forGetter(ResearchNoteData::color), Codec.BOOL.optionalFieldOf("complete", false).forGetter(ResearchNoteData::complete),
                    Codec.INT.optionalFieldOf("copies", 0).forGetter(ResearchNoteData::copies), Cell.CODEC.listOf().fieldOf("cells").forGetter(ResearchNoteData::cells))
            .apply(instance, ResearchNoteData::new));

    public Map<HexGrid.Hex, Cell> cellMap() {
        Map<HexGrid.Hex, Cell> map = new LinkedHashMap<>();
        for (Cell cell : cells) {
            map.put(cell.hex(), cell);
        }
        return map;
    }

    public @Nullable Cell cellAt(HexGrid.Hex hex) {
        for (Cell cell : cells) {
            if (cell.hex().equals(hex)) {
                return cell;
            }
        }
        return null;
    }

    public ResearchNoteData withCell(HexGrid.Hex hex, int type, @Nullable ResourceLocation aspect) {
        List<Cell> updated = new ArrayList<>(cells.size());
        boolean replaced = false;
        Cell next = new Cell(hex, type, Optional.ofNullable(aspect));
        for (Cell cell : cells) {
            if (cell.hex().equals(hex)) {
                updated.add(next);
                replaced = true;
            } else {
                updated.add(cell);
            }
        }
        if (!replaced) {
            updated.add(next);
        }
        return new ResearchNoteData(entry, index, color, complete, copies, List.copyOf(updated));
    }

    public ResearchNoteData withCells(List<Cell> newCells) {
        return new ResearchNoteData(entry, index, color, complete, copies, List.copyOf(newCells));
    }

    public ResearchNoteData asComplete() {
        return new ResearchNoteData(entry, index, color, true, copies, cells);
    }

    public ResearchNoteData withCopies(int newCopies) {
        return new ResearchNoteData(entry, index, color, complete, newCopies, cells);
    }

    public ResourceLocation learnKey() {
        return learnKey(entry, index);
    }

    public static ResourceLocation learnKey(ResourceLocation entry, int index) {
        return new ResourceLocation(entry.getNamespace(), "note/" + entry.getPath() + "/" + index);
    }
}
