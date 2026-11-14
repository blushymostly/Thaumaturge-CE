package com.leclowndu93150.thaumaturge.content.research.table;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

final class ResearchTableBonuses {
    private static final int RADIUS = 8;
    private static final int BONUS_UNIT = 1;
    private static final int DARKNESS_LIGHT_LIMIT = 4;
    private static final int DARKNESS_ODDS = 20;
    private static final int ALTITUDE_ODDS = 20;
    private static final int BOOKSHELF_ODDS = 300;
    private static final int CRYSTAL_ODDS = 10;
    private static final int SUBSTRATE_ODDS = 20;
    private static final int WATER_ODDS = 15;
    private static final int HEAT_ODDS = 20;
    private static final float[] ALTITUDE_FACTORS = {0.5F, 0.66F, 0.75F};
    private static final List<CrystalRow> CRYSTAL_ROWS = List.of(new CrystalRow(() -> TTBlocks.CRYSTAL_AER.get(), TTAspects.AER),
            new CrystalRow(() -> TTBlocks.CRYSTAL_IGNIS.get(), TTAspects.IGNIS), new CrystalRow(() -> TTBlocks.CRYSTAL_AQUA.get(), TTAspects.AQUA),
            new CrystalRow(() -> TTBlocks.CRYSTAL_TERRA.get(), TTAspects.TERRA), new CrystalRow(() -> TTBlocks.CRYSTAL_ORDO.get(), TTAspects.ORDO),
            new CrystalRow(() -> TTBlocks.CRYSTAL_PERDITIO.get(), TTAspects.PERDITIO));

    private final Level level;
    private final RandomSource random;
    private final List<ResourceLocation> successes = new ArrayList<>();
    private AspectList bonus;

    private ResearchTableBonuses(Level level, RandomSource random, AspectList bonus) {
        this.level = level;
        this.random = random;
        this.bonus = bonus;
    }

    private record CrystalRow(Supplier<? extends Block> block, ResourceLocation aspect) {
    }

    static AspectList recalculate(Level level, BlockPos tablePos, AspectList current, RandomSource random) {
        return new ResearchTableBonuses(level, random, current).run(tablePos);
    }

    private AspectList run(BlockPos tablePos) {
        BlockPos above = tablePos.above();
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, above), level.getBrightness(LightLayer.SKY, above));
        if (light < DARKNESS_LIGHT_LIMIT && !level.canSeeSky(above) && roll(DARKNESS_ODDS)) {
            add(TTAspects.PERDITIO);
        }
        for (float factor : ALTITUDE_FACTORS) {
            if (tablePos.getY() > level.getHeight() * factor && roll(ALTITUDE_ODDS)) {
                add(TTAspects.AER);
            }
        }
        sampleSurroundings(tablePos);
        return bonus;
    }

    private void sampleSurroundings(BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    successes.clear();
                    collect(level.getBlockState(cursor));
                    if (!successes.isEmpty() && add(successes.get(random.nextInt(successes.size())))) {
                        return;
                    }
                }
            }
        }
    }

    private void collect(BlockState state) {
        if (state.is(Blocks.BOOKSHELF) && roll(BOOKSHELF_ODDS)) {
            offer(randomAspect());
        }
        for (CrystalRow row : CRYSTAL_ROWS) {
            if (state.is(row.block().get()) && roll(CRYSTAL_ODDS)) {
                offer(row.aspect());
            }
        }
        if ((state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)) && roll(SUBSTRATE_ODDS)) {
            offer(TTAspects.TERRA);
        }
        FluidState fluid = state.getFluidState();
        if (fluid.is(FluidTags.WATER) && roll(WATER_ODDS)) {
            offer(TTAspects.AQUA);
        }
        if ((fluid.is(FluidTags.LAVA) || state.getBlock() instanceof BaseFireBlock) && roll(HEAT_ODDS)) {
            offer(TTAspects.IGNIS);
        }
    }

    private boolean roll(int odds) {
        return random.nextInt(odds) == 0;
    }

    private void offer(@Nullable ResourceLocation aspect) {
        if (aspect != null) {
            successes.add(aspect);
        }
    }

    private boolean add(@Nullable ResourceLocation aspect) {
        if (aspect == null || bonus.amountOf(aspect) > 0) {
            return false;
        }
        bonus = bonus.add(new AspectInstance(aspect, BONUS_UNIT));
        return true;
    }

    private @Nullable ResourceLocation randomAspect() {
        List<ResourceLocation> all = new ArrayList<>(TTAspects.all().keySet());
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }
}
