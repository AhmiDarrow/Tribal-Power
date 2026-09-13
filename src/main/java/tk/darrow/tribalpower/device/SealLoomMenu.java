package tk.darrow.tribalpower.device;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SealLoomMenu extends AbstractContainerMenu {
    private final Container container;

    public SealLoomMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SealLoomBlockEntity.SIZE));
    }

    public SealLoomMenu(int id, Inventory inventory, Container container) {
        super(DeviceRegistry.SEAL_LOOM_MENU.get(), id);
        this.container = container;
        checkContainerSize(container, SealLoomBlockEntity.SIZE);
        container.startOpen(inventory.player);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new Slot(container, col + row * 3, 26 + col * 18, 18 + row * 18));
        addSlot(new Slot(container, SealLoomBlockEntity.SEAL, 90, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof RecipeSealItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(container, SealLoomBlockEntity.OUTPUT + i, 116 + (i % 3) * 18, 18 + (i / 3) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < SealLoomBlockEntity.SIZE) {
            if (!moveItemStackTo(stack, SealLoomBlockEntity.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof RecipeSealItem) {
            if (!moveItemStackTo(stack, SealLoomBlockEntity.SEAL, SealLoomBlockEntity.SEAL + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SealLoomBlockEntity.GRID, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
