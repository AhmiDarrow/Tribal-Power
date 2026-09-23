package tk.darrow.tribalpower.api.pulse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.ley.LeyMath;

/**
 * What a block is making a second. The Codex diagnosis and the Ley Lens both report this number, so it is
 * worked out in one place: two readouts that disagreed about the same Drumheart would be worse than none.
 *
 * <p>The three cases that are not a plain {@link PulseGenerator} each measure themselves differently — a
 * collector by the ley strength under it, a resonator by its fuel and voices — so they are asked directly.
 */
public final class PulseRate {
    private PulseRate() {}

    /** Pulse a second, or 0 for anything that stores rather than makes (and for a stilled generator). */
    public static int perSecond(ServerLevel level, BlockPos pos, BlockEntity be) {
        if (be instanceof LeyCollectorBlockEntity collector) {
            if (level.hasNeighborSignal(pos)) return 0;
            int gain = LeyCollectorBlockEntity.beatFor(LeyMath.gain(level, pos));
            gain += MachineRank.bonusGain(collector, gain);
            return (int) Math.round(gain * 20.0 / LeyCollectorBlockEntity.GAIN_INTERVAL);
        }
        if (be instanceof PulseResonatorBlockEntity resonator) {
            return level.hasNeighborSignal(pos) ? 0 : resonator.getGain();
        }
        // A Drumheart reports what its last beat was worth; the six voices report their steady rate.
        return be instanceof PulseGenerator generator ? Math.max(0, generator.currentOutput()) : 0;
    }

    /**
     * Pulse a second this block is trying to spend. Zero for generators, stores, and anything a lever has stopped.
     * Starved machines still count: the lens is how you see that out has outgrown in.
     */
    public static int drawPerSecond(ServerLevel level, BlockPos pos, BlockEntity be) {
        if (!(be instanceof PulseSpend spend) || level.hasNeighborSignal(pos)) return 0;
        return Math.max(0, spend.spendPerSecond());
    }
}
