package tk.darrow.tribalpower.tribe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import tk.darrow.tribalpower.config.TribalConfig;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Standing gains, losses, rank-up toasts and listener fan-out (design 3.0 §2 Standing). Every number lives in
 * TribalConfig's tribes section and is read when it is needed, never at class load.
 */
public final class TribeStanding {
    public static int gainFavoured() { return TribalConfig.standingFavoured(); }
    public static int gainReagent() { return TribalConfig.standingReagent(); }
    public static int gainFood() { return TribalConfig.standingFood(); }
    public static int gainPulsePer10() { return TribalConfig.standingPulsePer10(); }
    public static int maxCellDrain() { return TribalConfig.hearthCellDrain(); }
    public static int gainKill() { return TribalConfig.standingKill(); }
    public static int killCapPerDay() { return TribalConfig.killCapPerDay(); }
    public static int offerCapPerDay() { return TribalConfig.offerCapPerDay(); }
    public static int killRadius() { return TribalConfig.killRadius(); }
    public static int gainTrade() { return TribalConfig.standingTrade(); }
    public static int lossHurtKin() { return TribalConfig.standingHurtKin(); }
    public static int lossCampBlock() { return TribalConfig.standingCampBlock(); }
    public static int lossHearth() { return TribalConfig.standingHearth(); }
    public static int hunterAngerTicks() { return TribalConfig.hunterAngerTicks(); }
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
        tk.darrow.tribalpower.quest.QuestEvents.standingChanged(player, tribe);
        // The requested delta, not after-before: personal standing floors at 0, the camp mirror does not.
        for (StandingListener l : LISTENERS) l.onStandingChanged(player, tribe, delta, after);
        return after;
    }

    /**
     * Adds standing from a hearth offering, clamped to {@link #offerCapPerDay()} for that tribe today.
     * @return standing actually granted, which is 0 once the day is spent
     */
    public static int offerGain(ServerPlayer player, TribeDefinition tribe, int want) {
        long day = player.serverLevel().getDayTime() / 24000L;
        int allowed = TribeStandingSavedData.get(player.server).allowOffering(player.getUUID(), tribe, day, offerCapPerDay(), want);
        if (allowed <= 0) return 0;
        add(player, tribe, allowed);
        return allowed;
    }

    /** Offering standing still available today, without spending it. */
    public static int offerRoom(ServerPlayer player, TribeDefinition tribe) {
        long day = player.serverLevel().getDayTime() / 24000L;
        return offerCapPerDay() - TribeStandingSavedData.get(player.server).offeringsToday(player.getUUID(), tribe, day);
    }

    /** Standing per hostile kill within {@link #killRadius()} of a hearth, capped per Minecraft day per tribe. */
    public static boolean killGain(ServerPlayer player, TribeDefinition tribe) {
        long day = player.serverLevel().getDayTime() / 24000L;
        if (!TribeStandingSavedData.get(player.server).tryKillGain(player.getUUID(), tribe, day, killCapPerDay())) return false;
        add(player, tribe, gainKill());
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
