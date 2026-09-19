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
     * @param delta   requested change (may be negative; not reduced by the personal floor at 0)
     * @param total   new standing after the change
     */
    void onStandingChanged(ServerPlayer player, TribeDefinition tribe, int delta, int total);
}
