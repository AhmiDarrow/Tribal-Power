package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Totem Lattice helpers — proximity scans, chalk links, and Pulse draw from nearby sources.
 */
public final class LatticeNetwork {
    public static final int DEFAULT_RADIUS = 8;
    public static final int LINK_RANGE = 16;

    private LatticeNetwork() {}

    public static boolean canLink(BlockPos from, BlockPos to) {
        return from.closerThan(to, LINK_RANGE);
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
        if (!canLink(a, b)) {
            return false;
        }
        if (from.isLinkedTo(b) || to.isLinkedTo(a)) {
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
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof ResonanceTotemBlockEntity totem) {
                        found.add(totem);
                    }
                }
            }
        }
        return found;
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
                BlockEntity be = level.getBlockEntity(linked);
                if (be instanceof ResonanceTotemBlockEntity linkedTotem) {
                    set.add(linkedTotem.getAttunement());
                }
            }
        }
        return set;
    }

    public static boolean hasAttunement(Level level, BlockPos origin, int radius, Attunement needed) {
        return collectAttunements(level, origin, radius).contains(needed);
    }

    /**
     * Pull Pulse from nearby Drumhearts, Ley Collectors, then Totems.
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
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof PulseHandler handler)) {
                        continue;
                    }
                    boolean isGenerator = be instanceof DrumheartBlockEntity || be instanceof LeyCollectorBlockEntity;
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
