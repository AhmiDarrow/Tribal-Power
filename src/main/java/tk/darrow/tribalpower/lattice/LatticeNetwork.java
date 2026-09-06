package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

/**
 * Totem Lattice scaffolding — counts and (later) pathfinds harmonic links between totems.
 * Full line-of-sight / ritual chalk linking lands in a later phase.
 */
public final class LatticeNetwork {
    private LatticeNetwork() {}

    public static int countNearbyTotems(Level level, BlockPos origin, int radius) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof ResonanceTotemBlockEntity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Stub: future path of harmonics for Echo-stage material routing.
     */
    public static boolean canRoute(Level level, BlockPos from, BlockPos to) {
        return from.closerThan(to, 16.0);
    }
}
