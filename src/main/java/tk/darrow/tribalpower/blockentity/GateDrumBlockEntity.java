package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;

public class GateDrumBlockEntity extends BlockEntity implements PulseHandler {
    public static final int CAPACITY = 200;
    public static final int TRAVEL_COST = 20;
    public static final int CELL_CHARGE = 25;
    public static final int MANUAL_CHARGE = 5;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);

    public GateDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE_DRUM.get(), pos, state);
    }

    public boolean tryConsumeTravelPulse() {
        if (pulse.getPulseStored() < TRAVEL_COST) {
            return false;
        }
        extractPulse(TRAVEL_COST, false);
        return true;
    }

    public int chargeFromCell() {
        return insertPulse(CELL_CHARGE, false);
    }

    public int manualCharge() {
        return insertPulse(MANUAL_CHARGE, false);
    }

    @Override
    public int getPulseStored() {
        return pulse.getPulseStored();
    }

    @Override
    public int getPulseCapacity() {
        return pulse.getPulseCapacity();
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
    }
}
