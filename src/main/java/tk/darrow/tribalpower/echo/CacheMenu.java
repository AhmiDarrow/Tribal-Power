package tk.darrow.tribalpower.echo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Six-row cache with packed side-IO synced for the face widget. */
public class CacheMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<CacheMenu>> TYPE =
            tk.darrow.tribalpower.echo.ModMenus.CACHE;
    private final Container container;
    private final ContainerData data;

    public CacheMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(54), new SimpleContainerData(7)); }

    public CacheMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(TYPE.get(), id);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 54);
        checkContainerDataCount(data, 7);
        container.startOpen(inventory.player);
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
        int invY = 103 + 36;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, invY + 58));
        addDataSlots(data);
    }

    public Container container() { return container; }
    public int ioPacked() { return data.get(0); }
    public BlockPos machinePos() {
        return new BlockPos(StationMenu.whole(data.get(4), data.get(1)), StationMenu.whole(data.get(5), data.get(2)), StationMenu.whole(data.get(6), data.get(3)));
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 54) {
            if (!moveItemStackTo(stack, 54, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 54, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
