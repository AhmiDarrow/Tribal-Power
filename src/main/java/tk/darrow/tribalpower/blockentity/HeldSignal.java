package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Whether redstone holds a block, remembered between neighbour changes. A hopper or pipe asks a container about every
 * slot it tries, and a machine that runs every tick asks every tick; each ask used to read all six neighbours' signals
 * again (54 times over for a cache). The block's {@code neighborChanged} calls {@link #forget}, which is how a signal
 * reaches a block at all, so one that comes or goes is still seen at once; the reading also lapses after
 * {@code lifetime} ticks in case something changed a neighbour without telling it.
 */
final class HeldSignal {
    private final int lifetime;
    private long readAt = Long.MIN_VALUE;
    private boolean held;

    /** Read again every tick, or sooner on a neighbour change. */
    HeldSignal() {
        this(1);
    }

    HeldSignal(int lifetime) {
        this.lifetime = Math.max(1, lifetime);
    }

    boolean get(Level level, BlockPos pos) {
        long now = level.getGameTime();
        if (readAt == Long.MIN_VALUE || now - readAt >= lifetime || now < readAt) {
            held = level.hasNeighborSignal(pos);
            readAt = now;
        }
        return held;
    }

    void forget() {
        readAt = Long.MIN_VALUE;
    }
}
