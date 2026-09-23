package tk.darrow.tribalpower.ley;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The planet's ley lines. Nothing is placed and nothing is saved: every dimension has the same kind of
 * field the moment Tribal Power is loaded, folded from the world seed and the dimension id.
 *
 * <p>A line is a slow sine woven through space, not a row of blocks. A collector feels a vein only while
 * it stands inside the vein's soft tube. Six veins meeting is the rarest roll of a nexus, and a point
 * never counts more than those six even where neighbouring veins crowd in.
 *
 * <p>The lens draws only the stretch of each vein near the player who is holding it, and only for that
 * player. A camp with nobody looking pays nothing.
 */
public final class LeyField {
    public static final int MAX_LINES = 6;
    /** Blocks between nexus cells. Veins travel well past one cell, so the field reads as long rivers. */
    public static final int CELL = 896;
    /** How far a vein runs from the meeting that births it. */
    public static final int REACH = 1400;
    /** Soft radius, in blocks, inside which a collector is touching a vein. */
    public static final double TUBE = 7.5;
    /** How far the lens bothers to draw a vein. */
    public static final double SIGHT = 42;
    public static final int PER_LINE = 2;

    private LeyField() {}

    /**
     * One vein near a viewer: the curve's origin and bends, and how far along it the viewer is standing.
     * The client draws the rope from this and scrolls the braid itself, so the flow is every frame.
     */
    public record Rope(double x, double y, double z, double angle, double amp, double freq, double phase,
                       double yAmp, double yFreq, double travel) {}

    /** Veins within sight of {@code pos}, nearest first, at most six. */
    public static List<Rope> ropes(ServerLevel level, BlockPos pos) {
        List<Hit> hits = gather(level, pos, SIGHT);
        if (hits.isEmpty()) return List.of();
        hits.sort(Comparator.comparingDouble(Hit::dist));
        if (hits.size() > MAX_LINES) hits = hits.subList(0, MAX_LINES);
        List<Rope> out = new ArrayList<>();
        for (Hit hit : hits) {
            if (hit.dist > SIGHT) continue;
            Vein vein = hit.vein;
            out.add(new Rope(vein.origin.x, vein.origin.y, vein.origin.z, vein.angle, vein.amp, vein.freq,
                    vein.phase, vein.yAmp, vein.yFreq, hit.travel));
        }
        return out;
    }

    /** A point on a rope's centre. {@code t} is blocks of travel from the meeting. */
    public static Vec3 along(Rope rope, double t) {
        return curve(rope.x, rope.y, rope.z, rope.angle, rope.amp, rope.freq, rope.phase, rope.yAmp, rope.yFreq, t);
    }

    public record Reading(int lines, int points, int voices) {
        public static final Reading QUIET = new Reading(0, 0, 0);

        public boolean heard(Attunement voice) {
            return (voices & (1 << voice.ordinal())) != 0;
        }
    }

    /** World seed folded with the dimension, so the Nether is not a copy of the overworld. */
    public static long salt(ServerLevel level) {
        long fold = 0x9E3779B97F4A7C15L;
        String id = level.dimension().location().toString();
        for (int i = 0; i < id.length(); i++) fold = fold * 31 + id.charAt(i);
        return level.getSeed() ^ fold;
    }

    /** 0 when this cell has no meeting, otherwise 1 to 6 veins leave from it. */
    public static int nexusRank(long salt, int cellX, int cellZ) {
        long h = mix(salt, cellX, cellZ);
        if ((h & 255) >= 18) return 0;
        int roll = (int) ((h >>> 8) & 255);
        if (roll < 140) return 1;
        if (roll < 200) return 2;
        if (roll < 230) return 3;
        if (roll < 248) return 4;
        if (roll < 254) return 5;
        return MAX_LINES;
    }

    /** Block at the heart of a cell's meeting. Y rides the dimension's sea level, not a fixed layer. */
    public static BlockPos nexusPos(ServerLevel level, int cellX, int cellZ) {
        long h = mix(salt(level), cellX, cellZ);
        int x = cellX * CELL + CELL / 2 + (int) ((h >>> 16) & 511) - 256;
        int z = cellZ * CELL + CELL / 2 + (int) ((h >>> 25) & 511) - 256;
        int y = level.getSeaLevel() + (int) ((h >>> 40) & 15) - 8;
        return new BlockPos(x, y, z);
    }

