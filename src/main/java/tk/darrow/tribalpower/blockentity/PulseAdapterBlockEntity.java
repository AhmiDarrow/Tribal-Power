package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * One-way conversion from Pulse to Forge Energy. Cannot receive FE, so conversion never feeds itself.
 *
 * <p>The rate is set against the FE mods this is actually played beside. It used to draw 20 Pulse a
 * second at 100 FE each: 2,000 FE a second, or 100 FE/t, which is less than a Mekanism Enrichment
 * Chamber needs to run (125 FE/t) and below a Powah basic furnator. One adapter could not power one
 * machine, so nobody built one.
 *
 * <p>The ratio stays 1 Pulse = 100 FE, which is the number the Codex has always given. What changed
 * is throughput: it draws {@link #RATE} Pulse a second now rather than 20, so it puts out 6,000 FE a
 * second, or <b>300 FE/t</b>, and a rank 3 adapter reaches 545. See {@link PulseEconomy} for where
 * that sits among the pack's own generators and what it is measured against.
 *
 * <p>Sixty Pulse a second is more than a zone of drums can feed, so running one flat out wants a
 * Resonator behind it. That coupling is deliberate: the bridge to FE should cost a real generator.
 */
public class PulseAdapterBlockEntity extends BlockEntity implements Diagnosable, tk.darrow.tribalpower.api.pulse.PulseSpend {
    private int energy;
    /** The six neighbours' FE handlers, cached: the adapter offers power every tick it holds any. */
    private net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>[] sinks;
    public static final int CAPACITY = 48000;
    public static final int RATE = 60;
    public static final int FE_PER_PULSE = 100;
    /** The redstone hold, asked every tick and on every transfer: read again on a neighbour change, or after a second. */
    private final HeldSignal held = new HeldSignal(20);
    /** The block saw a neighbour change: read the redstone hold afresh. */
    public void neighbourChanged() { held.forget(); }
    public PulseAdapterBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PULSE_ADAPTER.get(), pos, state); }
    public final IEnergyStorage handler = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) { return 0; }
        public int extractEnergy(int amount, boolean simulate) {
            if (level != null && held.get(level, worldPosition)) return 0;
            int take = Math.min(energy, Math.max(0, Math.min(1000, amount)));
            if (!simulate && take > 0) { energy -= take; changed(); } return take;
        }
        public int getEnergyStored() { return energy; }
        public int getMaxEnergyStored() { return CAPACITY; }
        public boolean canExtract() { return true; }
        public boolean canReceive() { return false; }
    };
    /** The comparator level last announced; FE moves every tick, the level only now and then. */
    private int shownSignal = -1;
    private void changed() {
        // Only the chunk is marked here: setChanged() would also wake every comparator, every tick FE moves.
        if (level != null) level.blockEntityChanged(worldPosition);
        int now = signal();
        if (now != shownSignal && level != null) {
            shownSignal = now;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    /** Comparator: how full the FE buffer is. */
    public int signal() { return energy == 0 ? 0 : 1 + 14 * energy / CAPACITY; }
    public int pulseRate() { return MachineRank.scalePulse(this, RATE); }

    @Override
    public int spendPerSecond() {
        int room = (CAPACITY - energy) / FE_PER_PULSE;
        if (room <= 0) return 0;
        return Math.min(pulseRate(), room);
    }
    public Component status() {
        return Component.translatable("message.tribalpower.adapter.status", energy, CAPACITY, pulseRate(), RATE);
    }
    @Override
    public List<Component> diagnose(ServerLevel level, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.adapter.buffer", energy, CAPACITY));
        lines.add(Component.translatable("diag.tribalpower.adapter.rate", pulseRate(), RATE));
        return lines;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, PulseAdapterBlockEntity be) {
        boolean beat = (level.getGameTime() + pos.asLong()) % 20 == 0;
        // An empty adapter between beats has nothing to do: skip the six-neighbour redstone read.
        if (!beat && be.energy <= 0) return;
        if (be.held.get(level, pos)) return;
        if (beat) {
            int want = Math.min(be.pulseRate(), (CAPACITY - be.energy) / FE_PER_PULSE);
            if (want > 0) { int pulse = LatticeNetwork.extractPulseNearby(level, pos, 8, want, false); be.energy += pulse * FE_PER_PULSE; if (pulse > 0) be.changed(); }
        }
        int budget = Math.min(1000, be.energy);
        if (budget <= 0 || !(level instanceof ServerLevel server)) return;
        if (be.sinks == null) be.sinks = sinks(server, pos);
        int sent = 0;
        for (Direction face : Direction.values()) {
            if (budget <= 0 || !level.hasChunkAt(pos.relative(face))) continue;
            var sink = be.sinks[face.ordinal()].getCapability();
            if (sink == null || !sink.canReceive()) continue;
            int moved = Math.max(0, Math.min(budget, sink.receiveEnergy(budget, false)));
            budget -= moved; be.energy -= moved; sent += moved;
        }
        if (sent > 0) be.changed();   // once a tick, not once a face
    }
    @SuppressWarnings("unchecked")
    private static net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>[] sinks(ServerLevel server, BlockPos pos) {
        var caches = new net.neoforged.neoforge.capabilities.BlockCapabilityCache[6];
        for (Direction face : Direction.values())
            caches[face.ordinal()] = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, server, pos.relative(face), face.getOpposite());
        return caches;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putInt("FE", energy); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); energy = Math.max(0, Math.min(CAPACITY, tag.getInt("FE"))); }
}
