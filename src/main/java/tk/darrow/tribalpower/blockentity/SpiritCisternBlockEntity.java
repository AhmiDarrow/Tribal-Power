package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidStack;

public class SpiritCisternBlockEntity extends BlockEntity implements tk.darrow.tribalpower.lattice.HasSideIo {
    private final tk.darrow.tribalpower.lattice.SideIo sides = new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH);
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    private static final int[] NONE = new int[0];
    @Override public int[] inputSlots(net.minecraft.core.Direction face) { return NONE; }
    @Override public int[] outputSlots(net.minecraft.core.Direction face) { return NONE; }
    public final FluidTank tank = new FluidTank(16000) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return paused() ? 0 : super.fill(resource, action);
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return paused() ? FluidStack.EMPTY : super.drain(resource, action);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return paused() ? FluidStack.EMPTY : super.drain(amount, action);
        }
        @Override protected void onContentsChanged() {
            if (level != null) {
                // setChanged() would wake the comparators on every fill, and this used to wake them a second time on
                // top: mark the chunk, and wake them only when the level they read moves.
                level.blockEntityChanged(worldPosition);
                int signal = signal();
                if (signal != shownSignal) {
                    shownSignal = signal;
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                }
                // The windows show the level. A pipe changes the tank every tick, so a packet goes out only when the
                // fluid or the drawn height moves; the exact amount follows within a second for anything reading it.
                if (level.isClientSide || shownChanged()) sync();
                else syncPending = true;
            }
        }
    };
    /** Steps the window's fill height is sent at: one is a sixteenth of a texture pixel of the four-pixel window. */
    private static final int SHOWN_STEPS = 64;
    /** Ticks an unsent exact amount may wait for its packet. */
    private static final int SYNC_INTERVAL = 20;
    private FluidStack shownFluid = FluidStack.EMPTY;
    private int shownStep = -1;
    private boolean syncPending;
    private int shownStep() {
        return tank.getCapacity() <= 0 ? 0 : (int) ((long) tank.getFluidAmount() * SHOWN_STEPS / tank.getCapacity());
    }
    private boolean shownChanged() {
        return shownStep() != shownStep || !FluidStack.isSameFluidSameComponents(tank.getFluid(), shownFluid);
    }
    private void sync() {
        shownFluid = tank.getFluid().copy();
        shownStep = shownStep();
        syncPending = false;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    /** The comparator level last announced. */
    private int shownSignal = -1;
    /** Comparator: how full the tank is. */
    public int signal() { return tank.getFluidAmount() == 0 ? 0 : 1 + 14 * tank.getFluidAmount() / tank.getCapacity(); }
    /** The redstone hold, asked on every fill and drain: read again on a neighbour change, or after a second. */
    private final HeldSignal held = new HeldSignal(20);
    /** The block saw a neighbour change: read the redstone hold afresh. */
    public void neighbourChanged() { held.forget(); }
    private boolean paused() { return level != null && held.get(level, worldPosition); }
    public SpiritCisternBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SPIRIT_CISTERN.get(), pos, state); }
    public Component status() { return Component.translatable("message.tribalpower.cistern.status", tank.getFluidAmount(), tank.getCapacity()); }
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SpiritCisternBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if (be.syncPending && (level.getGameTime() + pos.asLong()) % SYNC_INTERVAL == 0) be.sync();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); tag.put("Tank", tank.writeToNBT(registries, new CompoundTag())); return tag; }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.put("Tank", tank.writeToNBT(registries, new CompoundTag())); sides.save(tag); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); tank.readFromNBT(registries, tag.getCompound("Tank")); sides.load(tag); }
}
