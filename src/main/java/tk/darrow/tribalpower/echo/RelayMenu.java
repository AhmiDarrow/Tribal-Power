package tk.darrow.tribalpower.echo;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;

/**
 * Rune, channel tabs, and the selected channel's eight-slot whitelist or blacklist. Filter slots hold ghost copies: clicking one with an item marks it,
 * clicking with an empty hand clears it, and nothing is ever taken from the player. Hoppers never see any of it.
 */
public class RelayMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<RelayMenu>> TYPE =
            tk.darrow.tribalpower.echo.ModMenus.RELAY;
    public static final int RUNE_SLOT = 0, FILTER_START = 1, FILTER_END = FILTER_START + WirelessRelayBlockEntity.FILTERS;
    /** Menu button ids: flip the selected channel's list, pick channel n (SELECT + n), switch routing, unlink. */
    public static final int TOGGLE_ALLOW = 0, SELECT = 1, TOGGLE_ROUTE = SELECT + WirelessRelayBlockEntity.MAX_CHANNELS, UNLINK = TOGGLE_ROUTE + 1;
    public static final int DATA_SELECTED = 0, DATA_ALLOW = 1, DATA_CHANNELS = 2, DATA_ROUND_ROBIN = 3, DATA_LINKED = 4;
    /** Seven shorts per channel: coordinate halves, then face and flags. See {@link WirelessRelayBlockEntity#channelSync}. */
    public static final int DATA_CHAN = 5, CHAN_STRIDE = 7, DATA_COUNT = DATA_CHAN + WirelessRelayBlockEntity.MAX_CHANNELS * CHAN_STRIDE;
    private final Container container;
    private final Container filters;
    private final ContainerData data;

    public RelayMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainer(WirelessRelayBlockEntity.FILTERS), new SimpleContainerData(DATA_COUNT));
    }

    public RelayMenu(int id, Inventory inventory, Container container, Container filters, ContainerData data) {
        super(TYPE.get(), id);
        this.container = container;
        this.filters = filters;
        this.data = data;
        checkContainerSize(container, 2);
        checkContainerSize(filters, WirelessRelayBlockEntity.FILTERS);
        checkContainerDataCount(data, DATA_COUNT);
        addSlot(new Slot(container, WirelessRelayBlockEntity.RUNE, 14, 40) {
            @Override public boolean mayPlace(ItemStack stack) { return WirelessRelayBlockEntity.isRune(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < WirelessRelayBlockEntity.FILTERS; i++) {
            addSlot(new Slot(filters, i, 42 + (i % 4) * 18, 31 + (i / 4) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    /** True for a whitelist (only listed goods pass), false for a blacklist; of the selected channel. */
    public boolean allowing() { return data.get(DATA_ALLOW) != 0; }
    public int selected() { return data.get(DATA_SELECTED); }
    public int channels() { return Math.max(1, data.get(DATA_CHANNELS)); }
    public boolean roundRobin() { return data.get(DATA_ROUND_ROBIN) != 0; }
    public boolean linked(int channel) { return (data.get(DATA_LINKED) & (1 << channel)) != 0; }

    /** Destination of a channel, synced in 16-bit halves so a far astral mark still arrives whole. */
    public boolean hasTarget(int channel) { return (channelMeta(channel) & 8) != 0; }
    public boolean sameWorld(int channel) { return (channelMeta(channel) & 16) != 0; }
    public int targetFace(int channel) { return channelMeta(channel) & 7; }
    public int targetCoord(int channel, int axis) {
        if (channel < 0 || channel >= WirelessRelayBlockEntity.MAX_CHANNELS || axis < 0 || axis > 2) return 0;
        int base = DATA_CHAN + channel * CHAN_STRIDE + axis * 2;
        return ((data.get(base + 1) & 0xFFFF) << 16) | (data.get(base) & 0xFFFF);
    }
    private int channelMeta(int channel) {
        if (channel < 0 || channel >= WirelessRelayBlockEntity.MAX_CHANNELS) return 0;
        return data.get(DATA_CHAN + channel * CHAN_STRIDE + 6);
    }

    public static boolean isFilter(int slot) { return slot >= FILTER_START && slot < FILTER_END; }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (isFilter(slotId)) {
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.SWAP) {
                ItemStack carried = getCarried();
                filters.setItem(slotId - FILTER_START, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            }
            return;   // a ghost slot never trades a real item
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof WirelessRelayBlockEntity relay)) return id >= TOGGLE_ALLOW && id <= UNLINK;
        if (id == TOGGLE_ALLOW) relay.toggleAllow(null);
        else if (id >= SELECT && id < SELECT + WirelessRelayBlockEntity.MAX_CHANNELS) relay.select(id - SELECT);
        else if (id == TOGGLE_ROUTE) relay.toggleRoundRobin();
        else if (id == UNLINK) relay.unlink(relay.selected());
        else return false;
        return true;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == RUNE_SLOT) {
            if (!moveItemStackTo(stack, FILTER_END, slots.size(), true)) return ItemStack.EMPTY;
        } else if (WirelessRelayBlockEntity.isRune(stack)) {
            if (!moveItemStackTo(stack, RUNE_SLOT, RUNE_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            // Shift-clicking goods marks them in the filter; the stack stays in the inventory.
            int free = -1;
            for (int i = 0; i < filters.getContainerSize(); i++) {
                ItemStack entry = filters.getItem(i);
                if (!entry.isEmpty() && ItemStack.isSameItem(entry, stack)) return ItemStack.EMPTY;
                if (entry.isEmpty() && free < 0) free = i;
            }
            if (free >= 0) filters.setItem(free, stack.copyWithCount(1));
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
