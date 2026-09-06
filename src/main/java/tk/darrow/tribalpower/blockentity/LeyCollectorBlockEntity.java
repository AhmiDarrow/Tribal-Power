package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;

/**
 * Slow ambient Spirit Pulse siphon — steady mid-game generation without constant drumming.
 */
public class LeyCollectorBlockEntity extends BlockEntity implements PulseHandler {
    public static final int CAPACITY = 2000;
    public static final int GAIN_INTERVAL = 40;
    public static final int GAIN_AMOUNT = 2;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int tickCounter;

    public LeyCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEY_COLLECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeyCollectorBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < GAIN_INTERVAL) {
            return;
        }
        be.tickCounter = 0;
        if (be.insertPulse(GAIN_AMOUNT, false) > 0) {
            be.setChanged();
        }
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
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        tickCounter = tag.getInt("TickCounter");
    }
}
