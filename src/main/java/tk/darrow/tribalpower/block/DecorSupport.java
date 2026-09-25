package tk.darrow.tribalpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a decoration can hang or stand. A full sturdy face was too strict: a sconce refused a fence post, a
 * charm refused a chain or a leaf canopy, a shelf refused a wall. Anything with a body to hold on to will do;
 * only air, water, grass and the like (whatever a placement would simply replace) will not.
 */
public final class DecorSupport {
    private DecorSupport() {}

    /** True when the block at {@code support} can hold a decoration against its {@code face}. */
    public static boolean holds(LevelReader level, BlockPos support, Direction face) {
        BlockState state = level.getBlockState(support);
        if (state.isAir() || state.canBeReplaced()) return false;
        return state.isFaceSturdy(level, support, face, SupportType.CENTER)
                || !state.getCollisionShape(level, support).isEmpty();
    }
}
