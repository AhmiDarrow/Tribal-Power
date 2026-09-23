package tk.darrow.tribalpower.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseEconomy;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Forge Energy into Pulse, the opposite number of the Harmonic Energizer.
 *
 * <p>It is not a plain inverse. Raw FE is noise to a lattice, and it takes voices to beat it into
 * something the stones will hold: the conversion starts at {@link #COST_BASE} FE a Pulse and every
 * distinct answered Resonance Totem voice within {@link #VOICE_RADIUS} blocks knocks
 * {@link #COST_PER_VOICE} off, down to a floor of {@link #COST_FLOOR}. A converter dropped in a field
 * works, badly. One standing in a ring of totems is nearly twice as efficient.
 *
 * <p><b>The floor is not decoration.</b> The Energizer pays out {@value PulseEconomy#FE_PER_PULSE} FE
 * a Pulse, so a converter that ever bought a Pulse for less than that would close a loop that made
 * energy out of nothing. {@link #COST_FLOOR} sits above it deliberately, and a test holds it there.
 */
public class LatticeConverterBlockEntity extends BlockEntity implements Diagnosable {
    /** FE the buffer holds: a few seconds of intake, so a stuttering supply still converts smoothly. */
    public static final int CAPACITY = 48_000;
    /** Pulse a second it will make at most, before ranking. */
    public static final int RATE = 40;
    /** How far it listens for voices. The same reach as everything else in the lattice. */
    public static final int VOICE_RADIUS = 8;

    public static final int COST_BASE = 220;
    public static final int COST_PER_VOICE = 20;
    /** Above the Energizer's payout, so Pulse to FE and back is always a loss. */
    public static final int COST_FLOOR = 120;

    private int energy;
    private int voices;
    private int lastMade;

    public LatticeConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATTICE_CONVERTER.get(), pos, state);
    }

    /** FE a Pulse costs at this many voices. */
    public static int costAt(int voices) {
        return Math.max(COST_FLOOR, COST_BASE - COST_PER_VOICE * Math.max(0, voices));
    }

    public int cost() { return costAt(voices); }
    public int getVoices() { return voices; }
    public int rate() { return MachineRank.scalePulse(this, RATE); }
    public int getEnergy() { return energy; }

    /** Takes FE and will not give it back, so it can never feed the Energizer that fed it. */
    public final IEnergyStorage handler = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (level != null && level.hasNeighborSignal(worldPosition)) return 0;
            int room = Math.min(Math.max(0, amount), CAPACITY - energy);
            if (!simulate && room > 0) { energy += room; changed(); }
            return room;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy; }
        @Override public int getMaxEnergyStored() { return CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private void changed() {
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LatticeConverterBlockEntity be) {
        if (level.hasNeighborSignal(pos)) return;
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.voices = be.countVoices(level, pos);
        int cost = be.cost();
        int affordable = be.energy / cost;
        int want = Math.min(be.rate(), affordable);
        if (want <= 0) { be.lastMade = 0; return; }
        // Offer it to the lattice first: Pulse made with nowhere to go would be FE burned for nothing.
        int taken = LatticeNetwork.insertPulseNearby(level, pos, VOICE_RADIUS, want, false);
        be.lastMade = taken;
        if (taken <= 0) return;
        be.energy -= taken * cost;
        be.changed();
        if (level instanceof ServerLevel server)
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server, pos.getCenter().add(0, 0.4, 0),
                    Attunement.SPIRIT, 0.45, 6);
    }

    /** Distinct answered voices within reach, exactly as the Resonator counts them. */
    private int countVoices(Level level, BlockPos pos) {
        EnumSet<Attunement> heard = EnumSet.noneOf(Attunement.class);
        for (var totem : LatticeNetwork.findNearbyTotems(level, pos, VOICE_RADIUS))
            if (totem.keeping() == Keeping.State.ANSWERED) heard.add(totem.getAttunement());
        return heard.size();
    }

    public Component status() {
        return Component.translatable("message.tribalpower.converter.status",
                energy, CAPACITY, cost(), voices, rate());
    }

    @Override
    public List<Component> diagnose(ServerLevel level, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.converter.buffer", energy, CAPACITY));
        lines.add(Component.translatable("diag.tribalpower.converter.cost", cost(), voices, COST_FLOOR));
        lines.add(Component.translatable("diag.tribalpower.converter.rate", lastMade, rate()));
        if (voices == 0)
            lines.add(Component.translatable("diag.tribalpower.converter.no_voices")
                    .withStyle(ChatFormatting.YELLOW));
        if (energy < cost())
            lines.add(Component.translatable("diag.tribalpower.converter.starved")
                    .withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FE", energy);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = Math.max(0, Math.min(CAPACITY, tag.getInt("FE")));
    }
}
