package tk.darrow.tribalpower.song;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import tk.darrow.tribalpower.entity.CreatureProfile;

/** While a pouch is on the hotbar, reagents picked up off the ground go into it. */
public final class ReagentPouchHooks {
    private ReagentPouchHooks() {}

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        int left = absorb(event.getPlayer(), entity.getItem());
        if (left == entity.getItem().getCount()) return;
        ItemStack stack = entity.getItem();
        stack.setCount(left);
        entity.setItem(stack);
        if (left <= 0) {
            event.setCanPickup(TriState.FALSE);
            entity.discard();
        }
    }

    /** Pulls as many reagents as the hotbar pouch can hold. Returns how many remain in the stack. */
    public static int absorb(Player player, ItemStack stack) {
        CreatureProfile profile = Reagents.of(stack.getItem());
        if (profile == null || stack.isEmpty()) return stack.getCount();
        ItemStack pouch = Reagents.hotbarPouch(player);
        if (pouch == null) return stack.getCount();
        int moved = ReagentPouch.addRaw(pouch, profile, stack.getCount());
        return stack.getCount() - moved;
    }
}
