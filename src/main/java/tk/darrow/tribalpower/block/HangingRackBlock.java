package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A rail high on a wall with four copper hooks, for pots, tools and bundles of herbs. It works like the
 * {@link WallShelfBlock} it extends (the same four places, the same hands-only use, a comparator reading),
 * only what you put up hangs below the rail instead of lying on a board.
 *
 * <p>The outline reaches down over the hooks' hanging space so a click on a hanging pot takes that pot; the
 * collision is the rail alone, so you walk under the rack rather than bump into what hangs from it.
 */
public class HangingRackBlock extends WallShelfBlock {
    public static final MapCodec<HangingRackBlock> CODEC = simpleCodec(HangingRackBlock::new);
    // FACING north: the wall is south, at z = 16. Rail and hooks, then the space the items hang in.
    private static final VoxelShape[] RAIL = rotations(Block.box(0, 10, 10, 16, 16, 16));
    private static final VoxelShape[] OUTLINE = rotations(Shapes.or(Block.box(0, 10, 10, 16, 16, 16), Block.box(0, 3, 8, 16, 10, 16)));

    public HangingRackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** North, south, west, east from a north-facing shape. */
    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] out = new VoxelShape[4];
        out[0] = north;
        out[1] = turn(north, (x0, z0, x1, z1) -> new double[]{1 - x1, 1 - z1, 1 - x0, 1 - z0});
        out[2] = turn(north, (x0, z0, x1, z1) -> new double[]{z0, 1 - x1, z1, 1 - x0});
        out[3] = turn(north, (x0, z0, x1, z1) -> new double[]{1 - z1, x0, 1 - z0, x1});
        return out;
    }

    private interface Turn { double[] apply(double x0, double z0, double x1, double z1); }

    private static VoxelShape turn(VoxelShape shape, Turn turn) {
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double[] r = turn.apply(x0, z0, x1, z1);
            out[0] = Shapes.or(out[0], Shapes.box(r[0], y0, r[1], r[2], y1, r[3]));
        });
        return out[0].optimize();
    }

    private static int index(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE[index(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RAIL[index(state)];
    }
}
