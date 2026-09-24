package tk.darrow.tribalpower.cuisine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A March crop: four stages of growth, its own harvest for a seed, at home on tilled ground and, grown wild, on
 * the bare ground of its own country. Vanilla's hoe, the Spiritgear Hoe and the Sprout boon all treat it as any
 * other crop.
 */
public class MarchCropBlock extends CropBlock {
    public static final MapCodec<MarchCropBlock> CODEC = simpleCodec(properties -> new MarchCropBlock(MarchCrop.STEPPE_GRAIN, properties));
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 0, 0, 16, 12, 16), Block.box(0, 0, 0, 16, 16, 16)};

    public final MarchCrop crop;

    public MarchCropBlock(MarchCrop crop, Properties properties) {
        super(properties);
        this.crop = crop;
    }

    @Override public MapCodec<MarchCropBlock> codec() { return CODEC; }
    @Override protected IntegerProperty getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return 3; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override protected ItemLike getBaseSeedId() { return CuisineRegistry.CROP_ITEMS.get(crop).get(); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    /** Tilled ground for a farmer; grown wild, the plain ground of the March will do. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.world.level.block.Blocks.FARMLAND) || state.is(BlockTags.DIRT)
                || state.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get()) || state.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_SOIL.get())
                || state.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_MOSS.get()) || state.is(net.minecraft.world.level.block.Blocks.SAND);
    }
}
