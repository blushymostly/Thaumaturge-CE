package com.leclowndu93150.thaumaturge.content.essentia.bellows;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.mixin.world.level.block.entity.AbstractFurnaceBlockEntityAccessor;
import com.leclowndu93150.thaumaturge.registry.TTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TTIds.MODID)
public final class BlockEntityBellows extends BlockEntity {
    private static final float PUFF_VOLUME = 0.02F;
    private static final float PUFF_BASE_PITCH = 0.5F;
    private static final float PUFF_PITCH_SPREAD = 0.08F;
    private static final int BOOST_INTERVAL = 2;
    private static final int BOOST_STEP = 1;
    private static final int BOOST_MARGIN = 5;

    public BlockEntityBellows(BlockPos pos, BlockState state) {
        super(TTBlockEntities.BELLOWS.get(), pos, state);
    }

    @SubscribeEvent
    public static void restoreLegacyBellows(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide) return;
        // Previous versions saved bellows as ordinary blocks, without any block entity.
        var sections = chunk.getSections();
        var cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (!section.maybeHas(state -> state.getBlock() instanceof BlockBellows)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!(section.getBlockState(x, y, z).getBlock() instanceof BlockBellows)) continue;
                        cursor.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                        if (chunk.getBlockEntity(cursor) == null) {
                            chunk.getBlockEntity(cursor.immutable(), LevelChunk.EntityCreationType.IMMEDIATE);
                            chunk.setUnsaved(true);
                        }
                    }
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntityBellows bellows) {
        if (!bellows.bellowsEnabled()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (BellowsStroke.startsSqueeze(gameTime, pos)) {
            RandomSource random = level.getRandom();
            level.playSound(null, pos, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, PUFF_VOLUME, PUFF_BASE_PITCH + (random.nextFloat() - random.nextFloat()) * PUFF_PITCH_SPREAD);
        }
        if (gameTime % BOOST_INTERVAL == 0) {
            bellows.boostFurnace(level, pos.relative(state.getValue(BlockBellows.FACING)));
        }
    }

    private void boostFurnace(Level level, BlockPos target) {
        if (!(level.getBlockEntity(target) instanceof AbstractFurnaceBlockEntity furnace)) return;
        BlockState furnaceState = furnace.getBlockState();
        if (!furnaceState.hasProperty(AbstractFurnaceBlock.LIT) || !furnaceState.getValue(AbstractFurnaceBlock.LIT)) return;
        AbstractFurnaceBlockEntityAccessor access = (AbstractFurnaceBlockEntityAccessor) furnace;
        int progress = access.thaumaturge$getCookTime();
        if (progress > 0 && progress < access.thaumaturge$getCookTimeTotal() - BOOST_MARGIN) {
            access.thaumaturge$setCookTime(progress + BOOST_STEP);
            furnace.setChanged();
        }
    }

    public float inflation(float partialTick) {
        if (level == null || !bellowsEnabled()) {
            return BellowsStroke.FULL;
        }
        return BellowsStroke.inflation(level.getGameTime(), partialTick, worldPosition);
    }

    public Direction bellowsFacing() {
        return getBlockState().getValue(BlockBellows.FACING);
    }

    public boolean bellowsEnabled() {
        return getBlockState().getValue(BlockBellows.ENABLED);
    }
}
