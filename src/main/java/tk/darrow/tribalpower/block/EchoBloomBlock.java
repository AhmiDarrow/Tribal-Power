package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ambient March flora that sheds spirit motes for atmosphere.
 */
public class EchoBloomBlock extends MarchPlantBlock {
    public static final MapCodec<EchoBloomBlock> CODEC = simpleCodec(EchoBloomBlock::new);

    public EchoBloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double y = pos.getY() + 0.4 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.2,
                    0.05,
                    (random.nextDouble() - 0.5) * 0.2
            );
        }
    }
}
