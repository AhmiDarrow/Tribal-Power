package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.camp.Ownership;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A pedestal that holds one thing and shows it (design 3.1 sections 2 and 5).
 *
 * <p>This is what makes a rite loop buildable out of parts that already exist: a relay restocks the
 * pedestal, a clock strikes the brazier, the rite fires, and the comparator reports the pedestal empty.
 *
 * <p>Migration: pedestals placed in 3.0 worlds have no saved block entity. Chunk loading creates one from
 * the block on demand, so an old pedestal comes back empty and working rather than broken.
 */
public class RitePedestalBlockEntity extends BlockEntity implements WorldlyContainer, Diagnosable, Ownership.Owned {
    public static final int SLOT = 0;
    private static final int[] SLOTS = {SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private UUID owner;

    public RitePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RITE_PEDESTAL.get(), pos, state);
    }

    public ItemStack held() { return items.get(SLOT); }

    public boolean stilled() { return level != null && level.hasNeighborSignal(worldPosition); }

    @Override public UUID owner() { return owner; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }

    // ---- Container -------------------------------------------------------------------------

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return held().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public int getMaxStackSize() { return 1; }

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
        items.set(slot, stack.copyWithCount(Math.min(1, stack.getCount())));
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return !stilled() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override public void clearContent() { items.clear(); sync(); }

    @Override public int[] getSlotsForFace(Direction face) { return SLOTS; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return held().isEmpty(); }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return !stilled() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return !stilled();
    }

    /** Comparator: occupied or not. A pedestal holds one thing, so there is nothing finer to report. */
    public int signal() { return held().isEmpty() ? 0 : 15; }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
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
        Ownership.save(tag, owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = Ownership.load(tag);
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(held().isEmpty()
                ? Component.translatable("diag.tribalpower.pedestal.empty")
                : Component.translatable("diag.tribalpower.pedestal.holding", held().getHoverName()));
        return lines;
    }
}
