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

/** Camp furniture: stool, table, urn. Optional facing for the table's grain. */
public class CampDecorBlock extends HorizontalDirectionalBlock {
    public enum Kind {
        STOOL(Shapes.or(Block.box(4, 0, 4, 6, 8, 6), Block.box(10, 0, 4, 12, 8, 6),
                Block.box(4, 0, 10, 6, 8, 12), Block.box(10, 0, 10, 12, 8, 12),
                Block.box(3, 8, 3, 13, 10, 13))),
        TABLE(Shapes.or(Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3),
                Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15),
                Block.box(0, 13, 0, 16, 16, 16))),
        URN(Shapes.or(Block.box(4, 0, 4, 12, 10, 12), Block.box(5, 10, 5, 11, 12, 11),
                Block.box(6, 12, 6, 10, 14, 10)));

        public final VoxelShape shape;

        Kind(VoxelShape shape) {
            this.shape = shape;
        }
    }

    private final Kind kind;
    private final MapCodec<CampDecorBlock> codec;

    public CampDecorBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new CampDecorBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape;
    }
}
