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

    /**
     * Personal vault by default. A camp member gets the shared camp vault (design 3.0 §6) unless they toggled
     * personal mode; sneak-using a Deep Cache or Wayfarer Satchel flips that toggle (persisted as
     * {@code TribalVaultPersonal} in player persistent data) and then opens the chosen vault.
     */
    public static DeepCacheContainer openContainer(ServerPlayer player) {
        var camp = tk.darrow.tribalpower.camp.identity.Camps.campOf(player.server, player.getUUID());
        if (camp != null && player.isShiftKeyDown()) {
            boolean personal = !tk.darrow.tribalpower.camp.identity.Camps.personalVault(player);
            tk.darrow.tribalpower.camp.identity.Camps.setPersonalVault(player, personal);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(personal ? "message.tribalpower.camp.vault_personal" : "message.tribalpower.camp.vault_camp", camp.name), true);
        }
        return openContainer(player.server, player.getUUID(), tk.darrow.tribalpower.camp.identity.Camps.personalVault(player));
    }

    /** Resolve the vault a player id opens: the camp vault when in a camp and not in personal mode, else the personal one. */
    public static DeepCacheContainer openContainer(net.minecraft.server.MinecraftServer server, java.util.UUID playerId, boolean personal) {
        var camps = tk.darrow.tribalpower.camp.identity.Camps.data(server);
        var camp = camps.campOf(playerId);
        if (camp != null && !personal) {
            return new tk.darrow.tribalpower.camp.identity.CampVaultContainer(camps, camp);
        }
        return new DeepCacheContainer(data(server), playerId);
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
