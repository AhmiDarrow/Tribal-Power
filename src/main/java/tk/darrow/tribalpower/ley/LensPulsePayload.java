package tk.darrow.tribalpower.ley;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Ley Lens Pulse mode. Pulse machines never sync their store to clients, so the lens asks: client → server is an
 * empty request, server → client the zone's totals around the asking player.
 */
public record LensPulsePayload(int stored, int capacity, int count) implements CustomPacketPayload {
    public static final Type<LensPulsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "lens_pulse"));
    public static final StreamCodec<ByteBuf, LensPulsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LensPulsePayload::stored,
            ByteBufCodecs.VAR_INT, LensPulsePayload::capacity,
            ByteBufCodecs.VAR_INT, LensPulsePayload::count,
            LensPulsePayload::new);
    /** The client's latest reading. Plain data, so nothing here touches client classes. */
    public static volatile LensPulsePayload latest = new LensPulsePayload(0, 0, 0);
    private static final java.util.Map<java.util.UUID, Long> ASKED = new java.util.HashMap<>();

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) { latest = payload; return; }
            if (!(context.player() instanceof ServerPlayer player) || LeyLensItem.held(player).isEmpty()) return;
            long now = player.serverLevel().getGameTime();
            Long last = ASKED.get(player.getUUID());
            if (last != null && now - last < 10 && now >= last) return;
            ASKED.put(player.getUUID(), now);
            context.reply(measure(player));
        });
    }

    private static LensPulsePayload measure(ServerPlayer player) {
        var level = player.serverLevel();
        var origin = player.blockPosition();
        int r = LatticeNetwork.DEFAULT_RADIUS;
        long stored = 0, capacity = 0;
        int count = 0;
        for (int cx = (origin.getX() - r) >> 4; cx <= (origin.getX() + r) >> 4; cx++)
            for (int cz = (origin.getZ() - r) >> 4; cz <= (origin.getZ() + r) >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    var p = be.getBlockPos();
                    if (Math.abs(p.getX() - origin.getX()) > r || Math.abs(p.getY() - origin.getY()) > r
                            || Math.abs(p.getZ() - origin.getZ()) > r) continue;
                    if (be instanceof PulseHandler pulse && pulse.getPulseCapacity() > 0) {
                        stored += pulse.getPulseStored();
                        capacity += pulse.getPulseCapacity();
                        count++;
                    }
                }
            }
        return new LensPulsePayload((int) Math.min(Integer.MAX_VALUE, stored), (int) Math.min(Integer.MAX_VALUE, capacity), count);
    }
}
