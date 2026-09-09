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
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
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

/**
 * Totem Lattice helpers — proximity scans, chalk links, Conductor routing, and Pulse draw.
 */
public final class LatticeNetwork {
    public static final int DEFAULT_RADIUS = 8;
    public static final int LINK_RANGE = 16;

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

    public static List<ResonanceTotemBlockEntity> findNearbyTotems(Level level, BlockPos origin, int radius) {
        List<ResonanceTotemBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.hasChunkAt(cursor) ? level.getBlockEntity(cursor) : null;
                    if (be instanceof ResonanceTotemBlockEntity totem) {
                        found.add(totem);
                    }
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
        Set<BlockPos> seen = new HashSet<>();
        List<SongBenchBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos hub : hubs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        cursor.set(hub.getX() + dx, hub.getY() + dy, hub.getZ() + dz);
                        BlockPos key = cursor.immutable();
                        if (!seen.add(key)) {
                            continue;
                        }
                        BlockEntity be = level.hasChunkAt(key) ? level.getBlockEntity(key) : null;
                        if (be instanceof SongBenchBlockEntity bench) {
                            found.add(bench);
                        }
                    }
                }
            }
        }
        return found;
    }

    public static List<AncestralCacheBlockEntity> findCachesNearHubs(Level level, Collection<BlockPos> hubs, int radius) {
        Set<BlockPos> seen = new HashSet<>();
        List<AncestralCacheBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos hub : hubs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        cursor.set(hub.getX() + dx, hub.getY() + dy, hub.getZ() + dz);
                        BlockPos key = cursor.immutable();
                        if (!seen.add(key)) {
                            continue;
                        }
                        BlockEntity be = level.hasChunkAt(key) ? level.getBlockEntity(key) : null;
                        if (be instanceof AncestralCacheBlockEntity cache) {
                            found.add(cache);
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Pull Pulse only from Drumhearts / Ley Collectors / Pulse Resonators near {@code origin}.
     */
    public static int extractPulseFromGenerators(Level level, BlockPos origin, int radius, int amount, boolean simulate) {
        return drainHandlers(level, origin, radius, amount, true, simulate);
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
     * Move one Echo-stage item along the lattice: finished grit into caches,
     * processable grit from caches into empty benches, or bench-to-bench handoff.
     * @return true if an item moved
     */
    public static boolean routeEchoItems(Level level, List<SongBenchBlockEntity> benches,
                                         List<AncestralCacheBlockEntity> caches) {
        // Finished products: Song Bench → Ancestral Cache
        for (SongBenchBlockEntity bench : benches) {
            ItemStack stack = bench.getItem(SongBenchBlockEntity.SLOT);
            if (stack.isEmpty() || EchoStage.isProcessable(stack) || bench.isSinging()) {
                continue;
            }
            if (level.hasNeighborSignal(bench.getBlockPos())) continue;
            if (insertIntoAny(caches, stack.copyWithCount(1))) {
                bench.removeItem(SongBenchBlockEntity.SLOT, 1);
                return true;
            }
        }

        // Feed: Ancestral Cache → empty Song Bench
        for (AncestralCacheBlockEntity cache : caches) {
            if (level.hasNeighborSignal(cache.getBlockPos())) continue;
            int slot = findProcessableSlot(cache);
            if (slot < 0) {
                continue;
            }
            ItemStack feed = cache.getItem(slot).copyWithCount(1);
            for (SongBenchBlockEntity bench : benches) {
                if (level.hasNeighborSignal(bench.getBlockPos())) continue;
                if (bench.insertItem(feed.copy())) {
                    cache.removeItem(slot, 1);
                    bench.startSong();
                    return true;
                }
            }
        }

        // Bench → bench handoff of idle processable grit
        for (int i = 0; i < benches.size(); i++) {
            SongBenchBlockEntity from = benches.get(i);
            if (level.hasNeighborSignal(from.getBlockPos())) continue;
            ItemStack stack = from.getItem(SongBenchBlockEntity.SLOT);
            if (stack.isEmpty() || !EchoStage.isProcessable(stack) || from.isSinging()) {
                continue;
            }
            for (int j = 0; j < benches.size(); j++) {
                if (i == j) {
                    continue;
                }
                SongBenchBlockEntity to = benches.get(j);
                if (level.hasNeighborSignal(to.getBlockPos())) continue;
                if (!to.isEmpty()) {
                    continue;
                }
                ItemStack moved = stack.copyWithCount(1);
                if (to.insertItem(moved)) {
                    from.removeItem(SongBenchBlockEntity.SLOT, 1);
                    to.startSong();
                    return true;
                }
            }
        }
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
        List<tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.hasChunkAt(cursor) ? level.getBlockEntity(cursor) : null;
                    if (be instanceof tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity kinship) found.add(kinship);
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
     * Pull Pulse from nearby Drumhearts, Ley Collectors, Pulse Resonators, then Totems.
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
        return amount - remaining;
    }

    private static int drainHandlers(Level level, BlockPos origin, int radius, int amount,
                                     boolean generatorsFirst, boolean simulate) {
        int taken = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius && taken < amount; dx++) {
            for (int dy = -radius; dy <= radius && taken < amount; dy++) {
                for (int dz = -radius; dz <= radius && taken < amount; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.hasChunkAt(cursor) ? level.getBlockEntity(cursor) : null;
                    if (!(be instanceof PulseHandler handler)) {
                        continue;
                    }
                    boolean isGenerator = be instanceof DrumheartBlockEntity
                            || be instanceof LeyCollectorBlockEntity
                            || be instanceof PulseResonatorBlockEntity;
                    if (generatorsFirst != isGenerator) {
                        continue;
                    }
                    int got = handler.extractPulse(amount - taken, simulate);
                    taken += got;
                }
            }
        }
        return taken;
    }
}
