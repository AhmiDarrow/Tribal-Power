package tk.darrow.tribalpower.song;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.CreatureItems;

/** Creature reagents, looked up by item or by the id stored on a sheet. */
public final class Reagents {
    private static final Map<String, CreatureProfile> BY_ID = new java.util.HashMap<>();

    static {
        for (CreatureProfile profile : CreatureProfile.values()) {
            BY_ID.put(profile.reagent, profile);
        }
    }

    private Reagents() {}

    public static @Nullable CreatureProfile byId(String id) {
        return id == null || id.isEmpty() ? null : BY_ID.get(id);
    }

    public static @Nullable CreatureProfile of(Item item) {
        if (item == null) return null;
        var key = BuiltInRegistries.ITEM.getKey(item);
        if (!key.getNamespace().equals(tk.darrow.tribalpower.TribalPower.MOD_ID)) return null;
        return BY_ID.get(key.getPath());
    }

    public static boolean isReagent(ItemStack stack) {
        return !stack.isEmpty() && of(stack.getItem()) != null;
    }

    public static Item item(CreatureProfile profile) {
        return CreatureItems.REAGENTS.get(profile).get();
    }

    /**
     * How many of this reagent one withdrawal can put in the main inventory.
     * A click moves at most one stack, and room is the sum of every partial stack plus one empty slot.
     */
    public static int inventoryRoom(Player player, CreatureProfile profile) {
        Item item = item(profile);
        int limit = new ItemStack(item).getMaxStackSize();
        int room = 0;
        boolean empty = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) empty = true;
            else if (stack.is(item)) room += limit - stack.getCount();
        }
        if (empty) room += limit;
        return Math.min(limit, Math.max(room, 0));
    }

    /** The pouch a pickup should fill: the selected hotbar pouch, otherwise the first one on the hotbar. */
    public static @Nullable ItemStack hotbarPouch(Player player) {
        ItemStack selected = player.getInventory().getSelected();
        if (selected.getItem() instanceof ReagentPouchItem) return selected;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof ReagentPouchItem) return stack;
        }
        return null;
    }

    public static Map<Note, java.util.List<CreatureProfile>> byNote() {
        Map<Note, java.util.List<CreatureProfile>> grouped = new EnumMap<>(Note.class);
        for (Note note : Note.values()) grouped.put(note, new java.util.ArrayList<>());
        for (CreatureProfile profile : CreatureProfile.values()) grouped.get(Note.of(profile)).add(profile);
        for (var list : grouped.values()) list.sort(java.util.Comparator.comparing(profile -> profile.reagent));
        return grouped;
    }
}
