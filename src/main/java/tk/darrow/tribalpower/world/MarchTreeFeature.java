package tk.darrow.tribalpower.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.WillowStrandBlock;
import tk.darrow.tribalpower.world.MarchWoods.Wood;

/**
 * The March's own trees, grown procedurally so no two match, each from its own wood. A shape first builds a
 * {@link Plan} (logs, leaves, strands), which is then written in one pass: leaves get their true distance from
 * the wood so they decay properly once it is cut, and leaves beyond reach of any wood are left out.
 *
 * <p>A plan depends only on its origin and seed, so a tree too big for one feature (the Weeping Colossus,
 * grown by {@link WillowGroveStructure}) can be written chunk by chunk and still come out whole.
 */
public class MarchTreeFeature extends Feature<MarchTreeFeature.Config> {
    public enum Shape implements StringRepresentable {
        WEEPING_COLOSSUS(Wood.WILLOW), YOUNG_WILLOW(Wood.WILLOW), HEARTHOAK(Wood.HEARTHOAK), BELLCAP(Wood.BELLCAP),
        FROSTPINE(Wood.FROSTPINE), CINDER_SNAG(Wood.CINDER), STRIDER(Wood.STRIDER);

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);
        public final Wood wood;

        Shape(Wood wood) {
            this.wood = wood;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Config(Shape shape) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Shape.CODEC.fieldOf("shape").forGetter(Config::shape)).apply(i, Config::new));
    }

    public MarchTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Shape shape = context.config().shape();
        BlockPos origin = context.origin();
        if (!rooted(level, origin, shape)) return false;
        Plan plan = grow(shape, origin, context.random().nextLong(), 1.0);
        if (plan.top > level.getMaxBuildHeight() - 2) return false;
        return plan.write(level, writable(level));
    }

    /** Plans a tree. {@code scale} only matters to the Weeping Colossus, which groves grow at varied sizes. */
    public static Plan grow(Shape shape, BlockPos origin, long seed, double scale) {
        RandomSource random = RandomSource.create(seed);
        Plan plan = new Plan(origin, MarchWoods.of(shape.wood), seed);
        switch (shape) {
            case WEEPING_COLOSSUS -> willow(plan, random, 2.0 * scale);
            case YOUNG_WILLOW -> willow(plan, random, 0.3);
            case HEARTHOAK -> hearthoak(plan, random);
            case BELLCAP -> bellcap(plan, random);
            case FROSTPINE -> frostpine(plan, random);
            case CINDER_SNAG -> snag(plan, random);
            case STRIDER -> strider(plan, random);
        }
        plan.settle();
        return plan;
    }

    /** Soil (or a shallow fen bed) underfoot and open air above. */
    static boolean rooted(WorldGenLevel level, BlockPos origin, Shape shape) {
        BlockState below = level.getBlockState(origin.below());
        if (!below.is(BlockTags.DIRT) && !below.is(Blocks.MUD)) return false;
        int water = 0;
        for (int y = 0; y < 5; y++) {
            BlockState at = level.getBlockState(origin.above(y));
            if (at.getFluidState().is(FluidTags.WATER)) water++;
            else if (!replaceable(at)) return false;
        }
        return water <= (shape == Shape.STRIDER ? 3 : 1);
    }

    private static Predicate<BlockPos> writable(WorldGenLevel level) {
        if (!(level instanceof WorldGenRegion region)) return pos -> !level.isOutsideBuildHeight(pos);
        ChunkPos centre = region.getCenter();
        return pos -> !level.isOutsideBuildHeight(pos)
                && Math.abs((pos.getX() >> 4) - centre.x) <= 1 && Math.abs((pos.getZ() >> 4) - centre.z) <= 1;
    }

    static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof WillowStrandBlock
                || (state.canBeReplaced() && (state.getFluidState().isEmpty() || state.getFluidState().is(FluidTags.WATER)));
    }

    private static boolean ground(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.MUD) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)
                || state.getFluidState().is(FluidTags.WATER);
    }

    // ------------------------------------------------------------------ shapes

    /**
     * A weeping willow at any size: a flared, swaying trunk, buttress roots, and a crown of arching limbs whose
     * curtains of strands fall toward the ground. At colossus size ({@code s} about 2) it stands a hundred
     * blocks and more, with a hollow trunk; a young willow ({@code s} 0.3) is a small tree of the fen.
     */
    private static void willow(Plan p, RandomSource r, double s) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int height = Math.max(9, (int) ((40 + r.nextInt(18)) * s));
        double phase = r.nextDouble() * Mth.TWO_PI, sway = (0.8 + r.nextDouble() * 0.8) * Math.max(0.5, s);
        double leafScale = Math.min(Math.max(s, 0.75), 1.45);
        java.util.function.IntFunction<Vec3> axis = y -> p.at(Math.sin(y * 0.07 / Math.max(s, 0.5) + phase) * sway * y / height, y,
                Math.cos(y * 0.06 / Math.max(s, 0.5) + phase) * sway * y / height);
        java.util.function.IntToDoubleFunction girth = y -> Math.max(0.5,
                s * (4.6 - 3.0 * Math.pow(y / (double) height, 0.6)) + Math.max(0, 4 * s - y) * 0.55);
        double shell = s >= 1.5 ? 2.2 : 99;
        for (int y = 0; y <= height; y++) p.disc(axis.apply(y), girth.applyAsDouble(y), y == 0, y > 2 ? shell : 99);
        int roots = s >= 1 ? 7 + r.nextInt(4) : 3;
        for (int i = 0; i < roots; i++) {
            Vec3 dir = dir(i * Mth.TWO_PI / roots + r.nextDouble() * 0.5);
            int from = (int) ((3 + r.nextInt(4)) * s);
            Vec3 start = axis.apply(from).add(dir.scale(girth.applyAsDouble(from) - 0.5));
            Vec3 end = p.at(0, -2 * Math.max(1, s), 0).add(dir.scale((8 + r.nextInt(4)) * s));
            p.rooting = true;
            p.bezier(start, start.add(dir.scale(4 * s)).add(0, -from * 0.4, 0), end, Math.max(0.5, 1.6 * s), Math.max(0.5, 0.6 * s));
            p.rooting = false;
        }
        int limbs = (s >= 1 ? 10 : 6) + r.nextInt(3);
        for (int i = 0; i < limbs; i++) {
            double angle = i * Mth.TWO_PI / limbs + r.nextDouble() * 0.45;
            Vec3 dir = dir(angle);
            int from = height - (int) (12 * Math.max(s, 0.4)) + r.nextInt(Math.max(1, (int) (10 * Math.max(s, 0.4))));
            Vec3 start = axis.apply(from).add(dir.scale(girth.applyAsDouble(from) * 0.6));
            double reach = Math.max(4, (12 + r.nextInt(5)) * s);
            Vec3 end = p.at(dir.x * reach, height + (2 + r.nextInt(5)) * Math.max(s, 0.5), dir.z * reach);
            Vec3 bend = start.add(dir.scale(reach * 0.45)).add(0, (8 + r.nextInt(5)) * Math.max(s, 0.35), 0);
            List<Vec3> path = p.bezier(start, bend, end, Math.max(0.5, 1.7 * s), Math.max(0.5, 0.6 * s));
            for (int k = path.size() * 2 / 5; k < path.size(); k++) p.blob(leaf, path.get(k), 3.2 * leafScale, 1.8 * leafScale, r);
            for (double at : s >= 1 ? new double[]{0.45, 0.65, 0.85} : new double[]{0.7}) {
                Vec3 base = path.get((int) (at * (path.size() - 1)));
                Vec3 side = dir(angle + (r.nextBoolean() ? 0.9 : -0.9));
                Vec3 twig = base.add(side.scale((4 + r.nextInt(3)) * Math.max(s * 0.8, 0.5))).add(0, -1, 0);
                p.limb(base, twig, Math.max(0.5, 0.4 * s), 0.5);
                p.blob(leaf, twig, 2.6 * leafScale, 1.6 * leafScale, r);
            }
            Vec3 tip = end.add(dir.scale(2 * Math.max(s, 0.5))).add(0, -2 * Math.max(s, 0.5), 0);
            p.limb(end, tip, 0.5, 0.5);
            p.blob(leaf, tip, 2.4 * leafScale, 1.5 * leafScale, r);
        }
        Vec3 crown = axis.apply(height).add(0, 3 * Math.max(s, 0.5), 0);
        p.limb(axis.apply(height), crown, Math.max(0.5, s), 0.5);
        p.blob(leaf, crown, 5.0 * leafScale, 3.0 * leafScale, r);
        if (s >= 1) p.strands(0.24, (int) (4 * s), (int) (25 * s), 0.03, 1.1 * s / 2);
        else p.strands(0.45, 2, 9, 0.0, 0.8);
    }

    /** The Hearthoak: a squat, broad steppe giant whose limbs spread wide and low, meant to be camped under. */
    private static void hearthoak(Plan p, RandomSource r) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int height = 7 + r.nextInt(3);
        for (int y = 0; y <= height; y++) p.disc(p.at(0, y, 0), 1.6 - 0.5 * y / height + Math.max(0, 2 - y) * 0.5, y == 0, 99);
        for (int i = 0; i < 4; i++) {
            Vec3 dir = dir(i * Mth.HALF_PI + r.nextDouble());
            p.rooting = true;
            p.limb(p.at(dir.x * 1.5, 1, dir.z * 1.5), p.at(dir.x * 4, -1, dir.z * 4), 0.8, 0.5);
            p.rooting = false;
        }
        int limbs = 5 + r.nextInt(2);
        for (int i = 0; i < limbs; i++) {
            Vec3 dir = dir(i * Mth.TWO_PI / limbs + r.nextDouble() * 0.6);
            double reach = 9 + r.nextInt(4);
            Vec3 start = p.at(0, height - 2 + r.nextInt(3), 0);
            Vec3 end = p.at(dir.x * reach, height + 3 + r.nextInt(3), dir.z * reach);
            List<Vec3> path = p.bezier(start, start.add(dir.scale(reach * 0.5)).add(0, 3, 0), end, 1.2, 0.5);
            for (int k = path.size() / 2; k < path.size(); k += 2) p.blob(leaf, path.get(k), 3.4, 2.0, r);
            p.blob(leaf, end, 4.0, 2.2, r);
        }
        p.blob(leaf, p.at(0, height + 3, 0), 4.5, 2.5, r);
    }

    /** The Bellcap: a thin, swaying trunk under a broad violet bell of blossom, sometimes with a smaller twin. */
    private static void bellcap(Plan p, RandomSource r) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int height = 11 + r.nextInt(6);
        Vec3 lean = dir(r.nextDouble() * Mth.TWO_PI).scale(2 + r.nextInt(2));
        Vec3 top = p.at(lean.x, height, lean.z);
        List<Vec3> trunk = p.bezier(p.at(0, 0, 0), p.at(-lean.x, height * 0.5, -lean.z), top, 0.5, 0.5);
        p.base(trunk.getFirst());
        cap(p, leaf, top, 4.5 + r.nextDouble() * 1.5);
        if (r.nextFloat() < 0.4) {
            Vec3 fork = trunk.get(trunk.size() * 2 / 3);
            Vec3 twin = fork.add(dir(r.nextDouble() * Mth.TWO_PI).scale(3)).add(0, 3, 0);
            p.limb(fork, twin, 0.5, 0.5);
            cap(p, leaf, twin, 2.8);
        }
    }

    private static void cap(Plan p, BlockState leaf, Vec3 c, double radius) {
        for (int i = 0; i < 6; i++) {
            Vec3 d = dir(i * Mth.TWO_PI / 6);
            p.limb(c, c.add(d.scale(radius - 1.5)), 0.5, 0.5);
        }
        p.ring(leaf, c.add(0, 2, 0), 0, radius - 2.5);
        p.ring(leaf, c.add(0, 1, 0), 0, radius - 1.0);
        p.ring(leaf, c, radius - 2.0, radius + 0.3);
        p.ring(leaf, c.add(0, -1, 0), radius - 0.6, radius + 0.6);
    }

    /** The Frostpine: a tall, straight sentinel of flat needle tiers with upturned rims, snow country's tree. */
    private static void frostpine(Plan p, RandomSource r) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int height = 15 + r.nextInt(7);
        p.limb(p.at(0, 0, 0), p.at(0, height, 0), 0.5, 0.5);
        p.base(p.at(0, 0, 0));
        double twist = r.nextDouble() * Mth.TWO_PI;
        for (int y = 4 + r.nextInt(2); y < height - 1; y += 3 + r.nextInt(2)) {
            double t = y / (double) height, radius = Mth.lerp(t, 4.5, 1.6) + r.nextDouble() * 0.5;
            int arms = radius > 3 ? 6 : 4;
            for (int i = 0; i < arms; i++) {
                Vec3 d = dir(twist + i * Mth.TWO_PI / arms);
                p.limb(p.at(0, y, 0), p.at(d.x * (radius - 0.8), y, d.z * (radius - 0.8)), 0.5, 0.5);
            }
            p.ring(leaf, p.at(0, y, 0), 0, radius);
            p.ring(leaf, p.at(0, y + 1, 0), radius - 0.9, radius + 0.2);
            p.ring(leaf, p.at(0, y + 1, 0), 0, 1.2);
            twist += 0.5;
        }
        p.ring(leaf, p.at(0, height, 0), 0, 1.2);
        p.ring(leaf, p.at(0, height + 1, 0), 0, 0.6);
        p.ring(leaf, p.at(0, height + 2, 0), 0, 0.6);
    }

    /** The Cinder Snag: a scorched, leaning survivor of the Ember Wastes with a few smouldering tufts. */
    private static void snag(Plan p, RandomSource r) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int height = 6 + r.nextInt(5);
        Vec3 lean = dir(r.nextDouble() * Mth.TWO_PI);
        Vec3 at = p.at(0, 0, 0);
        p.base(at);
        List<Vec3> spine = new ArrayList<>();
        spine.add(at);
        for (int y = 2; y <= height; y += 2) {
            Vec3 next = p.at(lean.x * y * 0.35 + r.nextDouble() - 0.5, y, lean.z * y * 0.35 + r.nextDouble() - 0.5);
            p.limb(at, next, y < 3 ? 1.0 : 0.5, 0.5);
            spine.add(next);
            at = next;
        }
        for (int i = 0; i < 3; i++) {
            Vec3 d = dir(r.nextDouble() * Mth.TWO_PI);
            p.rooting = true;
            p.limb(p.at(0, 0, 0), p.at(d.x * 3, -1, d.z * 3), 0.5, 0.5);
            p.rooting = false;
        }
        int branches = 2 + r.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Vec3 from = spine.get(spine.size() / 2 + r.nextInt(Math.max(1, spine.size() / 2)));
            Vec3 tip = from.add(dir(r.nextDouble() * Mth.TWO_PI).scale(3 + r.nextInt(3))).add(0, 2 + r.nextInt(3), 0);
            p.limb(from, tip, 0.5, 0.5);
            if (r.nextFloat() < 0.45) p.blob(leaf, tip, 1.6, 1.2, r);
        }
        if (r.nextFloat() < 0.8) p.blob(leaf, at, 1.8, 1.3, r);
    }

    /** The Strider: a fen tree that stands above the water on arching stilt roots, trailing a few strands. */
    private static void strider(Plan p, RandomSource r) {
        BlockState leaf = p.set.leaves.get().defaultBlockState();
        int lift = 4 + r.nextInt(2), height = 6 + r.nextInt(3);
        for (int y = lift; y <= lift + height; y++) p.disc(p.at(0, y, 0), y < lift + 3 ? 1.0 : 0.5, false, 99);
        int roots = 5 + r.nextInt(2);
        for (int i = 0; i < roots; i++) {
            Vec3 d = dir(i * Mth.TWO_PI / roots + r.nextDouble() * 0.4);
            Vec3 start = p.at(d.x * 0.6, lift + 1 + r.nextInt(2), d.z * 0.6);
            Vec3 end = p.at(d.x * (4 + r.nextInt(3)), -1, d.z * (4 + r.nextInt(3)));
            p.rooting = true;
            List<Vec3> path = p.bezier(start, start.add(d.scale(2.5)).add(0, 1.5, 0), end, 0.7, 0.5);
            p.rooting = false;
            p.base(path.getLast());
        }
        Vec3 crown = p.at(0, lift + height, 0);
        for (int i = 0; i < 3 + r.nextInt(2); i++) {
            Vec3 tip = crown.add(dir(r.nextDouble() * Mth.TWO_PI).scale(3 + r.nextInt(2))).add(0, 1 + r.nextInt(2), 0);
            p.limb(crown.add(0, -2, 0), tip, 0.5, 0.5);
            p.blob(leaf, tip, 3.0, 2.0, r);
        }
        p.blob(leaf, crown.add(0, 1, 0), 3.5, 2.5, r);
        p.strands(0.18, 2, 6, 0.0, 0.8);
    }

    private static Vec3 dir(double angle) {
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    // ------------------------------------------------------------------ the plan

    public static final class Plan {
        final BlockPos origin;
        final MarchWoods.Set set;
        final long seed;
        final Map<BlockPos, Direction.Axis> logs = new HashMap<>();
        final Map<BlockPos, BlockState> leaves = new HashMap<>();
        final Set<BlockPos> rootable = new HashSet<>();
        final Set<BlockPos> bases = new HashSet<>();
        /** Leaves within reach of the wood, with their distance. */
        final Map<BlockPos, Integer> distance = new HashMap<>();
        boolean rooting;
        public int top;
        private double strandChance, glowChance, strandReach;
        private int strandMin, strandMax;

        Plan(BlockPos origin, MarchWoods.Set set, long seed) {
            this.origin = origin;
            this.set = set;
            this.seed = seed;
            this.top = origin.getY();
        }

        Vec3 at(double x, double y, double z) {
            return new Vec3(origin.getX() + 0.5 + x, origin.getY() + y, origin.getZ() + 0.5 + z);
        }

        void log(BlockPos pos, Direction.Axis axis) {
            logs.merge(pos, axis, (old, now) -> old == Direction.Axis.Y ? old : now);
            if (rooting) rootable.add(pos);
            top = Math.max(top, pos.getY());
        }

        void base(Vec3 v) {
            bases.add(BlockPos.containing(v));
        }

        /** A horizontal slice of trunk; with a {@code shell} thickness it is hollow inside. */
        void disc(Vec3 c, double radius, boolean base, double shell) {
            int r = Mth.ceil(radius);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                double x = Math.floor(c.x) + dx + 0.5 - c.x, z = Math.floor(c.z) + dz + 0.5 - c.z;
                double d = Math.sqrt(x * x + z * z);
                if (d > radius + 0.25 || d < radius - shell) continue;
                BlockPos pos = BlockPos.containing(c.x + dx, c.y, c.z + dz);
                log(pos, Direction.Axis.Y);
                if (base) bases.add(pos);
            }
        }

        /** A straight branch of tapering thickness. */
        void limb(Vec3 a, Vec3 b, double r0, double r1) {
            Vec3 d = b.subtract(a);
            double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
            Direction.Axis axis = ay >= Math.max(ax, az) ? Direction.Axis.Y : ax >= az ? Direction.Axis.X : Direction.Axis.Z;
            int steps = Math.max(1, Mth.ceil(d.length() * 2));
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps, radius = Mth.lerp(t, r0, r1);
                Vec3 c = a.add(d.scale(t));
                if (radius <= 0.6) {
                    log(BlockPos.containing(c), axis);
                    continue;
                }
                int r = Mth.ceil(radius);
                for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++)
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) log(BlockPos.containing(c.add(dx, dy, dz)), axis);
            }
        }

        /** A curved limb through a control point; returns the points along it. */
        List<Vec3> bezier(Vec3 a, Vec3 c, Vec3 b, double r0, double r1) {
            int segments = Math.max(4, Mth.ceil(a.distanceTo(b) / 2));
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments, u = 1 - t;
                points.add(a.scale(u * u).add(c.scale(2 * u * t)).add(b.scale(t * t)));
            }
            for (int i = 0; i < segments; i++)
                limb(points.get(i), points.get(i + 1), Mth.lerp(i / (double) segments, r0, r1), Mth.lerp((i + 1) / (double) segments, r0, r1));
            return points;
        }

        /** A rough ellipsoid of leaves. */
        void blob(BlockState leaf, Vec3 c, double rx, double ry, RandomSource r) {
            int ix = Mth.ceil(rx), iy = Mth.ceil(ry);
            for (int dx = -ix; dx <= ix; dx++) for (int dy = -iy; dy <= iy; dy++) for (int dz = -ix; dz <= ix; dz++) {
                double f = (dx * dx + dz * dz) / (rx * rx) + dy * dy / (ry * ry);
                if (f <= 1 - r.nextDouble() * 0.28) leaf(leaf, BlockPos.containing(c.add(dx, dy, dz)));
            }
        }

        /** A flat annulus of leaves between two radii. */
        void ring(BlockState leaf, Vec3 c, double inner, double outer) {
            int r = Mth.ceil(outer);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= inner && d <= outer) leaf(leaf, BlockPos.containing(c.add(dx, 0, dz)));
            }
        }

        void leaf(BlockState leaf, BlockPos pos) {
            leaves.putIfAbsent(pos, leaf);
            top = Math.max(top, pos.getY());
        }

        void strands(double chance, int min, int max, double glow, double reachFactor) {
            strandChance = chance;
            strandMin = min;
            strandMax = max;
            glowChance = glow;
            strandReach = reachFactor;
        }

        /** Works out each leaf's distance from the wood; leaves out of reach are dropped from the plan. */
        void settle() {
            distance.putAll(distances(Set.of()));
        }

        /** Leaf distances from the planned wood, less any logs that could not be placed. */
        private Map<BlockPos, Integer> distances(Set<BlockPos> missing) {
            Map<BlockPos, Integer> distance = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            for (BlockPos pos : logs.keySet()) {
                if (missing.contains(pos)) continue;
                for (Direction d : Direction.values()) {
                    BlockPos next = pos.relative(d);
                    if (leaves.containsKey(next) && !logs.containsKey(next) && distance.putIfAbsent(next, 1) == null) queue.add(next);
                }
            }
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                int d = distance.get(pos);
                if (d >= 6) continue;
                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.relative(dir);
                    if (leaves.containsKey(next) && !logs.containsKey(next) && distance.putIfAbsent(next, d + 1) == null) queue.add(next);
                }
            }
            return distance;
        }

        /** A stable per-position random number, so every chunk of a large tree agrees on its strands. */
        private double hash(BlockPos pos, int salt) {
            long h = seed ^ pos.asLong() * 0x9E3779B97F4A7C15L ^ salt * 0xC2B2AE3D27D4EB4FL;
            h ^= h >>> 33;
            h *= 0xFF51AFD7ED558CCDL;
            h ^= h >>> 33;
            return (h >>> 11) * 0x1.0p-53;
        }

        public boolean write(WorldGenLevel level, Predicate<BlockPos> writable) {
            BlockState wood = set.log.get().defaultBlockState();
            // A sapling grows its tree in the live world: skip neighbour updates there, or a colossus stalls the tick.
            int flags = level instanceof WorldGenRegion ? Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE
                    : Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            boolean any = false;
            Set<BlockPos> missing = new HashSet<>();
            for (var entry : logs.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!writable.test(pos)) continue;
                BlockState there = level.getBlockState(pos);
                if (!replaceable(there) && !(rootable.contains(pos) && ground(there))) {
                    if (!there.is(BlockTags.LOGS)) missing.add(pos);
                    continue;
                }
                level.setBlock(pos, wood.setValue(RotatedPillarBlock.AXIS, entry.getValue()), flags);
                any = true;
            }
            // Sink the base into the ground so a tree on a slope never floats.
            for (BlockPos base : bases) {
                BlockPos.MutableBlockPos cursor = base.below().mutable();
                for (int i = 0; i < 12 && writable.test(cursor); i++, cursor.move(Direction.DOWN)) {
                    BlockState there = level.getBlockState(cursor);
                    if (!replaceable(there) && !there.getFluidState().is(FluidTags.WATER)) break;
                    level.setBlock(cursor, wood, flags);
                }
            }
            // Leaves beside a log that could not be placed would never decay: measure again without it.
            Map<BlockPos, Integer> distance = missing.isEmpty() ? this.distance : distances(missing);
            for (var entry : distance.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!writable.test(pos)) continue;
                BlockState there = level.getBlockState(pos);
                if (!there.isAir() && !there.is(BlockTags.REPLACEABLE_BY_TREES) || !there.getFluidState().isEmpty()) continue;
                level.setBlock(pos, leaves.get(pos).setValue(LeavesBlock.DISTANCE, entry.getValue()), flags);
            }
            if (strandChance > 0) hang(level, writable, distance);
            return any;
        }

        private void hang(WorldGenLevel level, Predicate<BlockPos> writable, Map<BlockPos, Integer> distance) {
            BlockState strand = MarchTrees.WILLOW_STRAND.get().defaultBlockState();
            int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            double cx = origin.getX() + 0.5, cz = origin.getZ() + 0.5;
            for (BlockPos leaf : distance.keySet()) {
                BlockPos below = leaf.below();
                if (!writable.test(below) || distance.containsKey(below) || logs.containsKey(below)) continue;
                if (!(level.getBlockState(leaf).getBlock() instanceof LeavesBlock)) continue;
                double reach = Math.sqrt((leaf.getX() + 0.5 - cx) * (leaf.getX() + 0.5 - cx) + (leaf.getZ() + 0.5 - cz) * (leaf.getZ() + 0.5 - cz));
                if (reach < 4 || hash(leaf, 1) > strandChance) continue;
                int length = strandMin + (int) (hash(leaf, 2) * 3) + (int) (hash(leaf, 3) * Math.max(0, reach - 4) * strandReach);
                length = Math.min(strandMax, length);
                BlockPos.MutableBlockPos cursor = below.mutable();
                int placed = 0;
                while (placed < length && writable.test(cursor) && level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, strand.setValue(WillowStrandBlock.TIP, false), flags);
                    placed++;
                    cursor.move(Direction.DOWN);
                }
                if (placed > 0) {
                    cursor.move(Direction.UP);
                    level.setBlock(cursor, strand.setValue(WillowStrandBlock.TIP, true)
                            .setValue(WillowStrandBlock.GLOW, hash(leaf, 4) < glowChance), flags);
                }
            }
        }
    }
}
