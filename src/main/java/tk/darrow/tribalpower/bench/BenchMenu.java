package tk.darrow.tribalpower.bench;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.TribalBenchBlockEntity;

import java.util.Optional;

/**
 * The Tribal Bench's grid.
 *
 * <p>Shaped like a vanilla crafting menu and behaves like one, with the single difference that
 * matters: the nine places belong to the block, not to the screen. Closing the bench leaves the
 * work where it is instead of throwing it on the floor, which is why {@link #removed} does not
 * clear the grid the way {@code CraftingMenu} does.
 */
public class BenchMenu extends AbstractContainerMenu {
    public static final int GRID_START = 1;
    public static final int GRID_END = 10;

    private final CraftingContainer grid;
    private final ResultContainer result = new ResultContainer();
    private final Player player;
    @Nullable private final TribalBenchBlockEntity bench;

    /** Client side: a loose grid of the same shape, filled by the server's slot updates. */
    public BenchMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public BenchMenu(int id, Inventory inventory, @Nullable TribalBenchBlockEntity bench) {
        super(BenchRegistry.MENU.get(), id);
        this.player = inventory.player;
        this.bench = bench;
        this.grid = bench != null ? bench.craftingGrid(this) : new TransientCraftingContainer(this, 3, 3);

        addSlot(new ResultSlot(player, grid, result, 0, 124, 35) {
            /** Another player may have changed the shared grid since this result was offered: check it still holds. */
            @Override
            public boolean mayPickup(Player taker) {
                refreshResult();
                return hasItem() && super.mayPickup(taker);
            }
        });
        if (bench != null) bench.opened(this);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new Slot(grid, col + row * 3, 30 + col * 18, 17 + row * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));

        if (bench != null) slotsChanged(grid);
    }

    @Override
    public void slotsChanged(Container container) {
        if (player.level().isClientSide) return;
        refreshResult();
        if (bench != null) bench.gridChanged();
        super.slotsChanged(container);
    }

    /** What the grid makes right now, for this player. Called for every viewer whenever the shared grid changes. */
    public void refreshResult() {
        if (player.level().isClientSide) return;
        CraftingInput input = CraftingInput.of(3, 3, grid.getItems());
        Optional<RecipeHolder<CraftingRecipe>> found = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level());
        ItemStack output = ItemStack.EMPTY;
        if (found.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = found.get();
            if (result.setRecipeUsed(player.level(), (net.minecraft.server.level.ServerPlayer) player, holder))
                output = holder.value().assemble(input, player.level().registryAccess());
        }
        result.setItem(0, output);
    }

    /**
     * The grid stays put.
     *
     * <p>Vanilla drops the contents here, which is exactly the behaviour the bench exists to avoid.
     * Only the result slot is cleared, because a result is recomputed from the grid anyway.
     */
    @Override
    public void removed(Player player) {
        if (bench != null) bench.closed(this);
        super.removed(player);
        result.clearContent();
        if (bench == null) clearContainer(player, grid);   // a loose grid still has to give items back
    }

    @Override
    public boolean stillValid(Player player) {
        return bench == null || Container.stillValidBlockEntity(bench, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int inventoryStart = GRID_END;
        int inventoryEnd = slots.size();
        if (index == 0) {                                   // the result: straight to the player
            if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index < GRID_END) {                      // out of the grid
            if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
            // Into the grid first; failing that, between the two halves of the player's own bags.
            int mainEnd = inventoryStart + 27;
            if (index < mainEnd) {
                if (!moveItemStackTo(stack, mainEnd, inventoryEnd, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, inventoryStart, mainEnd, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    /** For the recipe book and for tests: what the grid currently makes. */
    public ItemStack currentResult() { return result.getItem(0); }

    public Level level() { return player.level(); }
}
