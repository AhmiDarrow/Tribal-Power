package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import tk.darrow.tribalpower.api.pulse.Attunement;

/**
 * Immutable record of a chalk or proximity link between two Resonance Totems.
 */
public record TotemLink(BlockPos from, BlockPos to, Attunement preferred) {
    public boolean connects(BlockPos a, BlockPos b) {
        return (from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a));
    }

    public BlockPos otherEnd(BlockPos pos) {
        if (from.equals(pos)) {
            return to;
        }
        if (to.equals(pos)) {
            return from;
        }
        return null;
    }
}
