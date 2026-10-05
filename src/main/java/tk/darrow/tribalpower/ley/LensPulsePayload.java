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
import tk.darrow.tribalpower.lattice.Weave;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ley Lens Pulse mode. Pulse machines never sync their store to clients, so the lens asks: client → server is an
 * empty request, server → client the lattice network the asking player stands on (the one of their nearest Lattice
 * Conductor): its conductors and rated throughput, the tapped conductor's rate and what it carried last second,
 * the totals of its generators and cairns, and a line per member.
 *
 * <p>The per-machine rows carry only numbers and a position: the client already has the block states, so it
 * resolves each machine's name itself rather than paying for a string on the wire.
 *
 * @param conductors 0 when the player is off the lattice; the rest of the lattice fields are then 0 too
 */
public record LensPulsePayload(int stored, int capacity, int count, List<Entry> machines, int incoming, int outgoing,
                               int conductors, int rate, int tapRank, int tapRate, int tapCarried)
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
    private static final StreamCodec<ByteBuf, List<Entry>> ROWS_CODEC = Entry.STREAM_CODEC.apply(ByteBufCodecs.list(ROWS));
    public static final StreamCodec<ByteBuf, LensPulsePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.stored());
        ByteBufCodecs.VAR_INT.encode(buf, p.capacity());
        ByteBufCodecs.VAR_INT.encode(buf, p.count());
        ROWS_CODEC.encode(buf, p.machines());
        ByteBufCodecs.VAR_INT.encode(buf, p.incoming());
        ByteBufCodecs.VAR_INT.encode(buf, p.outgoing());
        ByteBufCodecs.VAR_INT.encode(buf, p.conductors());
        ByteBufCodecs.VAR_INT.encode(buf, p.rate());
        ByteBufCodecs.VAR_INT.encode(buf, p.tapRank());
        ByteBufCodecs.VAR_INT.encode(buf, p.tapRate());
        ByteBufCodecs.VAR_INT.encode(buf, p.tapCarried());
    }, buf -> new LensPulsePayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ROWS_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));
    /** The client's latest reading. Plain data, so nothing here touches client classes. */
    public static volatile LensPulsePayload latest = empty();
    private static final java.util.Map<java.util.UUID, Long> ASKED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void forget(java.util.UUID player) { ASKED.remove(player); }

    public static LensPulsePayload empty() {
        return new LensPulsePayload(0, 0, 0, List.of(), 0, 0, 0, 0, 0, 0, 0);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playBidirectional(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) { latest = payload; return; }
            if (!(context.player() instanceof ServerPlayer player) || LeyLensItem.held(player).isEmpty()) return;
            long now = player.serverLevel().getGameTime();
            Long last = ASKED.get(player.getUUID());
            if (last != null && now - last < 10 && now >= last) return;
            ASKED.put(player.getUUID(), now);
            context.reply(measureZone(player.serverLevel(), player.blockPosition()));
        });
    }

    /**
     * The network {@code origin} is on. Incoming is its generators' output; outgoing is what the machines on it are
     * trying to spend; stored and capacity count only its sources (generators and cairns), the Pulse machines can
     * actually draw. Off the lattice, every number is 0 and the HUD says to place a conductor.
     */
    public static LensPulsePayload measureZone(ServerLevel level, BlockPos origin) {
        Weave.Tap tap = Weave.tapsAt(level, origin).first();
        if (tap == null) return empty();
        Weave.Net network = Weave.members(level, tap.net());
        long stored = 0, capacity = 0, incoming = 0, outgoing = 0;
        int count = 0;
        List<Entry> machines = new ArrayList<>();
        java.util.Set<Long> piles = new java.util.HashSet<>();
        for (BlockEntity be : members(level, network)) {
            int made = PulseRate.perSecond(level, be.getBlockPos(), be);
            int draw = PulseRate.drawPerSecond(level, be.getBlockPos(), be);
            incoming += made;
            outgoing += draw;
            int held = 0, room = 0;
            // A Pulse Cairn pile answers for itself from every stone; count it once. Only sources count: a totem's
            // or a station's buffer is not Pulse the network can lend.
            if (be instanceof PulseHandler pulse && pulse.getPulseCapacity() > 0
                    && (Weave.isGenerator(be) || be instanceof tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity)
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
        var nearest = tap.nearest();
        return new LensPulsePayload(cap(stored), cap(capacity), count, List.copyOf(machines), cap(incoming), cap(outgoing),
                network.conductors().size(), cap(network.rate()), tk.darrow.tribalpower.item.MachineRank.rank(nearest),
                nearest.rate(), nearest.carriedLastSecond(level.getGameTime()));
    }

    /**
     * Every block entity on {@code network}: within reach of one of its conductors and with that network among its taps.
     * The chunks the conductors' cubes cover are walked once each.
     */
    private static List<BlockEntity> members(ServerLevel level, Weave.Net network) {
        it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
        int r = Weave.REACH;
        for (var conductor : network.conductors()) {
            BlockPos c = conductor.getBlockPos();
            for (int cx = (c.getX() - r) >> 4; cx <= (c.getX() + r) >> 4; cx++)
                for (int cz = (c.getZ() - r) >> 4; cz <= (c.getZ() + r) >> 4; cz++)
                    chunks.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
        }
        List<BlockEntity> found = new ArrayList<>();
        for (long key : chunks) {
            var chunk = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(key),
                    net.minecraft.world.level.ChunkPos.getZ(key));
            if (chunk == null) continue;
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                // Only blocks that hold, make or spend Pulse are rows; nothing else is looked up.
                if (be.isRemoved() || !(be instanceof PulseHandler || be instanceof tk.darrow.tribalpower.api.pulse.PulseSpend)) continue;
                for (Weave.Tap tap : Weave.tapsAt(level, be.getBlockPos()).byNet()) {
                    if (tap.net() == network) {
                        found.add(be);
                        break;
                    }
                }
            }
        }
        return found;
    }

    private static int cap(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }
}
