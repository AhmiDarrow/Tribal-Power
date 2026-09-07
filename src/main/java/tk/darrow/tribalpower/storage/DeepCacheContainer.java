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

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
