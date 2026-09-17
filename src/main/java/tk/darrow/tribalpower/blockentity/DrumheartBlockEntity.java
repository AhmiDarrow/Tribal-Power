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
 * "The drum is not loud. The drum is steady. Be the drum."
 *
 * <p>Retuned in 3.1 (design 3.1 section 9.3): the redstone path now uses the same tempo window the hand
 * already used, instead of paying a flat rate on a fixed cooldown. Beats {@link #TEMPO_MIN} to
 * {@link #TEMPO_MAX} ticks apart pay {@link #ON_TEMPO}; anything else pays {@link #OFF_TEMPO}; nothing
 * closer than {@link #MIN_SPACING} ticks counts at all. A well-built one-second clock makes 24 a second
 * and a sloppy one makes 10 -- so redstone skill is worth something, and spam clocks are not.
 *
 * <p>Naming note: this block rings Earth. The Drumhearts tribe keeps Spark and the Fire voice, and their
 * own craft is the Ember Horn. The block keeps its id because renaming it would break saves.
 */
public class DrumheartBlockEntity extends BlockEntity implements PulseHandler, tk.darrow.tribalpower.api.pulse.PulseGenerator, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CAPACITY = 1000;
    public static final int MIN_SPACING = 8;
    public static final int TEMPO_MIN = 17;
    public static final int TEMPO_MAX = 23;
    public static final int ON_TEMPO = 24;
    public static final int OFF_TEMPO = 10;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int redstoneCooldown;
    private long lastManualBeat = -100;
    private long lastRedstoneBeat = -100;
    private int lastRedstoneGain;
    private boolean lastSignal;

    public DrumheartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRUMHEART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrumheartBlockEntity be) {
        if (be.redstoneCooldown > 0) {
            be.redstoneCooldown--;
        }
    }

    /** Pulse a beat is worth at this spacing: the one place the tempo rule lives. */
    public static int beatValue(long interval) {
        if (interval < MIN_SPACING) return 0;
        return interval >= TEMPO_MIN && interval <= TEMPO_MAX ? ON_TEMPO : OFF_TEMPO;
    }

    public int drumBeat() {
        long now = level == null ? 0 : level.getGameTime();
        long interval = now - lastManualBeat;
        if (interval < MIN_SPACING) return 0;
        boolean inTime = interval >= TEMPO_MIN && interval <= TEMPO_MAX;
        lastManualBeat = now;
        int beat = tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(beatValue(interval));
        beat += tk.darrow.tribalpower.item.MachineRank.bonusGain(this, beat);
        lastRedstoneGain = beat;
        int gained = insertPulse(beat, false);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server, worldPosition.getCenter().add(0, 0.4, 0),
                    tk.darrow.tribalpower.api.pulse.Attunement.EARTH, inTime ? 1 : 0.55, inTime ? 16 : 8);
            strike(inTime);
            tk.darrow.tribalpower.lattice.Keeping.livingBeat(level, worldPosition);
        }
        setChanged();
        return gained;
    }

    /**
     * A drum placed beside a lit torch has to start from the signal that is already there, or the first
     * unrelated neighbour update is a beat nobody asked for.
     */
    public void seedSignal(Level level) {
        lastSignal = tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition);
        setChanged();
    }

    /** A struck signal is an edge. Holding the line high is not a faster drum. */
    public int onRedstoneChanged() {
        if (level == null) return 0;
        boolean signal = tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition);
        boolean rising = signal && !lastSignal;
        lastSignal = signal;
        setChanged();
        return rising ? onRedstonePulse() : 0;
    }

    /**
     * A struck redstone signal is a beat, and is paid for like one. Holding the line high does nothing:
     * only a rising edge reaches here, and only spacing decides what it is worth.
     */
    public int onRedstonePulse() {
        if (redstoneCooldown > 0) return 0;
        long now = level == null ? 0 : level.getGameTime();
        long interval = now - lastRedstoneBeat;
        int value = beatValue(interval);
        lastRedstoneBeat = now;
        redstoneCooldown = MIN_SPACING;
        if (value <= 0) {
            lastRedstoneGain = 0;
            return 0;
        }
        int beat = tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(value);
        beat += tk.darrow.tribalpower.item.MachineRank.bonusGain(this, beat);
        lastRedstoneGain = beat;
        int gained = insertPulse(beat, false);
        strike(value == ON_TEMPO);
        setChanged();
        return gained;
    }

    private void strike(boolean inTime) {
        tk.darrow.tribalpower.sound.ModSounds.play(level, worldPosition,
                inTime ? tk.darrow.tribalpower.sound.ModSounds.DRUMHEART_TEMPO
                        : tk.darrow.tribalpower.sound.ModSounds.DRUMHEART_OFF_TEMPO,
                0.7F, inTime ? 1.05F : 0.85F);
    }

    @Override
    public tk.darrow.tribalpower.api.pulse.Attunement voice() {
        return tk.darrow.tribalpower.api.pulse.Attunement.EARTH;
    }

    @Override
    public int currentOutput() {
        // A drum is beaten, not run: report what the last beat was worth rather than a steady rate.
        return level != null && tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition) ? 0 : lastRedstoneGain;
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> breakdown() {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable(
                "drum.tribalpower.tempo", TEMPO_MIN, TEMPO_MAX, ON_TEMPO, OFF_TEMPO, lastRedstoneGain));
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
        tag.putBoolean("LastSignal", lastSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        redstoneCooldown = net.minecraft.util.Mth.clamp(tag.getInt("RedstoneCooldown"), 0, MIN_SPACING);
        lastRedstoneGain = 0;
        lastSignal = tag.getBoolean("LastSignal");
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        long since = server.getGameTime() - lastManualBeat;
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.drumheart.beat",
                since < 200 ? Long.toString(since) : "-", ON_TEMPO, OFF_TEMPO, redstoneCooldown));
        lines.addAll(breakdown());
        if (!canReceivePulse()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
