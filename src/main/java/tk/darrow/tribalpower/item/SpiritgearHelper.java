package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared Pulse drain for Spiritgear — prefers held Pulse Cells, then inventory.
 */
public final class SpiritgearHelper {
    public static final int MINE_COST = 2;
    public static final int HIT_COST = 3;
    public static final int USE_COST = 1;

    private SpiritgearHelper() {}

    public static int availablePulse(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PulseCellItem) {
                total += PulseCellItem.getPulse(stack);
            }
        }
        return total;
    }

    public static boolean tryConsumePulse(Player player, int amount) {
        return reservePulse(player, amount) != null;
    }

    /** A refundable charge for actions that other mods can cancel. */
    public static PulseCharge reservePulse(Player player, int amount) {
        var charge = new PulseCharge(player);
        if (amount <= 0 || player.getAbilities().instabuild) return charge;
        if (availablePulse(player) < amount) return null;
        int remaining = amount;
        remaining -= charge.drain(player.getOffhandItem(), remaining);
        remaining -= charge.drain(player.getMainHandItem(), remaining);
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            remaining -= charge.drain(player.getInventory().getItem(i), remaining);
        }
        player.getInventory().setChanged();
        if (remaining > 0) { charge.refund(); return null; }
        return charge;
    }

    public static final class PulseCharge {
        private record Drain(ItemStack stack, int amount) {}
        private final Player player;
        private final java.util.List<Drain> drains = new java.util.ArrayList<>();
        private PulseCharge(Player player) { this.player = player; }
        private int drain(ItemStack stack, int amount) {
            int taken = PulseCellItem.extractPulse(stack, amount, false);
            if (taken > 0) drains.add(new Drain(stack, taken));
            return taken;
        }
        public void refund() {
            for (Drain drain : drains) PulseCellItem.insertPulse(drain.stack(), drain.amount(), false);
            drains.clear();
            player.getInventory().setChanged();
        }
    }

    public static void notifyStarved(Player player) {
        player.displayClientMessage(Component.translatable("message.tribalpower.spiritgear.starved"), true);
    }

    public static void notifyFueled(Player player) {
        if (player.level().random.nextInt(12) == 0) {
            player.displayClientMessage(Component.translatable("message.tribalpower.spiritgear.pulse"), true);
        }
    }
}
