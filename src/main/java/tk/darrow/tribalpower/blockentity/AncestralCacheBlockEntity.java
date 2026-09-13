package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AncestralCacheBlockEntity extends RandomizableContainerBlockEntity implements net.minecraft.world.WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo {
    public static final int SIZE = 54;
    private static final int[] ALL = java.util.stream.IntStream.range(0, SIZE).toArray();
    private final tk.darrow.tribalpower.lattice.SideIo sides = new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH);
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(net.minecraft.core.Direction face) { return ALL; }
    @Override public int[] outputSlots(net.minecraft.core.Direction face) { return ALL; }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction face) { return tk.darrow.tribalpower.lattice.SideIo.slots(this, face); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return !level.hasNeighborSignal(worldPosition) && sides.get(face).insert(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return !level.hasNeighborSignal(worldPosition) && sides.get(face).extract(); }
    @Override public boolean stillValid(Player player) { return super.stillValid(player) && !level.hasNeighborSignal(worldPosition); }

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {}

        @Override
        protected void onClose(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {}

        @Override
        protected void openerCountChanged(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, int count, int openCount) {}

        @Override
        protected boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof tk.darrow.tribalpower.echo.CacheMenu cache)
                return cache.container() == AncestralCacheBlockEntity.this;
            if (!(player.containerMenu instanceof ChestMenu)) {
                return false;
            }
            return ((ChestMenu) player.containerMenu).getContainer() == AncestralCacheBlockEntity.this;
        }
    };

    public AncestralCacheBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCESTRAL_CACHE.get(), pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.tribalpower.ancestral_cache");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new tk.darrow.tribalpower.echo.CacheMenu(id, inventory, this, new net.minecraft.world.inventory.ContainerData() {
            public int get(int index) {
                return switch (index) {
                    case 0 -> sides.pack();
                    case 1 -> worldPosition.getX();
                    case 2 -> worldPosition.getY();
                    default -> worldPosition.getZ();
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return 4; }
        });
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
        sides.load(tag);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openersCounter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }
}
