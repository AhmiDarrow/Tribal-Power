package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.building.BuildPattern;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.HashSet;
import java.util.List;

/** The Builder's Chalk draws building guides, so its shapes have to be exactly what they claim to be. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class BuildingGameTests {

    /** A guide that repeats a position or strays past its own size would mislead whoever built to it. */
    @GameTest(template="empty")
    public static void everyPatternStaysInsideItsOwnSize(GameTestHelper h) {
        for (BuildPattern pattern : BuildPattern.values())
            for (int size : BuildPattern.SIZES) {
                if (size > 24) continue;   // the whole ladder, up to where the solids get big; 128 has its own test
                List<BlockPos> marks = pattern.offsets(size);
                h.assertTrue(!marks.isEmpty(), pattern + " at " + size + " drew nothing");
                h.assertTrue(new HashSet<>(marks).size() == marks.size(), pattern + " at " + size + " repeats a position");
                for (BlockPos p : marks) {
                    h.assertTrue(Math.abs(p.getX()) <= size && Math.abs(p.getZ()) <= size,
                            pattern + " at " + size + " reaches out to " + p);
                    h.assertTrue(p.getY() >= 0 && p.getY() <= size + BuildPattern.WALL_HEIGHT,
                            pattern + " at " + size + " stands " + p.getY() + " high");
                }
            }
        h.succeed();
    }

    /** The flat outlines are the ones players count blocks against, so their counts are pinned. */
    @GameTest(template="empty")
    public static void flatOutlinesHaveTheCountsTheyLookLike(GameTestHelper h) {
        for (int r : BuildPattern.SIZES) {
            h.assertTrue(BuildPattern.SQUARE.offsets(r).size() == 8 * r,
                    "A square of " + r + " is a perimeter of " + (8 * r) + ", drew " + BuildPattern.SQUARE.offsets(r).size());
            h.assertTrue(BuildPattern.CROSS.offsets(r).size() == 4 * r + 1,
                    "A cross of " + r + " is " + (4 * r + 1) + " marks, drew " + BuildPattern.CROSS.offsets(r).size());
        }
        h.succeed();
    }

    /**
     * The whole point of the size ladder: a builder who wants a circle a couple of hundred blocks across
     * gets one, and it is still a closed ring one block thick rather than a scatter of marks.
     */
    @GameTest(template="empty", timeoutTicks=200)
    public static void theLargestCircleIsWholeAndOneBlockThick(GameTestHelper h) {
        int r = BuildPattern.MAX_SIZE;
        List<BlockPos> marks = BuildPattern.CIRCLE.offsets(r);
        h.assertTrue(r == 128, "The ladder must reach 128, stops at " + r);
        h.assertTrue(marks.size() > 6 * r, "A circle of " + r + " is a long ring, drew only " + marks.size());
        HashSet<BlockPos> set = new HashSet<>(marks);
        for (BlockPos p : marks) {
            h.assertTrue(p.getY() == 0, "A circle is flat, found " + p);
            h.assertTrue((int) Math.round(Math.sqrt(p.getX() * p.getX() + p.getZ() * p.getZ())) == r,
                    "A circle of " + r + " must be one block thick, found " + p);
        }
        // Closed: every mark touches another, so there is a line to build along rather than gaps.
        for (BlockPos p : marks) {
            boolean joined = false;
            for (int dx = -1; dx <= 1 && !joined; dx++)
                for (int dz = -1; dz <= 1 && !joined; dz++)
                    if ((dx != 0 || dz != 0) && set.contains(new BlockPos(p.getX() + dx, 0, p.getZ() + dz))) joined = true;
            h.assertTrue(joined, "A circle of " + r + " must be unbroken; " + p + " stands alone");
        }
        for (BlockPos cardinal : List.of(new BlockPos(r, 0, 0), new BlockPos(-r, 0, 0),
                new BlockPos(0, 0, r), new BlockPos(0, 0, -r)))
            h.assertTrue(set.contains(cardinal), "A circle of " + r + " must pass through " + cardinal);
        h.succeed();
    }

    /** The solids are the expensive ones; at the top of the ladder they must still be built and bounded. */
    @GameTest(template="empty", timeoutTicks=400)
    public static void theSolidsSurviveTheTopOfTheLadder(GameTestHelper h) {
        for (BuildPattern pattern : List.of(BuildPattern.DOME, BuildPattern.HUT,
                BuildPattern.WALLS, BuildPattern.LONGHOUSE)) {
            int r = BuildPattern.MAX_SIZE;
            List<BlockPos> marks = pattern.offsets(r);
            h.assertTrue(!marks.isEmpty(), pattern + " at " + r + " drew nothing");
            for (BlockPos p : marks)
                h.assertTrue(Math.abs(p.getX()) <= r && Math.abs(p.getZ()) <= r
                                && p.getY() >= 0 && p.getY() <= r + BuildPattern.WALL_HEIGHT,
                        pattern + " at " + r + " strays to " + p);
        }
        h.succeed();
    }

    /** A hut you cannot walk into is a box. The doorway is cut on the +Z face and left open two high. */
    @GameTest(template="empty")
    public static void theHutHasADoorwayYouCanWalkThrough(GameTestHelper h) {
        for (int r = 3; r <= 8; r++) {
            List<BlockPos> marks = BuildPattern.HUT.offsets(r);
            h.assertTrue(!marks.contains(new BlockPos(0, 0, r)), "Hut of " + r + " has no doorway at foot height");
            h.assertTrue(!marks.contains(new BlockPos(0, 1, r)), "Hut of " + r + " has no doorway at head height");
            h.assertTrue(marks.contains(new BlockPos(0, 2, r)), "Hut of " + r + " must keep the lintel over its door");
            h.assertTrue(marks.contains(new BlockPos(0, BuildPattern.WALL_HEIGHT + r, 0)),
                    "Hut of " + r + " must close its roof at the apex");
        }
        h.succeed();
    }

    /** Lattice camp is a claim about the game's own rules, so it has to match the zone the scans use. */
    @GameTest(template="empty")
    public static void latticeCampMarksTheRealZoneAndItsTotemSpots(GameTestHelper h) {
        int r = LatticeNetwork.DEFAULT_RADIUS;
        List<BlockPos> marks = BuildPattern.LATTICE_CAMP.offsets(r);
        for (BlockPos spot : List.of(new BlockPos(r, 1, 0), new BlockPos(-r, 1, 0),
                new BlockPos(0, 1, r), new BlockPos(0, 1, -r)))
            h.assertTrue(marks.contains(spot), "Lattice camp must mark the totem spot at " + spot);
        h.assertTrue(marks.contains(new BlockPos(r, 0, r)),
                "The zone is a cube, so its corner at " + r + "," + r + " is in range and must be drawn");
        h.assertTrue(marks.contains(new BlockPos(0, 0, 0)), "Lattice camp must mark its own centre");
        h.succeed();
    }

    /** The ladder has to climb, reach the top, and wrap cleanly at both ends. */
    @GameTest(template="empty")
    public static void theSizeLadderClimbsAndWraps(GameTestHelper h) {
        int[] sizes = BuildPattern.SIZES;
        h.assertTrue(sizes[0] == BuildPattern.MIN_SIZE && sizes[sizes.length - 1] == BuildPattern.MAX_SIZE,
                "The ladder must run the whole range, " + sizes[0] + " to " + sizes[sizes.length - 1]);
        for (int i = 1; i < sizes.length; i++)
            h.assertTrue(sizes[i] > sizes[i - 1], "The ladder must climb, " + sizes[i - 1] + " then " + sizes[i]);
        int at = BuildPattern.MIN_SIZE;
        for (int i = 1; i < sizes.length; i++) {
            at = BuildPattern.nextSize(at);
            h.assertTrue(at == sizes[i], "Stepping up must land on " + sizes[i] + ", landed on " + at);
        }
        h.assertTrue(BuildPattern.nextSize(BuildPattern.MAX_SIZE) == BuildPattern.MIN_SIZE, "Past the top must wrap round");
        h.assertTrue(BuildPattern.previousSize(BuildPattern.MIN_SIZE) == BuildPattern.MAX_SIZE, "Below the bottom must wrap round");
        h.assertTrue(BuildPattern.previousSize(BuildPattern.MAX_SIZE) == sizes[sizes.length - 2], "Stepping down must land on the rung below");
        h.assertTrue(BuildPattern.clampSize(-40) == BuildPattern.MIN_SIZE, "Under-size must clamp up");
        h.assertTrue(BuildPattern.clampSize(9999) == BuildPattern.MAX_SIZE, "Over-size must clamp down");
        h.assertTrue(BuildPattern.byIndex(-1) == BuildPattern.values()[BuildPattern.values().length - 1],
                "Cycling back from the first shape must wrap to the last");
        h.assertTrue(BuildPattern.byIndex(BuildPattern.values().length) == BuildPattern.SQUARE,
                "Cycling past the last shape must wrap to the first");
        h.succeed();
    }
}
