package tk.darrow.tribalpower.building;

import net.minecraft.core.BlockPos;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The shapes a Builder's Chalk can throw on the ground (design: building aids, 3.8).
 *
 * <p>Pure geometry, offsets from the mark, so the shapes can be checked without a world. Every pattern
 * returns each position once and in a settled order, which is what lets the painter sample a big shape
 * evenly instead of drawing one dense corner of it.
 *
 * <p>{@code size} is a half-extent: a square of size 5 is eleven blocks across, and the largest circle
 * is {@value #MAX_SIZE} out, {@code 257} across. Sizes that big are the reason the round shapes are
 * walked band by band rather than by testing every position of the box they sit in — a size-128 dome
 * tested that way is eight and a half million checks to find a hundred thousand marks.
 *
 * <p>{@link #LATTICE_CAMP} is the odd one out and means something in the game's own terms: at size
 * {@value LatticeNetwork#DEFAULT_RADIUS} its square is exactly the cube a machine draws from, because
 * every Pulse scan tests each axis on its own.
 */
public enum BuildPattern {
    SQUARE,
    CIRCLE,
    CROSS,
    HEXAGON,
    WALLS,
    DOME,
    HUT,
    LONGHOUSE,
    LATTICE_CAMP;

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 128;
    /** How tall a wall the room patterns stand up. */
    public static final int WALL_HEIGHT = 3;

    /**
     * The sizes a step of the chalk lands on. Stepping one block at a time would be 127 flicks to reach
     * the far end, so it opens out as it grows: fine control where a hut is built, coarse where a wall
     * is a hundred blocks long.
     */
    public static final int[] SIZES = {2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128};

    public static BuildPattern byIndex(int index) {
        BuildPattern[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    public int index() {
        return ordinal();
    }

    public static int clampSize(int size) {
        return Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    /** The next size up the ladder, wrapping back to the smallest past the top. */
    public static int nextSize(int current) {
        for (int size : SIZES) if (size > current) return size;
        return SIZES[0];
    }

    /** The next size down the ladder, wrapping to the largest below the bottom. */
    public static int previousSize(int current) {
        for (int i = SIZES.length - 1; i >= 0; i--) if (SIZES[i] < current) return SIZES[i];
        return SIZES[SIZES.length - 1];
    }

    /** The translation key for this pattern's name. */
    public String key() {
        return "gui.tribalpower.chalk.shape." + name().toLowerCase(Locale.ROOT);
    }

    /** Offsets from the mark, deduplicated, ordered low to high so a sampled subset still reads as the shape. */
    public List<BlockPos> offsets(int size) {
        int r = clampSize(size);
        Set<BlockPos> out = new LinkedHashSet<>();
        switch (this) {
            case SQUARE -> square(out, r, 0);
            case CIRCLE -> circle(out, r, 0);
            case CROSS -> {
                for (int d = -r; d <= r; d++) {
                    out.add(new BlockPos(d, 0, 0));
                    out.add(new BlockPos(0, 0, d));
                }
            }
            case HEXAGON -> hexagon(out, r);
            case WALLS -> {
                for (int y = 0; y < WALL_HEIGHT; y++) square(out, r, y);
            }
            case DOME -> dome(out, r);
            case HUT -> hut(out, r);
            case LONGHOUSE -> longhouse(out, r);
            case LATTICE_CAMP -> latticeCamp(out, r);
        }
        List<BlockPos> list = new ArrayList<>(out);
        list.sort(Comparator.comparingInt((BlockPos p) -> p.getY())
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        return List.copyOf(list);
    }

    // ---- shapes ------------------------------------------------------------------------------------

    private static void square(Set<BlockPos> out, int r, int y) {
        for (int d = -r; d <= r; d++) {
            out.add(new BlockPos(d, y, -r));
            out.add(new BlockPos(d, y, r));
            out.add(new BlockPos(-r, y, d));
            out.add(new BlockPos(r, y, d));
        }
    }

    /** A one-block-thick circle: the positions whose distance from the mark rounds to the radius. */
    private static void circle(Set<BlockPos> out, int r, int y) {
        band(out, y, sq(r - 0.5), sq(r + 0.5), r);
    }

    /** Shell of a hemisphere, so it reads as a roof rather than a solid lump. */
    private static void dome(Set<BlockPos> out, int r) {
        for (int dy = 0; dy <= r; dy++) band(out, dy, sq(r - 0.5) - sq(dy), sq(r + 0.5) - sq(dy), r);
    }

    /**
     * One horizontal slice of a shell: the positions whose squared distance falls in {@code [inner, outer)}.
     * Each column of the slice is a contiguous run, so this costs the marks it finds rather than the
     * square it searches — which is what makes a size-128 dome affordable at all.
     */
    private static void band(Set<BlockPos> out, int y, double inner, double outer, int span) {
        for (int dx = -span; dx <= span; dx++) {
            double reach = outer - sq(dx);
            if (reach <= 0) continue;
            int hi = (int) Math.floor(Math.sqrt(Math.nextDown(reach)));
            double floor = inner - sq(dx);
            int lo = floor <= 0 ? 0 : (int) Math.ceil(Math.sqrt(floor));
            for (int dz = lo; dz <= hi; dz++) {
                out.add(new BlockPos(dx, y, dz));
                if (dz != 0) out.add(new BlockPos(dx, y, -dz));
            }
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static void hexagon(Set<BlockPos> out, int r) {
        int[] xs = new int[6];
        int[] zs = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            xs[i] = (int) Math.round(Math.cos(a) * r);
            zs[i] = (int) Math.round(Math.sin(a) * r);
        }
        for (int i = 0; i < 6; i++) line(out, xs[i], 0, zs[i], xs[(i + 1) % 6], zs[(i + 1) % 6]);
    }

    /** Four walls with a doorway on the +Z face, then a pyramid roof stepping in a block a course. */
    private static void hut(Set<BlockPos> out, int r) {
        for (int y = 0; y < WALL_HEIGHT; y++) square(out, r, y);
        for (int y = 0; y < WALL_HEIGHT - 1; y++) out.remove(new BlockPos(0, y, r));
        for (int step = 0; r - step >= 0; step++) square(out, r - step, WALL_HEIGHT + step);
    }

    /** A hall twice as long as it is wide: walls, a ridge beam, and the two gable slopes under it. */
    private static void longhouse(Set<BlockPos> out, int r) {
        int half = Math.max(1, r / 2);
        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int dx = -r; dx <= r; dx++) {
                out.add(new BlockPos(dx, y, -half));
                out.add(new BlockPos(dx, y, half));
            }
            for (int dz = -half; dz <= half; dz++) {
                out.add(new BlockPos(-r, y, dz));
                out.add(new BlockPos(r, y, dz));
            }
        }
        int ridge = WALL_HEIGHT + half;
        for (int dx = -r; dx <= r; dx++) out.add(new BlockPos(dx, ridge, 0));
        for (int step = 1; step <= half; step++) {
            int y = ridge - step;
            out.add(new BlockPos(-r, y, -step));
            out.add(new BlockPos(-r, y, step));
            out.add(new BlockPos(r, y, -step));
            out.add(new BlockPos(r, y, step));
        }
    }

    /**
     * The shape the mod itself cares about: the square a machine at the mark actually reaches, with the
     * four spots a Resonance Totem would stand on to carry that reach outward.
     */
    private static void latticeCamp(Set<BlockPos> out, int r) {
        square(out, r, 0);
        out.add(new BlockPos(0, 0, 0));
        out.add(new BlockPos(r, 1, 0));
        out.add(new BlockPos(-r, 1, 0));
        out.add(new BlockPos(0, 1, r));
        out.add(new BlockPos(0, 1, -r));
    }

    private static void line(Set<BlockPos> out, int x1, int y, int z1, int x2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            out.add(new BlockPos((int) Math.round(x1 + (x2 - x1) * t), y, (int) Math.round(z1 + (z2 - z1) * t)));
        }
    }
}
