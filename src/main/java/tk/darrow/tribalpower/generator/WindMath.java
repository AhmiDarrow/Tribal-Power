package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * What the wind is worth, and how many harps are already asking (design 3.1 sections 9.1 and 9.5).
 *
 * <p>Same shape as {@code LeyMath}: documented integers and one breakdown shared by the Codex, the Ley
 * Lens and the block's own diagnosis, so the number a player reads is the number the server used.
 *
 * <p>Two a second at y 80 under open sky; six a second high in a storm. It costs nothing, so it is capped
 * instead: harps within {@link #CROWD_RANGE} divide the wind between them. Harps steal each other's wind.
 */
public final class WindMath {
    public static final int BASE = 1;
    public static final int OPEN_SKY = 1;
    public static final int HIGH_Y = 128;
    public static final int HIGHER_Y = 192;
    public static final int HIGH = 1;
    public static final int HIGHER = 2;
    public static final int RAIN = 1;
    public static final int THUNDER = 2;
    public static final int MAX_GAIN = BASE + OPEN_SKY + HIGHER + THUNDER;

    /** Harps closer than this share one wind between them. */
    public static final int CROWD_RANGE = 12;

    private WindMath() {}

    public record Factors(boolean sky, int altitude, boolean rain, boolean thunder, int crowd) {
        /** Pulse a second before the config multiplier. */
        public int gain() {
            if (!sky) return 0; // A harp under a roof is a decoration.
            int gain = BASE + OPEN_SKY + altitude + (thunder ? THUNDER : rain ? RAIN : 0);
            return gain / Math.max(1, crowd);
        }
    }

    public static Factors factors(Level level, BlockPos pos) {
        boolean sky = level.canSeeSky(pos.above());
        int altitude = pos.getY() >= HIGHER_Y ? HIGHER : pos.getY() >= HIGH_Y ? HIGH : 0;
        boolean thunder = level.isThundering() && level.isRainingAt(pos.above());
        boolean rain = level.isRainingAt(pos.above());
        return new Factors(sky, altitude, rain, thunder, crowd(level, pos));
    }

    public static int gain(Level level, BlockPos pos) {
        return factors(level, pos).gain();
    }

    /**
     * Harps within {@link #CROWD_RANGE}, this one included. Walks the loaded chunks' block-entity maps
     * rather than probing every position of the cube: this runs on every harp's beat.
     */
    public static int crowd(Level level, BlockPos origin) {
        int count = 0;
        int min = CROWD_RANGE;
        for (int cx = (origin.getX() - min) >> 4; cx <= (origin.getX() + min) >> 4; cx++) {
            for (int cz = (origin.getZ() - min) >> 4; cz <= (origin.getZ() + min) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof WindHarpBlockEntity) || be.isRemoved()) continue;
                    if (be.getBlockPos().closerThan(origin, CROWD_RANGE)) count++;
                }
            }
        }
        return Math.max(1, count);
    }

    /** One line per factor and then the total, in the integers the server used. */
    public static List<Component> breakdown(Level level, BlockPos pos) {
        Factors f = factors(level, pos);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(f.sky() ? "wind.tribalpower.sky" : "wind.tribalpower.sky_none",
                f.sky() ? BASE + OPEN_SKY : 0));
        lines.add(Component.translatable("wind.tribalpower.altitude", pos.getY(), f.altitude()));
        lines.add(Component.translatable(f.thunder() ? "wind.tribalpower.thunder"
                : f.rain() ? "wind.tribalpower.rain" : "wind.tribalpower.calm",
                f.thunder() ? THUNDER : f.rain() ? RAIN : 0));
        lines.add(Component.translatable("wind.tribalpower.crowd", f.crowd(), CROWD_RANGE));
        lines.add(Component.translatable("wind.tribalpower.total", f.gain(), MAX_GAIN));
        return lines;
    }
}
