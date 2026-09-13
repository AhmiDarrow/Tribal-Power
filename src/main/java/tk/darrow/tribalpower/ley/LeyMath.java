package tk.darrow.tribalpower.ley;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.tribe.TribeHearthBlockEntity;
import tk.darrow.tribalpower.world.ModDimensions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Ley strength maths shared by the Ley Collector, the Ley Lens and Codex diagnostics.
 * A collector surveys {@link #RADIUS} (the lattice listen range). The Lens HUD uses a cheap
 * glimpse so painting the ground does not scan a cube every frame.
 *
 * <p>Gain per collection beat is capped at {@link #MAX_GAIN}. A roofed hole stays at {@link #BASE}.
 * A grove with sky, water, plants, voices and life can reach the cap without matching a Resonator.
 */
public final class LeyMath {
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;
    public static final int STEP = 1;
    public static final int BASE = 1;
    public static final int SKY_DAY = 1;
    public static final int SKY_NIGHT = 3;
    public static final int RAIN = 2;
    public static final int THUNDER = 1;
    public static final int WATER = 4;
    public static final int GREENERY = 4;
    public static final int VOICES = 3;
    public static final int HEARTH = 1;
    public static final int LIFE = 2;
    public static final int MARCH = 2;
    public static final int MOON = 1;
    /** Soft cap so a cathedral grove is strong ambient, not a Resonator. */
    public static final int MAX_GAIN = 16;
    public static final double PAD = 0.5;

    private LeyMath() {}

    public record Factors(boolean sky, boolean night, boolean rain, boolean thunder, int water, int greenery,
                          int voices, int hearth, int life, int march, int moon, int raw) {
        public int gain() { return Math.min(MAX_GAIN, raw); }
        public double strength() { return gain() / (double) MAX_GAIN; }
        public boolean pad() { return strength() >= PAD; }
    }

    /** Cheap column read for the Lens grid and HUD. */
    public static Factors glimpse(Level level, BlockPos pos) {
        boolean sky = level.canSeeSky(pos.above());
        boolean night = level.isNight();
        boolean rain = level.isRainingAt(pos.above());
        boolean thunder = rain && level.isThundering();
        int water = 0, green = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                cursor.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (!level.hasChunkAt(cursor)) continue;
                BlockState state = level.getBlockState(cursor);
                if (water(state)) water++;
                if (living(state)) green++;
            }
        }
        int waterPts = Math.min(WATER, water);
        int greenPts = Math.min(GREENERY, green);
        int raw = BASE;
        if (sky) raw += night ? SKY_NIGHT : SKY_DAY;
        if (rain) raw += RAIN;
        if (thunder) raw += THUNDER;
        raw += waterPts + greenPts;
        return new Factors(sky, night, rain, thunder, waterPts, greenPts, 0, 0, 0, 0, 0, raw);
    }

    /** Full pad survey: radius 8, used by the Collector and sneak-Lens print. */
    public static Factors factors(Level level, BlockPos pos) {
        boolean sky = level.canSeeSky(pos.above());
        boolean night = level.isNight();
        boolean rain = level.isRainingAt(pos.above());
        boolean thunder = rain && level.isThundering();
        int waterHits = 0, greenHits = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx += STEP) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz += STEP) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (water(state)) waterHits++;
                    if (living(state)) greenHits++;
                }
            }
        }
        int waterPts = Math.min(WATER, waterHits);
        int greenPts = Math.min(GREENERY, greenHits / 4);

        EnumSet<Attunement> heard = EnumSet.noneOf(Attunement.class);
        boolean hearth = false;
        int minY = pos.getY() - RADIUS, maxY = pos.getY() + RADIUS;
        int minX = pos.getX() - RADIUS, maxX = pos.getX() + RADIUS;
        int minZ = pos.getZ() - RADIUS, maxZ = pos.getZ() + RADIUS;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.isRemoved()) continue;
                    BlockPos at = be.getBlockPos();
                    if (at.getX() < minX || at.getX() > maxX || at.getY() < minY || at.getY() > maxY
                            || at.getZ() < minZ || at.getZ() > maxZ) continue;
                    if (be instanceof ResonanceTotemBlockEntity totem) heard.add(totem.getAttunement());
                    if (be instanceof TribeHearthBlockEntity) hearth = true;
                }
            }
        }
        int voicePts = Math.min(VOICES, heard.size());
        int hearthPts = hearth ? HEARTH : 0;

        AABB box = new AABB(pos).inflate(RADIUS);
        int animals = level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive() && !a.isRemoved()).size();
        int lifePts = Math.min(LIFE, animals / 2);

        int marchPts = level.dimension().equals(ModDimensions.THE_MARCH) ? MARCH : 0;
        int moonPts = night && level.getMoonBrightness() >= 0.9F ? MOON : 0;

        int raw = BASE;
        if (sky) raw += night ? SKY_NIGHT : SKY_DAY;
        if (rain) raw += RAIN;
        if (thunder) raw += THUNDER;
        raw += waterPts + greenPts + voicePts + hearthPts + lifePts + marchPts + moonPts;
        return new Factors(sky, night, rain, thunder, waterPts, greenPts, voicePts, hearthPts, lifePts, marchPts, moonPts, raw);
    }

    public static int gain(Level level, BlockPos pos) {
        return factors(level, pos).gain();
    }

    public static double strength(Level level, BlockPos pos) {
        return factors(level, pos).strength();
    }

    public static int percent(Level level, BlockPos pos) {
        return (int) Math.round(strength(level, pos) * 100);
    }

    public static List<Component> breakdown(Level level, BlockPos pos) {
        Factors f = factors(level, pos);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("ley.tribalpower.base", BASE));
        lines.add(Component.translatable(f.sky() ? (f.night() ? "ley.tribalpower.sky_night" : "ley.tribalpower.sky_day") : "ley.tribalpower.sky_none",
                f.sky() ? (f.night() ? SKY_NIGHT : SKY_DAY) : 0));
        lines.add(Component.translatable(f.rain() ? "ley.tribalpower.rain" : "ley.tribalpower.rain_none", f.rain() ? RAIN : 0));
        if (f.thunder()) lines.add(Component.translatable("ley.tribalpower.thunder", THUNDER));
        lines.add(Component.translatable(f.water() > 0 ? "ley.tribalpower.water" : "ley.tribalpower.water_none", f.water()));
        lines.add(Component.translatable(f.greenery() > 0 ? "ley.tribalpower.greenery" : "ley.tribalpower.greenery_none", f.greenery()));
        lines.add(Component.translatable(f.voices() > 0 ? "ley.tribalpower.voices" : "ley.tribalpower.voices_none", f.voices()));
        lines.add(Component.translatable(f.hearth() > 0 ? "ley.tribalpower.hearth" : "ley.tribalpower.hearth_none", f.hearth()));
        lines.add(Component.translatable(f.life() > 0 ? "ley.tribalpower.life" : "ley.tribalpower.life_none", f.life()));
        if (f.march() > 0) lines.add(Component.translatable("ley.tribalpower.march", f.march()));
        if (f.moon() > 0) lines.add(Component.translatable("ley.tribalpower.moon", f.moon()));
        lines.add(Component.translatable("ley.tribalpower.total", f.gain(), MAX_GAIN, (int) Math.round(f.strength() * 100)));
        if (f.pad()) lines.add(Component.translatable("ley.tribalpower.pad"));
        return lines;
    }

    private static boolean water(BlockState state) {
        return !state.getFluidState().isEmpty() && state.getFluidState().is(FluidTags.WATER);
    }

    static boolean living(BlockState state) {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)) return true;
        if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSS_CARPET) || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO) || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.SWEET_BERRY_BUSH))
            return true;
        return state.is(ModBlocks.MARCH_MOSS.get()) || state.is(ModBlocks.MARCH_GRASS.get())
                || state.is(ModBlocks.MARCH_LEAVES.get()) || state.is(ModBlocks.MARCH_LEAF.get());
    }

    /** @deprecated kept so old 4-neighbour checks still compile; use {@link #factors}. */
    @Deprecated
    public static boolean adjacent(Level level, BlockPos pos, Direction direction) {
        return living(level.getBlockState(pos.relative(direction)))
                || water(level.getBlockState(pos.relative(direction)));
    }
}
