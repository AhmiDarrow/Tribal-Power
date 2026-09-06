package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import tk.darrow.tribalpower.api.pulse.Attunement;

/**
 * Immutable record of a chalk or LOS link between two Resonance Totems.
 */
public record TotemLink(BlockPos from, BlockPos to, Attunement preferred) {
}
