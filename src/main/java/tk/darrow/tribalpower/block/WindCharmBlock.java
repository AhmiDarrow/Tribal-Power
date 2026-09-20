package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hangs from a sturdy ceiling. No collision — walk under it. */
public class WindCharmBlock extends Block {
    public static final MapCodec<WindCharmBlock> CODEC = simpleCodec(WindCharmBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.0, 2.0, 5.0, 11.0, 16.0, 11.0);

    public WindCharmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.UP && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.02, -0.01, (random.nextDouble() - 0.5) * 0.02);
        }
        // A charm that never sounds is only a shape hanging from a beam. Rare and quiet, so a camp
        // full of them stays somewhere you want to stand.
        if (random.nextInt(220) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                    0.25F, 0.8F + random.nextFloat() * 0.6F, false);
        }
    }

    /** Nudge it and it rings. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        level.playSound(player, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                0.5F, 0.9F + level.getRandom().nextFloat() * 0.4F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
