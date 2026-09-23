package tk.darrow.tribalpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * March Quartz: the clear crystal of the Glimmer Ridge. It grows from a floor or a ceiling, and its
 * blockstate picks one of the ridge's formations at random, so a seam of it is never two of the same.
 *
 * <p>No collision — you walk through a crystal bed rather than tripping over it — and it waterlogs,
 * because the ridge's springs run straight through the seams.
 */
public class MarchQuartzBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.VERTICAL_DIRECTION;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape UP_SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private static final VoxelShape DOWN_SHAPE = Block.box(2, 3, 2, 14, 16, 14);

    public MarchQuartzBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? DOWN_SHAPE : UP_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Clicking the underside of a block hangs it; anything else stands it up.
        Direction face = context.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP;
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(FACING, face).setValue(WATERLOGGED, water);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos anchor = state.getValue(FACING) == Direction.DOWN ? pos.above() : pos.below();
        Direction face = state.getValue(FACING) == Direction.DOWN ? Direction.DOWN : Direction.UP;
        return level.getBlockState(anchor).isFaceSturdy(level, anchor, face);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return !state.canSurvive(level, pos)
                ? (state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
