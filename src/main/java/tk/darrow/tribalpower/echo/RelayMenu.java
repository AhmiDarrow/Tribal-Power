package tk.darrow.tribalpower.echo;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Bond item + rune. Hoppers never see these slots. */
public class RelayMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<RelayMenu>> TYPE =
            StationMenu.MENUS.register("relay", () -> new MenuType<>(RelayMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private final Container container;
    private final ContainerData data;

    public RelayMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(2), new SimpleContainerData(2)); }

    public RelayMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(TYPE.get(), id);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 2);
        addSlot(new Slot(container, 0, 44, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(container, 1, 116, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(1, stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public boolean extracting() { return data.get(0) != 0; }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
