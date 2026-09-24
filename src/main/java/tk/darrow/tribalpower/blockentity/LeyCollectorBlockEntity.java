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
 * Siphons Spirit Pulse from the ley lines that pass through it, with a smaller gift from the land around it.
 */
public class LeyCollectorBlockEntity extends BlockEntity implements PulseHandler, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CAPACITY = 2000;
    /** Collection beat: {@link tk.darrow.tribalpower.ley.LeyMath#factors} gain is added every 40 ticks (two seconds). */
    public static final int GAIN_INTERVAL = 40;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int tickCounter;

    public LeyCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEY_COLLECTOR.get(), pos, state);
    }

    /**
     * Pulse a beat is worth for a site of this ley strength.
     *
     * <p>Site quality still runs 1..{@link tk.darrow.tribalpower.ley.LeyMath#MAX_GAIN}, because the
     * Ley Lens reads that scale to say whether a spot is any good -- raising the cap instead would
     * have quietly told every player their site had got worse. The yield is applied here: a perfect
     * site now pays 48 a beat, which is 24 Pulse a second, against the 8 it used to make. A single
     * Ember Kiln draws 32, so a collector was never going to run one on its own.
     */
    public static final int YIELD = 3;

    public static int beatFor(int gain) { return gain * YIELD; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeyCollectorBlockEntity be) {
        if (level.hasNeighborSignal(pos)) return;
        be.tickCounter++;
        if (be.tickCounter < GAIN_INTERVAL) {
            return;
        }
        be.tickCounter = 0;
        // Factor maths live in ley/LeyMath so the Ley Lens and Codex diagnostics show the same numbers.
        var factors = tk.darrow.tribalpower.ley.LeyMath.factors(level, pos);
        int gain = beatFor(factors.gain());
        gain = (int) Math.round(gain * tk.darrow.tribalpower.event.LeySurges.multiplier(level, pos));
        gain += tk.darrow.tribalpower.item.MachineRank.bonusGain(be, gain);
        gain = tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(gain);
        if (be.insertPulse(gain, false) > 0) {
            be.setChanged();
        }
        if (factors.pad()) tk.darrow.tribalpower.lattice.Keeping.livingBeat(level, pos);
    }

    /** Landscape beat plus machine rank — the same Pulse the tick inserts. */
    public int currentBeat(Level world, BlockPos pos) {
        int gain = beatFor(tk.darrow.tribalpower.ley.LeyMath.gain(world, pos));
        gain = (int) Math.round(gain * tk.darrow.tribalpower.event.LeySurges.multiplier(world, pos));
        return tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(gain + tk.darrow.tribalpower.item.MachineRank.bonusGain(this, gain));
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

    @Override
    public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>(tk.darrow.tribalpower.ley.LeyMath.breakdown(level, pos));
        lines.add(net.minecraft.network.chat.Component.translatable("ley.tribalpower.live", currentBeat(level, pos),
                tk.darrow.tribalpower.ley.LeyMath.MAX_GAIN));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.ley_collector.beat", GAIN_INTERVAL - tickCounter));
        return lines;
    }
}
