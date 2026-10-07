package tk.darrow.tribalpower.song;

import net.minecraft.stats.Stats;
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
        // The pickup event fires before vanilla's own checks: a stack just thrown, or dropped for someone else, waits.
        if (entity.hasPickUpDelay()) return;
        if (entity.getTarget() != null && !entity.getTarget().equals(event.getPlayer().getUUID())) return;
        int left = absorb(event.getPlayer(), entity.getItem());
        if (left == entity.getItem().getCount()) return;
        ItemStack stack = entity.getItem();
        int taken = stack.getCount() - left;
        stack.setCount(left);
        entity.setItem(stack);
        if (left <= 0) {
            // Vanilla's pickup never runs for a fully absorbed stack: play its part (the fly-to-player animation,
            // the pickup sound on the client, the stat) before the entity goes.
            Player player = event.getPlayer();
            player.take(entity, taken);
            player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), taken);
            event.setCanPickup(TriState.FALSE);
            entity.discard();
        }
    }

    /**
     * Pulls as many reagents as the hotbar pouch can hold. Returns how many remain in the stack. A reagent with a
     * {@link ReagentThread} stays out: the pouch keeps plain counts, and would flatten its Thread away.
     */
    public static int absorb(Player player, ItemStack stack) {
        CreatureProfile profile = Reagents.of(stack.getItem());
        if (profile == null || stack.isEmpty() || ReagentThread.get(stack) > 0) return stack.getCount();
        ItemStack pouch = Reagents.hotbarPouch(player);
        if (pouch == null) return stack.getCount();
        int moved = ReagentPouch.addRaw(pouch, profile, stack.getCount());
        return stack.getCount() - moved;
    }
}
