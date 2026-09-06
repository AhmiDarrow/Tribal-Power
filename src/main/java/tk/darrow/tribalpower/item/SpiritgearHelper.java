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
        if (amount <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        if (availablePulse(player) < amount) {
            return false;
        }

        int remaining = amount;
        remaining -= drainHand(player.getOffhandItem(), remaining);
        if (remaining > 0) {
            remaining -= drainHand(player.getMainHandItem(), remaining);
        }
        if (remaining > 0) {
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                remaining -= PulseCellItem.extractPulse(player.getInventory().getItem(i), remaining, false);
            }
        }
        return remaining <= 0;
    }

    private static int drainHand(ItemStack stack, int amount) {
        if (amount <= 0 || !(stack.getItem() instanceof PulseCellItem)) {
            return 0;
        }
        return PulseCellItem.extractPulse(stack, amount, false);
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
