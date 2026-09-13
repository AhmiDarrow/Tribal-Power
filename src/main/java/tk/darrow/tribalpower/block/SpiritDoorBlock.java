package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Gateway frame — walk the middle; lintel and lamp are spirit, not collision. */
public class SpiritDoorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SpiritDoorBlock> CODEC = simpleCodec(SpiritDoorBlock::new);
    /** Posts on the east/west edges; opening 0.75 so a player (0.6) fits north-south. */
    private static final VoxelShape NS = Shapes.or(
            Block.box(0.0, 0.0, 5.0, 2.0, 16.0, 11.0),
            Block.box(14.0, 0.0, 5.0, 16.0, 16.0, 11.0)
    );
    /** Posts on the north/south edges; opening east-west. */
    private static final VoxelShape EW = Shapes.or(
            Block.box(5.0, 0.0, 0.0, 11.0, 16.0, 2.0),
            Block.box(5.0, 0.0, 14.0, 11.0, 16.0, 16.0)
    );

    public SpiritDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<SpiritDoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }
}
