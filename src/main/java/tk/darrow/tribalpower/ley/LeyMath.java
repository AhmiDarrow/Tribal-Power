package tk.darrow.tribalpower.ley;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Ley strength maths shared by the Ley Collector (generation), the Ley Lens (HUD and particles)
 * and Codex diagnostics. Every input is client-visible, so the same numbers appear on both sides.
 *
 * <p>Gain per collection beat: 1 base, +1 open sky by day or +3 by night, +2 rain, +2 adjacent water,
 * +2 adjacent greenery (leaves, moss, March Moss). Maximum {@link #MAX_GAIN}.
 */
public final class LeyMath {
    public static final int BASE = 1;
    public static final int SKY_DAY = 1;
    public static final int SKY_NIGHT = 3;
    public static final int RAIN = 2;
    public static final int WATER = 2;
    public static final int GREENERY = 2;
    public static final int MAX_GAIN = BASE + SKY_NIGHT + RAIN + WATER + GREENERY;

    private LeyMath() {}

    /** Individual factors at a position. */
    public record Factors(boolean sky, boolean night, boolean rain, boolean water, boolean greenery) {
        public int gain() {
            int gain = BASE;
            if (sky) gain += night ? SKY_NIGHT : SKY_DAY;
            if (rain) gain += RAIN;
            if (water) gain += WATER;
            if (greenery) gain += GREENERY;
            return gain;
        }
        public double strength() { return gain() / (double) MAX_GAIN; }
    }

    public static Factors factors(Level level, BlockPos pos) {
        boolean sky = level.canSeeSky(pos.above());
        boolean night = level.isNight();
        boolean rain = level.isRainingAt(pos.above());
        boolean water = false, living = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState nearby = level.getBlockState(pos.relative(direction));
            water |= !nearby.getFluidState().isEmpty() && nearby.getFluidState().is(FluidTags.WATER);
            living |= nearby.is(BlockTags.LEAVES) || nearby.is(Blocks.MOSS_BLOCK) || nearby.is(ModBlocks.MARCH_MOSS.get());
        }
        return new Factors(sky, night, rain, water, living);
    }

    /** Pulse gained per Ley Collector beat at this position (1..{@link #MAX_GAIN}). */
    public static int gain(Level level, BlockPos pos) {
        return factors(level, pos).gain();
    }

    /** Ley strength 0..1 (gain divided by the best possible gain). */
    public static double strength(Level level, BlockPos pos) {
        return factors(level, pos).strength();
    }

    /** Strength as a whole percentage, 10..100. */
    public static int percent(Level level, BlockPos pos) {
        return (int) Math.round(strength(level, pos) * 100);
    }

    /** Human-readable factor breakdown, one line per factor plus the total. */
    public static List<Component> breakdown(Level level, BlockPos pos) {
        Factors f = factors(level, pos);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("ley.tribalpower.base", BASE));
        lines.add(Component.translatable(f.sky() ? (f.night() ? "ley.tribalpower.sky_night" : "ley.tribalpower.sky_day") : "ley.tribalpower.sky_none",
                f.sky() ? (f.night() ? SKY_NIGHT : SKY_DAY) : 0));
        lines.add(Component.translatable(f.rain() ? "ley.tribalpower.rain" : "ley.tribalpower.rain_none", f.rain() ? RAIN : 0));
        lines.add(Component.translatable(f.water() ? "ley.tribalpower.water" : "ley.tribalpower.water_none", f.water() ? WATER : 0));
        lines.add(Component.translatable(f.greenery() ? "ley.tribalpower.greenery" : "ley.tribalpower.greenery_none", f.greenery() ? GREENERY : 0));
        lines.add(Component.translatable("ley.tribalpower.total", f.gain(), MAX_GAIN, (int) Math.round(f.strength() * 100)));
        return lines;
    }
}
