package tk.darrow.tribalpower.echo;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.*;

public class StationMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "tribalpower");
    public static final DeferredHolder<MenuType<?>, MenuType<StationMenu>> TYPE = MENUS.register("echo_station", () -> new MenuType<>(StationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private final Container container;
    private final ContainerData data;
    public StationMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(9), new SimpleContainerData(3)); }
    public StationMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(TYPE.get(), id); this.container = container; this.data = data;
        checkContainerSize(container, 9); checkContainerDataCount(data, 3);
        container.startOpen(inventory.player);
        addSlot(new Slot(container, 0, 26, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); }
        });
        for (int i = 0; i < 8; i++) addSlot(new Slot(container, i + 1, 89 + (i % 4) * 18, 33 + (i / 4) * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        addDataSlots(data);
    }
    public int work() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public String statusKey() { return "message.tribalpower.station." + new String[]{"idle", "working", "paused", "full", "attunement", "pulse"}[Math.max(0, Math.min(5, data.get(2)))]; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 9) { if (!moveItemStackTo(stack, 9, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!container.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
