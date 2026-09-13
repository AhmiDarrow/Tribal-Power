package tk.darrow.tribalpower.charm;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import tk.darrow.tribalpower.echo.StationMenu;

public class CharmMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<CharmMenu>> TYPE =
            StationMenu.MENUS.register("charm_slots", () -> new MenuType<>(CharmMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private final CharmInventory charms;

    public CharmMenu(int id, Inventory inventory) {
        this(id, inventory, inventory.player instanceof Player player ? CharmSlots.of(player) : new CharmInventory());
    }

    public CharmMenu(int id, Inventory inventory, CharmInventory charms) {
        super(TYPE.get(), id);
        this.charms = charms;
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            addSlot(new Slot(charms, i, 62 + i * 18, 20) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SpiritCharmItem; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 54 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 112));
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < CharmInventory.SIZE) {
            if (!moveItemStackTo(stack, CharmInventory.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof SpiritCharmItem)
                || !moveItemStackTo(stack, 0, CharmInventory.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
