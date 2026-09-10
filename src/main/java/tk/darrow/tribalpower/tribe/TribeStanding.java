package tk.darrow.tribalpower.tribe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Standing gains, losses, rank-up toasts and listener fan-out (design 3.0 §2 Standing). */
public final class TribeStanding {
    public static final int GAIN_FAVOURED = 3;
    public static final int GAIN_REAGENT = 8;
    public static final int GAIN_FOOD = 1;
    public static final int GAIN_PULSE_PER_10 = 2;
    public static final int MAX_CELL_DRAIN = 40;
    public static final int GAIN_KILL = 1;
    public static final int KILL_CAP_PER_DAY = 20;
    /**
     * Standing a player can take from one tribe's hearth in a Minecraft day (design 3.1 section 7.7).
     * The Listening Pit makes ore renewable, and the hearth is what gates the pit; without this cap the
     * pit would feed the standing that unlocked it.
     */
    public static final int OFFER_CAP_PER_DAY = 60;
    public static final int KILL_RADIUS = 24;
    public static final int GAIN_TRADE = 2;
    public static final int LOSS_HURT_KIN = -25;
    public static final int LOSS_CAMP_BLOCK = -5;
    public static final int LOSS_HEARTH = -40;
    public static final int HUNTER_ANGER_TICKS = 60 * 20;

    private static final List<StandingListener> LISTENERS = new CopyOnWriteArrayList<>();

    private TribeStanding() {}

    public static void addListener(StandingListener listener) { LISTENERS.add(listener); }
    public static void removeListener(StandingListener listener) { LISTENERS.remove(listener); }

    public static int get(MinecraftServer server, UUID player, TribeDefinition tribe) {
        return TribeStandingSavedData.get(server).get(player, tribe);
    }

    public static TribeRank rank(MinecraftServer server, UUID player, TribeDefinition tribe) {
        return TribeRank.of(get(server, player, tribe));
    }

    public static TribeRank rank(ServerPlayer player, TribeDefinition tribe) {
        return rank(player.server, player.getUUID(), tribe);
    }

    /**
     * Applies {@code delta}, announces rank changes and notifies listeners.
     * @return new standing
     */
    public static int add(ServerPlayer player, TribeDefinition tribe, int delta) {
        if (delta == 0) return get(player.server, player.getUUID(), tribe);
        TribeStandingSavedData data = TribeStandingSavedData.get(player.server);
        int before = data.get(player.getUUID(), tribe);
        int after = data.set(player.getUUID(), tribe, before + delta);
        TribeRank was = TribeRank.of(before);
        TribeRank now = TribeRank.of(after);
        if (now.ordinal() > was.ordinal()) {
            player.sendSystemMessage(Component.translatable("message.tribalpower.standing.rank_up",
                    tribe.displayNameComponent(), Component.translatable(now.translationKey())).withStyle(colour(tribe)));
            // "On good terms" is about reaching Friend by any route (offerings, trades, kills), not only via the hearth.
            if (now.ordinal() >= TribeRank.FRIEND.ordinal())
                tk.darrow.tribalpower.camp.CampHooks.award(player.serverLevel(), player.getUUID(), "tribes/friend");
        } else if (now.ordinal() < was.ordinal()) {
            player.sendSystemMessage(Component.translatable("message.tribalpower.standing.rank_down",
                    tribe.displayNameComponent(), Component.translatable(now.translationKey())).withStyle(ChatFormatting.RED));
        }
        for (StandingListener l : LISTENERS) l.onStandingChanged(player, tribe, after - before, after);
        return after;
    }

    /**
     * Adds standing from a hearth offering, clamped to {@link #OFFER_CAP_PER_DAY} for that tribe today.
     * @return standing actually granted, which is 0 once the day is spent
     */
    public static int offerGain(ServerPlayer player, TribeDefinition tribe, int want) {
        long day = player.serverLevel().getDayTime() / 24000L;
        int allowed = TribeStandingSavedData.get(player.server).allowOffering(player.getUUID(), tribe, day, OFFER_CAP_PER_DAY, want);
        if (allowed <= 0) return 0;
        add(player, tribe, allowed);
        return allowed;
    }

    /** +1 per hostile kill within {@link #KILL_RADIUS} of a hearth, capped per Minecraft day per tribe. */
    public static boolean killGain(ServerPlayer player, TribeDefinition tribe) {
        long day = player.serverLevel().getDayTime() / 24000L;
        if (!TribeStandingSavedData.get(player.server).tryKillGain(player.getUUID(), tribe, day, KILL_CAP_PER_DAY)) return false;
        add(player, tribe, GAIN_KILL);
        return true;
    }

    public static Style colour(TribeDefinition tribe) {
        return Style.EMPTY.withColor(TextColor.fromRgb(tribe.colour()));
    }

    public static Component report(MinecraftServer server, UUID player) {
        var text = Component.literal("");
        int[] all = TribeStandingSavedData.get(server).all(player);
        for (TribeDefinition tribe : TribeDefinition.values()) {
            int value = all[tribe.ordinal()];
            text.append(Component.literal("\n").append(tribe.displayNameComponent().copy().withStyle(colour(tribe)))
                    .append(Component.literal(": " + value + " (").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(TribeRank.of(value).translationKey()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
        }
        return text;
    }
}
