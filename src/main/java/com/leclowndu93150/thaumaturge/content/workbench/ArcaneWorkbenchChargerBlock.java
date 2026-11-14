package com.leclowndu93150.thaumaturge.content.workbench;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ArcaneWorkbenchChargerBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(box(0.0, 0.0, 0.0, 2.0, 11.0, 2.0), box(14.0, 0.0, 0.0, 16.0, 11.0, 2.0), box(0.0, 0.0, 14.0, 2.0, 11.0, 16.0),
            box(14.0, 0.0, 14.0, 16.0, 11.0, 16.0), box(6.0, 9.0, 6.0, 10.0, 15.0, 10.0), box(1.0, 11.0, 1.0, 3.0, 13.0, 3.0), box(3.0, 11.0, 3.0, 5.0, 13.0, 5.0), box(5.0, 11.0, 5.0, 7.0, 13.0, 7.0),
            box(13.0, 11.0, 1.0, 15.0, 13.0, 3.0), box(11.0, 11.0, 3.0, 13.0, 13.0, 5.0), box(9.0, 11.0, 5.0, 11.0, 13.0, 7.0), box(1.0, 11.0, 13.0, 3.0, 13.0, 15.0),
            box(3.0, 11.0, 11.0, 5.0, 13.0, 13.0), box(5.0, 11.0, 9.0, 7.0, 13.0, 11.0), box(13.0, 11.0, 13.0, 15.0, 13.0, 15.0), box(11.0, 11.0, 11.0, 13.0, 13.0, 13.0),
            box(9.0, 11.0, 9.0, 11.0, 13.0, 11.0));

    public ArcaneWorkbenchChargerBlock() {
        super(Properties.of().strength(2).noOcclusion());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(TTBlockTags.WORKBENCH_CHARGER_HOSTS);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos bench = pos.below();
        if (!canSurvive(state, level, pos) || !level.isClientSide && !TTAttachments.knowledge(player).isComplete(TTIds.rl("workbench_charger"))) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(bench).use(level, player, hand, hit.withPosition(bench));
    }
}
