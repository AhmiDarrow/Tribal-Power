package tk.darrow.tribalpower.ley;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseRate;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ley Lens Pulse mode. Pulse machines never sync their store to clients, so the lens asks: client → server is an
 * empty request, server → client the zone's totals around the asking player plus a line per machine.
 *
 * <p>The per-machine rows carry only numbers and a position: the client already has the block states, so it
 * resolves each machine's name itself rather than paying for a string on the wire.
 */
public record LensPulsePayload(int stored, int capacity, int count, List<Entry> machines, int incoming, int outgoing)
        implements CustomPacketPayload {
    /** How many machines the HUD has room for. The zone totals still count every one of them. */
    public static final int ROWS = 5;

    /** One machine's reading: where it is, what it holds, what it makes, and what it spends. */
    public record Entry(BlockPos pos, int stored, int capacity, int perSecond, int draw) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.VAR_INT, Entry::stored,
                ByteBufCodecs.VAR_INT, Entry::capacity,
                ByteBufCodecs.VAR_INT, Entry::perSecond,
                ByteBufCodecs.VAR_INT, Entry::draw,
                Entry::new);
    }

    public static final Type<LensPulsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "lens_pulse"));
    public static final StreamCodec<ByteBuf, LensPulsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LensPulsePayload::stored,
            ByteBufCodecs.VAR_INT, LensPulsePayload::capacity,
            ByteBufCodecs.VAR_INT, LensPulsePayload::count,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(ROWS)), LensPulsePayload::machines,
            ByteBufCodecs.VAR_INT, LensPulsePayload::incoming,
            ByteBufCodecs.VAR_INT, LensPulsePayload::outgoing,
            LensPulsePayload::new);
    /** The client's latest reading. Plain data, so nothing here touches client classes. */
    public static volatile LensPulsePayload latest = empty();
    private static final java.util.Map<java.util.UUID, Long> ASKED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void forget(java.util.UUID player) { ASKED.remove(player); }

    public static LensPulsePayload empty() {
        return new LensPulsePayload(0, 0, 0, List.of(), 0, 0);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) { latest = payload; return; }
            if (!(context.player() instanceof ServerPlayer player) || LeyLensItem.held(player).isEmpty()) return;
            long now = player.serverLevel().getGameTime();
            Long last = ASKED.get(player.getUUID());
            if (last != null && now - last < 10 && now >= last) return;
            ASKED.put(player.getUUID(), now);
            context.reply(measureZone(player.serverLevel(), player.blockPosition()));
        });
    }

    /** Zone totals around {@code origin}. Incoming is generation; outgoing is what machines are trying to spend. */
    public static LensPulsePayload measureZone(ServerLevel level, BlockPos origin) {
        int r = LatticeNetwork.DEFAULT_RADIUS;
        long stored = 0, capacity = 0, incoming = 0, outgoing = 0;
        int count = 0;
        List<Entry> machines = new ArrayList<>();
        java.util.Set<Long> piles = new java.util.HashSet<>();
        for (BlockEntity be : LatticeNetwork.blockEntitiesAround(level, origin, r)) {
            int made = PulseRate.perSecond(level, be.getBlockPos(), be);
            int draw = PulseRate.drawPerSecond(level, be.getBlockPos(), be);
            incoming += made;
            outgoing += draw;
            int held = 0, room = 0;
            // A Pulse Cairn pile answers for itself from every stone; count it once.
            if (be instanceof PulseHandler pulse && pulse.getPulseCapacity() > 0
                    && !tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity.repeatsPile(be, piles)) {
                held = pulse.getPulseStored();
                room = pulse.getPulseCapacity();
                stored += held;
                capacity += room;
                count++;
            }
            if (room <= 0 && made <= 0 && draw <= 0) continue;
            machines.add(new Entry(be.getBlockPos(), held, room, made, draw));
        }
        // What is moving Pulse leads, whether it is coming in or going out. A full store breaks the tie.
        machines.sort(Comparator.comparingInt((Entry entry) -> Math.max(entry.perSecond(), entry.draw()))
                .thenComparingInt(Entry::stored).reversed());
        if (machines.size() > ROWS) machines = new ArrayList<>(machines.subList(0, ROWS));
        return new LensPulsePayload(cap(stored), cap(capacity), count, List.copyOf(machines), cap(incoming), cap(outgoing));
    }

    private static int cap(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }
}
