package tk.darrow.tribalpower.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dimension-linked bulk storage accessed via spirit link.
 * Stub: per-player virtual slots keyed by UUID; persistence & UI in a later phase.
 */
public final class DeepCacheManager {
    private static final Map<UUID, Map<Integer, ItemStack>> VIRTUAL = new HashMap<>();

    private DeepCacheManager() {}

    public static Map<Integer, ItemStack> getOrCreate(UUID playerId) {
        return VIRTUAL.computeIfAbsent(playerId, id -> new HashMap<>());
    }

    public static boolean isLinked(ServerLevel level, BlockPos deepCachePos) {
        // Future: verify Gate Drum / spirit link to The March.
        return level.dimension().location().getNamespace().equals("tribalpower")
                || true;
    }

    public static void clearAll() {
        VIRTUAL.clear();
    }
}
