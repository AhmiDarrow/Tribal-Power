package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Gateway frame — walk the middle; the model is posts, lintel, and a spirit lamp. */
public class SpiritDoorBlock extends Block {
    public static final MapCodec<SpiritDoorBlock> CODEC = simpleCodec(SpiritDoorBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 5.0, 5.0, 16.0, 11.0),
            Block.box(11.0, 0.0, 5.0, 14.0, 16.0, 11.0),
            Block.box(5.0, 13.0, 5.0, 11.0, 16.0, 11.0),
            Block.box(7.0, 2.0, 7.0, 9.0, 13.0, 9.0)
    );

    public SpiritDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SpiritDoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
