package tk.darrow.tribalpower.lattice;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LatticeConductorBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Pulse lattice: how Pulse moves from where it is made to where it is spent.
 *
 * <p>The rules, the same ones the Codex tells players (power/spirit_pulse, power/lattice_conductor):
 * <ol>
 *   <li><b>Joining.</b> Anything that makes, holds or spends Pulse (generator, machine, camp device, relay, totem,
 *   Pulse Cairn) is on the lattice when it stands within {@link #REACH} blocks of a Lattice Conductor: the same
 *   17x17x17 cube every lattice reach in the mod uses, each axis tested on its own.</li>
 *   <li><b>Linking.</b> Conductors within {@link #REACH} of each other link. A network is one connected component of
 *   linked conductors, plus every member within reach of any of them. Two groups of conductors out of reach of each
 *   other are two networks, even with a member standing between them; a member in reach of both is on both. A
 *   conductor with a redstone signal is lifted out of the weave until the signal ends.</li>
 *   <li><b>Sources.</b> A network's Pulse sits in the buffers of its generators and in its Pulse Cairns, nowhere
 *   else. A consumer draws only from the networks it is on: generators first, then cairns. Nothing draws straight
 *   from a generator off the lattice any more, and totem buffers, station buffers and every other store are not
 *   sources (a totem's buffer is its own voice; see {@link ResonanceTotemBlockEntity}).</li>
 *   <li><b>Throughput.</b> A member's <i>taps</i> on a network are that network's conductors within reach of it.
 *   Pulse delivered from a source to a consumer leaves through one of the source's taps and arrives through one of
 *   the consumer's taps. Each conductor carries at most its rank's rate a second ({@link #rate}: Woven 64,
 *   Attuned 256, Bound 1,024, Manifested 4,096), counting Pulse it lifts and Pulse it hands down alike; Pulse that
 *   leaves and arrives at the same conductor is counted once. The links between conductors carry freely, so a
 *   network's total rate is the sum of its tapped conductors' rates. More conductors, or higher ranks, around a big
 *   generator is how it gets to deliver fully: a top Ley Heart wants a Manifested conductor, or four Bound ones.</li>
 *   <li><b>Cairns.</b> A Pulse Cairn pile is the network's storage. It drinks only a generator's surplus (what sits
 *   above half its buffer) and moves Pulse in and out at its stones' summed rate, the same per-rank numbers as a
 *   conductor, on top of the conductor taps it passes through.</li>
 *   <li><b>Ley Binding.</b> While the rite's thread hums between two totems, the conductors in reach of one end weave
 *   into the network of the conductors in reach of the other.</li>
 * </ol>
 *
 * <p>Performance: networks are resolved once and cached per level. A conductor arriving or leaving (placed,
 * broken, its chunk loaded or unloaded, its redstone signal changing) drops a level's networks; a generator, cairn
 * or totem arriving or leaving drops only the member lists of the networks around it. Nothing here walks the
 * world per tick per machine: a draw is a cached tap lookup, then a walk over the network's source list. As a
 * safety net, a network re-reads its members every {@link #REFRESH_TICKS}, so a third-party generator placed by
 * a command still joins.
 */
public final class Weave {
    /** The lattice reach: the same 8-block cube as {@link LatticeNetwork#DEFAULT_RADIUS}. */
    public static final int REACH = LatticeNetwork.DEFAULT_RADIUS;
    /** Pulse a second a conductor carries, and a cairn stone moves in and out, by machine rank. */
    private static final int[] RATE = {64, 256, 1024, 4096};
    /** Safety stop for one network walk: far above any base, it only keeps a solid cube of conductors finite. */
    public static final int MAX_CONDUCTORS = 4096;
    /** How long a network trusts its member list without any change being reported. */
    public static final int REFRESH_TICKS = 600;
    private static final int TAPS_CACHE_LIMIT = 16384;

    private Weave() {}

    /** Conductor rate (and cairn stone rate) for a machine rank: 64 / 256 / 1,024 / 4,096 Pulse a second. */
    public static int rate(int rank) {
        return RATE[Math.clamp(rank, 0, RATE.length - 1)];
    }

    /** True for a block that makes Pulse (and so is a network source), rather than only holding or spending it. */
    public static boolean isGenerator(BlockEntity be) {
        return be instanceof DrumheartBlockEntity
                || be instanceof LeyCollectorBlockEntity
                || be instanceof PulseResonatorBlockEntity
                // The six voices of 3.1 and the Ley Heart all implement PulseGenerator.
                || be instanceof tk.darrow.tribalpower.api.pulse.PulseGenerator;
    }

    // ---- invalidation ------------------------------------------------------------------------------------

    /*
     * Counters rather than map edits: a block entity can arrive while its chunk is still being built, off the
     * server thread. The caches compare against these on the server thread and drop what they must.
     */
    private static final java.util.concurrent.atomic.AtomicInteger CONDUCTORS = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.ConcurrentLinkedQueue<Change> MEMBER_CHANGES = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** A reported change: a member (generator, cairn, totem) or a conductor ({@code conductor}) at {@code pos}. */
    private record Change(Level level, long pos, long seenAt, boolean conductor) {}

    /** Something changed that no position can describe: every network in every level is woven again. */
    public static void conductorsChanged() {
        CONDUCTORS.incrementAndGet();
    }

    /**
     * A conductor at {@code pos} arrived, left, or had its redstone lock change. Only the networks in reach of it are
     * woven again, so a far base's chunk loading never costs this one a re-weave. Safe from any thread.
     */
    public static void conductorChanged(@Nullable Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        if (QUEUED.incrementAndGet() > MAX_QUEUED) {
            MEMBER_CHANGES.clear();
            QUEUED.set(0);
            FLUSHES.incrementAndGet();
            CONDUCTORS.incrementAndGet();
            return;
        }
        MEMBER_CHANGES.add(new Change(level, pos.asLong(), Long.MIN_VALUE, true));
    }

    /**
     * A generator, cairn or totem at {@code pos} arrived or left: the networks in reach re-read their members. Safe
     * from any thread; only server levels are tracked.
     */
    public static void memberChanged(@Nullable Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        // A level nobody draws in never drains its changes; past this many, every network simply re-reads.
        if (QUEUED.incrementAndGet() > MAX_QUEUED) {
            MEMBER_CHANGES.clear();
            QUEUED.set(0);
            FLUSHES.incrementAndGet();
            return;
        }
        MEMBER_CHANGES.add(new Change(level, pos.asLong(), Long.MIN_VALUE, false));
    }

    private static final int MAX_QUEUED = 8192;
    /** Conductor changes handled one by one in a single pass; past this, the level's weave is simply dropped. */
    private static final int MAX_LOCAL_UNWEAVES = 16;
    private static final java.util.concurrent.atomic.AtomicInteger QUEUED = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger FLUSHES = new java.util.concurrent.atomic.AtomicInteger();

    /** Server stopped ({@code level} null) or a level unloaded: forget what was woven there. */
    public static void clear(@Nullable Level level) {
        if (level == null) {
            LEVELS.clear();
            MEMBER_CHANGES.clear();
            QUEUED.set(0);
        } else {
            LEVELS.remove(level);
            MEMBER_CHANGES.removeIf(change -> change.level() == level);
        }
    }

    // ---- the cache ---------------------------------------------------------------------------------------

    private static final Map<Level, LevelWeave> LEVELS = new java.util.HashMap<>();

    /** One network: its conductors, and (read lazily) the sources and totems in their reach. */
    public static final class Net {
        private final int id;
        private final List<LatticeConductorBlockEntity> conductors;
        private List<Source> generators = List.of();
        private List<Source> cairns = List.of();
        private List<ResonanceTotemBlockEntity> totems = List.of();
        private boolean membersRead;
        private long membersReadAt;

        private Net(int id, List<LatticeConductorBlockEntity> conductors) {
            this.id = id;
            this.conductors = conductors;
        }

        public int id() { return id; }
        public List<LatticeConductorBlockEntity> conductors() { return conductors; }
        public List<Source> generators() { return generators; }
        public List<Source> cairns() { return cairns; }
        public List<ResonanceTotemBlockEntity> totems() { return totems; }

        /** The network's rated throughput: every conductor's rate, summed. */
        public long rate() {
            long sum = 0;
            for (LatticeConductorBlockEntity conductor : conductors) sum += conductor.rate();
            return sum;
        }

        /** Pulse held by every source on the network (generator buffers and cairn piles). */
        public long stored() {
            long sum = 0;
            for (Source source : generators) if (!source.be().isRemoved()) sum += source.handler().getPulseStored();
            for (Source source : cairns) if (!source.be().isRemoved()) sum += source.handler().getPulseStored();
            return sum;
        }

        public long capacity() {
            long sum = 0;
            for (Source source : generators) if (!source.be().isRemoved()) sum += source.handler().getPulseCapacity();
            for (Source source : cairns) if (!source.be().isRemoved()) sum += source.handler().getPulseCapacity();
            return sum;
        }

        /** Up and active: some source on it holds Pulse to give. */
        public boolean active() {
            for (Source source : generators) if (!source.be().isRemoved() && source.handler().getPulseStored() > 0) return true;
            for (Source source : cairns) if (!source.be().isRemoved() && source.handler().getPulseStored() > 0) return true;
            return false;
        }

        private void dropMembers() {
            membersRead = false;
        }
    }

    /** A source on one network and the conductors of that network it can leave through, nearest first. */
    public record Source(BlockEntity be, LatticeConductorBlockEntity[] taps, @Nullable PulseCairnBlockEntity.Pile pile) {
        public PulseHandler handler() { return (PulseHandler) be; }
    }

    /** The conductors of one network within reach of a spot, nearest first. */
    public record Tap(Net net, LatticeConductorBlockEntity[] conductors) {
        public LatticeConductorBlockEntity nearest() { return conductors[0]; }
    }

    /** Every network a spot is on, the one with the nearest conductor first. Empty: not on the lattice. */
    public record Taps(List<Tap> byNet) {
        static final Taps NONE = new Taps(List.of());
        public boolean isEmpty() { return byNet.isEmpty(); }
        public @Nullable Tap first() { return byNet.isEmpty() ? null : byNet.get(0); }
    }

    private static final class LevelWeave {
        private final Level level;
        private int conductorGeneration;
        private final Long2ObjectOpenHashMap<Net> nets = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<Taps> taps = new Long2ObjectOpenHashMap<>();
        private int nextId;
        /** Live Ley Bindings in this level, and their fingerprint when the networks were woven. */
        private List<tk.darrow.tribalpower.rite.world.RiteSavedData.LeyLine> ley = List.of();
        private int leySignature;

        LevelWeave(Level level) {
            this.level = level;
            this.conductorGeneration = CONDUCTORS.get();
            this.ley = leyLines(level);
            this.leySignature = ley.hashCode();
        }

        private int flushes = FLUSHES.get();
        private long leyReadAt = Long.MIN_VALUE;

        void validate() {
            int flushed = FLUSHES.get();
            if (flushed != flushes) {
                flushes = flushed;
                for (Net net : nets.values()) net.dropMembers();
            }
            int generation = CONDUCTORS.get();
            // Bindings come and go on the scale of minutes: read them once a tick, not once a draw.
            long now = level.getGameTime();
            List<tk.darrow.tribalpower.rite.world.RiteSavedData.LeyLine> lines = ley;
            int signature = leySignature;
            if (now != leyReadAt) {
                leyReadAt = now;
                lines = leyLines(level);
                signature = lines.hashCode();
            }
            if (generation != conductorGeneration || signature != leySignature) {
                nets.clear();
                taps.clear();
                conductorGeneration = generation;
                ley = lines;
                leySignature = signature;
            }
        }
    }

    /** The level's live Ley Bindings: empty (and cheap) when none is humming. */
    private static List<tk.darrow.tribalpower.rite.world.RiteSavedData.LeyLine> leyLines(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || server.getServer() == null) return List.of();
        return tk.darrow.tribalpower.rite.world.RiteSavedData.get(server.getServer()).leyLines(server);
    }

    private static boolean inReach(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= REACH && Math.abs(a.getY() - b.getY()) <= REACH
                && Math.abs(a.getZ() - b.getZ()) <= REACH;
    }

    /** The cached weave for a server level on its own thread; anything else gets a throwaway one. */
    private static LevelWeave weave(Level level) {
        boolean cached = level instanceof net.minecraft.server.level.ServerLevel server && server.getServer().isSameThread();
        if (!cached) return new LevelWeave(level);
        LevelWeave weave = LEVELS.computeIfAbsent(level, LevelWeave::new);
        weave.validate();
        applyMemberChanges(level, weave);
        return weave;
    }

    /** Marks the networks in reach of each reported member change; a change in a chunk not yet visible waits. */
    private static void applyMemberChanges(Level level, LevelWeave weave) {
        if (MEMBER_CHANGES.isEmpty()) return;
        long now = level.getGameTime();
        List<Change> later = null;
        int unwoven = 0;
        for (var it = MEMBER_CHANGES.iterator(); it.hasNext(); ) {
            Change change = it.next();
            if (change.level() != level) continue;
            it.remove();
            QUEUED.decrementAndGet();
            BlockPos pos = BlockPos.of(change.pos());
            // A conductor that left (broken, or its chunk unloaded) must leave the weave at once.
            boolean visible = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
            if (change.conductor() && (visible || change.seenAt() == Long.MIN_VALUE)) {
                // A chunk full of conductors loading at once: forgetting the level's weave is cheaper than
                // working out, change by change, which networks each one touched.
                if (++unwoven > MAX_LOCAL_UNWEAVES) {
                    weave.nets.clear();
                    weave.taps.clear();
                } else {
                    unweave(weave, pos);
                }
            }
            if (!visible) {
                // One arriving with a chunk still being built: look again once the chunk can be seen.
                long seen = change.seenAt() == Long.MIN_VALUE ? now : change.seenAt();
                if (now - seen < REFRESH_TICKS) {
                    if (later == null) later = new ArrayList<>();
                    later.add(new Change(level, change.pos(), seen, change.conductor()));
                }
                continue;
            }
            if (change.conductor()) continue;
            for (LatticeConductorBlockEntity conductor : conductorsAround(level, pos)) {
                Net net = weave.nets.get(conductor.getBlockPos().asLong());
                if (net != null) net.dropMembers();
            }
        }

        if (later != null) {
            QUEUED.addAndGet(later.size());
            MEMBER_CHANGES.addAll(later);
        }
    }

    /**
     * Forgets the networks a conductor change at {@code pos} could touch: the one it belonged to and every one with a
     * conductor in reach of it (a new conductor can join several into one, a lost one can split one in two), and every
     * cached tap in reach of it or onto one of those networks. Other networks keep their weave.
     */
    private static void unweave(LevelWeave weave, BlockPos pos) {
        if (weave.nets.isEmpty() && weave.taps.isEmpty()) return; // nothing woven yet, nothing to forget
        Set<Net> dead = Collections.newSetFromMap(new IdentityHashMap<>());
        Net own = weave.nets.get(pos.asLong());
        if (own != null) dead.add(own);
        List<BlockPos> touched = new ArrayList<>(1);
        touched.add(pos);
        // A conductor by a Ley-bound totem reaches the networks at the binding's other end as well.
        for (var line : weave.ley) {
            if (inReach(pos, line.a())) touched.add(line.b());
            if (inReach(pos, line.b())) touched.add(line.a());
        }
        for (BlockPos at : touched)
            for (LatticeConductorBlockEntity conductor : conductorsAround(weave.level, at)) {
                Net net = weave.nets.get(conductor.getBlockPos().asLong());
                if (net != null) dead.add(net);
            }
        for (Net net : dead)
            for (LatticeConductorBlockEntity conductor : net.conductors)
                weave.nets.remove(conductor.getBlockPos().asLong(), net);
        for (var it = weave.taps.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();
            boolean stale = inReach(BlockPos.of(entry.getLongKey()), pos);
            if (!stale)
                for (Tap tap : entry.getValue().byNet())
                    if (dead.contains(tap.net())) {
                        stale = true;
                        break;
                    }
            if (stale) it.remove();
        }
    }

    // ---- resolving --------------------------------------------------------------------------------------------

    private static List<LatticeConductorBlockEntity> conductorsAround(Level level, BlockPos at) {
        return LatticeNetwork.inBox(level, LatticeConductorBlockEntity.class,
                at.getX() - REACH, at.getY() - REACH, at.getZ() - REACH,
                at.getX() + REACH, at.getY() + REACH, at.getZ() + REACH);
    }

    /**
     * Conductors bucketed into 8-block cells (one cell is one lattice reach), each chunk's block-entity map read at
     * most once. A walk asks "who is in reach of this conductor" for every conductor it meets; a cell whose every
     * conductor the walk has already met is skipped, so even a solid block of conductors walks in about linear time.
     */
    private static final class Grid {
        private static final class Cell {
            final List<LatticeConductorBlockEntity> conductors = new ArrayList<>(4);
            int unmet;
        }

        private final Level level;
        private final boolean readWorld;
        private final Long2ObjectOpenHashMap<Cell> cells = new Long2ObjectOpenHashMap<>();
        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet chunksRead = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

        private Grid(Level level, boolean readWorld) {
            this.level = level;
            this.readWorld = readWorld;
        }

        /** A grid that reads the conductors of the loaded chunks it is asked about, for a network walk. */
        static Grid ofWorld(Level level) {
            return new Grid(level, true);
        }

        /** A grid over conductors already known, for a network's own lookups. */
        static Grid of(Level level, List<LatticeConductorBlockEntity> conductors) {
            Grid grid = new Grid(level, false);
            for (LatticeConductorBlockEntity conductor : conductors) grid.add(conductor);
            return grid;
        }

        private static long cellKey(int x, int y, int z) {
            return BlockPos.asLong(x >> 3, y >> 3, z >> 3);
        }

        private void add(LatticeConductorBlockEntity conductor) {
            BlockPos p = conductor.getBlockPos();
            Cell cell = cells.computeIfAbsent(cellKey(p.getX(), p.getY(), p.getZ()), k -> new Cell());
            cell.conductors.add(conductor);
            cell.unmet++;
        }

        private void read(int cx, int cz) {
            if (!readWorld || !chunksRead.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) return;
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) return;
            for (BlockEntity be : chunk.getBlockEntities().values())
                if (be instanceof LatticeConductorBlockEntity conductor && !be.isRemoved()) add(conductor);
        }

        /** The walk has met {@code conductor}: once a cell's every conductor is met, the cell is skipped. */
        void met(LatticeConductorBlockEntity conductor) {
            BlockPos p = conductor.getBlockPos();
            Cell cell = cells.get(cellKey(p.getX(), p.getY(), p.getZ()));
            if (cell != null) cell.unmet--;
        }

        /**
         * Conductors within reach of {@code at}, in no particular order.
         *
         * @param unmetOnly skip cells whose every conductor has been {@link #met} (a walk's neighbours)
         */
        List<LatticeConductorBlockEntity> around(BlockPos at, boolean unmetOnly) {
            for (int cx = (at.getX() - REACH) >> 4; cx <= (at.getX() + REACH) >> 4; cx++)
                for (int cz = (at.getZ() - REACH) >> 4; cz <= (at.getZ() + REACH) >> 4; cz++) read(cx, cz);
            List<LatticeConductorBlockEntity> out = new ArrayList<>();
            for (int x = (at.getX() - REACH) >> 3; x <= (at.getX() + REACH) >> 3; x++)
                for (int y = (at.getY() - REACH) >> 3; y <= (at.getY() + REACH) >> 3; y++)
                    for (int z = (at.getZ() - REACH) >> 3; z <= (at.getZ() + REACH) >> 3; z++) {
                        Cell cell = cells.get(BlockPos.asLong(x, y, z));
                        if (cell == null || (unmetOnly && cell.unmet <= 0)) continue;
                        for (LatticeConductorBlockEntity conductor : cell.conductors)
                            if (inReach(conductor.getBlockPos(), at)) out.add(conductor);
                    }
            return out;
        }
    }

    private static boolean woven(Level level, LatticeConductorBlockEntity conductor) {
        return !conductor.isRemoved() && !level.hasNeighborSignal(conductor.getBlockPos());
    }

    /** The network {@code conductor} belongs to, walked once and cached for every conductor in it; null when locked. */
    private static @Nullable Net netOf(LevelWeave weave, LatticeConductorBlockEntity conductor) {
        long key = conductor.getBlockPos().asLong();
        Net known = weave.nets.get(key);
        if (known != null) return known;
        Level level = weave.level;
        if (!woven(level, conductor)) return null;
        List<LatticeConductorBlockEntity> found = new ArrayList<>();
        Grid grid = Grid.ofWorld(level);
        grid.around(conductor.getBlockPos(), false); // reads the seed's own chunk before it is marked met
        it.unimi.dsi.fastutil.longs.LongOpenHashSet seen = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        ArrayDeque<LatticeConductorBlockEntity> queue = new ArrayDeque<>();
        queue.add(conductor);
        seen.add(key);
        grid.met(conductor);
        while (!queue.isEmpty() && found.size() < MAX_CONDUCTORS) {
            LatticeConductorBlockEntity current = queue.removeFirst();
            found.add(current);
            for (LatticeConductorBlockEntity next : grid.around(current.getBlockPos(), true)) {
                if (!seen.add(next.getBlockPos().asLong())) continue;
                grid.met(next);
                if (woven(level, next)) queue.add(next);
            }
            // A Ley Binding ties two distant totems: while it hums, the conductors in reach of one end weave into
            // the network of the conductors in reach of the other.
            for (var line : weave.ley) {
                BlockPos far = inReach(current.getBlockPos(), line.a()) ? line.b()
                        : inReach(current.getBlockPos(), line.b()) ? line.a() : null;
                if (far == null) continue;
                for (LatticeConductorBlockEntity next : grid.around(far, true)) {
                    if (!seen.add(next.getBlockPos().asLong())) continue;
                    grid.met(next);
                    if (woven(level, next)) queue.add(next);
                }
            }
        }
        found.sort(LatticeNetwork.CUBE_ORDER);
        Net net = new Net(weave.nextId++, List.copyOf(found));
        for (LatticeConductorBlockEntity member : found) weave.nets.put(member.getBlockPos().asLong(), net);
        return net;
    }

    /** The networks {@code at} is on and its taps on each, cached until a conductor changes. */
    public static Taps tapsAt(Level level, BlockPos at) {
        LevelWeave weave = weave(level);
        long key = at.asLong();
        Taps known = weave.taps.get(key);
        if (known != null) return known;
        Taps taps = resolveTaps(weave, at);
        if (weave.taps.size() >= TAPS_CACHE_LIMIT) weave.taps.clear();
        weave.taps.put(key, taps);
        return taps;
    }

    private static Taps resolveTaps(LevelWeave weave, BlockPos at) {
        List<LatticeConductorBlockEntity> near = new ArrayList<>(conductorsAround(weave.level, at));
        if (near.isEmpty()) return Taps.NONE;
        near.sort(nearestTo(at));
        LinkedHashMap<Net, List<LatticeConductorBlockEntity>> grouped = new LinkedHashMap<>();
        for (LatticeConductorBlockEntity conductor : near) {
            Net net = netOf(weave, conductor);
            if (net != null) grouped.computeIfAbsent(net, n -> new ArrayList<>()).add(conductor);
        }
        if (grouped.isEmpty()) return Taps.NONE;
        List<Tap> taps = new ArrayList<>(grouped.size());
        grouped.forEach((net, conductors) -> taps.add(new Tap(net, conductors.toArray(LatticeConductorBlockEntity[]::new))));
        return new Taps(List.copyOf(taps));
    }

    private static java.util.Comparator<BlockEntity> nearestTo(BlockPos at) {
        return java.util.Comparator.comparingDouble((BlockEntity be) -> be.getBlockPos().distSqr(at))
                .thenComparingLong(be -> be.getBlockPos().asLong());
    }

    /**
     * Reads (or re-reads) a network's sources and totems. The chunks its conductors' cubes cover are walked once;
     * only the few generators, cairns and totems found there look up the conductors in reach of them.
     */
    private static void readMembers(Level level, Net network) {
        long now = level.getGameTime();
        if (network.membersRead && now - network.membersReadAt < REFRESH_TICKS && now >= network.membersReadAt) return;
        LinkedHashMap<Object, List<LatticeConductorBlockEntity>> tapsOf = new LinkedHashMap<>();
        Map<Object, BlockEntity> beOf = new java.util.HashMap<>();
        Map<Object, PulseCairnBlockEntity.Pile> pileOf = new java.util.HashMap<>();
        LinkedHashMap<Long, ResonanceTotemBlockEntity> totems = new LinkedHashMap<>();
        Grid mine = Grid.of(level, network.conductors);
        it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
        for (LatticeConductorBlockEntity conductor : network.conductors) {
            BlockPos c = conductor.getBlockPos();
            for (int cx = (c.getX() - REACH) >> 4; cx <= (c.getX() + REACH) >> 4; cx++)
                for (int cz = (c.getZ() - REACH) >> 4; cz <= (c.getZ() + REACH) >> 4; cz++)
                    chunks.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
        }
        for (long chunkKey : chunks) {
            var chunk = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkKey),
                    net.minecraft.world.level.ChunkPos.getZ(chunkKey));
            if (chunk == null) continue;
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be.isRemoved()) continue;
                boolean totem = be instanceof ResonanceTotemBlockEntity;
                boolean cairn = be instanceof PulseCairnBlockEntity;
                if (!totem && !cairn && !(be instanceof PulseHandler && isGenerator(be))) continue;
                List<LatticeConductorBlockEntity> reach = mine.around(be.getBlockPos(), false);
                if (reach.isEmpty()) continue;
                if (totem) {
                    totems.putIfAbsent(be.getBlockPos().asLong(), (ResonanceTotemBlockEntity) be);
                    continue;
                }
                Object key = be;
                if (cairn) {
                    PulseCairnBlockEntity.Pile pile = ((PulseCairnBlockEntity) be).pile();
                    key = pile;
                    pileOf.put(pile, pile);
                    beOf.putIfAbsent(pile, pile.leader());
                } else {
                    beOf.put(be, be);
                }
                List<LatticeConductorBlockEntity> list = tapsOf.computeIfAbsent(key, k -> new ArrayList<>(2));
                for (LatticeConductorBlockEntity conductor : reach) if (!list.contains(conductor)) list.add(conductor);
            }
        }
        List<Source> generators = new ArrayList<>();
        List<Source> cairns = new ArrayList<>();
        for (var entry : tapsOf.entrySet()) {
            BlockEntity be = beOf.get(entry.getKey());
            List<LatticeConductorBlockEntity> list = entry.getValue();
            list.sort(nearestTo(be.getBlockPos()));
            PulseCairnBlockEntity.Pile pile = pileOf.get(entry.getKey());
            Source source = new Source(be, list.toArray(LatticeConductorBlockEntity[]::new), pile);
            (pile != null ? cairns : generators).add(source);
        }
        java.util.Comparator<Source> order = java.util.Comparator.comparing(Source::be, LatticeNetwork.CUBE_ORDER);
        generators.sort(order);
        cairns.sort(order);
        network.generators = List.copyOf(generators);
        network.cairns = List.copyOf(cairns);
        network.totems = List.copyOf(totems.values());
        network.membersRead = true;
        network.membersReadAt = now;
    }

    /** The network {@code at} is on (its nearest conductor's), with members read; null when off the lattice. */
    public static @Nullable Net netAt(Level level, BlockPos at) {
        Tap tap = tapsAt(level, at).first();
        if (tap == null) return null;
        readMembers(level, tap.net());
        return tap.net();
    }

    /** The members of {@code net}, read if they are stale. */
    public static Net members(Level level, Net net) {
        readMembers(level, net);
        return net;
    }

    /** True when {@code at} stands within reach of a woven conductor. */
    public static boolean onLattice(Level level, BlockPos at) {
        return !tapsAt(level, at).isEmpty();
    }

    /** True when {@code at} is on a network whose sources hold Pulse: the lattice there is up and active. */
    public static boolean active(Level level, BlockPos at) {
        for (Tap tap : tapsAt(level, at).byNet()) {
            readMembers(level, tap.net());
            if (tap.net().active()) return true;
        }
        return false;
    }

    // ---- moving Pulse ----------------------------------------------------------------------------------------

    /**
     * What one draw has charged so far. A real draw charges conductors and piles as it goes; a simulated one only
     * remembers here, so a second source in the same simulated draw sees the budget the first one used.
     */
    private static final class Ledger {
        final long now;
        final boolean simulate;
        /** Made on first use: most draws touch one source through one conductor. */
        private IdentityHashMap<Object, int[]> pending;
        private Set<Object> drawn;

        Ledger(long now, boolean simulate) {
            this.now = now;
            this.simulate = simulate;
        }

        boolean drew(Object source) {
            return drawn != null && drawn.contains(source);
        }

        void drawing(Object source) {
            if (drawn == null) drawn = Collections.newSetFromMap(new IdentityHashMap<>());
            drawn.add(source);
        }

        private int pending(Object key) {
            if (pending == null) return 0;
            int[] used = pending.get(key);
            return used == null ? 0 : used[0];
        }

        private void remember(Object key, int n) {
            if (pending == null) pending = new IdentityHashMap<>(4);
            pending.computeIfAbsent(key, k -> new int[1])[0] += n;
        }

        int left(LatticeConductorBlockEntity conductor) {
            return Math.max(0, conductor.budgetLeft(now) - pending(conductor));
        }

        void charge(LatticeConductorBlockEntity conductor, int n) {
            if (n <= 0) return;
            if (simulate) remember(conductor, n);
            else conductor.carry(n, now);
        }

        int pileOutLeft(PulseCairnBlockEntity.Pile pile) {
            return Math.max(0, pile.outLeft(now) - pending(pile.outKey()));
        }

        int pileInLeft(PulseCairnBlockEntity.Pile pile) {
            return Math.max(0, pile.inLeft(now) - pending(pile.inKey()));
        }

        void pileOut(PulseCairnBlockEntity.Pile pile, int n) {
            if (n <= 0) return;
            if (simulate) remember(pile.outKey(), n);
            else pile.spendOut(n, now);
        }

        void pileIn(PulseCairnBlockEntity.Pile pile, int n) {
            if (n <= 0) return;
            if (simulate) remember(pile.inKey(), n);
            else pile.spendIn(n, now);
        }
    }

    private static boolean contains(LatticeConductorBlockEntity[] conductors, LatticeConductorBlockEntity wanted) {
        for (LatticeConductorBlockEntity conductor : conductors) if (conductor == wanted) return true;
        return false;
    }

    /** Whether {@code conductor} is one of {@code from}: by reach when the far end is one block, else by the list. */
    private static boolean taps(LatticeConductorBlockEntity[] from, @Nullable BlockPos fromAt, LatticeConductorBlockEntity conductor) {
        if (fromAt == null) return contains(from, conductor);
        BlockPos c = conductor.getBlockPos();
        return Math.abs(c.getX() - fromAt.getX()) <= REACH && Math.abs(c.getY() - fromAt.getY()) <= REACH
                && Math.abs(c.getZ() - fromAt.getZ()) <= REACH;
    }

    /**
     * How much of {@code want} can travel from a member tapping {@code from} to one tapping {@code to}, charging the
     * conductors as it goes: through a conductor both share first (counted once), then out through one and in through
     * another (counted on both).
     *
     * @param fromAt the sending block when it is a single block (every conductor of the network within reach of it is
     *               one of {@code from}, which makes the shared test a range check); null for a cairn pile
     */
    private static int route(LatticeConductorBlockEntity[] from, @Nullable BlockPos fromAt, LatticeConductorBlockEntity[] to,
                             int want, Ledger ledger) {
        int moved = 0;
        for (LatticeConductorBlockEntity shared : to) {
            if (moved >= want) return moved;
            if (!taps(from, fromAt, shared)) continue;
            int n = Math.min(want - moved, ledger.left(shared));
            if (n <= 0) continue;
            ledger.charge(shared, n);
            moved += n;
        }
        if (moved >= want) return moved;
        // Only conductors with budget left can pair; on a busy network most are spent, so filter them once.
        List<LatticeConductorBlockEntity> ins = new ArrayList<>(Math.min(to.length, 8));
        for (LatticeConductorBlockEntity in : to) if (ledger.left(in) > 0) ins.add(in);
        if (ins.isEmpty()) return moved;
        for (LatticeConductorBlockEntity out : from) {
            if (moved >= want || ins.isEmpty()) return moved;
            if (ledger.left(out) <= 0) continue;
            for (var it = ins.iterator(); it.hasNext() && moved < want; ) {
                LatticeConductorBlockEntity in = it.next();
                if (in == out) continue;
                int n = Math.min(want - moved, Math.min(ledger.left(out), ledger.left(in)));
                if (ledger.left(in) <= 0) {
                    it.remove();
                    continue;
                }
                if (n <= 0) break;
                ledger.charge(out, n);
                ledger.charge(in, n);
                moved += n;
                if (ledger.left(in) <= 0) it.remove();
            }
        }
        return moved;
    }

    /**
     * Draws up to {@code amount} for a consumer at {@code at} from the networks it is on: generators first, then
     * cairns, each limited by the conductor taps it passes through and a cairn by its own out rate.
     *
     * @return what was (or, simulated, could be) drawn
     */
    public static int draw(Level level, BlockPos at, int amount, boolean simulate) {
        if (amount <= 0 || level.isClientSide) return 0;
        Taps taps = tapsAt(level, at);
        if (taps.isEmpty()) return 0;
        Ledger ledger = new Ledger(level.getGameTime(), simulate);
        int taken = 0;
        for (Tap tap : taps.byNet()) {
            readMembers(level, tap.net());
            taken += drawFrom(tap.net().generators, tap.conductors(), amount - taken, ledger, false);
            if (taken < amount) taken += drawFrom(tap.net().cairns, tap.conductors(), amount - taken, ledger, false);
            if (taken >= amount) break;
        }
        return taken;
    }

    /** All or nothing: takes {@code amount} only when the whole of it can come, and says whether it did. */
    public static boolean tryDraw(Level level, BlockPos at, int amount) {
        if (amount <= 0) return true;
        if (draw(level, at, amount, true) < amount) return false;
        draw(level, at, amount, false);
        return true;
    }

    private static int drawFrom(List<Source> sources, LatticeConductorBlockEntity[] sink, int want, Ledger ledger,
                                boolean surplusOnly) {
        int taken = 0;
        for (Source source : sources) {
            if (taken >= want) break;
            if (source.be().isRemoved()) continue;
            // A pile that regrouped since the network was read answers through its stone's current pile.
            PulseCairnBlockEntity.Pile pile = source.be() instanceof PulseCairnBlockEntity stone ? stone.pile() : null;
            Object identity = pile != null ? pile : source.be();
            if (ledger.drew(identity)) continue;
            PulseHandler handler = source.handler();
            int avail = handler.extractPulse(want - taken, true);
            if (surplusOnly) avail = Math.min(avail, handler.getPulseStored() - handler.getPulseCapacity() / 2);
            if (pile != null) avail = Math.min(avail, ledger.pileOutLeft(pile));
            if (avail <= 0) continue;
            int moved = route(source.taps(), pile == null ? source.be().getBlockPos() : null, sink, avail, ledger);
            if (moved <= 0) continue;
            ledger.drawing(identity);
            if (!ledger.simulate) moved = handler.extractPulse(moved, false);
            if (pile != null) ledger.pileOut(pile, moved);
            taken += moved;
        }
        return taken;
    }

    /**
     * Pushes Pulse made at {@code at} (the Lattice Converter) onto its networks: into cairns first, then into
     * generator buffers, where machines can draw it. Limited by the conductor taps it passes through.
     *
     * @return what the lattice took (or, simulated, would take)
     */
    public static int insert(Level level, BlockPos at, int amount, boolean simulate) {
        if (amount <= 0 || level.isClientSide) return 0;
        Taps taps = tapsAt(level, at);
        if (taps.isEmpty()) return 0;
        Ledger ledger = new Ledger(level.getGameTime(), simulate);
        int given = 0;
        for (Tap tap : taps.byNet()) {
            readMembers(level, tap.net());
            given += fill(tap.net().cairns, tap.conductors(), amount - given, ledger, at);
            if (given < amount) given += fill(tap.net().generators, tap.conductors(), amount - given, ledger, at);
            if (given >= amount) break;
        }
        return given;
    }

    private static int fill(List<Source> targets, LatticeConductorBlockEntity[] from, int want, Ledger ledger, BlockPos at) {
        int given = 0;
        for (Source target : targets) {
            if (given >= want) break;
            if (target.be().isRemoved() || target.be().getBlockPos().equals(at)) continue;
            PulseCairnBlockEntity.Pile pile = target.be() instanceof PulseCairnBlockEntity stone ? stone.pile() : null;
            Object identity = pile != null ? pile : target.be();
            if (ledger.drew(identity)) continue;
            PulseHandler handler = target.handler();
            int room = handler.insertPulse(want - given, true);
            if (pile != null) room = Math.min(room, ledger.pileInLeft(pile));
            if (room <= 0) continue;
            int moved = route(from, at, target.taps(), room, ledger);
            if (moved <= 0) continue;
            ledger.drawing(identity);
            if (!ledger.simulate) moved = handler.insertPulse(moved, false);
            if (pile != null) ledger.pileIn(pile, moved);
            given += moved;
        }
        return given;
    }

    /**
     * A cairn pile drinks generator surplus off every network one of its stones is on, up to its in rate and its
     * room, through the conductors in reach of its stones.
     *
     * @return what the pile took
     */
    public static int fillPile(Level level, PulseCairnBlockEntity.Pile pile) {
        long now = level.getGameTime();
        int room = Math.min(pile.capacity() - pile.stored(), pile.inLeft(now));
        if (room <= 0) return 0;
        LinkedHashMap<Net, List<LatticeConductorBlockEntity>> sinks = new LinkedHashMap<>();
        for (PulseCairnBlockEntity stone : pile.members()) {
            for (Tap tap : tapsAt(level, stone.getBlockPos()).byNet()) {
                List<LatticeConductorBlockEntity> list = sinks.computeIfAbsent(tap.net(), n -> new ArrayList<>());
                for (LatticeConductorBlockEntity conductor : tap.conductors()) if (!list.contains(conductor)) list.add(conductor);
            }
        }
        if (sinks.isEmpty()) return 0;
        Ledger ledger = new Ledger(now, false);
        int taken = 0;
        for (var entry : sinks.entrySet()) {
            readMembers(level, entry.getKey());
            int drawn = drawFrom(entry.getKey().generators, entry.getValue().toArray(LatticeConductorBlockEntity[]::new),
                    room - taken, ledger, true);
            if (drawn > 0) {
                int kept = pile.insert(drawn, false);
                pile.spendIn(kept, now);
                taken += kept;
            }
            if (taken >= room) break;
        }
        return taken;
    }

    /** What a lattice reading shows: the network, the tapped conductor and its rate, and the sources on it. */
    public record Reading(int conductors, long rate, int generators, int cairns, int totems, long stored, long capacity,
                          @Nullable BlockPos tap, int tapRank, int tapRate, int tapCarried, boolean active) {
        public static final Reading NONE = new Reading(0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, false);
        public boolean onLattice() { return tap != null; }
    }

    public static Reading read(Level level, BlockPos at) {
        Tap tap = tapsAt(level, at).first();
        if (tap == null) return Reading.NONE;
        Net net = tap.net();
        readMembers(level, net);
        LatticeConductorBlockEntity nearest = tap.nearest();
        long now = level.getGameTime();
        return new Reading(net.conductors().size(), net.rate(), net.generators().size(), net.cairns().size(),
                net.totems().size(), net.stored(), net.capacity(), nearest.getBlockPos(),
                tk.darrow.tribalpower.item.MachineRank.rank(nearest), nearest.rate(), nearest.carriedLastSecond(now),
                net.active());
    }

    /** Test and diagnostics hook: how many networks the level currently holds resolved. */
    public static int cachedNets(Level level) {
        LevelWeave weave = LEVELS.get(level);
        return weave == null ? 0 : (int) weave.nets.values().stream().distinct().count();
    }
}
