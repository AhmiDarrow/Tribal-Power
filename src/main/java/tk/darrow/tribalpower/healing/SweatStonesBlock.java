package tk.darrow.tribalpower.healing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sweat Stones: a heap of river stones that steam when they sit over heat. Sleep the night through under a roof
 * near hot stones and you wake cleansed and blessed -- a sweat lodge.
 */
public class SweatStonesBlock extends Block {
    public static final MapCodec<SweatStonesBlock> CODEC = simpleCodec(SweatStonesBlock::new);
    public static final BooleanProperty HOT = BooleanProperty.create("hot");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 7, 15);

    public SweatStonesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HOT, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(HOT); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HOT, SpiritKettleBlockEntity.heated(context.getLevel(), context.getClickedPos()));
    }

    /** Placed any other way than by hand -- a structure, a command -- the stones still check for heat. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        super.onPlace(state, level, pos, previous, moved);
        boolean hot = SpiritKettleBlockEntity.heated(level, pos);
        if (!level.isClientSide && state.getValue(HOT) != hot) level.setBlock(pos, state.setValue(HOT, hot), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && level instanceof Level real)
            return state.setValue(HOT, SpiritKettleBlockEntity.heated(real, pos));
        return state;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HOT)) return;
        for (int i = 0; i < 2; i++)
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.5,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
    }
}
