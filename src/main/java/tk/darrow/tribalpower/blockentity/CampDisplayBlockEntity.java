package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

/**
 * The places a piece of camp furniture can hold something: four along a Wall Shelf's board, four on
 * a March Table's top, one in the mouth of a Spirit Urn. One item to a place, set down and taken
 * back by hand — this is furniture for showing a thing, not for storing a stack of them, so places
 * hold one item each and no face accepts a hopper. A comparator reads how many places are filled.
 */
public class CampDisplayBlockEntity extends BlockEntity implements WorldlyContainer {
    /** The most places any piece has; the array is sized to this and capacity trims it. */
    public static final int MAX_SLOTS = 4;
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);

    public CampDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMP_DISPLAY.get(), pos, state);
    }

    /** For furniture that is display plus something else, such as the Tribal Bench and its grid. */
    protected CampDisplayBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                     BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How many places this piece has. An urn is a single vessel; a board and a table top take four. */
    public static int capacityOf(BlockState state) {
        if (state.is(ModBlocks.SPIRIT_URN.get())) return 1;
        // Each half of a bench carries two of the four places along its back board.
        if (state.getBlock() instanceof tk.darrow.tribalpower.block.TribalBenchBlock) return 2;
        return MAX_SLOTS;
    }

    public int capacity() {
        return capacityOf(getBlockState());
    }

    /** The item in a place, for the renderer and for interaction. */
    public ItemStack at(int slot) {
        return slot < 0 || slot >= capacity() ? ItemStack.EMPTY : items.get(slot);
    }

    /** Puts one item down in a place. Returns what is left of the stack the player was holding. */
    public ItemStack place(int slot, ItemStack stack) {
        if (slot < 0 || slot >= capacity() || stack.isEmpty() || !items.get(slot).isEmpty()) return stack;
        items.set(slot, stack.split(1));
        sync();
        return stack;
    }

    /** Takes the item back out of a place. */
    public ItemStack take(int slot) {
        if (slot < 0 || slot >= capacity()) return ItemStack.EMPTY;
        ItemStack taken = items.get(slot);
        if (taken.isEmpty()) return ItemStack.EMPTY;
        items.set(slot, ItemStack.EMPTY);
        sync();
        return taken;
    }

    /** The first place with nothing on it, starting from the one asked for. -1 when the board is full. */
    public int freeSlotFrom(int preferred) {
        if (at(preferred).isEmpty()) return preferred;
        for (int i = 0; i < capacity(); i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /** The nearest filled place, starting from the one asked for. -1 when the board is bare. */
    public int filledSlotFrom(int preferred) {
        if (!at(preferred).isEmpty()) return preferred;
        for (int i = 0; i < capacity(); i++) {
            if (!items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /** Comparator: one step per filled place, so an empty piece reads 0 and a full one 15. */
    public int signal() {
        int filled = 0;
        for (int i = 0; i < capacity(); i++) {
            if (!items.get(i).isEmpty()) filled++;
        }
        return filled == 0 ? 0 : Math.max(1, filled * 15 / capacity());
    }

    protected void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ---- Container -------------------------------------------------------------------------
    // Implemented so the shelf drops its items when broken and a comparator can read it. Nothing
    // is reachable through a face, so hoppers and pipes leave it alone; the caches are for storage.

    @Override public int getContainerSize() { return capacity(); }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < capacity(); i++) {
            if (!items.get(i).isEmpty()) return false;
        }
        return true;
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) sync();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        sync();
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        sync();
    }

    @Override public int[] getSlotsForFace(Direction side) { return NO_SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return false;
    }

    // ---- rendering / persistence -----------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
