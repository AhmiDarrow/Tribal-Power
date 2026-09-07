package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/** One-way conversion: 1 Pulse = 100 FE. Cannot receive FE, so conversion never feeds itself. */
public class PulseAdapterBlockEntity extends BlockEntity {
    private int energy;
    public static final int CAPACITY = 16000;
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
    public Component status() { return Component.translatable("message.tribalpower.adapter.status", energy, CAPACITY); }
    public static void tick(Level level, BlockPos pos, BlockState state, PulseAdapterBlockEntity be) {
        if (level.hasNeighborSignal(pos)) return;
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) {
            int want = Math.min(20, (CAPACITY - be.energy) / 100);
            if (want > 0) { int pulse = LatticeNetwork.extractPulseNearby(level, pos, 8, want, false); be.energy += pulse * 100; if (pulse > 0) be.changed(); }
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
