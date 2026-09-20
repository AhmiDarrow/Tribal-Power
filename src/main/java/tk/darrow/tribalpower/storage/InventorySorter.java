package tk.darrow.tribalpower.storage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tidying for any inventory the mod is willing to touch: the player's own pack, and plain storage such as
 * chests, barrels, shulkers, hoppers and the mod's caches. Stacks of a kind are merged and then laid out in
 * a stable order.
 *
 * <p>A machine's slots each take only their own thing, so they are left alone: a group is only sorted when
 * every slot in it is an ordinary slot of one container and will accept everything the group holds. The
 * same test runs on both sides, so the client only draws a button for a group the server would sort.</p>
 */
public final class InventorySorter {
    /** Order: by the item's own name, then its display name, then fullest stack first. */
    private static final Comparator<ItemStack> ORDER = Comparator
            .comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
            .thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
            .thenComparing(stack -> stack.getHoverName().getString())
            .thenComparing(stack -> -stack.getCount())
            .thenComparing(ItemStack::getDamageValue);

    private InventorySorter() {}

    /** The slots a sort would move, or an empty list when that side cannot be sorted. */
    public static List<Slot> group(AbstractContainerMenu menu, Player player, boolean containerSide) {
        Inventory inventory = player.getInventory();
        List<Slot> group = new ArrayList<>();
        for (Slot slot : menu.slots) {
            boolean own = slot.container == inventory;
            if (own == containerSide) continue;
            // The hotbar and the equipment slots stay where the player put them.
            if (own && (slot.getContainerSlot() < Inventory.getSelectionSize() || slot.getContainerSlot() > 35)) continue;
            group.add(slot);
        }
        if (group.size() < 2) return List.of();
        return containerSide && !plainStorage(group) ? List.of() : group;
    }

    public static boolean sortable(AbstractContainerMenu menu, Player player, boolean containerSide) {
        List<Slot> group = group(menu, player, containerSide);
        if (group.isEmpty()) return false;
        for (Slot slot : group) if (slot.hasItem()) return true;
        return false;
    }

    /** True when every slot belongs to one container, is an ordinary slot, and takes what the group holds. */
    private static boolean plainStorage(List<Slot> group) {
        Container container = group.get(0).container;
        if (container == null) return false;
        for (Slot slot : group) {
            if (slot.container != container) return false;
            if (slot.getClass() != Slot.class && !(slot instanceof ShulkerBoxSlot)) return false;
        }
        if (group.size() != container.getContainerSize()) return false;
        for (Slot slot : group) {
            for (Slot other : group) {
                ItemStack held = other.getItem();
                if (!held.isEmpty() && !slot.mayPlace(held)) return false;
            }
        }
        return true;
    }

    /** Merge and order one side of the open menu. Returns false when there was nothing to do. */
    public static boolean sort(Player player, boolean containerSide) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return false;
        List<Slot> group = group(menu, player, containerSide);
        if (group.isEmpty()) return false;

        List<ItemStack> stacks = new ArrayList<>();
        for (Slot slot : group) {
            ItemStack held = slot.getItem();
            if (!held.isEmpty()) stacks.add(held.copy());
        }
        if (stacks.isEmpty()) return false;

        List<ItemStack> merged = merge(stacks, group.get(0));
        merged.sort(ORDER);
        if (merged.size() > group.size()) return false;
        // Never half-sort: if any stack would not go back where it is headed, nothing moves.
        for (int i = 0; i < merged.size(); i++) {
            if (!group.get(i).mayPlace(merged.get(i))) return false;
        }
        if (unchanged(group, merged)) return false;

        for (Slot slot : group) slot.set(ItemStack.EMPTY);
        for (int i = 0; i < merged.size(); i++) group.get(i).set(merged.get(i));
        menu.broadcastChanges();
        return true;
    }

    private static boolean unchanged(List<Slot> group, List<ItemStack> merged) {
        for (int i = 0; i < group.size(); i++) {
            ItemStack was = group.get(i).getItem();
            ItemStack now = i < merged.size() ? merged.get(i) : ItemStack.EMPTY;
            if (!ItemStack.matches(was, now)) return false;
        }
        return true;
    }

    private static List<ItemStack> merge(List<ItemStack> stacks, Slot slot) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            int cap = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
            boolean pooled = false;
            for (ItemStack kept : out) {
                if (kept.getCount() >= cap || !ItemStack.isSameItemSameComponents(kept, stack)) continue;
                int moved = Math.min(cap - kept.getCount(), stack.getCount());
                kept.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) { pooled = true; break; }
            }
            if (!pooled && !stack.isEmpty()) out.add(stack);
        }
        return out;
    }
}
