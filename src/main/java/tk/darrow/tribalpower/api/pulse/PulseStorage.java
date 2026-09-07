package tk.darrow.tribalpower.api.pulse;

import net.minecraft.nbt.CompoundTag;

/**
 * Simple mutable pulse buffer used by block entities.
 */
public class PulseStorage implements PulseHandler {
    private int stored;
    private final int capacity;

    public PulseStorage(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    @Override
    public int getPulseStored() {
        return stored;
    }

    @Override
    public int getPulseCapacity() {
        return capacity;
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, capacity - stored);
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, stored);
        if (!simulate) {
            stored -= taken;
        }
        return taken;
    }

    public void setStored(int value) {
        stored = Math.max(0, Math.min(capacity, value));
    }

    public void save(CompoundTag tag) {
        tag.putInt("Pulse", stored);
    }

    public void load(CompoundTag tag) {
        setStored(tag.getInt("Pulse"));
    }
}
