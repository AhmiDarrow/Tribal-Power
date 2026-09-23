package tk.darrow.tribalpower.ley;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ResonanceTotemBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A resonance totem pulls the ley thread of its own voice through itself. Nothing is saved: the bend is
 * read from the totems that are standing in loaded chunks. Other colours are left on their own course,
 * so a ring of totems can steer the six threads across one collector.
 */
public final class LeyMagnets {
    /** How far a totem can reach to take hold of its thread. */
    public static final int RANGE = 48;
    /** How long the thread spends coming into the totem and leaving it, in blocks of travel. */
    public static final double SPAN = 36;
    private static final int MAX_PULLS = 4;

    private LeyMagnets() {}

    public record Magnet(BlockPos pos, Attunement voice) {}

    /** One totem's hold on a thread. {@code t} is where the thread passes through the totem. */
    public record Pull(double t, double back, double fore, double x, double y, double z) {}

    /** Totems within {@link #RANGE} of {@code center}, ignoring chunks that are not loaded. */
    public static List<Magnet> near(Level level, BlockPos center) {
        int minX = (center.getX() - RANGE) >> 4;
        int maxX = (center.getX() + RANGE) >> 4;
        int minZ = (center.getZ() - RANGE) >> 4;
        int maxZ = (center.getZ() + RANGE) >> 4;
        int reach = RANGE * RANGE;
        List<Magnet> out = new ArrayList<>();
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be.getBlockState().getBlock() instanceof ResonanceTotemBlock totem)) continue;
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(center) > reach) continue;
                    out.add(new Magnet(pos, totem.getAttunement()));
                }
            }
        }
        return out;
    }

    /** Holds this rope's voice has on it, nearest to the thread first, in order along the thread. */
    public static List<Pull> pulls(List<Magnet> magnets, LeyField.Rope rope) {
        if (magnets.isEmpty()) return List.of();
        Attunement voice = Attunement.values()[Math.floorMod(rope.voice(), Attunement.values().length)];
        List<Raw> raw = new ArrayList<>();
        for (Magnet magnet : magnets) {
            if (magnet.voice != voice) continue;
            Vec3 at = Vec3.atCenterOf(magnet.pos);
            double guess = travel(rope, at);
            if (guess < -SPAN || guess > LeyField.REACH + SPAN) continue;
            double bestT = clamp(guess, 0, LeyField.REACH);
            double best = LeyField.along(rope, bestT).distanceToSqr(at);
            for (double window = 20; window >= 1; window *= 0.5) {
                double stride = window / 4.0;
                for (double nudge = -window; nudge <= window; nudge += stride) {
                    double t = clamp(bestT + nudge, 0, LeyField.REACH);
                    double dist = LeyField.along(rope, t).distanceToSqr(at);
                    if (dist < best) {
                        best = dist;
                        bestT = t;
                    }
                }
            }
            if (best > (double) RANGE * RANGE) continue;
            Raw pull = new Raw();
            pull.t = bestT;
            pull.back = Math.min(SPAN, bestT);
            pull.fore = Math.min(SPAN, LeyField.REACH - bestT);
            pull.x = at.x;
            pull.y = at.y;
            pull.z = at.z;
            pull.gap = best;
            raw.add(pull);
        }
        if (raw.isEmpty()) return List.of();
        raw.sort(Comparator.comparingDouble(p -> p.gap));
        if (raw.size() > MAX_PULLS) raw.subList(MAX_PULLS, raw.size()).clear();
        raw.sort(Comparator.comparingDouble(p -> p.t));
        for (int i = 1; i < raw.size(); i++) {
            Raw left = raw.get(i - 1);
            Raw right = raw.get(i);
            double mid = (left.t + right.t) * 0.5;
            left.fore = Math.min(left.fore, mid - left.t);
            right.back = Math.min(right.back, right.t - mid);
        }
        List<Pull> out = new ArrayList<>(raw.size());
        for (Raw pull : raw) out.add(new Pull(pull.t, pull.back, pull.fore, pull.x, pull.y, pull.z));
        return List.copyOf(out);
    }

    /** The thread at {@code t}, swung through any totem whose window covers that travel. */
    public static Vec3 apply(LeyField.Rope rope, double t, List<Pull> pulls) {
        if (pulls.isEmpty()) return LeyField.along(rope, t);
        for (Pull pull : pulls) {
            double along = t - pull.t;
            if (along < -pull.back || along > pull.fore) continue;
            boolean incoming = along < 0;
            double span = incoming ? pull.back : pull.fore;
            if (span < 1.0e-3) return new Vec3(pull.x, pull.y, pull.z);
            double endT = incoming ? pull.t - span : pull.t + span;
            Vec3 end = LeyField.along(rope, endT);
            double s = Math.abs(along) / span;
            double w = s * s * (3 - 2 * s);
            return new Vec3(
                    pull.x + (end.x - pull.x) * w,
                    pull.y + (end.y - pull.y) * w,
                    pull.z + (end.z - pull.z) * w);
        }
        return LeyField.along(rope, t);
    }

    private static double travel(LeyField.Rope rope, Vec3 point) {
        double dx = point.x - rope.x();
        double dz = point.z - rope.z();
        return dx * Math.cos(rope.angle()) + dz * Math.sin(rope.angle());
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private static final class Raw {
        double t, back, fore, x, y, z, gap;
    }
}
