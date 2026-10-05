package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity;
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

/**
 * Totem Lattice helpers: proximity scans, chalk links and voices, and the Pulse draw entry points, which go
 * through the conductor networks of {@link Weave}.
 */
public final class LatticeNetwork {
    public static final int DEFAULT_RADIUS = 8;
    public static final int LINK_RANGE = 16;

    /** The x, y, z order a walk of the cube met block entities in. Built once: every draw sorts by it, often twice. */
    static final java.util.Comparator<BlockEntity> CUBE_ORDER = java.util.Comparator
            .comparingInt((BlockEntity be) -> be.getBlockPos().getX())
            .thenComparingInt(be -> be.getBlockPos().getY())
            .thenComparingInt(be -> be.getBlockPos().getZ());

    private LatticeNetwork() {}

    /**
     * Counts every change that could reshape a chalk network anywhere: a totem arriving or leaving
     * (placed, broken, its chunk loaded or unloaded), or a totem's links being written. A conductor
     * keeps the network it walked while this number stays the same.
     */
    private static final java.util.concurrent.atomic.AtomicInteger CHALK_GENERATION = new java.util.concurrent.atomic.AtomicInteger();

    public static void chalkChanged() {
        CHALK_GENERATION.incrementAndGet();
    }

    public static int chalkGeneration() {
        return CHALK_GENERATION.get();
    }

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
    static <T extends BlockEntity> List<T> inBox(Level level, Class<T> type,
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

        /** A totem of {@code voice} stands here but its buffer is empty: it is off the lattice, or the lattice is dry. */
        public boolean silent(Attunement voice) {
            if (has(voice)) return false;
            for (ResonanceTotemBlockEntity totem : totems) if (totem.getAttunement() == voice) return true;
            return false;
        }
    }

    private static List<ResonanceTotemBlockEntity> voiceTotems(Level level, BlockPos origin,
                                                               List<ResonanceTotemBlockEntity> nearby, Attunement voice) {
        List<ResonanceTotemBlockEntity> found = new ArrayList<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (ResonanceTotemBlockEntity totem : nearby) {
            if (totem.getAttunement() == voice && totem.voiced() && seen.add(totem.getBlockPos())) found.add(totem);
            for (BlockPos linked : totem.getLinks()) {
                if (!linked.closerThan(origin, LINK_RANGE)) continue;
                BlockEntity be = level.hasChunkAt(linked) ? level.getBlockEntity(linked) : null;
                if (be instanceof ResonanceTotemBlockEntity linkedTotem && canLink(totem.getBlockPos(), linked)
                        && linkedTotem.isLinkedTo(totem.getBlockPos()) && linkedTotem.getAttunement() == voice
                        && linkedTotem.voiced() && seen.add(linkedTotem.getBlockPos())) {
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
            // An empty totem is silent: it lends no voice until its lattice fills it again. Its chalk links still
            // carry the voices of the totems at their other ends.
            if (totem.voiced()) set.add(totem.getAttunement());
            for (BlockPos linked : totem.getLinks()) {
                if (!linked.closerThan(origin, LINK_RANGE)) {
                    continue;
                }
                BlockEntity be = level.hasChunkAt(linked) ? level.getBlockEntity(linked) : null;
                if (be instanceof ResonanceTotemBlockEntity linkedTotem && canLink(totem.getBlockPos(), linked)
                        && linkedTotem.isLinkedTo(totem.getBlockPos()) && linkedTotem.voiced()) {
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
     * Draws up to {@code amount} for a consumer at {@code origin} from the Pulse lattice: the networks of the Lattice
     * Conductors within reach of it, their generators first and then their Pulse Cairns, limited by each conductor's
     * rate. Nothing is drawn straight from a generator any more: a machine with no conductor in reach starves even
     * beside a full one. See {@link Weave} for the rules.
     *
     * @param radius kept for callers; the lattice reach is always {@link #DEFAULT_RADIUS}
     * @return amount actually extracted
     */
    public static int extractPulseNearby(Level level, BlockPos origin, int radius, int amount) {
        return Weave.draw(level, origin, amount, false);
    }

    /**
     * @param simulate when true, probes available Pulse without draining
     */
    public static int extractPulseNearby(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        return Weave.draw(level, origin, amount, simulate);
    }

    /** All or nothing: takes {@code amount} from the lattice only when the whole of it can come. */
    public static boolean tryExtractPulseNearby(Level level, BlockPos origin, int radius, int amount) {
        return Weave.tryDraw(level, origin, amount);
    }

    /**
     * Pushes Pulse made at {@code origin} onto its lattice networks, cairns before generator buffers, for the
     * Lattice Converter.
     *
     * @return amount actually taken by the lattice
     */
    public static int insertPulseNearby(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        return Weave.insert(level, origin, amount, simulate);
    }

    /** A conductor joined or left {@code level}, or its redstone lock changed: the networks are woven again. */
    public static void conductorLinesChanged(Level level) {
        Weave.conductorsChanged();
    }

    /** Drops every cached network, for a level unload ({@code level} non-null) or a server stop (null). */
    public static void clearConductorLines(@org.jetbrains.annotations.Nullable Level level) {
        Weave.clear(level);
    }
}
