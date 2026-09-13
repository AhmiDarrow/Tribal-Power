package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where the Calls are. An Answer must not scan every block entity in range; a Call registers on load
 * and forgets itself on remove, the same way a Wake Bell does.
 */
public final class VerseCalls {
    private static final Map<ServerLevel, Set<BlockPos>> CALLS = new WeakHashMap<>();

    private VerseCalls() {}

    public static void add(ServerLevel level, BlockPos pos) {
        CALLS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> calls = CALLS.get(level);
        if (calls != null) calls.remove(pos);
    }

    public static int hear(ServerLevel level, BlockPos listener, int verse) {
        Set<BlockPos> calls = CALLS.get(level);
        if (calls == null || calls.isEmpty()) return 0;
        int best = 0;
        long rangeSq = (long) VerseLinkBlockEntity.RANGE * VerseLinkBlockEntity.RANGE;
        List<BlockPos> stale = null;
        for (BlockPos pos : calls) {
            if (pos.distSqr(listener) > rangeSq) continue;
            if (!(level.getBlockEntity(pos) instanceof VerseLinkBlockEntity call)
                    || call.isRemoved() || !call.call()) {
                if (stale == null) stale = new ArrayList<>();
                stale.add(pos);
                continue;
            }
            if (call.verse() != verse) continue;
            best = Math.max(best, call.heard());
            if (best >= 15) break;
        }
        if (stale != null) for (BlockPos pos : stale) calls.remove(pos);
        return best;
    }
}
