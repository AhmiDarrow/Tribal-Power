package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spirit crystal growth found on The March surface and shallow caves.
 */
public class MarchCrystalBlock extends Block {
    public static final MapCodec<MarchCrystalBlock> CODEC = simpleCodec(MarchCrystalBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public MarchCrystalBlock(BlockBehaviour.Properties properties) {
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(
                    ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    0.0, 0.02, 0.0
            );
        }
    }
}
