package tk.darrow.tribalpower.ley;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Ley sight asks the server for the beat, because ley lines are folded from the world seed and the
 * client does not have it. The reply is three numbers.
 */
public record LeySightPayload(int gain, int lines, int voices) implements CustomPacketPayload {
    public static final Type<LeySightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "ley_sight"));
    public static final StreamCodec<ByteBuf, LeySightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LeySightPayload::gain,
            ByteBufCodecs.VAR_INT, LeySightPayload::lines,
            ByteBufCodecs.VAR_INT, LeySightPayload::voices,
            LeySightPayload::new);
    public static volatile LeySightPayload latest = new LeySightPayload(0, 0, 0);
    /** True once a reply has arrived, so a quiet site is not mistaken for "still waiting". */
    public static volatile boolean seen;
    private static final java.util.Map<java.util.UUID, Long> ASKED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void forget(java.util.UUID player) { ASKED.remove(player); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static LeySightPayload empty() { return new LeySightPayload(0, 0, 0); }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                latest = payload;
                seen = true;
                return;
            }
            if (!(context.player() instanceof ServerPlayer player) || !LeyRopes.sees(player)) return;
            long now = player.serverLevel().getGameTime();
            Long last = ASKED.get(player.getUUID());
            if (last != null && now - last < 10 && now >= last) return;
            ASKED.put(player.getUUID(), now);
            var factors = LeyMath.factors(player.serverLevel(), player.blockPosition());
            LeyField.Reading ley = LeyField.sample(player.serverLevel(), player.blockPosition());
            context.reply(new LeySightPayload(factors.gain(), ley.lines(), ley.voices()));
        });
    }
}
