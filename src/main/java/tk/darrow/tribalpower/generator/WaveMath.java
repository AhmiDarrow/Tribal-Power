package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * What the water is worth (design 3.1 sections 9.1 and 9.5).
 *
 * <p>Three a second beside standing water, ten a second piped, doubled in the rain. The passive tier costs
 * nothing, so drums within {@link #CROWD_RANGE} divide it between them; the piped tier is limited by the
 * water supply itself and is exempt -- it is already paying.
 *
 * <p>This is the second job for fluid transport: a Spirit Cistern fed by a Spring Calling is a power plant.
 */
public final class WaveMath {
    public static final int PASSIVE = 3;
    public static final int PIPED = 10;
    /** Millibuckets a second the piped tier drinks. */
    public static final int PIPED_COST = 100;
    public static final int CROWD_RANGE = 8;

    private WaveMath() {}

    public record Factors(boolean water, boolean piped, boolean rain, int crowd) {
        public int gain() {
            int base = piped ? PIPED : water ? PASSIVE : 0;
            if (base == 0) return 0;
            if (rain) base *= 2;
            // Only the free tier is crowded out: piped drums pay in water for what they take.
            return piped ? base : base / Math.max(1, crowd);
        }
    }

    public static Factors factors(Level level, BlockPos pos, boolean piped) {
        return new Factors(adjacentWater(level, pos), piped, level.isRainingAt(pos.above()), crowd(level, pos));
    }

    public static boolean adjacentWater(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos side = pos.relative(face);
            if (level.hasChunkAt(side) && level.getFluidState(side).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    /** Drums within {@link #CROWD_RANGE}, this one included. Same chunk walk the harps use. */
    public static int crowd(Level level, BlockPos origin) {
        int count = 0;
        for (int cx = (origin.getX() - CROWD_RANGE) >> 4; cx <= (origin.getX() + CROWD_RANGE) >> 4; cx++) {
            for (int cz = (origin.getZ() - CROWD_RANGE) >> 4; cz <= (origin.getZ() + CROWD_RANGE) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof WaveDrumBlockEntity) || be.isRemoved()) continue;
                    if (be.getBlockPos().closerThan(origin, CROWD_RANGE)) count++;
                }
            }
        }
        return Math.max(1, count);
    }

    public static List<Component> breakdown(Level level, BlockPos pos, boolean piped) {
        Factors f = factors(level, pos, piped);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(f.piped() ? "wave.tribalpower.piped"
                : f.water() ? "wave.tribalpower.water" : "wave.tribalpower.dry",
                f.piped() ? PIPED : f.water() ? PASSIVE : 0, PIPED_COST));
        lines.add(Component.translatable(f.rain() ? "wave.tribalpower.rain" : "wave.tribalpower.clear"));
        if (!f.piped()) lines.add(Component.translatable("wave.tribalpower.crowd", f.crowd(), CROWD_RANGE));
        lines.add(Component.translatable("wave.tribalpower.total", f.gain()));
        return lines;
    }
}
