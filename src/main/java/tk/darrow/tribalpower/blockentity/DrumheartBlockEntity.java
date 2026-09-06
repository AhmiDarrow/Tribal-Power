package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;

public class DrumheartBlockEntity extends BlockEntity implements PulseHandler {
    public static final int CAPACITY = 1000;
    public static final int BEAT_GAIN = 10;
    public static final int REDSTONE_GAIN = 5;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int redstoneCooldown;

    public DrumheartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRUMHEART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrumheartBlockEntity be) {
        if (be.redstoneCooldown > 0) {
            be.redstoneCooldown--;
        }
    }

    public int drumBeat() {
        int gained = insertPulse(BEAT_GAIN, false);
        setChanged();
        return gained;
    }

    public void onRedstonePulse() {
        if (redstoneCooldown > 0) {
            return;
        }
        redstoneCooldown = 8;
        insertPulse(REDSTONE_GAIN, false);
        setChanged();
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
        tag.putInt("RedstoneCooldown", redstoneCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        redstoneCooldown = tag.getInt("RedstoneCooldown");
    }
}
