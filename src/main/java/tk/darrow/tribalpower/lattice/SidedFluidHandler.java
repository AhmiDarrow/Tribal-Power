package tk.darrow.tribalpower.lattice;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Honours {@link HasSideIo} on fluid faces the same way hoppers honour item faces. */
public record SidedFluidHandler(IFluidHandler inner, SideIo.Mode mode) implements IFluidHandler {
    public static IFluidHandler wrap(BlockEntity be, Direction side, IFluidHandler inner) {
        if (inner == null) return null;
        if (!(be instanceof HasSideIo io) || side == null) return inner;
        SideIo.Mode mode = io.sideIo().get(side);
        if (mode == SideIo.Mode.NONE) return null;
        return new SidedFluidHandler(inner, mode);
    }

    /**
     * For machines whose tanks only feed their own work (Stone Font, Resonance Mesh): a face may fill them but never
     * drain them. Their bottom face defaults to OUTPUT for items, which otherwise emptied the input tanks into a
     * cistern or pipe below.
     */
    public static IFluidHandler wrapInput(BlockEntity be, Direction side, IFluidHandler inner) {
        IFluidHandler sided = wrap(be, side, inner);
        if (sided == null || side == null) return sided;
        return sided instanceof SidedFluidHandler handler && handler.mode().insert()
                ? new SidedFluidHandler(inner, SideIo.Mode.INPUT) : null;
    }

    @Override public int getTanks() { return inner.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return inner.getFluidInTank(tank); }
    @Override public int getTankCapacity(int tank) { return inner.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return inner.isFluidValid(tank, stack); }
    @Override public int fill(FluidStack resource, FluidAction action) {
        return mode.insert() ? inner.fill(resource, action) : 0;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return mode.extract() ? inner.drain(resource, action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        return mode.extract() ? inner.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}
