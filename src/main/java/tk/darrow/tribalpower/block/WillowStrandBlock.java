package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A weeping willow's hanging strand. It hangs from leaves or from another strand, falls when what holds it goes,
 * and can be climbed. {@code tip} marks the lowest strand of a curtain; a few tips carry a glowing seed.
 */
public class WillowStrandBlock extends Block {
    public static final MapCodec<WillowStrandBlock> CODEC = simpleCodec(WillowStrandBlock::new);
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final BooleanProperty GLOW = BooleanProperty.create("glow");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public WillowStrandBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true).setValue(GLOW, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP, GLOW);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean holds(BlockState above) {
        return above.is(BlockTags.LEAVES) || above.getBlock() instanceof WillowStrandBlock;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return holds(level.getBlockState(pos.above()));
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState().setValue(TIP, !(below.getBlock() instanceof WillowStrandBlock));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP && !holds(neighbour)) {
            // Fall on the next tick with the usual break effects, so a whole curtain comes down strand by strand.
            level.scheduleTick(pos, this, 1);
            return state;
        }
        if (direction == Direction.DOWN) {
            boolean tip = !(neighbour.getBlock() instanceof WillowStrandBlock);
            return state.setValue(TIP, tip).setValue(GLOW, tip && state.getValue(GLOW));
        }
        return state;
    }
}
