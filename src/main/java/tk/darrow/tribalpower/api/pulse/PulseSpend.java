package tk.darrow.tribalpower.api.pulse;

/**
 * A block that spends Pulse on a steady beat. The Ley Lens sums these as the zone's out.
 */
public interface PulseSpend {
    /**
     * Pulse this block will try to spend each second. Zero when it is idle, full, or not yet at the spend.
     * A redstone lock is applied by {@link PulseRate#drawPerSecond}, not here.
     */
    int spendPerSecond();
}
