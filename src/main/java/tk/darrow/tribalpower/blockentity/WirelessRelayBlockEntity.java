package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/** Pulls from below into a linked block face. No force-loading or hidden global inventories. */
public class WirelessRelayBlockEntity extends BlockEntity {
    private BlockPos target;
    private String dimension = "";
    private Direction face = Direction.UP;
    private int cursor;
    private String status = "unlinked";
    public WirelessRelayBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.WIRELESS_RELAY.get(), pos, state); }
    public int tier() {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        return id.startsWith("astral_") ? 3 : id.startsWith("longreach_") ? 2 : 1;
    }
    public boolean fluid() { return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath().contains("fluid"); }
    public boolean bind(BlockPos target, Direction face, String dimension) {
        boolean same = level != null && dimension.equals(level.dimension().location().toString());
        int range = tier() == 1 ? 32 : 128;
        if ((!same && tier() < 3) || (same && (target.equals(worldPosition) || target.equals(worldPosition.below())))
                || (tier() < 3 && target.distSqr(worldPosition) > range * range)) return false;
        this.target = target.immutable(); this.face = face; this.dimension = dimension; updateStatus("waiting"); setChanged(); return true;
    }
    public Component status() { return Component.translatable("message.tribalpower.relay." + status); }
    public int signal() { return status.equals("working") ? 15 : target == null ? 0 : 1; }
    private void updateStatus(String value) {
        if (status.equals(value)) return;
        status = value;
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    public static void tick(Level level, BlockPos pos, BlockState state, WirelessRelayBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (be.target == null) { be.updateStatus("unlinked"); return; }
        if (level.hasNeighborSignal(pos)) { be.updateStatus("paused"); return; }
        var dim = net.minecraft.resources.ResourceLocation.tryParse(be.dimension);
        var destination = dim == null ? null : ((ServerLevel)level).getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
        // Block-item data preserves links when moved: validate the live position every cycle.
        boolean same = destination == level;
        int range = be.tier() == 1 ? 32 : 128;
        if ((!same && be.tier() < 3) || (same && (be.target.equals(pos) || be.target.equals(pos.below())))
                || (be.tier() < 3 && be.target.distSqr(pos) > range * range)) { be.updateStatus("unlinked"); return; }
        if (destination == null || !destination.hasChunkAt(be.target) || !level.hasChunkAt(pos.below())) { be.updateStatus("unloaded"); return; }
        if (destination.hasNeighborSignal(be.target)) { be.updateStatus("paused"); return; }
        int cost = be.tier() == 3 ? 16 : be.tier() == 2 ? 8 : 4;
        if (LatticeNetwork.extractPulseNearby(level, pos, 8, cost, true) < cost) { be.updateStatus("pulse"); return; }
        boolean moved = be.fluid() ? be.moveFluid(level, destination) : be.moveItem(level, destination);
        be.updateStatus(moved ? "working" : "waiting");
        if (moved) {
            LatticeNetwork.extractPulseNearby(level, pos, 8, cost, false);
            be.setChanged();
            if (Math.floorMod(level.getGameTime() + pos.asLong(), 80) != 0) return;
            if (destination == level && pos.distSqr(be.target) <= 32*32) SpiritEffects.beam((ServerLevel)level, pos.getCenter(), be.target.getCenter(), be.fluid() ? Attunement.WATER : Attunement.AIR);
            else {
                SpiritEffects.ring((ServerLevel)level, pos.getCenter(), Attunement.SPIRIT, 0.6, 8);
                SpiritEffects.ring(destination, be.target.getCenter(), Attunement.SPIRIT, 0.6, 8);
            }
            be.setChanged();
        }
    }
    private boolean moveItem(Level level, Level destination) {
        var source = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        var sink = destination.getCapability(Capabilities.ItemHandler.BLOCK, target, face);
        if (source == null || sink == null || source == sink || source.getSlots() == 0) return false;
        for (int n = 0; n < source.getSlots(); n++) {
            int slot = Math.floorMod(cursor + n, source.getSlots());
            ItemStack candidate = source.extractItem(slot, 16, true);
            if (candidate.isEmpty()) continue;
            int accepted = candidate.getCount() - ItemHandlerHelper.insertItemStacked(sink, candidate, true).getCount();
            if (accepted <= 0) continue;
            ItemStack actual = source.extractItem(slot, accepted, false);
            if (actual.isEmpty()) continue;
            int count = actual.getCount();
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(sink, actual, false);
            int transferred = count - remainder.getCount();
            if (!remainder.isEmpty()) {
                remainder = ItemHandlerHelper.insertItemStacked(source, remainder, false);
                if (!remainder.isEmpty()) Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, remainder);
            }
            cursor = (slot + 1) % source.getSlots();
            return transferred > 0;
        }
        return false;
    }
    private boolean moveFluid(Level level, Level destination) {
        var sink = destination.getCapability(Capabilities.FluidHandler.BLOCK, target, face);
        if (sink == null) return false;
        if (!pending.isEmpty()) {
            int filled = sink.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) { pending.shrink(filled); setChanged(); }
            return filled > 0;
        }
        var source = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP);
        if (source == null || source == sink) return false;
        var candidate = source.drain(250, IFluidHandler.FluidAction.SIMULATE);
        if (candidate.isEmpty()) return false;
        int accepted = sink.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        var actual = source.drain(candidate.copyWithAmount(Math.min(250, accepted)), IFluidHandler.FluidAction.EXECUTE);
        if (actual.isEmpty()) return false;
        int filled = sink.fill(actual.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled < actual.getAmount()) {
            // Retain a pending remainder rather than void fluid if a third-party tank changes acceptance.
            pending = actual.copyWithAmount(actual.getAmount() - filled);
            setChanged();
        }
        return filled > 0;
    }
    private net.neoforged.neoforge.fluids.FluidStack pending = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null) { tag.putLong("Target", target.asLong()); tag.putInt("Face", face.ordinal()); }
        tag.putString("Dimension", dimension);
        tag.putInt("Cursor", cursor);
        if (!pending.isEmpty()) tag.put("Pending", pending.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
        dimension = tag.getString("Dimension");
        face = Direction.from3DDataValue(tag.getInt("Face")); cursor = tag.getInt("Cursor");
        pending = net.neoforged.neoforge.fluids.FluidStack.parseOptional(registries, tag.getCompound("Pending"));
    }
}
