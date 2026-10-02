package tk.darrow.tribalpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A quartz glass pane laid flat: a 2px floor sheet or a 2px ceiling sheet. One cell holds one sheet.
 * Adjacent sheets of the same half join along the shared edge. Standing panes and iron bars do not.
 */
public class HorizontalGlassPaneBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape CEILING = Block.box(0, 14, 0, 16, 16, 16);

    @Nullable
    private final DyeColor dye;

    public HorizontalGlassPaneBlock(@Nullable DyeColor dye, Properties properties) {
        super(properties);
        this.dye = dye;
        registerDefaultState(stateDefinition.any()
                .setValue(HALF, Half.BOTTOM)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == Half.TOP ? CEILING : FLOOR;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    /**
     * Same click as a slab: the top face or the lower half of a side is a floor sheet, the bottom face
     * or the upper half is a ceiling sheet. A cell that already holds a sheet refuses another.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(pos);
        if (existing.getBlock() instanceof HorizontalGlassPaneBlock) return null;
        Direction clicked = context.getClickedFace();
        boolean floor = clicked != Direction.DOWN
                && (clicked == Direction.UP || !(context.getClickLocation().y - (double) pos.getY() > 0.5));
        Half half = floor ? Half.BOTTOM : Half.TOP;
        boolean water = context.getLevel().getFluidState(pos).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(HALF, half).setValue(WATERLOGGED, water);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(side);
            state = state.setValue(side(side), joins(
                    context.getLevel().getBlockState(neighborPos), side, context.getLevel(), neighborPos, half));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (!direction.getAxis().isHorizontal())
            return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return state.setValue(side(direction), joins(neighbor, direction, level, neighborPos, state.getValue(HALF)));
    }

    /** Same half joins any dye or light. A sturdy wall hides the rim. Bars and standing panes do not. */
    private static boolean joins(BlockState neighbor, Direction toward, BlockGetter level, BlockPos neighborPos, Half half) {
        if (neighbor.getBlock() instanceof HorizontalGlassPaneBlock)
            return neighbor.getValue(HALF) == half;
        if (neighbor.getBlock() instanceof IronBarsBlock) return false;
        Direction face = toward.getOpposite();
        return neighbor.isFaceSturdy(level, neighborPos, face, SupportType.FULL)
                || neighbor.isFaceSturdy(level, neighborPos, face, SupportType.CENTER);
    }

    /** Hide the shared side against another sheet of this half. Top and bottom stay; they only meet at an edge. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        if (side.getAxis().isHorizontal()
                && adjacent.getBlock() instanceof HorizontalGlassPaneBlock
                && adjacent.getValue(HALF) == state.getValue(HALF))
            return true;
        return super.skipRendering(state, adjacent, side);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case WATER -> state.getFluidState().is(FluidTags.WATER);
            case LAND, AIR -> false;
        };
    }

    /** Dyed sheets tint a beacon the way a stained pane does. Clear sheets leave the beam alone. */
    @Nullable
    @Override
    public Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
        return dye == null ? null : dye.getTextureDiffuseColor();
    }

    private static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException(direction.getName());
        };
    }
}
