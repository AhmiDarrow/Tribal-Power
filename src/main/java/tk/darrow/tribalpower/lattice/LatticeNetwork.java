package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
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
import tk.darrow.tribalpower.echo.EchoStage;

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

    private LatticeNetwork() {}

    public static boolean canLink(BlockPos from, BlockPos to) {
        return !from.equals(to) && from.closerThan(to, LINK_RANGE);
    }

    public static boolean canRoute(Level level, BlockPos from, BlockPos to) {
        return canLink(from, to);
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

    public static int countNearbyTotems(Level level, BlockPos origin, int radius) {
        return findNearbyTotems(level, origin, radius).size();
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
            found.sort(java.util.Comparator.comparingInt((T be) -> be.getBlockPos().getX())
                    .thenComparingInt(be -> be.getBlockPos().getY())
                    .thenComparingInt(be -> be.getBlockPos().getZ()));
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
        List<ResonanceTotemBlockEntity> found = new ArrayList<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (ResonanceTotemBlockEntity totem : findNearbyTotems(level, origin, radius)) {
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
        return drainHandlers(level, origin, radius, amount, true, simulate);
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
        int remaining = amount - drainHandlers(level, origin, radius, amount, true, simulate);
        if (remaining > 0) {
            remaining -= drain(level, origin, radius, remaining, be -> be instanceof PulseCairnBlockEntity, simulate);
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

    private static int findProcessableSlot(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (EchoStage.isProcessable(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean insertIntoAny(List<? extends Container> containers, ItemStack stack) {
        for (Container container : containers) {
            if (container instanceof BlockEntity be && be.getLevel().hasNeighborSignal(be.getBlockPos())) continue;
            if (tryInsert(container, stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryInsert(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                slot.grow(1);
                container.setItem(i, slot);
                return true;
            }
        }
        return false;
    }

    /**
     * Attunements present via proximity or chalk links from nearby totems.
     */
    public static Set<Attunement> collectAttunements(Level level, BlockPos origin, int radius) {
        EnumSet<Attunement> set = EnumSet.noneOf(Attunement.class);
        for (ResonanceTotemBlockEntity totem : findNearbyTotems(level, origin, radius)) {
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
        for (tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity kinship : findNearbyKinshipTotems(level, origin, radius))
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
        int remaining = amount;
        remaining -= drainHandlers(level, origin, radius, remaining, true, simulate);
        if (remaining > 0) {
            remaining -= drainHandlers(level, origin, radius, remaining, false, simulate);
        }
        if (remaining > 0) {
            remaining -= extractThroughConductors(level, origin, radius, remaining, simulate);
        }
        return amount - remaining;
    }

    /** Conductors on the line a draw from {@code origin} would walk, including one standing at the origin. */
    public static int countConductorZone(Level level, BlockPos origin, int radius) {
        return conductorZone(level, origin, radius).size();
    }

    /**
     * Breadth-first across conductors. A redstone signal cuts that block out of the line, the same way
     * it pauses the conductor's own totem fill. The walk starts at every conductor already inside the
     * caller's cube, so a machine never has to be the block that begins the chain.
     */
    private static List<LatticeConductorBlockEntity> conductorZone(Level level, BlockPos origin, int radius) {
        ArrayDeque<LatticeConductorBlockEntity> queue = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (LatticeConductorBlockEntity seed : inBox(level, LatticeConductorBlockEntity.class,
                origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius)) {
            if (level.hasNeighborSignal(seed.getBlockPos())) continue;
            if (seen.add(seed.getBlockPos().immutable())) queue.add(seed);
        }
        List<LatticeConductorBlockEntity> zone = new ArrayList<>();
        while (!queue.isEmpty() && zone.size() < MAX_CONDUCTOR_CHAIN) {
            LatticeConductorBlockEntity current = queue.removeFirst();
            zone.add(current);
            if (zone.size() >= MAX_CONDUCTOR_CHAIN) break;
            BlockPos at = current.getBlockPos();
            for (LatticeConductorBlockEntity next : inBox(level, LatticeConductorBlockEntity.class,
                    at.getX() - radius, at.getY() - radius, at.getZ() - radius,
                    at.getX() + radius, at.getY() + radius, at.getZ() + radius)) {
                if (level.hasNeighborSignal(next.getBlockPos())) continue;
                if (seen.add(next.getBlockPos().immutable())) queue.add(next);
            }
        }
        return zone;
    }

    /**
     * Sources a conductor line may lend to a machine. Generators first, then cairns, then totem buffers.
     * Positions already inside the caller's own cube were drained by the local pass and must not be
     * counted twice. Station buffers stay where they are: the line moves camp Pulse, not a machine's claim.
     */
    private static int extractThroughConductors(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        List<LatticeConductorBlockEntity> zone = conductorZone(level, origin, radius);
        if (zone.isEmpty() || amount <= 0) return 0;
        HashSet<BlockPos> seen = new HashSet<>();
        List<BlockEntity> generators = new ArrayList<>();
        List<BlockEntity> cairns = new ArrayList<>();
        List<BlockEntity> totems = new ArrayList<>();
        for (LatticeConductorBlockEntity conductor : zone) {
            for (BlockEntity be : blockEntitiesAround(level, conductor.getBlockPos(), radius)) {
                BlockPos at = be.getBlockPos();
                if (inCube(origin, radius, at) || be.isRemoved() || !(be instanceof PulseHandler)) continue;
                if (!seen.add(at.immutable())) continue;
                if (isGenerator(be)) generators.add(be);
                else if (be instanceof PulseCairnBlockEntity) cairns.add(be);
                else if (be instanceof ResonanceTotemBlockEntity) totems.add(be);
            }
        }
        int taken = drainOrdered(generators, amount, simulate);
        if (taken < amount) taken += drainOrdered(cairns, amount - taken, simulate);
        if (taken < amount) taken += drainOrdered(totems, amount - taken, simulate);
        return taken;
    }

    private static boolean inCube(BlockPos origin, int radius, BlockPos at) {
        return Math.abs(at.getX() - origin.getX()) <= radius
                && Math.abs(at.getY() - origin.getY()) <= radius
                && Math.abs(at.getZ() - origin.getZ()) <= radius;
    }

    private static int drainOrdered(List<BlockEntity> found, int amount, boolean simulate) {
        if (amount <= 0 || found.isEmpty()) return 0;
        if (found.size() > 1)
            found.sort(java.util.Comparator.comparingInt((BlockEntity be) -> be.getBlockPos().getX())
                    .thenComparingInt(be -> be.getBlockPos().getY())
                    .thenComparingInt(be -> be.getBlockPos().getZ()));
        int taken = 0;
        for (BlockEntity be : found) {
            if (taken >= amount) break;
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
                                     boolean generatorsFirst, boolean simulate) {
        return drain(level, origin, radius, amount, be -> isGenerator(be) == generatorsFirst, simulate);
    }

    private static int drain(Level level, BlockPos origin, int radius, int amount,
                             Predicate<BlockEntity> accept, boolean simulate) {
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
            found.sort(java.util.Comparator.comparingInt((BlockEntity be) -> be.getBlockPos().getX())
                    .thenComparingInt(be -> be.getBlockPos().getY())
                    .thenComparingInt(be -> be.getBlockPos().getZ()));
        int taken = 0;
        for (BlockEntity be : found) {
            if (taken >= amount) break;
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
        remaining -= fill(level, origin, radius, remaining, be -> !isGenerator(be), simulate);
        if (remaining > 0) remaining -= fill(level, origin, radius, remaining, LatticeNetwork::isGenerator, simulate);
        return amount - remaining;
    }

    private static int fill(Level level, BlockPos origin, int radius, int amount,
                            Predicate<BlockEntity> accept, boolean simulate) {
        int given = 0;
        for (BlockEntity be : blockEntitiesAround(level, origin, radius)) {
            if (given >= amount) break;
            if (be.isRemoved() || !(be instanceof PulseHandler handler)) continue;
            if (be.getBlockPos().equals(origin) || !accept.test(be)) continue;
            given += handler.insertPulse(amount - given, simulate);
        }
        return given;
    }
}
