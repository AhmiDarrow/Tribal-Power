package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.sound.ModSounds;

/**
 * Five copper rods and a crystal sail. Hangs from anything above it, or from a bracket on anything beside it
 * (see {@link DecorSupport}). No collision: walk under it, or through it, and it rings.
 *
 * <p>HANGING charms turn with FACING only so a row of them does not look stamped; a wall charm's FACING points
 * away from the block holding its bracket.
 */
public class WindCharmBlock extends Block {
    public static final MapCodec<WindCharmBlock> CODEC = simpleCodec(WindCharmBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape HUNG = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape[] WALL = {
            Shapes.or(HUNG, Block.box(6.5, 12.5, 12.0, 9.5, 16.0, 16.0)), // north: bracket on the south block
            Shapes.or(HUNG, Block.box(6.5, 12.5, 0.0, 9.5, 16.0, 4.0)),   // south
            Shapes.or(HUNG, Block.box(12.0, 12.5, 6.5, 16.0, 16.0, 9.5)), // west
            Shapes.or(HUNG, Block.box(0.0, 12.5, 6.5, 4.0, 16.0, 9.5))    // east
    };

    public WindCharmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, true).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction turn = context.getHorizontalDirection().getOpposite();
        // Whatever the player is looking at first: the ceiling, or the block they clicked the side of.
        for (Direction toward : context.getNearestLookingDirections()) {
            if (toward == Direction.DOWN) continue;
            BlockState state = toward == Direction.UP
                    ? defaultBlockState().setValue(HANGING, true).setValue(FACING, turn)
                    : defaultBlockState().setValue(HANGING, false).setValue(FACING, toward.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
        }
        return null;
    }

    /** The side the charm holds on by: up for a hanging charm, the bracket's wall otherwise. */
    private static Direction host(BlockState state) {
        return state.getValue(HANGING) ? Direction.UP : state.getValue(FACING).getOpposite();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction host = host(state);
        return DecorSupport.holds(level, pos.relative(host), host.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == host(state) && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HANGING)) return HUNG;
        return switch (state.getValue(FACING)) {
            case SOUTH -> WALL[1];
            case WEST -> WALL[2];
            case EAST -> WALL[3];
            default -> WALL[0];
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * How often, on average, a charm rings by itself (in client ticks): rarely on a still day, often in rain and
     * a lot in a storm. Under a roof it is sheltered and rings half as much.
     */
    static int ringEvery(Level level, BlockPos pos) {
        int every = level.isThundering() ? 45 : level.isRaining() ? 110 : 280;
        return level.canSeeSky(pos) ? every : every * 2;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.02, -0.01, (random.nextDouble() - 0.5) * 0.02);
        }
        // A charm that never sounds is only a shape hanging from a beam. Quiet and spaced out, so a camp
        // full of them stays somewhere you want to stand.
        if (random.nextInt(ringEvery(level, pos)) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.WIND_CHARM_GUST.get(), SoundSource.BLOCKS,
                    0.28F + random.nextFloat() * 0.12F, 0.97F + random.nextFloat() * 0.06F, false);
        }
    }

    /** Nudge it and it rings. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        level.playSound(player, pos, ModSounds.WIND_CHARM_GUST.get(), SoundSource.BLOCKS,
                0.6F, 0.97F + level.getRandom().nextFloat() * 0.06F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Walking through it knocks a rod or two. Only now and then, so standing in it is not a racket. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity.position().distanceToSqr(entity.xo, entity.yo, entity.zo) > 1.0E-3
                && server.random.nextInt(12) == 0)
            server.playSound(null, pos, ModSounds.WIND_CHARM_ROD.get(), SoundSource.BLOCKS,
                    0.4F, 0.98F + server.random.nextFloat() * 0.04F);
    }
}
