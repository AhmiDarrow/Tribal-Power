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

    // The lattice lists the generators on each network; one arriving or leaving (placed, broken, its chunk loaded
    // or unloaded) must tell it.
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
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
        // The threads are read once a beat and shared by the land survey and the surge check.
        var ley = be.reading(level, pos);
        var factors = tk.darrow.tribalpower.ley.LeyMath.factors(level, pos, ley);
        int gain = beatFor(factors.gain());
        gain = (int) Math.round(gain * (ley == null ? 1.0 : tk.darrow.tribalpower.event.LeySurges.multiplier(level, ley)));
        gain += tk.darrow.tribalpower.item.MachineRank.bonusGain(be, gain);
        gain = tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(gain);
        if (be.insertPulse(gain, false) > 0) {
            be.setChanged();
        }
        if (factors.pad()) tk.darrow.tribalpower.lattice.Keeping.livingBeat(level, pos);
    }

    /*
     * The ley reading here depends only on the dimension, this position and the totems standing
     * near it. Finding the totems is cheap; following the threads is not, so the last reading is
     * kept with the totems it was taken under and reused while they are exactly the same.
     */
    private java.util.List<tk.darrow.tribalpower.ley.LeyMagnets.Magnet> lastMagnets;
    private tk.darrow.tribalpower.ley.LeyField.Reading lastReading;
    /** A Ley Heart raising or lowering its threads changes the reading as surely as a totem moving. */
    private int lastHearts = -1;

    @org.jetbrains.annotations.Nullable
    private tk.darrow.tribalpower.ley.LeyField.Reading reading(Level level, BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return null;
        var magnets = tk.darrow.tribalpower.ley.LeyMagnets.near(server, pos);
        int hearts = tk.darrow.tribalpower.rite.world.LeyLines.heartEpoch(server);
        if (lastReading == null || !magnets.equals(lastMagnets) || hearts != lastHearts) {
            lastReading = tk.darrow.tribalpower.ley.LeyField.sample(server, pos, magnets);
            lastMagnets = magnets;
            lastHearts = hearts;
        }
        return lastReading;
    }

    /** Landscape beat plus machine rank — the same Pulse the tick inserts. */
    public int currentBeat(Level world, BlockPos pos) {
        // The same cached ley reading the tick uses: the lens asks every collector on a network once a second,
        // and a fresh LeyField sample per ask was the dearest thing in that packet.
        var ley = reading(world, pos);
        int gain = beatFor(tk.darrow.tribalpower.ley.LeyMath.factors(world, pos, ley).gain());
        gain = (int) Math.round(gain * (ley == null ? tk.darrow.tribalpower.event.LeySurges.multiplier(world, pos)
                : tk.darrow.tribalpower.event.LeySurges.multiplier(world, ley)));
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
