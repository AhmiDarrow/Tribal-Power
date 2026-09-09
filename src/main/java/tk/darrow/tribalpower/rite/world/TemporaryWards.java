package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Still Night wards: temporary hostile-spawn suppression that the Hush ward logic in
 * {@code camp/CampHooks.warded} consults. Backed by {@link RiteSavedData} so a ward
 * survives a restart and expires on schedule.
 */
public final class TemporaryWards {
    private TemporaryWards() {}

    public static void add(ServerLevel level, BlockPos center, int radius, int durationTicks) {
        RiteSavedData.get(level.getServer()).ward(level, center, radius, level.getGameTime() + durationTicks);
    }

    /** True when {@code target} lies inside any live temporary ward of this level. */
    public static boolean warded(ServerLevel level, BlockPos target) {
        for (RiteSavedData.Ward ward : RiteSavedData.get(level.getServer()).wards(level)) {
            long r = ward.radius();
            if (ward.center().distSqr(target) <= r * r) return true;
        }
        return false;
    }

    /** Remaining ticks on the longest ward covering {@code target}, or 0. */
    public static long remaining(ServerLevel level, BlockPos target) {
        long best = 0;
        for (RiteSavedData.Ward ward : RiteSavedData.get(level.getServer()).wards(level)) {
            long r = ward.radius();
            if (ward.center().distSqr(target) <= r * r) best = Math.max(best, ward.expiry() - level.getGameTime());
        }
        return best;
    }
}
