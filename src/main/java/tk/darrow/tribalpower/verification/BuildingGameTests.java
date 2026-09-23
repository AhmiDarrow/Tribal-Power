package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.building.BuildPattern;
import tk.darrow.tribalpower.building.BuildersChalkItem;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.pattern.BlockPredicate;
import tk.darrow.tribalpower.pattern.RitualPattern;

import java.util.HashSet;
import java.util.List;

/** The Builder's Chalk draws building guides, so its shapes have to be exactly what they claim to be. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class BuildingGameTests {

    /** A guide that repeats a position or strays past its own size would mislead whoever built to it. */
    @GameTest(template="empty")
    public static void everyPatternStaysInsideItsOwnSize(GameTestHelper h) {
        for (BuildPattern pattern : BuildPattern.values()) {
            if (pattern.rite() != null) continue; // a rite is a fixed footprint; its own test checks it
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

    /**
     * The rite shapes are the multiblocks themselves. The mark is the machine, air stays open, and an
     * unknown tier falls back to the first layout rather than inventing a third footprint.
     */
    @GameTest(template="empty")
    public static void riteLayoutsMatchTheStructuresTheyName(GameTestHelper h) {
        int rites = 0;
        for (BuildPattern pattern : BuildPattern.values()) {
            RitualPattern rite = pattern.rite();
            if (rite == null) continue;
            rites++;
            for (RitualPattern.Tier tier : rite.tiers()) {
                List<BlockPos> marks = pattern.offsets(tier.number());
                HashSet<BlockPos> expected = new HashSet<>();
                expected.add(BlockPos.ZERO);
                for (RitualPattern.Cell cell : tier.cells()) {
                    if (cell.predicate().trivial() || cell.predicate().role() == BlockPredicate.Role.AIR) continue;
                    Vec3i offset = cell.offset();
                    expected.add(new BlockPos(offset.getX(), offset.getY(), offset.getZ()));
                }
                HashSet<BlockPos> got = new HashSet<>(marks);
                h.assertTrue(got.size() == marks.size(), pattern + " tier " + tier.number() + " repeats a cell");
                if (!got.equals(expected)) {
                    BlockPos sample = null;
                    String which = "missing";
                    for (BlockPos p : expected) if (!got.contains(p)) { sample = p; break; }
                    if (sample == null) {
                        which = "extra";
                        for (BlockPos p : got) if (!expected.contains(p)) { sample = p; break; }
                    }
                    h.assertTrue(false, pattern + " tier " + tier.number() + " " + which + " " + sample
                            + " (drew " + got.size() + ", the rite wants " + expected.size() + ")");
                }
            }
        }
        h.assertTrue(rites == 7, "The chalk must carry every placement rite, found " + rites);
        List<BlockPos> pit = BuildPattern.LISTENING_PIT.offsets(1);
        h.assertTrue(pit.contains(BlockPos.ZERO), "The Listening Pit marks where the mesh stands");
        h.assertTrue(pit.contains(new BlockPos(0, -1, 0)), "The pit floor sits under the mesh");
        h.assertTrue(pit.contains(new BlockPos(2, 0, 2)), "Tier 1 braces the near corner");
        h.assertTrue(!pit.contains(new BlockPos(3, 0, 3)), "Tier 1 is the 5 by 5, not the deep pit");
        List<BlockPos> deep = BuildPattern.LISTENING_PIT.offsets(2);
        h.assertTrue(deep.contains(new BlockPos(3, 0, 0)), "Deep Listening seats a totem on the cardinal");
        h.assertTrue(deep.contains(new BlockPos(3, 0, 3)), "Deep Listening braces the outer corner");
        h.assertTrue(deep.contains(new BlockPos(0, -1, 3)), "Deep Listening floors the whole 7 by 7");
        h.assertTrue(!BuildPattern.LISTENING_PIT.offsets(99).contains(new BlockPos(3, 0, 3)),
                "An unknown tier falls back to the first layout");
        h.assertTrue(!BuildPattern.WAY_GATE.offsets(1).contains(new BlockPos(0, 1, 0)),
                "The inside of a Way Gate stays open");
        h.assertTrue(BuildPattern.WAY_GATE.offsets(1).contains(new BlockPos(-2, 1, 0)),
                "A Way Gate still draws its frame");
        h.assertTrue(BuildPattern.SHATTER_ARRAY.offsets(1).contains(new BlockPos(0, -1, 0)),
                "The Shatter Array marks the cache under the station");
        h.assertTrue(BuildPattern.VOICE_RING.offsets(1).contains(new BlockPos(3, 0, 0)),
                "The Voice Ring seats a totem three out on the cardinal");
        h.assertTrue(BuildPattern.turn(new BlockPos(-2, 1, 0), Rotation.CLOCKWISE_90).equals(new BlockPos(0, 1, -2)),
                "A clockwise turn swings the gate frame onto the west");
        h.assertTrue(BuildersChalkItem.facing(Direction.SOUTH) == Rotation.NONE, "Looking south keeps the written rite");
        h.assertTrue(BuildersChalkItem.facing(Direction.EAST) == Rotation.COUNTERCLOCKWISE_90,
                "Looking east turns the rite to face east");
        h.succeed();
    }
}
