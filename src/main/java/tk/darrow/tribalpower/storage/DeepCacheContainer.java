package tk.darrow.tribalpower.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Live view of a player's Deep Cache slots backed by {@link DeepCacheSavedData}.
 */
public class DeepCacheContainer implements Container {
    private final DeepCacheSavedData data;
    private final UUID owner;
    private final NonNullList<ItemStack> items;

    public DeepCacheContainer(DeepCacheSavedData data, UUID owner) {
        this.data = data;
        this.owner = owner;
        this.items = data.getOrCreateItems(owner);
    }

    public UUID getOwner() {
        return owner;
    }

    @Override
    public int getContainerSize() {
        return DeepCacheSavedData.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        data.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getUUID().equals(owner);
    }

    /** A Wayfarer Satchel is the key to this vault: it cannot be locked inside it. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !(stack.getItem() instanceof tk.darrow.tribalpower.item.WayfarerSatchelItem);
    }

    /**
     * A chest menu's slots take anything; swap the vault's for ones that ask {@link #canPlaceItem}, so a satchel is
     * refused by a click, a shift-click and a drag alike. The player's own slots are left as they are.
     */
    public static <M extends net.minecraft.world.inventory.AbstractContainerMenu> M guard(M menu, net.minecraft.world.Container vault) {
        for (int i = 0; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot old = menu.slots.get(i);
            if (old.container != vault) continue;
            net.minecraft.world.inventory.Slot guarded = new net.minecraft.world.inventory.Slot(vault, old.getContainerSlot(), old.x, old.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return vault.canPlaceItem(getContainerSlot(), stack);
                }
            };
            guarded.index = old.index;
            menu.slots.set(i, guarded);
        }
        return menu;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
