package tk.darrow.tribalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared conservative landing checks for player transport. */
public final class TravelSafety {
    private TravelSafety() {}

    public static boolean withinBounds(Level level, BlockPos feet) {
        return level.getWorldBorder().isWithinBounds(feet) && feet.getY() > level.getMinBuildHeight()
                && feet.getY() + 2 < level.getMaxBuildHeight();
    }

    public static boolean hasHazard(Level level, BlockPos feet) {
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1,-1,-1), feet.offset(1,1,1))) {
            var state = level.getBlockState(pos);
            boolean litCampfire = (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                    && state.getValue(BlockStateProperties.LIT);
            if (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                    || litCampfire || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.POWDER_SNOW) || state.getFluidState().is(FluidTags.LAVA)) return true;
        }
        return false;
    }
}
