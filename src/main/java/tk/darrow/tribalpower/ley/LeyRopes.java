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
    /** The last half-second beat each player could see the ropes. */
    private static final Map<UUID, Long> SENT = new HashMap<>();
    /** The ropes each player's client last received: the same set is not sent again every half second. */
    private static final Map<UUID, LeyRopePayload> SHOWN = new HashMap<>();

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

    /**
     * At most once every half second, and only when the ropes differ from what the client already holds. The
     * thread between packets is animated on the client.
     */
    public static void sync(ServerPlayer player) {
        // The clock first: worn goggles call this every tick, and nine ticks in ten would send nothing anyway.
        long now = player.serverLevel().getGameTime();
        if (now % 10 != 0) return;
        if (!sees(player)) return;
        UUID id = player.getUUID();
        Long last = SENT.get(id);
        if (last != null && last == now) return;
        SENT.put(id, now);
        // Sight that lapsed since the last beat (goggles or lens just put on) always gets the ropes afresh.
        if (last == null || now - last != 10) SHOWN.remove(id);
        LeyRopePayload ropes = LeyRopePayload.capture(player);
        if (ropes.equals(SHOWN.get(id))) return;
        SHOWN.put(id, ropes);
        PacketDistributor.sendToPlayer(player, ropes);
    }

    /** A new world, a respawn or a new login: the next beat sends the ropes whether or not they changed. */
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }

    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }

    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }

    /** Drop the send clock so a long-running server does not keep every player who ever looked. */
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
        SHOWN.remove(event.getEntity().getUUID());
        LensPulsePayload.forget(event.getEntity().getUUID());
        LeySightPayload.forget(event.getEntity().getUUID());
    }
}
