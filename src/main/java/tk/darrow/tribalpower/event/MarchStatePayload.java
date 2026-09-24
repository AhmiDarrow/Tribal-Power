package tk.darrow.tribalpower.event;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/** Server → client: which March weathers run, which voice surges and for how long, and whose festival it is. */
public record MarchStatePayload(int weatherMask, int surgeVoice, long surgeUntil, int festivalTribe) implements CustomPacketPayload {
    public static final Type<MarchStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "march_state"));
    public static final StreamCodec<ByteBuf, MarchStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MarchStatePayload::weatherMask, ByteBufCodecs.INT, MarchStatePayload::surgeVoice,
            ByteBufCodecs.VAR_LONG, MarchStatePayload::surgeUntil, ByteBufCodecs.INT, MarchStatePayload::festivalTribe, MarchStatePayload::new);
    public static final MarchStatePayload EMPTY = new MarchStatePayload(0, -1, 0L, -1);
    public static volatile MarchStatePayload latest = EMPTY;

    public static MarchStatePayload of(ServerLevel level) {
        MarchEventsSavedData data = MarchEventsSavedData.get(level.getServer());
        long now = level.getGameTime();
        int mask = 0;
        for (MarchWeather weather : MarchWeather.values()) if (data.weatherActive(weather, now)) mask |= 1 << weather.ordinal();
        Attunement surge = data.surgeVoice(now);
        TribeDefinition festival = Festivals.tribeOn(Festivals.day(level));
        return new MarchStatePayload(mask, surge == null ? -1 : surge.ordinal(), surge == null ? 0L : data.surgeUntil(),
                festival == null ? -1 : festival.ordinal());
    }

    public static void send(ServerPlayer player) {
        if (player.connection == null || player.connection.getConnection().channel() == null || !NetworkRegistry.hasChannel(player.connection, TYPE.id())) return;
        PacketDistributor.sendToPlayer(player, of(player.serverLevel()));
    }

    public boolean weather(MarchWeather weather) { return (weatherMask & (1 << weather.ordinal())) != 0; }
    public Attunement surge() { return surgeVoice >= 0 && surgeVoice < Attunement.values().length ? Attunement.values()[surgeVoice] : null; }
    /** Seconds the surge has left by the given game time. */
    public int surgeSecondsLeft(long now) { return surge() == null ? 0 : (int) Math.max(0, (surgeUntil - now) / 20); }
    public TribeDefinition festival() { return festivalTribe >= 0 && festivalTribe < TribeDefinition.values().length ? TribeDefinition.values()[festivalTribe] : null; }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
