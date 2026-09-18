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

/** One-way conversion: 1 Pulse = 100 FE. Cannot receive FE, so conversion never feeds itself. */
public class PulseAdapterBlockEntity extends BlockEntity implements Diagnosable {
    private int energy;
    public static final int CAPACITY = 16000;
    public static final int RATE = 20;
    public static final int FE_PER_PULSE = 100;
    public PulseAdapterBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PULSE_ADAPTER.get(), pos, state); }
    public final IEnergyStorage handler = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) { return 0; }
        public int extractEnergy(int amount, boolean simulate) {
            if (level != null && level.hasNeighborSignal(worldPosition)) return 0;
            int take = Math.min(energy, Math.max(0, Math.min(1000, amount)));
            if (!simulate && take > 0) { energy -= take; changed(); } return take;
        }
        public int getEnergyStored() { return energy; }
        public int getMaxEnergyStored() { return CAPACITY; }
        public boolean canExtract() { return true; }
        public boolean canReceive() { return false; }
    };
    private void changed() {
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    public int pulseRate() { return MachineRank.scalePulse(this, RATE); }
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
        if (level.hasNeighborSignal(pos)) return;
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) {
            int want = Math.min(be.pulseRate(), (CAPACITY - be.energy) / FE_PER_PULSE);
            if (want > 0) { int pulse = LatticeNetwork.extractPulseNearby(level, pos, 8, want, false); be.energy += pulse * FE_PER_PULSE; if (pulse > 0) be.changed(); }
        }
        int budget = Math.min(1000, be.energy);
        for (Direction face : Direction.values()) {
            if (budget <= 0 || !level.hasChunkAt(pos.relative(face))) continue;
            var sink = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(face), face.getOpposite());
            if (sink == null || !sink.canReceive()) continue;
            int moved = Math.max(0, Math.min(budget, sink.receiveEnergy(budget, false)));
            budget -= moved; be.energy -= moved; if (moved > 0) be.changed();
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putInt("FE", energy); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); energy = Math.max(0, Math.min(CAPACITY, tag.getInt("FE"))); }
}
