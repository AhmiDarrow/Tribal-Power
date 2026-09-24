package tk.darrow.tribalpower.echo;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import tk.darrow.tribalpower.blockentity.EchoStationBlockEntity;

/** Echo station: input, two catalyst slots, eight outputs, then the player's inventory. */
public class StationMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<StationMenu>> TYPE = ModMenus.STATION;
    public static final int WIDTH = 208, HEIGHT = 212;
    public static final int INPUT_X = 15, INPUT_Y = 36, CATALYST_Y = 70, OUTPUT_X = 76, OUTPUT_Y = 34;
    public static final int INVENTORY_X = 24, INVENTORY_Y = 130;
    private static final int MACHINE_SLOTS = EchoStationBlockEntity.SIZE;
    private static final String[] STATES = {"idle", "working", "paused", "full", "attunement", "pulse", "quiet", "catalyst"};
    private final Container container;
    private final ContainerData data;

    public StationMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(11));
    }

    public StationMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(TYPE.get(), id);
        this.container = container;
        this.data = data;
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, 11);
        container.startOpen(inventory.player);
        addSlot(new Slot(container, 0, INPUT_X, INPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); }
        });
        for (int i = 0; i < 8; i++) addSlot(new Slot(container, i + 1, OUTPUT_X + (i % 4) * 18, OUTPUT_Y + (i / 4) * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int i = 0; i < 2; i++) {
            int slot = EchoStationBlockEntity.CATALYST_A + i;
            addSlot(new Slot(container, slot, INPUT_X + i * 18, CATALYST_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(slot, stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        addDataSlots(data);
    }

    public int ioPacked() { return data.get(3); }
    /** Menu data travels as shorts, so each coordinate comes in two halves; a machine far from spawn stays addressable. */
    public net.minecraft.core.BlockPos machinePos() {
        return new net.minecraft.core.BlockPos(whole(data.get(8), data.get(4)), whole(data.get(9), data.get(5)), whole(data.get(10), data.get(6)));
    }
    public static int whole(int high, int low) { return (high << 16) | (low & 0xFFFF); }
    public int work() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public int pulsePerSecond() { return data.get(7); }
    public ItemStack input() { return container.getItem(0); }
    public String state() { return STATES[Math.max(0, Math.min(STATES.length - 1, data.get(2)))]; }
    public String statusKey() { return "message.tribalpower.station." + state(); }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (container.canPlaceItem(0, stack) && moveItemStackTo(stack, 0, 1, false)) {
            // Anything the station can work goes to the input first.
        } else if (!container.canPlaceItem(EchoStationBlockEntity.CATALYST_A, stack) || !moveItemStackTo(stack, 9, 11, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
