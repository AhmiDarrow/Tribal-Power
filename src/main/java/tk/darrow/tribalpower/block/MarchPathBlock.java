package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Path that reverts to March soil, not Overworld dirt. */
public class MarchPathBlock extends DirtPathBlock {
    public static final MapCodec<MarchPathBlock> CODEC = simpleCodec(MarchPathBlock::new);

    public MarchPathBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<DirtPathBlock> codec() {
        return (MapCodec<DirtPathBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? ModBlocks.MARCH_SOIL.get().defaultBlockState()
                : defaultBlockState();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            revert(null, state, level, pos);
        }
    }

    public static void revert(Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState soil = pushEntitiesUp(state, ModBlocks.MARCH_SOIL.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, soil);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, soil));
    }
}
