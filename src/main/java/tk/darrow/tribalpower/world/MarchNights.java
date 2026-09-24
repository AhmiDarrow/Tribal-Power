package tk.darrow.tribalpower.world;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

/**
 * Sleeping through a March night. The March keeps the overworld's clock, and a dimension that borrows a
 * clock cannot turn it: its players would wake at the same midnight they lay down in. So when they sleep
 * the night out, the overworld's clock is the one moved forward.
 */
public final class MarchNights {
    private MarchNights() {}

    public static void sleepFinished(SleepFinishedTimeEvent event) {
        if (!tk.darrow.tribalpower.config.TribalConfig.sleepSkipsNight()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(ModDimensions.THE_MARCH)) return;
        var overworld = level.getServer().overworld();
        overworld.setDayTime(event.getNewTime());
        // The March borrows the overworld's weather too, so the rain that a night's sleep ends is the overworld's.
        if (overworld.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE) && overworld.isRaining())
            overworld.setWeatherParameters(0, 0, false, false);
    }
}
