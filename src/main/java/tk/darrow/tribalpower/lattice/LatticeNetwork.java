package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LatticeConductorBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Totem Lattice helpers — proximity scans, chalk links, Conductor routing, and Pulse draw.
 */
public final class LatticeNetwork {
    public static final int DEFAULT_RADIUS = 8;
    public static final int LINK_RANGE = 16;
    /**
     * Safety stop for one draw walking a conductor line. Gameplay does not cap the line at the four a
     * craft gives; this only keeps a solid cube of conductors from scanning the whole loaded world.
     */
    public static final int MAX_CONDUCTOR_CHAIN = 64;

    /** The x, y, z order a walk of the cube met block entities in. Built once: every draw sorts by it, often twice. */
    private static final java.util.Comparator<BlockEntity> CUBE_ORDER = java.util.Comparator
            .comparingInt((BlockEntity be) -> be.getBlockPos().getX())
            .thenComparingInt(be -> be.getBlockPos().getY())
            .thenComparingInt(be -> be.getBlockPos().getZ());

    private LatticeNetwork() {}

    public static boolean canLink(BlockPos from, BlockPos to) {
        return !from.equals(to) && from.closerThan(to, LINK_RANGE);
    }

    /**
     * Bidirectional chalk link. Returns false if already linked or out of range.
     */
    public static boolean linkTotems(ResonanceTotemBlockEntity from, ResonanceTotemBlockEntity to) {
        BlockPos a = from.getBlockPos();
        BlockPos b = to.getBlockPos();
        if (from.getLevel() != to.getLevel() || !canLink(a, b)) {
            return false;
        }
        if (from.isLinkedTo(b) && to.isLinkedTo(a)) {
            return false;
        }
        from.addLink(b);
        to.addLink(a);
        return true;
    }

