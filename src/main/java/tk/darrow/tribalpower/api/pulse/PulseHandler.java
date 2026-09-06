package tk.darrow.tribalpower.api.pulse;

/**
 * Native energy API for Tribal Power. Spirit Pulse is rhythmic beats, not FE/RF.
 * Blocks and items that store or consume pulse implement this interface.
 */
public interface PulseHandler {
    int getPulseStored();

    int getPulseCapacity();

    /**
     * @return amount actually inserted
     */
    int insertPulse(int amount, boolean simulate);

    /**
     * @return amount actually extracted
     */
    int extractPulse(int amount, boolean simulate);

    default boolean canReceivePulse() {
        return getPulseStored() < getPulseCapacity();
    }

    default boolean canExtractPulse() {
        return getPulseStored() > 0;
    }
}
