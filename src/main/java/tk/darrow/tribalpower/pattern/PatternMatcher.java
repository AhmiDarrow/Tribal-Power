package tk.darrow.tribalpower.pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a {@link RitualPattern} against the world in all four rotations (design 3.1 section 4).
 *
 * <p>A failed match still returns the closest tier and rotation it found, so diagnostics can say what
 * is missing instead of only that something is. Reads stay inside the pattern's own bounding box and
 * treat an unloaded position as a miss rather than loading a chunk.
 */
public final class PatternMatcher {
    private static final Rotation[] ROTATIONS = Rotation.values();

    private PatternMatcher() {}

    /** One unmet cell: where it was and what was wanted there. */
    public record Miss(BlockPos pos, Component expected) {
        public Component describe() {
            return Component.translatable("pattern.tribalpower.miss", expected, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /**
     * The outcome of a match. When {@link #found()} is false this is the best partial attempt:
     * {@code tier} and {@code rotation} are the ones that came closest.
     */
    public record Match(RitualPattern pattern, boolean found, int tier, Rotation rotation, List<Miss> misses) {
        public static Match none(RitualPattern pattern) {
            return new Match(pattern, false, 0, Rotation.NONE, List.of());
        }

        /** World position of a cell of the matched tier, honouring the rotation that matched. */
        public BlockPos cell(RitualPattern.Cell cell, BlockPos anchor) {
            return cell.at(anchor, rotation);
        }

        /** Every position this match occupies, anchor included. Used for ghosts and for camp checks. */
        public List<BlockPos> positions(BlockPos anchor) {
            RitualPattern.Tier shape = pattern.tier(tier);
            if (shape == null) return List.of(anchor);
            List<BlockPos> all = new ArrayList<>(shape.cells().size() + 1);
            all.add(anchor);
            for (RitualPattern.Cell cell : shape.cells()) {
                if (!cell.predicate().trivial()) all.add(cell.at(anchor, rotation));
            }
            return all;
        }

        /** The first {@code limit} misses as chat lines, in the order the pattern reads. */
        public List<Component> report(int limit) {
            List<Component> lines = new ArrayList<>();
            if (found()) {
                lines.add(Component.translatable("pattern.tribalpower.complete", pattern.displayName(), tier,
                        Component.translatable("pattern.tribalpower.rotation." + rotation.getSerializedName()))
                        .withStyle(ChatFormatting.GREEN));
                return lines;
            }
            lines.add(Component.translatable("pattern.tribalpower.incomplete", pattern.displayName(), tier, misses.size())
                    .withStyle(ChatFormatting.YELLOW));
            for (int i = 0; i < Math.min(limit, misses.size()); i++) lines.add(misses.get(i).describe());
            return lines;
        }
    }

    /**
     * Best match for {@code pattern} with its anchor at {@code anchor}. Tiers are tried highest first,
     * and the first complete tier wins; otherwise the attempt with the fewest misses is returned.
     */
    public static Match match(Level level, BlockPos anchor, RitualPattern pattern) {
        Match best = null;
        for (RitualPattern.Tier tier : pattern.tiers()) {
            if (!tier.anchor().predicate().test(level, anchor)) continue;
            for (Rotation rotation : ROTATIONS) {
                List<Miss> misses = check(level, anchor, tier, rotation);
                if (misses.isEmpty()) return new Match(pattern, true, tier.number(), rotation, List.of());
                if (best == null || misses.size() < best.misses().size())
                    best = new Match(pattern, false, tier.number(), rotation, List.copyOf(misses));
            }
        }
        return best == null ? Match.none(pattern) : best;
    }

    /** Whether a complete match of at least {@code minimumTier} exists. */
    public static boolean matches(Level level, BlockPos anchor, RitualPattern pattern, int minimumTier) {
        Match match = match(level, anchor, pattern);
        return match.found() && match.tier() >= minimumTier;
    }

    private static List<Miss> check(Level level, BlockPos anchor, RitualPattern.Tier tier, Rotation rotation) {
        List<Miss> misses = new ArrayList<>();
        for (RitualPattern.Cell cell : tier.cells()) {
            if (cell.predicate().trivial()) continue;
            BlockPos pos = cell.at(anchor, rotation);
            // An unloaded neighbour is a miss, not a reason to load a chunk: patterns tick on a clock.
            boolean ok = level.hasChunkAt(pos) && cell.predicate().test(level, pos);
            if (!ok) misses.add(new Miss(pos, cell.predicate().description()));
        }
        return misses;
    }
}