    /**
     * Every block entity of {@code type} inside the box, found by walking the loaded chunks' block-entity
     * maps instead of probing each of the box's positions. A radius-8 cube is 4,913 lookups a call and this
     * runs on machine beats; the chunk maps hold only the handful of block entities that actually exist.
     * Results come back in the same x, y, z order the cube walk used, so callers see no change.
     */
    private static <T extends BlockEntity> List<T> inBox(Level level, Class<T> type,
                                                         int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List<T> found = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.isRemoved() || !type.isInstance(be)) continue;
                    BlockPos at = be.getBlockPos();
                    if (at.getX() < minX || at.getX() > maxX || at.getY() < minY || at.getY() > maxY
                            || at.getZ() < minZ || at.getZ() > maxZ) continue;
                    found.add(type.cast(be));
                }
            }
        }
        if (found.size() > 1)
            found.sort(CUBE_ORDER);
        return found;
    }

    /** Every block entity within {@code radius}, in the same order a walk of the cube would have found them. */
    public static List<BlockEntity> blockEntitiesAround(Level level, BlockPos origin, int radius) {
        return inBox(level, BlockEntity.class,
                origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius);
    }

    public static List<ResonanceTotemBlockEntity> findNearbyTotems(Level level, BlockPos origin, int radius) {
        List<ResonanceTotemBlockEntity> found = inBox(level, ResonanceTotemBlockEntity.class,
                origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius);
        found.removeIf(totem -> totem.getBlockPos().equals(origin));
        return found;
    }

    /** Resonance Totems of {@code voice} that {@link #collectAttunements} would count: nearby, plus chalk-linked within {@link #LINK_RANGE}. */
    public static List<ResonanceTotemBlockEntity> findVoiceTotems(Level level, BlockPos origin, int radius, Attunement voice) {
        return voiceTotems(level, origin, findNearbyTotems(level, origin, radius), voice);
    }

    /**
     * The Resonance and Kinship Totems around one spot, found in a single walk for a machine beat that asks about
     * them several times (voice present, keeping, feeding). Answers match the one-off lookups they stand in for;
     * a totem's own state (keeping, stored Pulse) is still read live each time.
     */
    public record TotemsNear(Level level, BlockPos origin, List<ResonanceTotemBlockEntity> totems,
                             List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> kinship) {
        public static TotemsNear of(Level level, BlockPos origin, int radius) {
            List<ResonanceTotemBlockEntity> totems = new ArrayList<>();
            List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> kinship = new ArrayList<>();
            int minY = origin.getY() - radius, maxY = origin.getY() + radius;
            int minX = origin.getX() - radius, maxX = origin.getX() + radius;
            int minZ = origin.getZ() - radius, maxZ = origin.getZ() + radius;
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be.isRemoved() || !(be instanceof ResonanceTotemBlockEntity
                                || be instanceof tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity)) continue;
                        BlockPos p = be.getBlockPos();
                        if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY || p.getZ() < minZ || p.getZ() > maxZ) continue;
                        if (be instanceof ResonanceTotemBlockEntity totem) {
                            if (!p.equals(origin)) totems.add(totem);
                        } else {
                            kinship.add((tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity) be);
                        }
                    }
                }
            }
            if (totems.size() > 1) totems.sort(CUBE_ORDER);
            return new TotemsNear(level, origin, totems, kinship);
        }

        /** As {@link #collectAttunements}. */
        public Set<Attunement> attunements() {
            return attunementsOf(level, origin, totems, kinship);
        }

        /** As {@link #hasAttunement}. */
        public boolean has(Attunement needed) {
            return attunements().contains(needed);
        }

        /** As {@link #findVoiceTotems}. */
        public List<ResonanceTotemBlockEntity> voice(Attunement voice) {
            return voiceTotems(level, origin, totems, voice);
        }
    }

    private static List<ResonanceTotemBlockEntity> voiceTotems(Level level, BlockPos origin,
                                                               List<ResonanceTotemBlockEntity> nearby, Attunement voice) {
        List<ResonanceTotemBlockEntity> found = new ArrayList<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (ResonanceTotemBlockEntity totem : nearby) {
            if (totem.getAttunement() == voice && seen.add(totem.getBlockPos())) found.add(totem);
            for (BlockPos linked : totem.getLinks()) {
                if (!linked.closerThan(origin, LINK_RANGE)) continue;
                BlockEntity be = level.hasChunkAt(linked) ? level.getBlockEntity(linked) : null;
                if (be instanceof ResonanceTotemBlockEntity linkedTotem && canLink(totem.getBlockPos(), linked)
                        && linkedTotem.isLinkedTo(totem.getBlockPos()) && linkedTotem.getAttunement() == voice
                        && seen.add(linkedTotem.getBlockPos())) {
                    found.add(linkedTotem);
                }
            }
        }
        return found;
    }

    /**
     * BFS across Ritual Chalk links starting from {@code seed}.
     */
    public static List<ResonanceTotemBlockEntity> collectChalkNetwork(Level level, ResonanceTotemBlockEntity seed) {
        LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();
        List<ResonanceTotemBlockEntity> network = new ArrayList<>();
        ArrayDeque<ResonanceTotemBlockEntity> queue = new ArrayDeque<>();
        queue.add(seed);
        visited.add(seed.getBlockPos().immutable());
        while (!queue.isEmpty()) {
            ResonanceTotemBlockEntity current = queue.removeFirst();
            network.add(current);
            for (BlockPos link : current.getLinks()) {
                if (!canLink(current.getBlockPos(), link)) continue;
                BlockPos key = link.immutable();
                BlockEntity candidate = level.hasChunkAt(key) ? level.getBlockEntity(key) : null;
                if (!(candidate instanceof ResonanceTotemBlockEntity linked) || !linked.isLinkedTo(current.getBlockPos())) continue;
                if (!visited.add(key)) {
                    continue;
                }
                queue.add(linked);
            }
            // Ley Binding: a live ley line makes two distant totems adjacent for routing (rite/world/LeyLines).
            for (BlockPos link : tk.darrow.tribalpower.rite.world.LeyLines.linked(level, current.getBlockPos())) {
                BlockPos key = link.immutable();
                BlockEntity candidate = level.hasChunkAt(key) ? level.getBlockEntity(key) : null;
                if (!(candidate instanceof ResonanceTotemBlockEntity linked) || !visited.add(key)) continue;
                queue.add(linked);
            }
        }
        return network;
    }

    /**
     * Union of chalk-linked components reachable from any totem near {@code origin}.
     */
    public static List<ResonanceTotemBlockEntity> collectChalkNetworkNear(Level level, BlockPos origin, int radius) {
        LinkedHashSet<BlockPos> seen = new LinkedHashSet<>();
        List<ResonanceTotemBlockEntity> network = new ArrayList<>();
        for (ResonanceTotemBlockEntity seed : findNearbyTotems(level, origin, radius)) {
            if (seen.contains(seed.getBlockPos())) {
                continue;
            }
            for (ResonanceTotemBlockEntity node : collectChalkNetwork(level, seed)) {
                if (seen.add(node.getBlockPos().immutable())) {
                    network.add(node);
                }
            }
        }
        return network;
    }

    /**
     * True when at least two totems share a chalk link path (Conductor's minimum network).
     */
    public static boolean isConductable(List<ResonanceTotemBlockEntity> network) {
        if (network.size() < 2) {
            return false;
        }
        var byPosition = new java.util.HashMap<BlockPos, ResonanceTotemBlockEntity>();
        for (ResonanceTotemBlockEntity totem : network) byPosition.put(totem.getBlockPos(), totem);
        for (ResonanceTotemBlockEntity totem : network) {
            for (BlockPos link : totem.getLinks()) {
                ResonanceTotemBlockEntity other = byPosition.get(link);
                if (other != null && totem.getLevel() == other.getLevel() && canLink(totem.getBlockPos(), other.getBlockPos())
                        && totem.isLinkedTo(other.getBlockPos()) && other.isLinkedTo(totem.getBlockPos())) return true;
            }
            // A live ley line between two network totems is also a conductable path (rite/world/LeyLines).
            for (BlockPos link : tk.darrow.tribalpower.rite.world.LeyLines.linked(totem.getLevel(), totem.getBlockPos()))
                if (byPosition.containsKey(link)) return true;
        }
        return false;
    }

    public static List<BlockPos> networkHubs(List<ResonanceTotemBlockEntity> network) {
        List<BlockPos> hubs = new ArrayList<>(network.size());
        for (ResonanceTotemBlockEntity totem : network) {
            hubs.add(totem.getBlockPos());
        }
        return hubs;
    }

    public static List<SongBenchBlockEntity> findSongBenchesNearHubs(Level level, Collection<BlockPos> hubs, int radius) {
        return nearHubs(level, SongBenchBlockEntity.class, hubs, radius);
    }

    public static List<AncestralCacheBlockEntity> findCachesNearHubs(Level level, Collection<BlockPos> hubs, int radius) {
        return nearHubs(level, AncestralCacheBlockEntity.class, hubs, radius);
    }

    /**
     * One pass over the box the hubs span, then a range test per candidate. The old walk repeated a whole
     * radius-8 cube for every hub and allocated a BlockPos per position to dedupe them; a conductor with
     * eight hubs paid tens of thousands of block-entity lookups a second for a handful of benches.
     */
    private static <T extends BlockEntity> List<T> nearHubs(Level level, Class<T> type,
                                                            Collection<BlockPos> hubs, int radius) {
        if (hubs.isEmpty()) return List.of();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos hub : hubs) {
            minX = Math.min(minX, hub.getX() - radius); maxX = Math.max(maxX, hub.getX() + radius);
            minY = Math.min(minY, hub.getY() - radius); maxY = Math.max(maxY, hub.getY() + radius);
            minZ = Math.min(minZ, hub.getZ() - radius); maxZ = Math.max(maxZ, hub.getZ() + radius);
        }
        List<T> found = inBox(level, type, minX, minY, minZ, maxX, maxY, maxZ);
        found.removeIf(be -> {
            BlockPos at = be.getBlockPos();
            for (BlockPos hub : hubs) {
                if (Math.abs(at.getX() - hub.getX()) <= radius && Math.abs(at.getY() - hub.getY()) <= radius
                        && Math.abs(at.getZ() - hub.getZ()) <= radius) return false;
            }
            return true;
        });
        return found;
    }

    /**
     * Pull Pulse from nearby generators: Drumheart, Ley Collector, Pulse Resonator,
     * or any {@link tk.darrow.tribalpower.api.pulse.PulseGenerator} (Wind Harp, Wave Drum,
     * Ember Horn, Loom Anchor, Wake Bell). Totem buffers and other stores are skipped.
     */
    public static int extractPulseFromGenerators(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        return drainHandlers(level, origin, radius, amount, true, simulate, new HashSet<>());
    }

    /**
     * What a Conductor may lift onto the lattice: live generators first, then a Pulse Cairn's held beat.
     *
     * <p>A Cairn swallows {@link PulseCairnBlockEntity#FILL_RATE} a second out of the same generators the
     * Conductor draws on, so a camp with a Cairn in it used to starve the lattice outright — the Cairn won
     * every beat and the Conductor could not see where the Pulse had gone. Totems are the Conductor's
     * destination and a station's buffer is Pulse it has already claimed, so neither is ever drained here.
     */
    public static int extractPulseForConductor(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        HashSet<Long> piles = new HashSet<>();
        int remaining = amount - drainHandlers(level, origin, radius, amount, true, simulate, piles);
        if (remaining > 0) {
            remaining -= drain(level, origin, radius, remaining, be -> be instanceof PulseCairnBlockEntity, simulate, piles);
        }
        return amount - remaining;
    }

    /**
     * How much the linked totems could still accept. Pulse that cannot land must not be drawn: taking it
     * and handing it back costs a full refund sweep every beat once a camp is charged.
     */
    public static int roomInTotems(List<ResonanceTotemBlockEntity> totems) {
        long room = 0;
        for (ResonanceTotemBlockEntity totem : totems) {
            room += Math.max(0, totem.getPulseCapacity() - totem.getPulseStored());
        }
        return (int) Math.min(room, Integer.MAX_VALUE);
    }

    /**
     * Spread Pulse round-robin into totem buffers that still have room.
     * @return amount actually inserted
     */
    public static int distributePulseToTotems(List<ResonanceTotemBlockEntity> totems, int amount) {
        if (amount <= 0 || totems.isEmpty()) {
            return 0;
        }
        int remaining = amount;
        boolean progressed;
        do {
            progressed = false;
            int open = 0;
            for (ResonanceTotemBlockEntity totem : totems) {
                if (totem.canReceivePulse()) {
                    open++;
                }
            }
            if (open == 0) {
                break;
            }
            int share = Math.max(1, remaining / open);
            for (ResonanceTotemBlockEntity totem : totems) {
                if (remaining <= 0) {
                    break;
                }
                if (!totem.canReceivePulse()) {
                    continue;
                }
                int got = totem.insertPulse(Math.min(share, remaining), false);
                if (got > 0) {
                    remaining -= got;
                    progressed = true;
                }
            }
        } while (progressed && remaining > 0);
        return amount - remaining;
    }

    /**
     * Prefer totems near benches that want Pulse assist, then the rest of the network.
     */
    public static int pushPulsePreferringAssist(Level level, List<ResonanceTotemBlockEntity> network,
                                                List<SongBenchBlockEntity> benches, int amount) {
        if (amount <= 0 || network.isEmpty()) {
            return 0;
        }
        List<ResonanceTotemBlockEntity> priority = new ArrayList<>();
        List<ResonanceTotemBlockEntity> rest = new ArrayList<>();
        Set<BlockPos> priorityPos = new HashSet<>();
        for (SongBenchBlockEntity bench : benches) {
            if (!bench.wantsPulseAssist()) {
                continue;
            }
            BlockPos benchPos = bench.getBlockPos();
            for (ResonanceTotemBlockEntity totem : network) {
                if (totem.getBlockPos().closerThan(benchPos, DEFAULT_RADIUS)
                        && priorityPos.add(totem.getBlockPos().immutable())) {
                    priority.add(totem);
                }
            }
        }
        for (ResonanceTotemBlockEntity totem : network) {
            if (!priorityPos.contains(totem.getBlockPos())) {
                rest.add(totem);
            }
        }
        int pushed = distributePulseToTotems(priority, amount);
        if (pushed < amount) {
            pushed += distributePulseToTotems(rest, amount - pushed);
        }
        return pushed;
    }

    /**
     * Echo items no longer travel through the Song Bench. The bench writes songs. Stations and
     * caches move their own items with hoppers. Kept so a conductor tick stays a single call.
     * @return false, always
     */
    public static boolean routeEchoItems(Level level, List<SongBenchBlockEntity> benches,
                                         List<AncestralCacheBlockEntity> caches) {
        return false;
    }

    /**
     * Attunements present via proximity or chalk links from nearby totems.
     */
    public static Set<Attunement> collectAttunements(Level level, BlockPos origin, int radius) {
        return attunementsOf(level, origin, findNearbyTotems(level, origin, radius), findNearbyKinshipTotems(level, origin, radius));
    }

    private static Set<Attunement> attunementsOf(Level level, BlockPos origin, List<ResonanceTotemBlockEntity> nearby,
                                                 List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> kinshipNearby) {
        EnumSet<Attunement> set = EnumSet.noneOf(Attunement.class);
        for (ResonanceTotemBlockEntity totem : nearby) {
            set.add(totem.getAttunement());
            for (BlockPos linked : totem.getLinks()) {
                if (!linked.closerThan(origin, LINK_RANGE)) {
                    continue;
                }
                BlockEntity be = level.hasChunkAt(linked) ? level.getBlockEntity(linked) : null;
                if (be instanceof ResonanceTotemBlockEntity linkedTotem && canLink(totem.getBlockPos(), linked)
                        && linkedTotem.isLinkedTo(totem.getBlockPos())) {
                    set.add(linkedTotem.getAttunement());
                }
            }
        }
        // Kinship Totems lend their tribe's voice to stations as well (design 3.0 §2).
        for (tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity kinship : kinshipNearby)
            set.add(kinship.attunement());
        return set;
    }

    /** Kinship Totems within {@code radius} (tribe voices, tracked separately from Attunement). */
    public static List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> findNearbyKinshipTotems(Level level, BlockPos origin, int radius) {
        // Walk the loaded chunks' block-entity maps instead of probing every position of the cube: this runs inside
        // collectAttunements for every station beat, so it must not double the cost of the totem scan.
        List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> found = new ArrayList<>();
        int minY = origin.getY() - radius, maxY = origin.getY() + radius;
        int minX = origin.getX() - radius, maxX = origin.getX() + radius;
        int minZ = origin.getZ() - radius, maxZ = origin.getZ() + radius;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity kinship) || be.isRemoved()) continue;
                    BlockPos p = be.getBlockPos();
                    if (p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY && p.getZ() >= minZ && p.getZ() <= maxZ)
                        found.add(kinship);
                }
            }
        }
        return found;
    }

    /** Number of distinct tribes with a Kinship Totem within {@code radius}: extra voices for the Pulse Resonator. */
    public static int countKinshipTribes(Level level, BlockPos origin, int radius) {
        EnumSet<tk.darrow.tribalpower.tribe.TribeDefinition> tribes = EnumSet.noneOf(tk.darrow.tribalpower.tribe.TribeDefinition.class);
        for (tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity kinship : findNearbyKinshipTotems(level, origin, radius)) tribes.add(kinship.tribe());
        return tribes.size();
    }

    public static boolean hasAttunement(Level level, BlockPos origin, int radius, Attunement needed) {
        return collectAttunements(level, origin, radius).contains(needed);
    }

    /**
     * Pull Pulse from nearby generators first (Drumheart, Ley Collector, Pulse Resonator, or any
     * {@link tk.darrow.tribalpower.api.pulse.PulseGenerator}), then other stores in that same cube.
     * Whatever is still wanted is drawn through Lattice Conductors: each conductor within {@code radius}
     * of the last extends the zone, and the draw may take generators, Pulse Cairns and totem buffers
     * beside any conductor on that line. A station's own buffer is never reached this way.
     * @return amount actually extracted
     */
    public static int extractPulseNearby(Level level, BlockPos origin, int radius, int amount) {
        return extractPulseNearby(level, origin, radius, amount, false);
    }

    /**
     * @param simulate when true, probes available Pulse without draining
     */
    public static int extractPulseNearby(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        return new PulseSources(level, origin, radius).draw(amount, simulate);
    }

    /**
     * All or nothing: takes {@code amount} only when the whole of it is there, and reports whether it did.
     * The same sources, order and amounts as a simulated {@link #extractPulseNearby} followed by a real one,
     * but the cube and any conductor line are gathered once instead of twice.
     */
    public static boolean tryExtractPulseNearby(Level level, BlockPos origin, int radius, int amount) {
        if (amount <= 0) {
            return true;
        }
        PulseSources sources = new PulseSources(level, origin, radius);
        if (sources.draw(amount, true) < amount) {
            return false;
        }
        sources.draw(amount, false);
        return true;
    }

    /**
     * What one draw from {@code origin} may take, in the order it takes it: generators in the cube, then the
     * cube's other stores, then what a conductor line reaches (generators, cairns, totem buffers). The cube is
     * walked once for both local passes; the line is walked only once a draw actually runs short.
     */
    private static final class PulseSources {
        private final Level level;
        private final BlockPos origin;
        private final int radius;
        private final List<BlockEntity> generators = new ArrayList<>();
        private final List<BlockEntity> stores = new ArrayList<>();
        private List<BlockEntity> lineGenerators, lineCairns, lineTotems;

        PulseSources(Level level, BlockPos origin, int radius) {
            this.level = level;
            this.origin = origin;
            this.radius = radius;
            // Probing all 17^3 positions of the cube cost thousands of block-entity lookups per call; the loaded
            // chunks' block-entity maps hold only the few that exist. Kept in the x, y, z order the cube walk used.
            int minY = origin.getY() - radius, maxY = origin.getY() + radius;
            int minX = origin.getX() - radius, maxX = origin.getX() + radius;
            int minZ = origin.getZ() - radius, maxZ = origin.getZ() + radius;
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof PulseHandler) || be.isRemoved()) continue;
                        BlockPos p = be.getBlockPos();
                        if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY || p.getZ() < minZ || p.getZ() > maxZ) continue;
                        (isGenerator(be) ? generators : stores).add(be);
                    }
                }
            }
            if (generators.size() > 1) generators.sort(CUBE_ORDER);
            if (stores.size() > 1) stores.sort(CUBE_ORDER);
        }

        int draw(int amount, boolean simulate) {
            // Every stone of a Pulse Cairn pile answers for the whole pile, so each pile is drawn once per call,
            // across all three passes: a simulated draw must not see a pile of three as three piles.
            HashSet<Long> piles = new HashSet<>();
            int taken = drainOrdered(generators, amount, simulate, piles);
            if (taken < amount) taken += drainOrdered(stores, amount - taken, simulate, piles);
            if (taken < amount) {
                if (lineGenerators == null) gatherLine();
                taken += drainOrdered(lineGenerators, amount - taken, simulate, piles);
                if (taken < amount) taken += drainOrdered(lineCairns, amount - taken, simulate, piles);
                if (taken < amount) taken += drainOrdered(lineTotems, amount - taken, simulate, piles);
            }
            return taken;
        }

        /**
         * Sources a conductor line may lend to a machine. Generators first, then cairns, then totem buffers.
         * Positions already inside the caller's own cube were drained by the local pass and must not be
         * counted twice. Station buffers stay where they are: the line moves camp Pulse, not a machine's claim.
         */
        private void gatherLine() {
            lineGenerators = new ArrayList<>();
            lineCairns = new ArrayList<>();
            lineTotems = new ArrayList<>();
            HashSet<BlockPos> seen = new HashSet<>();
            for (LatticeConductorBlockEntity conductor : conductorZone(level, origin, radius)) {
                for (BlockEntity be : blockEntitiesAround(level, conductor.getBlockPos(), radius)) {
                    BlockPos at = be.getBlockPos();
                    if (inCube(origin, radius, at) || be.isRemoved() || !(be instanceof PulseHandler)) continue;
                    if (!seen.add(at.immutable())) continue;
                    if (isGenerator(be)) lineGenerators.add(be);
                    else if (be instanceof PulseCairnBlockEntity) lineCairns.add(be);
                    else if (be instanceof ResonanceTotemBlockEntity) lineTotems.add(be);
                }
            }
            if (lineGenerators.size() > 1) lineGenerators.sort(CUBE_ORDER);
            if (lineCairns.size() > 1) lineCairns.sort(CUBE_ORDER);
            if (lineTotems.size() > 1) lineTotems.sort(CUBE_ORDER);
        }
    }

    /** Conductors on the line a draw from {@code origin} would walk, including one standing at the origin. */
    public static int countConductorZone(Level level, BlockPos origin, int radius) {
        return conductorZone(level, origin, radius).size();
    }

    /** A level's cache is dropped whole once it holds this many lines, rather than grown without bound. */
    private static final int CONDUCTOR_LINE_CACHE_LIMIT = 4096;
    /**
     * Resolved conductor lines per server level, keyed by draw origin and radius. Server thread only; emptied when
     * its level unloads or the server stops.
     */
    private static final java.util.Map<Level, java.util.Map<LineKey, ConductorLine>> CONDUCTOR_LINES = new java.util.HashMap<>();

    private record LineKey(long origin, int radius) {}

    /**
     * A resolved line and everything the walk that found it read: each conductor it met with that conductor's
     * redstone state, and each chunk it scanned as the level held it then. A conductor placed or removed drops
     * every line in its level; a chunk that loads, unloads or falls out of reach, a conductor that is gone, or a
     * signal that moved fails {@link #holds} and the line is walked again. Chunks are held weakly so a line kept
     * for a machine that stopped drawing never pins an unloaded chunk; one collected since could only matter
     * through its conductors, and those were removed with it.
     */
    private record ConductorLine(List<LatticeConductorBlockEntity> zone,
                                 java.util.IdentityHashMap<LatticeConductorBlockEntity, Boolean> signals,
                                 long[] chunkKeys, java.lang.ref.WeakReference<?>[] chunks) {
        boolean holds(Level level) {
            for (int i = 0; i < chunkKeys.length; i++) {
                if (level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkKeys[i]),
                        net.minecraft.world.level.ChunkPos.getZ(chunkKeys[i])) != (chunks[i] == null ? null : chunks[i].get())) return false;
            }
            for (var met : signals.entrySet()) {
                LatticeConductorBlockEntity conductor = met.getKey();
                if (conductor.isRemoved() || level.hasNeighborSignal(conductor.getBlockPos()) != met.getValue()) return false;
            }
            return true;
        }
    }

    /**
     * A conductor joined or left {@code level}: every line resolved there may have changed. A chunk still being
     * built off the server thread is not reachable by a draw yet; its arrival shows as a changed chunk instead.
     */
    public static void conductorLinesChanged(Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && server.getServer().isSameThread())
            CONDUCTOR_LINES.remove(level);
    }

    /** Drops every cached line, for a level unload ({@code level} non-null) or a server stop (null). */
    public static void clearConductorLines(@org.jetbrains.annotations.Nullable Level level) {
        if (level == null) CONDUCTOR_LINES.clear();
        else if (!level.isClientSide) CONDUCTOR_LINES.remove(level);
    }

    /**
     * Breadth-first across conductors. A redstone signal cuts that block out of the line, the same way
     * it pauses the conductor's own totem fill. The walk starts at every conductor already inside the
     * caller's cube, so a machine never has to be the block that begins the chain.
     *
     * <p>A line of 64 conductors scans every block entity near each of them, and every machine drawing through
     * it did that each beat; the resolved line is kept per level until something it read changes.
     */
    private static List<LatticeConductorBlockEntity> conductorZone(Level level, BlockPos origin, int radius) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || !server.getServer().isSameThread())
            return walkConductorLine(level, origin, radius).zone();
        var lines = CONDUCTOR_LINES.computeIfAbsent(level, l -> new java.util.HashMap<>());
        LineKey key = new LineKey(origin.asLong(), radius);
        ConductorLine line = lines.get(key);
        if (line == null || !line.holds(level)) {
            if (line == null && lines.size() >= CONDUCTOR_LINE_CACHE_LIMIT) lines.clear();
            line = walkConductorLine(level, origin, radius);
            lines.put(key, line);
        }
        return line.zone();
    }

    private static ConductorLine walkConductorLine(Level level, BlockPos origin, int radius) {
        // A conductor's signal cannot change within one walk, so each is read once; the answers are what the
        // cached line later checks against.
        var signals = new java.util.IdentityHashMap<LatticeConductorBlockEntity, Boolean>();
        var scanned = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
        ArrayDeque<LatticeConductorBlockEntity> queue = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (LatticeConductorBlockEntity seed : conductorsAround(level, origin, radius, scanned)) {
            if (signals.computeIfAbsent(seed, c -> level.hasNeighborSignal(c.getBlockPos()))) continue;
            if (seen.add(seed.getBlockPos().immutable())) queue.add(seed);
        }
        List<LatticeConductorBlockEntity> zone = new ArrayList<>();
        while (!queue.isEmpty() && zone.size() < MAX_CONDUCTOR_CHAIN) {
            LatticeConductorBlockEntity current = queue.removeFirst();
            zone.add(current);
            if (zone.size() >= MAX_CONDUCTOR_CHAIN) break;
            for (LatticeConductorBlockEntity next : conductorsAround(level, current.getBlockPos(), radius, scanned)) {
                if (signals.computeIfAbsent(next, c -> level.hasNeighborSignal(c.getBlockPos()))) continue;
                if (seen.add(next.getBlockPos().immutable())) queue.add(next);
            }
        }
        long[] chunkKeys = scanned.toLongArray();
        var chunks = new java.lang.ref.WeakReference<?>[chunkKeys.length];
        for (int i = 0; i < chunkKeys.length; i++) {
            var chunk = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkKeys[i]),
                    net.minecraft.world.level.ChunkPos.getZ(chunkKeys[i]));
            chunks[i] = chunk == null ? null : new java.lang.ref.WeakReference<>(chunk);
        }
        return new ConductorLine(List.copyOf(zone), signals, chunkKeys, chunks);
    }

    /** Conductors in the cube around {@code at}, noting each chunk the cube touches in {@code scanned}. */
    private static List<LatticeConductorBlockEntity> conductorsAround(Level level, BlockPos at, int radius,
                                                                      it.unimi.dsi.fastutil.longs.LongSet scanned) {
        for (int cx = (at.getX() - radius) >> 4; cx <= (at.getX() + radius) >> 4; cx++) {
            for (int cz = (at.getZ() - radius) >> 4; cz <= (at.getZ() + radius) >> 4; cz++) {
                scanned.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
            }
        }
        return inBox(level, LatticeConductorBlockEntity.class,
                at.getX() - radius, at.getY() - radius, at.getZ() - radius,
                at.getX() + radius, at.getY() + radius, at.getZ() + radius);
    }

    private static boolean inCube(BlockPos origin, int radius, BlockPos at) {
        return Math.abs(at.getX() - origin.getX()) <= radius
                && Math.abs(at.getY() - origin.getY()) <= radius
                && Math.abs(at.getZ() - origin.getZ()) <= radius;
    }

    /** Drains {@code found}, already in cube order, until {@code amount} is met. */
    private static int drainOrdered(List<BlockEntity> found, int amount, boolean simulate, Set<Long> piles) {
        if (amount <= 0 || found.isEmpty()) return 0;
        int taken = 0;
        for (BlockEntity be : found) {
            if (taken >= amount) break;
            if (PulseCairnBlockEntity.repeatsPile(be, piles)) continue;
            taken += ((PulseHandler) be).extractPulse(amount - taken, simulate);
        }
        return taken;
    }

    private static boolean isGenerator(BlockEntity be) {
        return be instanceof DrumheartBlockEntity
                || be instanceof LeyCollectorBlockEntity
                || be instanceof PulseResonatorBlockEntity
                // The six voices of 3.1 all implement PulseGenerator, so they need no case of their own.
                || be instanceof tk.darrow.tribalpower.api.pulse.PulseGenerator;
    }

    private static int drainHandlers(Level level, BlockPos origin, int radius, int amount,
                                     boolean generatorsFirst, boolean simulate, Set<Long> piles) {
        return drain(level, origin, radius, amount, be -> isGenerator(be) == generatorsFirst, simulate, piles);
    }

    private static int drain(Level level, BlockPos origin, int radius, int amount,
                             Predicate<BlockEntity> accept, boolean simulate, Set<Long> piles) {
        // Every machine calls this each working tick, often twice (simulate, then draw). Probing all 17^3 positions
        // of the cube cost thousands of block-entity lookups per call; the loaded chunks' block-entity maps hold only
        // the few that exist. Candidates are then drained in the same x, y, z order the cube walk used.
        List<BlockEntity> found = new ArrayList<>();
        int minY = origin.getY() - radius, maxY = origin.getY() + radius;
        int minX = origin.getX() - radius, maxX = origin.getX() + radius;
        int minZ = origin.getZ() - radius, maxZ = origin.getZ() + radius;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof PulseHandler) || be.isRemoved()) continue;
                    BlockPos p = be.getBlockPos();
                    if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY || p.getZ() < minZ || p.getZ() > maxZ) continue;
                    if (accept.test(be)) found.add(be);
                }
            }
        }
        if (found.size() > 1)
            found.sort(CUBE_ORDER);
        int taken = 0;
        for (BlockEntity be : found) {
            if (taken >= amount) break;
            if (PulseCairnBlockEntity.repeatsPile(be, piles)) continue;
            taken += ((PulseHandler) be).extractPulse(amount - taken, simulate);
        }
        return taken;
    }

    /**
     * Push Pulse out into whatever nearby will hold it, storage before generators.
     *
     * <p>The mirror of {@link #extractPulseNearby}, for the Lattice Converter. Totems and caches are
     * filled first and generators last, because a generator's buffer is its own working room and
     * filling it only stops it generating.
     *
     * @return amount actually taken by the lattice
     */
    public static int insertPulseNearby(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int remaining = amount;
        HashSet<Long> piles = new HashSet<>();
        remaining -= fill(level, origin, radius, remaining, be -> !isGenerator(be), simulate, piles);
        if (remaining > 0) remaining -= fill(level, origin, radius, remaining, LatticeNetwork::isGenerator, simulate, piles);
        return amount - remaining;
    }

    private static int fill(Level level, BlockPos origin, int radius, int amount,
                            Predicate<BlockEntity> accept, boolean simulate, Set<Long> piles) {
        int given = 0;
        for (BlockEntity be : blockEntitiesAround(level, origin, radius)) {
            if (given >= amount) break;
            if (be.isRemoved() || !(be instanceof PulseHandler handler)) continue;
            if (be.getBlockPos().equals(origin) || !accept.test(be)) continue;
            if (PulseCairnBlockEntity.repeatsPile(be, piles)) continue;
            given += handler.insertPulse(amount - given, simulate);
        }
        return given;
    }
}
