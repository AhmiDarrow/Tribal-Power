package tk.darrow.tribalpower.ley;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.item.SpiritGear;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Who is looking at the ropes, and the occasional packet that tells their client which veins those are. */
public final class LeyRopes {
    private static final Map<UUID, Long> SENT = new HashMap<>();

    private LeyRopes() {}

    /**
     * Ley ropes from a lens left on ley sight. A lens set to off steps aside, and open goggles
     * show the ropes instead. Goggles that have been turned off stay dark.
     */
    public static boolean sees(Player player) {
        ItemStack lens = LeyLensItem.held(player);
        if (!lens.isEmpty() && LeyLensItem.mode(lens) != LeyLensItem.OFF)
            return LeyLensItem.mode(lens) == LeyLensItem.LEY;
        return goggles(player);
    }

    /** Worn Spiritweave Hood whose ley goggles are fitted and switched on. */
    public static boolean goggles(Player player) {
        ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
        return SpiritGear.gogglesOpen(hood);
    }

    /** At most once every half second. The thread between packets is animated on the client. */
    public static void sync(ServerPlayer player) {
        if (!sees(player)) return;
        long now = player.serverLevel().getGameTime();
        if (now % 10 != 0) return;
        Long last = SENT.get(player.getUUID());
        if (last != null && last == now) return;
        SENT.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer(player, LeyRopePayload.capture(player));
    }

    /** Drop the send clock so a long-running server does not keep every player who ever looked. */
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
