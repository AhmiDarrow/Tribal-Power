package tk.darrow.tribalpower.logic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Surface tendril. One block can carry a vine on every face, crawling around corners the way a
 * Pattern-weaver's song follows the stone. Power decays by one per step, 0 to 15.
 */
public class SongVineBlock extends BaseEntityBlock {
    public static final MapCodec<SongVineBlock> CODEC = simpleCodec(SongVineBlock::new);
    public static final BooleanProperty[] FACES = {
            BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST
    };
    private static final VoxelShape[] SLABS = {
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, 2),
            Block.box(0, 0, 14, 16, 16, 16),
            Block.box(0, 0, 0, 2, 16, 16),
            Block.box(14, 0, 0, 16, 16, 16)
    };
    private static final VoxelShape[] COMBINED = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, SLABS[i]);
            COMBINED[mask] = shape.isEmpty() ? SLABS[0] : shape;
        }
    }

    public SongVineBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty face : FACES) state = state.setValue(face, false);
        registerDefaultState(state);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACES);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SongVineBlockEntity(pos, state); }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LogicRegistry.SONG_VINE_TYPE.get(), SongVineBlockEntity::tick);
    }

    public static BooleanProperty property(Direction face) { return FACES[face.ordinal()]; }
    public static boolean has(BlockState state, Direction face) { return state.getValue(property(face)); }
    public static int mask(BlockState state) {
        int bits = 0;
        for (int i = 0; i < 6; i++) if (state.getValue(FACES[i])) bits |= 1 << i;
        return bits;
    }
    public static int count(BlockState state) { return Integer.bitCount(mask(state)); }

    public static BlockState withFace(BlockState state, Direction face, boolean present) {
        return state.setValue(property(face), present);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COMBINED[mask(state)];
    }

    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof SongVineBlockEntity vine)) return 0;
        int best = vine.power(side.getOpposite());
        for (Direction face : Direction.values()) {
            if (face.getAxis() == side.getAxis()) continue;
            if (has(state, face)) best = Math.max(best, vine.power(face));
        }
        return best;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction attach = context.getClickedFace().getOpposite();
        BlockPos vinePos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(vinePos);
        BlockState base = existing.is(this) ? existing : defaultBlockState();
        if (!canAttach(context.getLevel(), vinePos, attach)) return null;
        return withFace(base, attach, true);
    }

    public static boolean canAttach(LevelReader level, BlockPos vinePos, Direction attach) {
        BlockPos support = vinePos.relative(attach);
        return level.getBlockState(support).isFaceSturdy(level, support, attach.getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction face : Direction.values()) if (has(state, face) && canAttach(level, pos, face)) return true;
        return false;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        BlockState next = state;
        for (Direction face : Direction.values()) {
            if (has(next, face) && !canAttach(level, pos, face)) {
                next = withFace(next, face, false);
                Block.popResource(level, pos, new ItemStack(asItem()));
            }
        }
        if (mask(next) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SongVineBlockEntity vine)) return;
        if (vine.recompute(level, pos, state)) {
            level.updateNeighborsAt(pos, this);
            for (Direction face : Direction.values()) level.updateNeighborsAt(pos.relative(face), this);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        int n = count(state);
        return n <= 0 ? List.of() : List.of(new ItemStack(asItem(), n));
    }

    static int incoming(Level level, BlockPos pos, Direction face, Direction along) {
        BlockPos neighbor = pos.relative(along);
        BlockState other = level.getBlockState(neighbor);
        if (other.getBlock() instanceof SongVineBlock && has(other, face)
                && level.getBlockEntity(neighbor) instanceof SongVineBlockEntity vine) {
            return vine.power(face) - 1;
        }
        if (other.getBlock() instanceof SongVineBlock && has(other, along.getOpposite())
                && level.getBlockEntity(neighbor) instanceof SongVineBlockEntity vine) {
            return vine.power(along.getOpposite()) - 1;
        }
        BlockPos around = neighbor.relative(face);
        BlockState wrap = level.getBlockState(around);
        if (wrap.getBlock() instanceof SongVineBlock && has(wrap, along.getOpposite())
                && level.getBlockEntity(around) instanceof SongVineBlockEntity vine) {
            return vine.power(along.getOpposite()) - 1;
        }
        if (other.getBlock() instanceof SongThreadBlock) return other.getValue(SongThreadBlock.POWER) - 1;
        return level.getSignal(neighbor, along);
    }
}
