package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.pattern.PatternState;
import tk.darrow.tribalpower.pattern.RitualPattern;

import java.util.UUID;

/**
 * Shared plumbing for the 3.1 pattern devices: a side-aware inventory, the cached pattern match, and
 * the owner recorded on placement.
 *
 * <p>Every device built on this obeys the automation contract of design 3.1 section 2 -- a held redstone
 * signal stills the work and closes the inventory to hoppers and relays alike.
 */
public abstract class LatticeDeviceBlockEntity extends BlockEntity
        implements WorldlyContainer, Diagnosable, Ownership.Owned {
    protected NonNullList<ItemStack> items;
    protected final PatternState pattern;
    private final int size;
    private UUID owner;

    protected LatticeDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       int size, RitualPattern pattern) {
        super(type, pos, state);
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.pattern = new PatternState(pattern);
    }

    public PatternState patternState() { return pattern; }

    /** Redstone held high stills the device: no work, no insertion, no extraction. */
    public boolean stilled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    public void onNeighbourChanged(BlockPos changed) {
        pattern.onNeighbourChanged(worldPosition, changed);
    }

    @Override public UUID owner() { return owner; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }

    // ---- Container -------------------------------------------------------------------------

    @Override public int getContainerSize() { return size; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !stilled() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) {
        return !stilled() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) {
        return !stilled() && isOutputSlot(slot);
    }

    /** Slots a hopper or relay may drain. */
    protected abstract boolean isOutputSlot(int slot);

    /**
     * Places {@code result} into the output slots.
     * @return false when there is no room, which is what makes the device stall instead of voiding
     */
    protected boolean placeOutput(ItemStack result, boolean simulate) {
        int remaining = result.getCount();
        for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
            if (!isOutputSlot(slot)) continue;
            ItemStack current = items.get(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, result)) continue;
            int room = Math.min(getMaxStackSize(), result.getMaxStackSize()) - current.getCount();
            int count = Math.min(remaining, Math.max(0, room));
            if (count <= 0) continue;
            if (!simulate) {
                if (current.isEmpty()) items.set(slot, result.copyWithCount(count));
                else current.grow(count);
            }
            remaining -= count;
        }
        return remaining == 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        Ownership.save(tag, owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = Ownership.load(tag);
        pattern.invalidate();
    }
}
