package tk.darrow.tribalpower.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * One cell of a {@link RitualPattern}. Every predicate carries its own description so a failed match
 * can say what was wanted rather than only where it looked (design 3.1 §4).
 */
public interface BlockPredicate {
    boolean test(Level level, BlockPos pos);

    /** What this cell wanted, e.g. "an anchor stone". Used verbatim in miss messages. */
    Component description();

    /** Cells that accept anything are never reported as misses and never drawn as ghosts. */
    default boolean trivial() { return false; }
}