    /** Veins whose tubes contain {@code pos}, nearest first, never more than six. */
    public static Reading sample(ServerLevel level, BlockPos pos) {
        List<Hit> hits = gather(level, pos, TUBE);
        if (hits.isEmpty()) return Reading.QUIET;
        hits.sort(Comparator.comparingDouble(Hit::dist));
        if (hits.size() > MAX_LINES) hits = hits.subList(0, MAX_LINES);
        int voices = 0;
        double points = 0;
        int lines = 0;
        for (Hit hit : hits) {
            if (hit.dist > TUBE) continue;
            lines++;
            voices |= 1 << hit.voice.ordinal();
            double u = 1 - hit.dist / TUBE;
            double smooth = u * u * (3 - 2 * u);
            points += smooth * PER_LINE;
        }
        return new Reading(lines, (int) Math.round(points), voices);
    }

    private static List<Hit> gather(ServerLevel level, BlockPos pos, double reach) {
        long salt = salt(level);
        int minX = Math.floorDiv(pos.getX() - REACH, CELL);
        int maxX = Math.floorDiv(pos.getX() + REACH, CELL);
        int minZ = Math.floorDiv(pos.getZ() - REACH, CELL);
        int maxZ = Math.floorDiv(pos.getZ() + REACH, CELL);
        Vec3 point = Vec3.atCenterOf(pos);
        List<Hit> hits = new ArrayList<>(4);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                int rank = nexusRank(salt, cx, cz);
                if (rank == 0) continue;
                long h = mix(salt, cx, cz);
                BlockPos heart = nexusPos(level, cx, cz);
                Vec3 origin = Vec3.atCenterOf(heart);
                int spin = (int) ((h >>> 48) & 7);
                double turn = ((h >>> 12) & 1023) / 1024.0 * 0.5;
                for (int i = 0; i < rank; i++) {
                    Attunement voice = Attunement.values()[(i + spin) % Attunement.values().length];
                    double angle = (i * Math.PI * 2 / rank) + turn;
                    long vh = mix(h, i, voice.ordinal());
                    Vein vein = new Vein(origin, angle,
                            6 + (vh & 3),
                            0.034 + (vh & 7) * 0.0045,
                            ((vh >>> 4) & 1023) / 1023.0 * Math.PI * 2,
                            4 + ((vh >>> 14) & 3),
                            0.041 + ((vh >>> 18) & 7) * 0.005);
                    double travel = vein.travel(point);
                    if (travel < -24 || travel > REACH + 24) continue;
                    double bestT = clamp(travel, 0, REACH);
                    double best = vein.at(bestT).distanceTo(point);
                    // The worm bends enough that the straight projection can sit a short way off.
                    if (best > reach + vein.amp * 1.6 + vein.yAmp * 1.5) continue;
                    for (double span = 24; span >= 3; span *= 0.5) {
                        double stride = span / 4.0;
                        for (double nudge = -span; nudge <= span; nudge += stride) {
                            double t = clamp(bestT + nudge, 0, REACH);
                            double d = vein.at(t).distanceTo(point);
                            if (d < best) { best = d; bestT = t; }
                        }
                    }
                    if (best <= reach) hits.add(new Hit(vein, voice, best, bestT));
                }
            }
        }
        return hits;
    }

    private static Vec3 curve(double ox, double oy, double oz, double angle, double amp, double freq, double phase,
                              double yAmp, double yFreq, double t) {
        // Subtract the value at t = 0 so every vein of a meeting passes through the same heart.
        double lateral = amp * (Math.sin(t * freq + phase) - Math.sin(phase))
                + amp * 0.38 * (Math.sin(t * freq * 2.2 + phase * 1.6) - Math.sin(phase * 1.6));
        double lift = yAmp * (Math.sin(t * yFreq + phase * 0.7) - Math.sin(phase * 0.7))
                + yAmp * 0.32 * (Math.sin(t * yFreq * 1.9 + phase) - Math.sin(phase));
        double sx = -Math.sin(angle);
        double sz = Math.cos(angle);
        return new Vec3(
                ox + Math.cos(angle) * t + sx * lateral,
                oy + lift,
                oz + Math.sin(angle) * t + sz * lateral);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private static long mix(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    private record Vein(Vec3 origin, double angle, double amp, double freq, double phase, double yAmp, double yFreq) {
        Vec3 at(double t) {
            return curve(origin.x, origin.y, origin.z, angle, amp, freq, phase, yAmp, yFreq, t);
        }

        /** Travel of the nearest point on the straight bearing. The sine is checked around it. */
        double travel(Vec3 point) {
            double dx = point.x - origin.x;
            double dz = point.z - origin.z;
            return dx * Math.cos(angle) + dz * Math.sin(angle);
        }
    }

    private record Hit(Vein vein, Attunement voice, double dist, double travel) {}
}
