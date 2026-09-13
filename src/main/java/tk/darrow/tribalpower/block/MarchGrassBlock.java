package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/** Grass that dies and spreads onto March soil, never Overworld dirt. */
public class MarchGrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock {
    public static final MapCodec<MarchGrassBlock> CODEC = simpleCodec(MarchGrassBlock::new);

    public MarchGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MarchGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStay(state, level, pos)) {
            if (!level.isAreaLoaded(pos, 1)) return;
            level.setBlockAndUpdate(pos, ModBlocks.MARCH_SOIL.get().defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        BlockState grass = defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(target).is(ModBlocks.MARCH_SOIL.get()) && canSpreadTo(grass, level, target)) {
                level.setBlockAndUpdate(target, grass.setValue(SNOWY, level.getBlockState(target.above()).is(Blocks.SNOW)));
            }
        }
    }

    private static boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(Blocks.SNOW) && cover.getValue(SnowLayerBlock.LAYERS) == 1) return true;
        if (cover.getFluidState().getAmount() == 8) return false;
        int blocked = LightEngine.getLightBlockInto(level, state, pos, cover, above, Direction.UP, cover.getLightBlock(level, above));
        return blocked < level.getMaxLightLevel();
    }

    private static boolean canSpreadTo(BlockState grass, LevelReader level, BlockPos pos) {
        return canStay(grass, level, pos) && !level.getFluidState(pos).is(FluidTags.WATER);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        BlockState leaf = ModBlocks.MARCH_LEAF.get().defaultBlockState();
        for (int i = 0; i < 128; i++) {
            BlockPos cursor = above;
            for (int j = 0; j < i / 16; j++) {
                cursor = cursor.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
            }
            if (level.getBlockState(cursor).isAir() && leaf.canSurvive(level, cursor)
                    && level.getBlockState(cursor.below()).is(this)) {
                level.setBlock(cursor, leaf, 3);
            }
        }
    }
}
