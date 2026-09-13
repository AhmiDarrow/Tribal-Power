package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;

/** Shared spawn footing so March fauna can stand on turf, moss, soil, and snow. */
public final class MarchSpawns {
    private MarchSpawns() {}

    public static boolean turf(BlockState below) {
        return below.is(BlockTags.DIRT)
                || below.is(ModBlocks.MARCH_GRASS.get())
                || below.is(ModBlocks.MARCH_MOSS.get())
                || below.is(ModBlocks.MARCH_SOIL.get())
                || below.is(BlockTags.SNOW)
                || below.is(BlockTags.ICE)
                || below.is(Blocks.SNOW_BLOCK)
                || below.is(Blocks.POWDER_SNOW);
    }

    public static boolean wispFooting(BlockState below) {
        return turf(below)
                || below.is(ModBlocks.MARCH_STONE.get())
                || below.is(ModBlocks.MARCH_COBBLE.get())
                || below.is(ModBlocks.MARCH_CRYSTAL.get());
    }

    public static boolean animal(
            EntityType<LatticeAnimal> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random
    ) {
        BlockState below = level.getBlockState(pos.below());
        return turf(below) && level.getRawBrightness(pos, 0) > 8 && level.getFluidState(pos).isEmpty();
    }

    public static boolean monster(
            EntityType<LatticeMonster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random
    ) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!Monster.isDarkEnoughToSpawn(level, pos, random)) {
            return false;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return turf(below) || wispFooting(below) || below.isValidSpawn(level, belowPos, type);
    }
}
