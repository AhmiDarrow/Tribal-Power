package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;

import java.util.List;

/**
 * The Rite Circle (design 3.1 section 5): placement is the ritual.
 *
 * <p>Brazier blessings are unchanged and still need nothing but a seated seal. Only tablet rites demand
 * the circle -- a brazier, four Rite Pedestals on the diagonals, and chalk joining them. Tier 2 seats a
 * seal-matched totem on each cardinal and pays for it: a quarter off the Pulse and twice the duration.
 *
 * <p>This is the one deliberate behaviour change in 3.1, so the Codex, JEI and the failure message all
 * point at the circle.
 */
public final class RiteCircle {
    /** How far the cardinal totems stand from the brazier in the tier 2 shape. */
    public static final int CARDINAL = 2;
    public static final double TIER2_COST = 0.75;
    public static final int TIER2_DURATION = 2;

    private RiteCircle() {}

    /** What the circle around a brazier is worth. */
    public record Result(boolean complete, int tier, PatternMatcher.Match match) {
        /** Pulse this circle charges for a rite that normally costs {@code base}. */
        public int cost(int base) {
            return tier >= 2 ? (int) Math.round(base * TIER2_COST) : base;
        }

        /** Ticks this circle makes a rite last, for rites that last at all. */
        public int duration(int base) {
            return tier >= 2 ? base * TIER2_DURATION : base;
        }

        public List<Component> report() {
            return match.report(3);
        }
    }

    /**
     * Evaluates the circle around {@code brazier} for a rite of {@code element}. A tier 2 shape only counts
     * as tier 2 when its four cardinal totems actually match the rite being asked for -- otherwise it is a
     * complete tier 1 circle with some totems standing near it.
     */
    public static Result evaluate(Level level, BlockPos brazier, Attunement element) {
        PatternMatcher.Match match = PatternMatcher.match(level, brazier, ModPatterns.RITE_CIRCLE);
        if (!match.found()) return new Result(false, 0, match);
        int tier = match.tier() >= 2 && sealMatched(level, brazier, element) ? 2 : 1;
        return new Result(true, tier, match);
    }

    /** True when all four cardinal slots hold a totem of {@code element}. */
    public static boolean sealMatched(Level level, BlockPos brazier, Attunement element) {
        // The cardinals are rotation-invariant as a set, so the four offsets need no rotation applied.
        for (int[] offset : new int[][]{{CARDINAL, 0}, {-CARDINAL, 0}, {0, CARDINAL}, {0, -CARDINAL}}) {
            BlockPos pos = brazier.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos)) return false;
            if (!(level.getBlockEntity(pos) instanceof ResonanceTotemBlockEntity totem)) return false;
            if (totem.getAttunement() != element) return false;
        }
        return true;
    }
}
