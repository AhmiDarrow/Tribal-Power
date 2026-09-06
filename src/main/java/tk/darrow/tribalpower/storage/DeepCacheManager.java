package tk.darrow.tribalpower.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Dimension-linked bulk storage accessed via Deep Cache blocks.
 * Inventories persist on the overworld {@link DeepCacheSavedData}.
 */
public final class DeepCacheManager {
    public static final int LINK_PULSE_COST = 5;

    private DeepCacheManager() {}

    public static DeepCacheSavedData data(MinecraftServer server) {
        return DeepCacheSavedData.get(server);
    }

    public static DeepCacheContainer openContainer(ServerPlayer player) {
        return new DeepCacheContainer(data(player.server), player.getUUID());
    }

    public static boolean hasSpiritLink(ServerPlayer player) {
        DeepCacheSavedData saved = data(player.server);
        if (saved.hasVisitedMarch(player.getUUID())) {
            return true;
        }
        return player.level().dimension().equals(ModDimensions.THE_MARCH);
    }

    public static void markVisited(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            data(serverPlayer.server).markVisitedMarch(serverPlayer.getUUID());
        }
    }

    /**
     * Pay Pulse from cells when the player has never linked to The March.
     * @return true if access is allowed
     */
    public static boolean tryAuthorize(ServerPlayer player) {
        if (hasSpiritLink(player)) {
            return true;
        }
        return consumePulseFromCells(player, LINK_PULSE_COST);
    }

    public static boolean consumePulseFromCells(ServerPlayer player, int cost) {
        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            available += PulseCellItem.getPulse(stack);
        }
        if (available < cost) {
            return false;
        }
        int remaining = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            remaining -= PulseCellItem.extractPulse(stack, remaining, false);
        }
        return remaining <= 0;
    }

    public static boolean isMarchDimension(ServerLevel level) {
        return level.dimension().equals(ModDimensions.THE_MARCH);
    }
}
