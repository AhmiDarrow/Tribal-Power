package tk.darrow.tribalpower.cuisine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Four ingredients in a square, the bowl beside them, the meal on the right. */
public class HearthPotMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public HearthPotMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(HearthPotBlockEntity.SIZE), new SimpleContainerData(3));
    }

    public HearthPotMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(CuisineRegistry.HEARTH_POT_MENU.get(), id);
        this.container = container;
        this.data = data;
        checkContainerDataCount(data, 3);
        for (int i = 0; i < 4; i++) addSlot(new Slot(container, i, 34 + (i % 2) * 18, 26 + (i / 2) * 18));
        addSlot(new Slot(container, HearthPotBlockEntity.CONTAINER, 80, 53));
        addSlot(new Slot(container, HearthPotBlockEntity.OUTPUT, 124, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public int total() { return Math.max(1, data.get(1)); }
    public int state() { return data.get(2); }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int size = HearthPotBlockEntity.SIZE;
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // A bowl or bottle goes to the container seat; everything else to the first free ingredient seat.
            boolean vessel = stack.is(net.minecraft.world.item.Items.BOWL) || stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE) || stack.is(net.minecraft.world.item.Items.BUCKET);
            boolean moved = vessel ? moveItemStackTo(stack, HearthPotBlockEntity.CONTAINER, HearthPotBlockEntity.CONTAINER + 1, false)
                    : moveItemStackTo(stack, 0, 4, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
