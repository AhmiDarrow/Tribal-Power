package tk.darrow.tribalpower.tribe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Decorative tribe banner: {@code tribe} 0-8, {@code facing}, {@code wall}. Floor-standing pole with cloth or wall-hung.
 */
public class TribeBannerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TribeBannerBlock> CODEC = simpleCodec(TribeBannerBlock::new);
    public static final IntegerProperty TRIBE = IntegerProperty.create("tribe", 0, TribeDefinition.values().length - 1);
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape FLOOR = Block.box(4, 0, 4, 12, 16, 12);
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Map.of(
            Direction.NORTH, Block.box(2, 0, 13, 14, 16, 16),
            Direction.SOUTH, Block.box(2, 0, 0, 14, 16, 3),
            Direction.WEST, Block.box(13, 0, 2, 16, 16, 14),
            Direction.EAST, Block.box(0, 0, 2, 3, 16, 14));

    public TribeBannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIBE, 0).setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TRIBE, FACING, WALL); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(WALL) ? WALL_SHAPES.get(state.getValue(FACING)) : FLOOR;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        TribeDefinition tribe = TribeDefinition.ofOrDefault(ctx.getItemInHand());
        Direction face = ctx.getClickedFace();
        BlockState state = defaultBlockState().setValue(TRIBE, tribe.ordinal());
        if (face.getAxis().isHorizontal()) {
            state = state.setValue(WALL, true).setValue(FACING, face);
            if (state.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return state;
        }
        state = state.setValue(WALL, false).setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return state.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(WALL)) {
            Direction facing = state.getValue(FACING);
            BlockPos behind = pos.relative(facing.getOpposite());
            return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        boolean support = state.getValue(WALL) ? dir == state.getValue(FACING).getOpposite() : dir == Direction.DOWN;
        if (support && !state.canSurvive(level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    public static TribeDefinition tribe(BlockState state) { return TribeDefinition.byOrdinal(state.getValue(TRIBE)); }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(tribe(state).stamped(this)); }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return tribe(state).stamped(this);
    }
}
