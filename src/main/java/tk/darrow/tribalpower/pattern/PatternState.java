package tk.darrow.tribalpower.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A block entity's cached view of its own pattern (design 3.1 section 4).
 *
 * <p>Matching costs on the order of a hundred block reads, so it runs at most every
 * {@link #REVALIDATE_TICKS} ticks -- staggered per position so a base full of devices never checks in
 * the same tick -- and immediately after a neighbour change lands inside the pattern's bounding box.
 */
public final class PatternState {
    public static final int REVALIDATE_TICKS = 40;

    private final RitualPattern pattern;
    private PatternMatcher.Match cached;
    private long nextCheck = Long.MIN_VALUE;
    private boolean dirty = true;

    public PatternState(RitualPattern pattern) {
        this.pattern = pattern;
    }

    public RitualPattern pattern() { return pattern; }

    /** Last known match without touching the world; null until the first {@link #get}. */
    public PatternMatcher.Match cached() { return cached; }

    /** The current match, re-running it when the cache is stale or dirty. */
    public PatternMatcher.Match get(Level level, BlockPos anchor) {
        long now = level.getGameTime();
        if (dirty || cached == null || now >= nextCheck) {
            cached = PatternMatcher.match(level, anchor, pattern);
            dirty = false;
            // Stagger by position so neighbouring devices never revalidate on the same tick.
            nextCheck = now + REVALIDATE_TICKS + Math.floorMod(anchor.asLong(), REVALIDATE_TICKS);
        }
        return cached;
    }

    /** True when a complete match of at least {@code minimumTier} is in place. */
    public boolean satisfied(Level level, BlockPos anchor, int minimumTier) {
        PatternMatcher.Match match = get(level, anchor);
        return match.found() && match.tier() >= minimumTier;
    }

    /** Matched tier, or 0 when the pattern is broken. */
    public int tier(Level level, BlockPos anchor) {
        PatternMatcher.Match match = get(level, anchor);
        return match.found() ? match.tier() : 0;
    }

    public void invalidate() {
        dirty = true;
    }

    /** Invalidate only when {@code changed} is a block this pattern actually cares about. */
    public void onNeighbourChanged(BlockPos anchor, BlockPos changed) {
        BoundingBox box = pattern.boundsAround(anchor);
        if (box.isInside(changed)) dirty = true;
    }
}
