package tk.darrow.tribalpower.tribe;

import net.minecraft.server.level.ServerPlayer;

/**
 * Hook for other systems (shared camps mirror gains at 25%). Register with {@link TribeStanding#addListener}.
 */
@FunctionalInterface
public interface StandingListener {
    /**
     * @param player  player whose standing changed
     * @param tribe   tribe concerned
     * @param delta   applied change (may be negative)
     * @param total   new standing after the change
     */
    void onStandingChanged(ServerPlayer player, TribeDefinition tribe, int delta, int total);
}
