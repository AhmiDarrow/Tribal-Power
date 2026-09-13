package tk.darrow.tribalpower.logic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Six-way song wire. Power decays by one per block, 0 to 15. */
public class SongThreadBlock extends Block {
    public static final MapCodec<SongThreadBlock> CODEC = simpleCodec(SongThreadBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final VoxelShape SHAPE = Block.box(5, 5, 5, 11, 11, 11);

    public SongThreadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWER); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean isSignalSource(BlockState state) { return true; }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWER);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int next = strength(level, pos);
        if (next != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, next), 3);
        }
    }

    static int strength(Level level, BlockPos pos) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState other = level.getBlockState(neighbor);
            if (other.getBlock() instanceof SongThreadBlock) {
                best = Math.max(best, other.getValue(POWER) - 1);
            } else if (other.getBlock() instanceof SongVineBlock
                    && level.getBlockEntity(neighbor) instanceof SongVineBlockEntity vine) {
                best = Math.max(best, vine.maxPower() - 1);
            } else {
                best = Math.max(best, level.getSignal(neighbor, direction));
            }
        }
        return Math.clamp(best, 0, 15);
    }
}
