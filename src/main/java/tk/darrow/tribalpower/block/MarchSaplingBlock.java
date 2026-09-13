package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Grows the March tree on March turf and farmland. */
public class MarchSaplingBlock extends SaplingBlock {
    public static final MapCodec<MarchSaplingBlock> CODEC = simpleCodec(MarchSaplingBlock::new);

    public MarchSaplingBlock(Properties properties) {
        super(ModBlocks.MARCH_GROWER, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<SaplingBlock> codec() {
        return (MapCodec<SaplingBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.MARCH_FARMLAND.get()) || super.mayPlaceOn(state, level, pos);
    }
}
