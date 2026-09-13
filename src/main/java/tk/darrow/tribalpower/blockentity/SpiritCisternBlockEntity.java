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
    @Override public int[] inputSlots(net.minecraft.core.Direction face) { return new int[0]; }
    @Override public int[] outputSlots(net.minecraft.core.Direction face) { return new int[0]; }
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
            setChanged();
            if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    };
    private boolean paused() { return level != null && level.hasNeighborSignal(worldPosition); }
    public SpiritCisternBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SPIRIT_CISTERN.get(), pos, state); }
    public Component status() { return Component.translatable("message.tribalpower.cistern.status", tank.getFluidAmount(), tank.getCapacity()); }
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SpiritCisternBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.put("Tank", tank.writeToNBT(registries, new CompoundTag())); sides.save(tag); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); tank.readFromNBT(registries, tag.getCompound("Tank")); sides.load(tag); }
}
