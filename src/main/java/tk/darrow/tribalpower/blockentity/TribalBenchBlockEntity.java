package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.bench.BenchMenu;

import java.util.List;

/**
 * The bench's two inventories.
 *
 * <p>The nine crafting places are part of the block and are saved with it, which is the whole point
 * of the bench: a vanilla table scatters its grid on the floor when you walk away. They are kept
 * apart from the shelf and are not reachable through any face, so a hopper under the bench cannot
 * reach into the middle of a half-built recipe.
 *
 * <p>The shelf is ordinary camp display, inherited whole from {@link CampDisplayBlockEntity}, so it
 * takes and gives back items exactly as a Wall Shelf does and a comparator reads it the same way.
 */
public class TribalBenchBlockEntity extends CampDisplayBlockEntity implements MenuProvider {
    public static final int GRID = 9;

    private final NonNullList<ItemStack> grid = NonNullList.withSize(GRID, ItemStack.EMPTY);

    public TribalBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIBAL_BENCH.get(), pos, state);
    }

    /** The crafting grid, as a container the menu can drive. Writes mark the block dirty. */
    public CraftingContainer craftingGrid(AbstractContainerMenu menu) {
        return new BenchGrid(this, menu);
    }

    public NonNullList<ItemStack> grid() { return grid; }

    public void gridChanged() { setChanged(); }

    /** Both inventories go on the floor when the bench is broken. */
    public void dropEverything(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, grid);
        grid.clear();
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.tribalpower.tribal_bench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BenchMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag bench = new CompoundTag();
        ContainerHelper.saveAllItems(bench, grid, registries);
        tag.put("Grid", bench);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        grid.clear();
        if (tag.contains("Grid")) ContainerHelper.loadAllItems(tag.getCompound("Grid"), grid, registries);
    }

    /** A {@link CraftingContainer} view of the bench's own nine places. */
    private record BenchGrid(TribalBenchBlockEntity bench, AbstractContainerMenu menu)
            implements CraftingContainer {

        @Override public int getWidth() { return 3; }
        @Override public int getHeight() { return 3; }
        @Override public int getContainerSize() { return GRID; }
        @Override public List<ItemStack> getItems() { return bench.grid; }

        @Override public boolean isEmpty() {
            for (ItemStack stack : bench.grid) if (!stack.isEmpty()) return false;
            return true;
        }

        @Override public ItemStack getItem(int slot) {
            return slot >= 0 && slot < GRID ? bench.grid.get(slot) : ItemStack.EMPTY;
        }

        @Override public ItemStack removeItem(int slot, int count) {
            ItemStack taken = ContainerHelper.removeItem(bench.grid, slot, count);
            if (!taken.isEmpty()) { bench.gridChanged(); menu.slotsChanged(this); }
            return taken;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = ContainerHelper.takeItem(bench.grid, slot);
            if (!taken.isEmpty()) bench.gridChanged();
            return taken;
        }

        @Override public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= GRID) return;
            bench.grid.set(slot, stack);
            bench.gridChanged();
            menu.slotsChanged(this);
        }

        @Override public void setChanged() { bench.gridChanged(); }

        @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(bench, player); }

        @Override public void clearContent() { bench.grid.clear(); bench.gridChanged(); }

        @Override public void fillStackedContents(net.minecraft.world.entity.player.StackedContents contents) {
            for (ItemStack stack : bench.grid) contents.accountSimpleStack(stack);
        }
    }
}
