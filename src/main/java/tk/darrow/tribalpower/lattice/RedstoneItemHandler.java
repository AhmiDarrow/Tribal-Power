package tk.darrow.tribalpower.lattice;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

/** Checks the live signal on each operation, including references cached by other mods. */
public record RedstoneItemHandler(BlockEntity owner, IItemHandler delegate) implements IItemHandler {
    private boolean paused() { return owner.getLevel() != null && owner.getLevel().hasNeighborSignal(owner.getBlockPos()); }
    public int getSlots() { return delegate.getSlots(); }
    public ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return paused() ? stack : delegate.insertItem(slot, stack, simulate); }
    public ItemStack extractItem(int slot, int amount, boolean simulate) { return paused() ? ItemStack.EMPTY : delegate.extractItem(slot, amount, simulate); }
    public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
    public boolean isItemValid(int slot, ItemStack stack) { return delegate.isItemValid(slot, stack); }
}
