package tk.darrow.tribalpower.world;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.WillowStrandBlock;

/**
 * Pieces of the old world: floating mountains hanging in the sky over every land, dressed as that land is (its turf and
 * stone, its wood for the vines, its flowers on the crown). Each is a tall crag of March rock, wide at the
 * top and tapering below, its faces banded and mossy, turf and trees on its crown, vines down every side, a
 * waterfall off the rim, small rocks drifting about it, and the others of its cluster at other heights, bridged by
 * giant vines. A cluster takes one of several shapes ({@link Formation}): a spire with satellites, a chain along an arc,
 * a flat mesa, a stack climbing skyward, a ring round an empty middle, or a field of thin shards. Bridged by
 * giant vines of willow wood. Giant roots hold the cluster to the mountain under it; over a grove, to a colossus's
 * crown. The roots and bridges are sheathed in leaves and hung with curtains of climbable strands.
 *
 * <p>A cluster spans several chunks, so it is a structure piece whose plan depends only on its seed and anchor:
 * every chunk it overlaps writes its own share, as the colossi do.
 */
public class FloatingIslandStructure extends Structure {
    public static final MapCodec<FloatingIslandStructure> CODEC = simpleCodec(FloatingIslandStructure::new);
    private static final int REACH = 100;

    public FloatingIslandStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return MarchStructures.FLOATING_ISLAND.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        var generator = context.chunkGenerator();
        int surface = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        int floor = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (surface - floor > 2 || floor <= generator.getSeaLevel()) return Optional.empty();   // dry rock, not a lake
        int max = context.heightAccessor().getMaxBuildHeight();
        // high enough that the crag's taper hangs clear of the peak with sky under it
        int centre = floor + 72 + random.nextInt(34);
        if (centre + 44 > max) centre = max - 44;
        if (centre - floor < 48) return Optional.empty();
        // the moorings: the cluster has drifted off to one side of where its roots hold the ground, so the roots
        // run at an angle, taut, as if the island were pulling away; only now and then does one hang straight
        List<BlockPos> targets = new ArrayList<>();
        int roots = 2 + random.nextInt(2);
        double drift = random.nextDouble() * Mth.TWO_PI;
        int pull = random.nextInt(7) == 0 ? random.nextInt(8) : 28 + random.nextInt(44);
        int mx = cx - (int) (Math.cos(drift) * pull), mz = cz - (int) (Math.sin(drift) * pull);
        int mooring = generator.getFirstOccupiedHeight(mx, mz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        for (int i = 0; i < roots; i++) {
            double angle = drift + Math.PI + (i - (roots - 1) / 2.0) * 0.5 + random.nextDouble() * 0.3;
            int d = 6 + random.nextInt(10);
            int x = mx + (int) (Math.cos(angle) * d), z = mz + (int) (Math.sin(angle) * d);
            int y = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (Math.abs(y - mooring) > 16 || y <= generator.getSeaLevel()) continue;
            targets.add(new BlockPos(x, y, z));
        }
        if (targets.isEmpty()) return Optional.empty();
        BlockPos anchor = new BlockPos(cx, floor + 1, cz);
        long seed = random.nextLong();
        int cy = centre;
        String biome = biomeAt(context, cx, floor, cz);
        return Optional.of(new GenerationStub(anchor, builder -> builder.addPiece(new Island(anchor, cy, 0, seed, targets, biome))));
    }

    /** The path of the biome at a point, as the piece records it: the cluster dresses itself for that land. */
    public static String biomeAt(GenerationContext context, int x, int y, int z) {
        var holder = context.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(y),
                net.minecraft.core.QuartPos.fromBlock(z), context.randomState().sampler());
        return holder.unwrapKey().map(k -> k.location().getPath()).orElse("march_steppe");
    }

    // ------------------------------------------------------------------ the palette

    /** What a cluster is made of in one land: its turf and soil, the stone of its faces and bands, its wood and its growth. */
    record Palette(BlockState top, BlockState soil, BlockState face, BlockState band, BlockState accent, MarchTreeFeature.Shape[] trees, Block[] plants,
                   BlockState leaf, BlockState log, MarchBiomes.Band zone) {}

    private static BlockState breadth(String id) {
        return ModBlocks.breadth(id).defaultBlockState();
    }

    /** The band gives the ground and the stone; the land's own wood, where it has one, gives the trees and the vines. */
    static Palette palette(String biome) {
        MarchBiomes land = MarchBiomes.of(biome);
        MarchBiomes.Band band = land == null ? MarchBiomes.Band.TEMPERATE : land.band;
        BlockState grass = ModBlocks.MARCH_GRASS.get().defaultBlockState(), soil = ModBlocks.MARCH_SOIL.get().defaultBlockState();
        BlockState moss = ModBlocks.MARCH_MOSS.get().defaultBlockState(), cobble = ModBlocks.MARCH_COBBLE.get().defaultBlockState();
        BlockState top = grass, sub = soil, face = moss, bands = cobble, accent = ModBlocks.MOSS_AGATE.get().defaultBlockState();
        MarchTreeFeature.Shape[] trees = {MarchTreeFeature.Shape.HEARTHOAK, MarchTreeFeature.Shape.SONGMAPLE};
        Block[] plants = {ModBlocks.ECHO_BLOOM.get(), ModBlocks.LEY_THISTLE.get(), ModBlocks.MARCH_LEAF.get()};
        switch (band) {
            case FROZEN -> {
                top = Blocks.SNOW_BLOCK.defaultBlockState(); face = breadth("hoarmoss"); bands = breadth("frost_shale"); accent = breadth("rime_ice");
                trees = new MarchTreeFeature.Shape[]{MarchTreeFeature.Shape.FROSTPINE, MarchTreeFeature.Shape.RIMEBIRCH};
                plants = new Block[]{ModBlocks.breadth("icebloom"), ModBlocks.breadth("frostcap")};
            }
            case COLD -> {
                sub = breadth("needle_loam"); bands = breadth("scree"); accent = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                trees = new MarchTreeFeature.Shape[]{MarchTreeFeature.Shape.THORNFIR, MarchTreeFeature.Shape.FROSTPINE};
                plants = new Block[]{ModBlocks.breadth("grey_heather"), ModBlocks.breadth("hoar_thistle")};
            }
            case WARM -> {
                sub = breadth("ochre_sand"); accent = breadth("ochre_sandstone");
                trees = new MarchTreeFeature.Shape[]{MarchTreeFeature.Shape.DRUMPALM, MarchTreeFeature.Shape.TANGLEWOOD};
                plants = new Block[]{ModBlocks.breadth("songgrass"), ModBlocks.breadth("thornbrush"), ModBlocks.breadth("tangle_fern")};
            }
            case HOT -> {
                top = breadth("ochre_sand"); sub = breadth("ochre_sandstone"); face = breadth("kiln_clay_rust"); bands = breadth("kiln_clay_ochre"); accent = breadth("salt_crust");
                trees = new MarchTreeFeature.Shape[]{MarchTreeFeature.Shape.SUNBARK, MarchTreeFeature.Shape.CINDER_SNAG};
                plants = new Block[]{ModBlocks.breadth("thornbrush"), ModBlocks.breadth("sunwheel")};
            }
            default -> { }
        }
        // the land's own wood
        MarchTreeFeature.Shape own = null;
        if (biome.contains("veilwood")) own = MarchTreeFeature.Shape.VEILWOOD;
        else if (biome.contains("chime")) own = MarchTreeFeature.Shape.CHIMEBLOSSOM;
        else if (biome.contains("songmaple")) own = MarchTreeFeature.Shape.SONGMAPLE;
        else if (biome.contains("hearthoak")) own = MarchTreeFeature.Shape.HEARTHOAK;
        else if (biome.contains("thornfir")) own = MarchTreeFeature.Shape.THORNFIR;
        else if (biome.contains("rimebirch")) own = MarchTreeFeature.Shape.RIMEBIRCH;
        else if (biome.contains("frostpine") || biome.contains("rime_spires") || biome.contains("hoar")) own = MarchTreeFeature.Shape.FROSTPINE;
        else if (biome.contains("drumpalm")) own = MarchTreeFeature.Shape.DRUMPALM;
        else if (biome.contains("tanglewood") || biome.contains("cane")) own = MarchTreeFeature.Shape.TANGLEWOOD;
        else if (biome.contains("sunbark")) own = MarchTreeFeature.Shape.SUNBARK;
        else if (biome.contains("strider") || biome.contains("fen") || biome.contains("mire")) own = MarchTreeFeature.Shape.STRIDER;
        else if (biome.contains("ember") || biome.contains("kiln") || biome.contains("cut_mesa")) own = MarchTreeFeature.Shape.CINDER_SNAG;
        else if (biome.contains("crystal") || biome.contains("glimmer") || biome.contains("bloom")) own = MarchTreeFeature.Shape.BELLCAP;
        if (own != null) trees = new MarchTreeFeature.Shape[]{own, trees[0]};
        // Bellcap leaves glow; a cluster of them over every meadow is more light than a chunk should carry
        if (trees[0] == MarchTreeFeature.Shape.BELLCAP) trees = new MarchTreeFeature.Shape[]{MarchTreeFeature.Shape.HEARTHOAK, MarchTreeFeature.Shape.BELLCAP};
        var set = MarchWoods.of(trees[0].wood);
        BlockState leaf = set.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 7);
        BlockState log = set.log.get().defaultBlockState();
        return new Palette(top, sub, face, bands, accent, trees, plants, leaf, log, band);
    }

    // ------------------------------------------------------------------ the plan

    private static final java.util.LinkedHashMap<String, Plan> PLANS = new java.util.LinkedHashMap<>(8, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Plan> eldest) {
            return size() > 16;
        }
    };

    public static void clearPlans() {
        synchronized (PLANS) {
            PLANS.clear();
        }
    }

    static Plan plan(BlockPos anchor, int centre, int radius, long seed, List<BlockPos> targets, String biome) {
        String key = anchor.asLong() + ":" + centre + ":" + radius + ":" + seed + ":" + biome;
        synchronized (PLANS) {
            Plan plan = PLANS.get(key);
            if (plan != null) return plan;
        }
        Plan plan = Plan.build(anchor, centre, radius, seed, targets, palette(biome));
        synchronized (PLANS) {
            PLANS.putIfAbsent(key, plan);
            return PLANS.get(key);
        }
    }

    /**
     * One crag of a cluster: where and how big; how it tapers ({@code taper} is the exponent of the draw-in: low is a
     * long teardrop, high a flat-bottomed slab); how domed its crown is (negative hollows it into a bowl); how many
     * trees it carries; whether the old ring of bricks stands on it; whether the roots hold it; how far its column
     * leans per block of depth; and a bite out of its rim ({@code gapWidth} 0 for none).
     */
    record Crag(int x, int y, int z, int radius, int height, double taper, double dome, int trees, boolean ruin, boolean main,
                double leanX, double leanZ, double gapAngle, double gapWidth) {
        Crag(int x, int y, int z, int radius, int height, double taper, double dome, int trees, boolean ruin, boolean main) {
            this(x, y, z, radius, height, taper, dome, trees, ruin, main, 0, 0, 0, 0);
        }
    }

    /**
     * The shapes a cluster can take. Each band of the March draws from its own list, so the frozen north hangs
     * glacier shelves and icicles where the salt hangs buttes and hoodoos.
     */
    enum Formation {
        SPIRE, TWIN, CHAIN, STAIR, MESA, TERRACES, STACK, RING, CRESCENT, SHARDS, NEEDLE, CRADLE, ARCH, CASCADE, SWARM, SPLIT,
        GARDEN, STONES, LEANING, SHELF, ICICLES, BERGS, HUMMOCKS, BARROW, BUTTE, HOODOOS, PAN, STILTS, ATOLL, TABLE
    }

    static final Map<MarchBiomes.Band, Formation[]> FORMATIONS = Map.of(
            MarchBiomes.Band.FROZEN, new Formation[]{Formation.SHELF, Formation.SHELF, Formation.ICICLES, Formation.ICICLES, Formation.BERGS, Formation.CRADLE, Formation.TWIN, Formation.NEEDLE, Formation.STAIR, Formation.ARCH},
            MarchBiomes.Band.COLD, new Formation[]{Formation.STACK, Formation.TERRACES, Formation.TERRACES, Formation.HUMMOCKS, Formation.BARROW, Formation.SPIRE, Formation.SPLIT, Formation.SHARDS, Formation.MESA, Formation.CASCADE},
            MarchBiomes.Band.TEMPERATE, new Formation[]{Formation.SPIRE, Formation.SPIRE, Formation.GARDEN, Formation.RING, Formation.CHAIN, Formation.TWIN, Formation.STACK, Formation.CRADLE, Formation.STONES, Formation.CRESCENT, Formation.ARCH, Formation.LEANING},
            MarchBiomes.Band.WARM, new Formation[]{Formation.TABLE, Formation.TABLE, Formation.STILTS, Formation.STILTS, Formation.ATOLL, Formation.GARDEN, Formation.SWARM, Formation.LEANING, Formation.TERRACES, Formation.CHAIN},
            MarchBiomes.Band.HOT, new Formation[]{Formation.BUTTE, Formation.BUTTE, Formation.HOODOOS, Formation.HOODOOS, Formation.PAN, Formation.SPLIT, Formation.STAIR, Formation.STACK, Formation.ARCH, Formation.NEEDLE},
            MarchBiomes.Band.UNDERGROUND, new Formation[]{Formation.SPIRE});

    /**
     * How a cluster's rock is dressed: how much its rim wanders, how often its bands show and how far apart they lie,
     * how mossy its faces are, how thick the vines cling and how long the strands fall. Set by band, jittered by seed.
     */
    record Style(double wobble, int bandEvery, float bandChance, float accentChance, float moss, float cling, float under, int strandMin, int strandMax) {}

    static Style style(MarchBiomes.Band band, RandomSource r) {
        double j = 0.8 + r.nextDouble() * 0.4;   // every cluster its own
        return switch (band) {
            case FROZEN -> new Style(0.08 * j, 4, 0.25F, 0.5F, 0.18F, 0.3F, 0.06F, 3, 9);
            case COLD -> new Style(0.2 * j, 3, 0.55F, 0.3F, 0.4F, 0.5F, 0.1F, 4, 12);
            case WARM -> new Style(0.26 * j, 3, 0.4F, 0.25F, 0.4F, 0.75F, 0.16F, 6, 18);
            case HOT -> new Style(0.1 * j, 2, 0.8F, 0.6F, 0.12F, 0.12F, 0.03F, 2, 6);
            default -> new Style(0.18 * j, 3, 0.6F, 0.35F, 0.28F, 0.5F, 0.1F, 4, 14);
        };
    }

    /** Everything a cluster is made of, keyed by position: the rock, growth, roots and bridges first, the strands after. */
    public static final class Plan {
        final Map<BlockPos, BlockState> solid = new HashMap<>();
        final Map<BlockPos, BlockState> strands = new HashMap<>();
        public int top, bottom;
        /** Glowing strand tips a cluster may hang; past this the tips are plain, whatever the draw says. */
        static final int GLOW_BUDGET = 48;
        private int glows;
        private final BlockState stone = ModBlocks.MARCH_STONE.get().defaultBlockState(), ore = ModBlocks.MARCH_ORE.get().defaultBlockState(),
                strand = MarchTrees.WILLOW_STRAND.get().defaultBlockState();
        /** The land's dress: its turf, soil, face stone, band and accent stone, its wood for the vines and roots. */
        private final BlockState cobble, soil, grass, moss, agate, leaf, log;
        private final Palette palette;
        private Style style;

        private Plan(Palette palette) {
            this.palette = palette;
            grass = palette.top(); soil = palette.soil(); moss = palette.face(); cobble = palette.band(); agate = palette.accent();
            leaf = palette.leaf(); log = palette.log();
        }

        /**
         * @param radius 0 for a mountain cluster sized by the seed; a grove hands the radius of its single crag
         */
        static Plan build(BlockPos anchor, int centre, int radius, long seed, List<BlockPos> targets, Palette palette) {
            Plan p = new Plan(palette);
            RandomSource r = RandomSource.create(seed);
            p.top = p.bottom = centre;
            p.style = style(palette.zone(), r);
            int ax = anchor.getX(), az = anchor.getZ();
            // the formation: a grove's island is a lone spire; elsewhere the land's band offers its own shapes
            Formation[] offer = FORMATIONS.getOrDefault(palette.zone(), FORMATIONS.get(MarchBiomes.Band.TEMPERATE));
            Formation form = radius > 0 ? Formation.SPIRE : offer[r.nextInt(offer.length)];
            List<Crag> crags = new ArrayList<>();
            List<int[]> bridges = new ArrayList<>();   // pairs of crag indices joined by a vine
            List<double[]> arches = new ArrayList<>(); // pairs joined by a vine that arches up instead of sagging
            switch (form) {
                case SPIRE -> spire(crags, bridges, ax, az, centre, radius, r, 2.0 + r.nextDouble() * 0.6, 3);
                case LEANING -> {
                    spire(crags, bridges, ax, az, centre, 0, r, 1.8 + r.nextDouble() * 0.5, 2.5);
                    double la = r.nextDouble() * Mth.TWO_PI, lean = 0.25 + r.nextDouble() * 0.3;
                    for (int i = 0; i < crags.size(); i++) crags.set(i, leaned(crags.get(i), Math.cos(la) * lean, Math.sin(la) * lean));
                }
                case NEEDLE -> {
                    // one very thin, very tall spire, strands all down it, rocks orbiting
                    int rr = 6 + r.nextInt(4);
                    crags.add(new Crag(ax, centre, az, rr, rr * (4 + r.nextInt(3)), 1.3 + r.nextDouble() * 0.4, 1.5, 1, false, true));
                }
                case TWIN -> {
                    // two near-equal crags shoulder to shoulder, one a little higher, bridged
                    double la = r.nextDouble() * Mth.TWO_PI;
                    int r1 = 11 + r.nextInt(6), r2 = 10 + r.nextInt(6), gap = r1 + r2 + 4 + r.nextInt(6);
                    crags.add(new Crag(ax - (int) (Math.cos(la) * gap / 2), centre, az - (int) (Math.sin(la) * gap / 2), r1, (int) (r1 * (1.5 + r.nextDouble() * 0.6)), 2.0 + r.nextDouble() * 0.5, 3, 2, true, true));
                    crags.add(new Crag(ax + (int) (Math.cos(la) * gap / 2), centre + 6 + r.nextInt(12), az + (int) (Math.sin(la) * gap / 2), r2, (int) (r2 * (1.5 + r.nextDouble() * 0.6)), 2.0 + r.nextDouble() * 0.5, 3, 1 + r.nextInt(2), false, false));
                    bridges.add(new int[]{0, 1});
                }
                case CHAIN -> chain(crags, bridges, ax, az, centre, r, 4 + r.nextInt(4), 0.12 + r.nextDouble() * 0.18, 0, 7, 6);
                case STAIR -> chain(crags, bridges, ax, az, centre, r, 4 + r.nextInt(3), 0.0, -(8 + r.nextInt(6)), 8, 5);
                case CASCADE -> chain(crags, bridges, ax, az, centre, r, 3 + r.nextInt(3), 0.05, -(10 + r.nextInt(6)), 10, 6);
                case MESA -> mesa(crags, ax, az, centre, r, 22 + r.nextInt(8), 0.5 + r.nextDouble() * 0.3, 4.5 + r.nextDouble() * 2, 1.2, 4 + r.nextInt(3));
                case TABLE -> mesa(crags, ax, az, centre, r, 18 + r.nextInt(8), 0.35 + r.nextDouble() * 0.2, 6 + r.nextDouble() * 2, 0.6, 2 + r.nextInt(3));
                case BUTTE -> {
                    // a tall, sheer, flat-topped butte, strongly banded, little on top
                    int rr = 12 + r.nextInt(8);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * (1.8 + r.nextDouble() * 0.8)), 7 + r.nextDouble() * 3, 0.5, 1 + r.nextInt(2), r.nextBoolean(), true));
                    if (r.nextBoolean()) {
                        double la = r.nextDouble() * Mth.TWO_PI; int sr = 5 + r.nextInt(4), dist = rr + sr + 6 + r.nextInt(8);
                        crags.add(new Crag(ax + (int) (Math.cos(la) * dist), centre - 10 - r.nextInt(20), az + (int) (Math.sin(la) * dist), sr, (int) (sr * 2.2), 6, 0.5, 0, false, false));
                    }
                }
                case PAN -> mesa(crags, ax, az, centre, r, 24 + r.nextInt(10), 0.18 + r.nextDouble() * 0.1, 8, 0.2, r.nextInt(2));
                case SHELF -> {
                    // a glacier shelf: broad, low, flat, with a steep snout at one end
                    int rr = 20 + r.nextInt(10);
                    double la = r.nextDouble() * Mth.TWO_PI;
                    crags.add(leaned(new Crag(ax, centre, az, rr, (int) (rr * (0.35 + r.nextDouble() * 0.2)), 5 + r.nextDouble() * 3, 0.8, 1 + r.nextInt(2), false, true), Math.cos(la) * 0.4, Math.sin(la) * 0.4));
                    if (r.nextBoolean()) {
                        int sr = 8 + r.nextInt(6), dist = rr + sr + 4;
                        crags.add(new Crag(ax + (int) (Math.cos(la) * dist), centre - 6 - r.nextInt(10), az + (int) (Math.sin(la) * dist), sr, (int) (sr * 0.6), 5, 0.6, 0, false, false));
                        bridges.add(new int[]{0, 1});
                    }
                }
                case TERRACES -> {
                    // two or three flat steps, each lower and a little aside from the last
                    int n = 2 + r.nextInt(2), rr = 16 + r.nextInt(6), y = centre + 8 * (n - 1);
                    double la = r.nextDouble() * Mth.TWO_PI;
                    int x = ax, z = az;
                    for (int i = 0; i < n; i++) {
                        crags.add(new Crag(x, y, z, rr, (int) (rr * (0.6 + r.nextDouble() * 0.3)), 4 + r.nextDouble() * 2, 1.0, i == 0 ? 2 : 1, i == 0, i == n / 2));
                        if (i > 0) bridges.add(new int[]{i - 1, i});
                        y -= 10 + r.nextInt(8);
                        x += (int) (Math.cos(la) * (rr + 4)); z += (int) (Math.sin(la) * (rr + 4));
                        rr = Math.max(9, (int) (rr * (0.75 + r.nextDouble() * 0.15)));
                    }
                }
                case STACK -> {
                    // three or four crags stacked skyward, each smaller than the one below, roped together
                    int tiers = 3 + r.nextInt(2), rr = 14 + r.nextInt(5), y = centre - 24, x = ax, z = az;
                    for (int i = 0; i < tiers; i++) {
                        crags.add(new Crag(x, y, z, rr, (int) (rr * (1.3 + r.nextDouble() * 0.5)), 2.0 + r.nextDouble() * 0.8, 2.5, i == tiers - 1 ? 2 : 1, i == 0, i == 0));
                        if (i > 0) bridges.add(new int[]{i - 1, i});
                        y += 8 + r.nextInt(6) + (int) (rr * 0.4);
                        x += (r.nextBoolean() ? 1 : -1) * (4 + r.nextInt(8)); z += (r.nextBoolean() ? 1 : -1) * (4 + r.nextInt(8));
                        rr = Math.max(6, (int) (rr * (0.65 + r.nextDouble() * 0.15)));
                    }
                }
                case RING -> ring(crags, bridges, ax, az, centre, r, 5 + r.nextInt(2), 26 + r.nextInt(10), true);
                case CRESCENT -> ring(crags, bridges, ax, az, centre, r, 4 + r.nextInt(2), 24 + r.nextInt(10), false);
                case ATOLL -> {
                    // a ring of low palms islets round a lagoon crag beneath
                    ring(crags, bridges, ax, az, centre, r, 6 + r.nextInt(2), 22 + r.nextInt(6), true);
                    crags.add(new Crag(ax, centre - 14 - r.nextInt(8), az, 12 + r.nextInt(5), 6 + r.nextInt(4), 6, -2.5, 0, false, false));
                }
                case SHARDS, BERGS, HOODOOS -> {
                    // a field of thin shards (bergs: squat and blocky; hoodoos: tall, banded, flat-capped)
                    int n = 8 + r.nextInt(7);
                    for (int i = 0; i < n; i++) {
                        double la = r.nextDouble() * Mth.TWO_PI, dist = i == 0 ? 0 : 8 + r.nextDouble() * 36;
                        int rr = form == Formation.BERGS ? 5 + r.nextInt(5) : 3 + r.nextInt(4);
                        double tall = form == Formation.BERGS ? 1.0 + r.nextDouble() : form == Formation.HOODOOS ? 3 + r.nextDouble() * 2 : 2.5 + r.nextDouble() * 1.5;
                        double taper = form == Formation.BERGS ? 3 + r.nextDouble() * 2 : form == Formation.HOODOOS ? 4 + r.nextDouble() * 3 : 1.1 + r.nextDouble() * 0.5;
                        crags.add(new Crag(ax + (int) (Math.cos(la) * dist), centre + (i == 0 ? 0 : (r.nextBoolean() ? 1 : -1) * r.nextInt(26)), az + (int) (Math.sin(la) * dist), rr, (int) (rr * tall), taper, form == Formation.HOODOOS ? 0.3 : 1.5, 0, false, i == 0));
                    }
                    for (int i = 1; i < n; i++) if (r.nextFloat() < 0.25F) bridges.add(new int[]{r.nextInt(i), i});
                }
                case ICICLES -> {
                    // a small snowy crown with long icicle shards hanging under and beside it
                    int rr = 9 + r.nextInt(5);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * 0.8), 3, 1.5, 1, false, true));
                    int n = 5 + r.nextInt(5);
                    for (int i = 0; i < n; i++) {
                        double la = r.nextDouble() * Mth.TWO_PI, dist = rr * (0.3 + r.nextDouble() * 1.2);
                        int sr = 2 + r.nextInt(3);
                        crags.add(new Crag(ax + (int) (Math.cos(la) * dist), centre - 2 - r.nextInt(6), az + (int) (Math.sin(la) * dist), sr, sr * (5 + r.nextInt(5)), 1.0 + r.nextDouble() * 0.3, 0.2, 0, false, false));
                    }
                }
                case HUMMOCKS -> {
                    // a close huddle of rounded mossy mounds at much the same height, no bridges, hop across
                    int n = 5 + r.nextInt(4);
                    for (int i = 0; i < n; i++) {
                        double la = r.nextDouble() * Mth.TWO_PI, dist = i == 0 ? 0 : 10 + r.nextDouble() * 24;
                        int rr = 6 + r.nextInt(6);
                        crags.add(new Crag(ax + (int) (Math.cos(la) * dist), centre + r.nextInt(7) - 3, az + (int) (Math.sin(la) * dist), rr, (int) (rr * (0.9 + r.nextDouble() * 0.5)), 1.6 + r.nextDouble() * 0.6, 3.5, r.nextInt(2), i == 0, i == 0));
                    }
                }
                case BARROW -> {
                    // one great rounded mound, a dome of turf, a ring of standing stones for a ruin
                    int rr = 18 + r.nextInt(8);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * (1.0 + r.nextDouble() * 0.4)), 1.7, 5, 3 + r.nextInt(3), true, true));
                }
                case CRADLE -> {
                    // a broad bowl with a lake in its hollow and a pillar rising from the middle
                    int rr = 18 + r.nextInt(8);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * (0.9 + r.nextDouble() * 0.4)), 3.5, -3.0, 3 + r.nextInt(2), false, true));
                    int pr = 4 + r.nextInt(3);
                    crags.add(new Crag(ax, centre + 4, az, pr, pr * (3 + r.nextInt(3)), 1.3, 1.0, 1, true, false));
                }
                case ARCH -> {
                    // two crags under a vine that arches up between them, and a slab hung from the arch
                    double la = r.nextDouble() * Mth.TWO_PI;
                    int r1 = 9 + r.nextInt(5), r2 = 9 + r.nextInt(5), gap = r1 + r2 + 18 + r.nextInt(14);
                    int x1 = ax - (int) (Math.cos(la) * gap / 2), z1 = az - (int) (Math.sin(la) * gap / 2), x2 = ax + (int) (Math.cos(la) * gap / 2), z2 = az + (int) (Math.sin(la) * gap / 2);
                    crags.add(new Crag(x1, centre, z1, r1, (int) (r1 * (1.6 + r.nextDouble() * 0.6)), 2.2, 3, 1, true, true));
                    crags.add(new Crag(x2, centre + r.nextInt(9) - 4, z2, r2, (int) (r2 * (1.6 + r.nextDouble() * 0.6)), 2.2, 3, 1, false, false));
                    arches.add(new double[]{x1, centre + 2, z1, x2, centre + 2, z2, 14 + r.nextInt(10)});
                    crags.add(new Crag(ax, centre - 10 - r.nextInt(8), az, 5 + r.nextInt(3), 4, 6, 0.5, 0, false, false));
                }
                case SWARM -> {
                    // one modest crag in a belt of many drifting rocks
                    int rr = 10 + r.nextInt(6);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * (1.4 + r.nextDouble() * 0.6)), 2.2, 3, 1 + r.nextInt(2), true, true));
                }
                case SPLIT -> {
                    // one great crag broken in two down a gap, each half with a bite out of its rim
                    int rr = 15 + r.nextInt(7); double la = r.nextDouble() * Mth.TWO_PI; int off = rr / 2 + 3;
                    crags.add(new Crag(ax - (int) (Math.cos(la) * off), centre, az - (int) (Math.sin(la) * off), rr, (int) (rr * (1.6 + r.nextDouble() * 0.6)), 2.2, 2.5, 2, true, true, 0, 0, la, 0.9));
                    crags.add(new Crag(ax + (int) (Math.cos(la) * off), centre + r.nextInt(7) - 3, az + (int) (Math.sin(la) * off), rr - 2, (int) (rr * (1.5 + r.nextDouble() * 0.6)), 2.2, 2.5, 1, false, false, 0, 0, la + Math.PI, 0.9));
                }
                case GARDEN -> {
                    // a hanging garden: a wooded mesa fringed with long curtains of strands all round and beneath
                    int rr = 18 + r.nextInt(8);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * (0.8 + r.nextDouble() * 0.4)), 3.5 + r.nextDouble(), 1.5, 5 + r.nextInt(3), r.nextBoolean(), true));
                    p.style = new Style(p.style.wobble(), p.style.bandEvery(), p.style.bandChance(), p.style.accentChance(), Math.min(0.6F, p.style.moss() + 0.2F), 0.9F, 0.3F, 10, 24);
                }
                case STILTS -> {
                    // mangrove stilts: a low wooded platform on many long root columns that reach down toward the land
                    int rr = 14 + r.nextInt(6);
                    crags.add(new Crag(ax, centre, az, rr, (int) (rr * 0.5), 5, 1.0, 3 + r.nextInt(3), false, true));
                    p.style = new Style(p.style.wobble(), p.style.bandEvery(), p.style.bandChance(), p.style.accentChance(), p.style.moss(), 0.8F, 0.35F, 12, 30);
                }
                case STONES -> {
                    // stepping stones: small flat discs at near one height, a hop apart, no bridges
                    int n = 6 + r.nextInt(4); double heading = r.nextDouble() * Mth.TWO_PI; double x = ax, z = az;
                    for (int i = 0; i < n; i++) {
                        int rr = 4 + r.nextInt(3);
                        crags.add(new Crag((int) x, centre + r.nextInt(5) - 2, (int) z, rr, 3 + r.nextInt(3), 6, 0.5, i % 3 == 0 ? 1 : 0, false, i == 0));
                        heading += (r.nextDouble() - 0.5) * 0.9;
                        double step = rr + 6 + r.nextInt(5);
                        x += Math.cos(heading) * step; z += Math.sin(heading) * step;
                    }
                }
            }
            // a long chain or walk of stones is pulled in rather than cut off: a chunk past the piece's box never writes it
            int fit = REACH - 28;
            for (int i = 0; i < crags.size(); i++) {
                Crag c = crags.get(i);
                int x = Mth.clamp(c.x, ax - fit, ax + fit), z = Mth.clamp(c.z, az - fit, az + fit);
                if (x != c.x || z != c.z) crags.set(i, new Crag(x, c.y, z, c.radius, c.height, c.taper, c.dome, c.trees, c.ruin, c.main, c.leanX, c.leanZ, c.gapAngle, c.gapWidth));
            }
            Crag main = crags.getFirst();
            for (Crag c : crags) if (c.main) main = c;
            // the cluster leans the way it pulls, away from its moorings
            if (!targets.isEmpty()) {
                double tx = 0, tz = 0;
                for (BlockPos t : targets) { tx += t.getX() - main.x; tz += t.getZ() - main.z; }
                double len = Math.hypot(tx, tz);
                if (len > 20) {
                    double lean = Math.min(0.22, len / targets.size() / 300.0);
                    for (int i = 0; i < crags.size(); i++) {
                        Crag c = crags.get(i);
                        if (c.leanX == 0 && c.leanZ == 0 && c.height > 8) crags.set(i, leaned(c, -tx / len * lean, -tz / len * lean));
                    }
                }
            }
            for (Crag c : crags) p.crag(c, r);
            // drifting rocks: small stones about the cluster, a few trailing a strand
            int rocks = switch (form) { case SWARM -> 16 + r.nextInt(8); case SHARDS, BERGS, HOODOOS, ICICLES -> 10 + r.nextInt(6); case PAN, SHELF, STONES -> 2 + r.nextInt(3); default -> 5 + r.nextInt(6); };
            for (int i = 0; i < rocks; i++) {
                Crag near = crags.get(r.nextInt(crags.size()));
                double angle = r.nextDouble() * Mth.TWO_PI;
                int dist = near.radius + 4 + r.nextInt(form == Formation.SWARM ? 18 : 10), rr = 1 + r.nextInt(3);
                int rx = Mth.clamp(near.x + (int) (Math.cos(angle) * dist), ax - REACH + 6, ax + REACH - 6), rz = Mth.clamp(near.z + (int) (Math.sin(angle) * dist), az - REACH + 6, az + REACH - 6);
                p.rock(new BlockPos(rx, near.y + r.nextInt(Math.max(1, near.height)) - near.height / 2, rz), rr, r);
            }
            // vine bridges and arches
            for (int[] pair : bridges) {
                Crag c1 = crags.get(pair[0]), c2 = crags.get(pair[1]);
                Vec3 a = new Vec3(c1.x + 0.5, c1.y - 2, c1.z + 0.5), b = new Vec3(c2.x + 0.5, c2.y - 2, c2.z + 0.5);
                if (a.distanceTo(b) > 90) continue;
                Vec3 dir = b.subtract(a).normalize();
                boolean rope = form == Formation.STACK;   // a stack's ropes hang more than they span
                p.vine(a.add(dir.scale(c1.radius * 0.7)), b.subtract(dir.scale(c2.radius * 0.7)), rope ? 1.0 : 1.3, 1.0, r, rope ? 0 : 0.18);
            }
            for (double[] arch : arches)
                p.vine(new Vec3(arch[0] + 0.5, arch[1], arch[2] + 0.5), new Vec3(arch[3] + 0.5, arch[4], arch[5] + 0.5), 1.8, 1.8, r, -arch[6] / Math.hypot(arch[3] - arch[0], arch[5] - arch[2]));
            // roots from the crag the anchor holds to the ground, with curtains for the climb
            int curtains = form == Formation.STILTS ? 5 : 3;
            for (BlockPos target : targets) {
                double angle = Math.atan2(target.getZ() - main.z, target.getX() - main.x);
                double out = main.radius * (0.3 + r.nextDouble() * 0.3);
                int sx = main.x + (int) (Math.cos(angle) * out), sz = main.z + (int) (Math.sin(angle) * out);
                Vec3 start = new Vec3(sx + 0.5, main.y - main.height * 0.5, sz + 0.5), end = new Vec3(target.getX() + 0.5, target.getY() - 2, target.getZ() + 0.5);
                p.vine(start, end, 2.1, 1.4, r, 0.05);   // taut: the island strains against it
                for (int k = 0; k < curtains; k++) {
                    double a2 = angle + (k - curtains / 2) * 0.5 + r.nextDouble() * 0.3;
                    int cx = sx + (int) (Math.cos(a2) * 4), cz = sz + (int) (Math.sin(a2) * 4);
                    int sy = main.y - (int) (main.height * 0.4);
                    BlockPos from = new BlockPos(cx, sy, cz);
                    p.solid.putIfAbsent(from, p.leaf);
                    p.hang(from.below(), sy - 1 - target.getY(), r);
                }
            }
            return p;
        }

        private static Crag leaned(Crag c, double lx, double lz) {
            return new Crag(c.x, c.y, c.z, c.radius, c.height, c.taper, c.dome, c.trees, c.ruin, c.main, lx, lz, c.gapAngle, c.gapWidth);
        }

        /** One tall teardrop with one to three lesser crags at other heights round it, bridged to it. */
        private static void spire(List<Crag> crags, List<int[]> bridges, int ax, int az, int centre, int radius, RandomSource r, double taper, double dome) {
            int mainR = radius > 0 ? radius : 13 + r.nextInt(10);
            crags.add(new Crag(ax, centre, az, mainR, (int) (mainR * (1.6 + r.nextDouble() * 0.8)), taper, dome, 2 + r.nextInt(2), true, true));
            int lesser = radius > 0 ? r.nextInt(2) : 1 + r.nextInt(3);
            for (int i = 0; i < lesser; i++) {
                double angle = r.nextDouble() * Mth.TWO_PI;
                int rr = 6 + r.nextInt(8), dist = mainR + rr + 8 + r.nextInt(14);
                int dy = (r.nextBoolean() ? 1 : -1) * (12 + r.nextInt(22));
                crags.add(new Crag(ax + (int) (Math.cos(angle) * dist), centre + dy, az + (int) (Math.sin(angle) * dist), rr, (int) (rr * (1.5 + r.nextDouble())), taper - 0.2 + r.nextDouble() * 0.6, dome, 1, false, false));
                bridges.add(new int[]{0, crags.size() - 1});
            }
            for (int i = 1; i + 1 < crags.size(); i++) if (r.nextFloat() < 0.6F) bridges.add(new int[]{i, i + 1});
        }

        /** Crags strung along an arc (or a line when {@code bend} is 0), each {@code drop} lower than the last, bridged in order. */
        private static void chain(List<Crag> crags, List<int[]> bridges, int ax, int az, int centre, RandomSource r, int n, double bend, int drop, int rMin, int rSpan) {
            double heading = r.nextDouble() * Mth.TWO_PI, turn = (r.nextBoolean() ? 1 : -1) * bend;
            double x = ax, z = az;
            int mid = n / 2;
            List<double[]> spots = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                spots.add(new double[]{x, z});
                double step = 20 + r.nextInt(10);
                heading += turn;
                x += Math.cos(heading) * step; z += Math.sin(heading) * step;
            }
            double cx = spots.get(mid)[0] - ax, cz = spots.get(mid)[1] - az;   // slide the chain so its middle sits on the anchor
            for (int i = 0; i < n; i++) {
                int rr = rMin + r.nextInt(rSpan);
                int y = centre + (drop != 0 ? (i - mid) * drop : i == mid ? 0 : (r.nextBoolean() ? 1 : -1) * (6 + r.nextInt(16)));
                crags.add(new Crag((int) (spots.get(i)[0] - cx), y, (int) (spots.get(i)[1] - cz), rr, (int) (rr * (1.4 + r.nextDouble() * 0.8)), 1.8 + r.nextDouble() * 0.8, 2.5, i == mid ? 2 : r.nextInt(2), i == mid, i == mid));
                if (i > 0) bridges.add(new int[]{i - 1, i});
            }
        }

        /** One broad, flat-topped table with sheer sides, a lake on top, a wood round it. */
        private static void mesa(List<Crag> crags, int ax, int az, int centre, RandomSource r, int rr, double heightFactor, double taper, double dome, int trees) {
            crags.add(new Crag(ax, centre, az, rr, Math.max(4, (int) (rr * heightFactor)), taper, dome, trees, r.nextBoolean(), true));
            if (r.nextInt(3) == 0) {
                double angle = r.nextDouble() * Mth.TWO_PI;
                int sr = 6 + r.nextInt(5), dist = rr + sr + 10 + r.nextInt(10);
                crags.add(new Crag(ax + (int) (Math.cos(angle) * dist), centre - 8 - r.nextInt(16), az + (int) (Math.sin(angle) * dist), sr, (int) (sr * 1.8), 2.2, 3, 1, false, false));
            }
        }

        /** Crags round an empty middle, bridged into a ring, or into a crescent when the ring is open. */
        private static void ring(List<Crag> crags, List<int[]> bridges, int ax, int az, int centre, RandomSource r, int n, double ringR, boolean closed) {
            double phase0 = r.nextDouble() * Mth.TWO_PI, span = closed ? Mth.TWO_PI : Math.PI * (0.9 + r.nextDouble() * 0.3);
            for (int i = 0; i < n; i++) {
                double angle = phase0 + i * span / (closed ? n : n - 1) + (r.nextDouble() - 0.5) * 0.3;
                int rr = 7 + r.nextInt(5);
                crags.add(new Crag(ax + (int) (Math.cos(angle) * ringR), centre + (r.nextBoolean() ? 1 : -1) * r.nextInt(10), az + (int) (Math.sin(angle) * ringR), rr, (int) (rr * (1.4 + r.nextDouble() * 0.8)), 1.8 + r.nextDouble() * 0.8, 2.5, i == 0 ? 2 : r.nextInt(2), i == 0, i == 0));
                if (i > 0) bridges.add(new int[]{i - 1, i});
            }
            if (closed) bridges.add(new int[]{n - 1, 0});
        }

        /** A floating mountain: wide at the crown, tapering to a point below, banded, mossy, vined, with a fall off the rim. */
        private void crag(Crag c, RandomSource r) {
            double phase = r.nextDouble() * Mth.TWO_PI, wobble = style.wobble() * (0.8 + r.nextDouble() * 0.4);
            Map<Long, Integer> tops = new HashMap<>();
            int R = c.radius + 4;
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                double angle = Math.atan2(dz, dx);
                double rim = c.radius * (1 + wobble * Math.sin(3 * angle + phase) + 0.1 * Math.sin(7 * angle - phase) + 0.06 * Math.sin(11 * angle));
                double d = Math.hypot(dx, dz) / rim;
                if (d > 1) continue;
                // a bite out of the rim: a split crag's two halves face each other across it
                if (c.gapWidth > 0 && d > 0.35 && Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - c.gapAngle))) < Math.toDegrees(c.gapWidth) / 2) continue;
                int top = c.y + (int) Math.round(c.dome * (1 - d * d) + (r.nextDouble() < 0.12 ? 1 : 0) + (Math.abs(c.dome) >= 2 ? 1.5 : 0.5) * Math.sin(dx * 0.5 + phase) * Math.cos(dz * 0.45));
                // the taper: the crag keeps its width a way down, then draws in; the exponent says how far
                double keep = Math.max(0, 1 - Math.pow(d, c.taper));
                int depth = (int) Math.round(c.height * keep * (0.85 + 0.3 * r.nextDouble()));
                int bottom = top - depth;
                for (int y = bottom; y <= top; y++) {
                    int lean = top - y;
                    BlockPos pos = new BlockPos(c.x + dx + (int) Math.round(c.leanX * lean), y, c.z + dz + (int) Math.round(c.leanZ * lean));
                    BlockState state;
                    boolean face = d > 0.72 || y - bottom < 2;
                    int band = Math.floorMod((y - c.y) / style.bandEvery(), 5);
                    if (y == top) state = grass;
                    else if (y >= top - 2) state = soil;
                    else if (face && r.nextFloat() < style.moss()) state = moss;
                    else if (band == 3 && r.nextFloat() < style.bandChance()) state = cobble;
                    else if (band == 1 && r.nextFloat() < style.accentChance()) state = agate;
                    else if (r.nextFloat() < 0.03F) state = ore;
                    else state = stone;
                    solid.put(pos, state);
                }
                tops.put(BlockPos.asLong(c.x + dx, 0, c.z + dz), top);
                this.top = Math.max(this.top, top);
                this.bottom = Math.min(this.bottom, bottom);
                // vines: leaves clinging to the face just outside the rim, strands falling from them and from the underside
                if (d > 0.82 && r.nextFloat() < style.cling()) {
                    BlockPos cling = new BlockPos(c.x + (int) Math.round(dx * 1.12), top - 1 - r.nextInt(3), c.z + (int) Math.round(dz * 1.12));
                    if (!solid.containsKey(cling)) {
                        solid.put(cling, leaf);
                        hang(cling.below(), style.strandMin() + r.nextInt(style.strandMax() - style.strandMin() + 1), r);
                    }
                }
                if (d > 0.25 && r.nextFloat() < style.under()) {
                    int lean = top - bottom + 1;
                    BlockPos under = new BlockPos(c.x + dx + (int) Math.round(c.leanX * lean), bottom - 1, c.z + dz + (int) Math.round(c.leanZ * lean));
                    solid.putIfAbsent(under, leaf);
                    hang(under.below(), style.strandMin() + r.nextInt(style.strandMax() - style.strandMin() + 1), r);
                }
            }
            // boulders and a few tall stones on the crown; turf growth
            for (var e : tops.entrySet()) {
                BlockPos spot = BlockPos.of(e.getKey()).atY(e.getValue() + 1);
                float f = r.nextFloat();
                if (f < 0.06F && !solid.containsKey(spot)) solid.put(spot, palette.plants()[(int) (f / 0.06F * palette.plants().length)].defaultBlockState());
                else if (f < 0.07F) { solid.put(spot, cobble); if (r.nextBoolean()) solid.put(spot.above(), moss); }
            }
            // trees on the main crag; a single one on the lesser
            int trees = c.radius >= 5 ? c.trees : 0;
            for (int i = 0; i < trees; i++) {
                int dx = (int) ((r.nextDouble() - 0.5) * c.radius * 1.2), dz = (int) ((r.nextDouble() - 0.5) * c.radius * 1.2);
                Integer top = tops.get(BlockPos.asLong(c.x + dx, 0, c.z + dz));
                if (top == null) continue;
                var shape = palette.trees()[r.nextInt(3) == 0 && palette.trees().length > 1 ? 1 : 0];
                MarchTreeFeature.Plan tree = MarchTreeFeature.grow(shape, new BlockPos(c.x + dx, top + 1, c.z + dz), r.nextLong(), 1.0);
                BlockState tlog = tree.set.log.get().defaultBlockState();
                for (var e : tree.logs.entrySet()) if (!solid.containsKey(e.getKey())) solid.put(e.getKey(), tlog.setValue(RotatedPillarBlock.AXIS, e.getValue()));
                for (var e : tree.distance.entrySet()) if (!solid.containsKey(e.getKey())) solid.put(e.getKey(), tree.leaves.get(e.getKey()).setValue(LeavesBlock.DISTANCE, e.getValue()));
                this.top = Math.max(this.top, tree.top);
            }
            // the old world on the main crag: a broken ring of bricks round a chest
            if (c.ruin && c.radius >= 6) {
                BlockState bricks = MarchBuilding.STONE_BRICKS.get().defaultBlockState();
                int ring = Math.min(4, c.radius / 3);
                for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                    boolean edge = Math.abs(dx) == ring || Math.abs(dz) == ring;
                    Integer top = tops.get(BlockPos.asLong(c.x + dx, 0, c.z + dz));
                    if (top == null) continue;
                    BlockPos floor = new BlockPos(c.x + dx, top, c.z + dz);
                    solid.put(floor, edge ? bricks : cobble);
                    solid.remove(floor.above());
                    if (edge && r.nextFloat() < 0.65F) {
                        solid.put(floor.above(), bricks);
                        if (r.nextFloat() < 0.5F) solid.put(floor.above(2), r.nextFloat() < 0.15F ? Blocks.LANTERN.defaultBlockState() : bricks);
                    }
                }
                Integer centreTop = tops.get(BlockPos.asLong(c.x, 0, c.z));
                if (centreTop != null) solid.put(new BlockPos(c.x, centreTop + 1, c.z), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            }
            // a spring on the crown that runs off the rim: a pool a few blocks in, a notch cut to the edge
            if (c.radius >= 7) {
                double angle = r.nextDouble() * Mth.TWO_PI;
                double reach = c.dome < 0 ? 0.0 : 0.55;   // a bowl keeps its lake in the hollow
                int px = c.x + (int) (Math.cos(angle) * c.radius * reach), pz = c.z + (int) (Math.sin(angle) * c.radius * reach);
                Integer ptop = tops.get(BlockPos.asLong(px, 0, pz));
                if (ptop != null) {
                    int level = ptop, pool = c.dome < 0 ? (int) (c.radius * 0.35) : c.radius >= 20 ? 3 : 1;   // a mesa holds a lake, a spire a spring
                    for (int dx = -pool; dx <= pool; dx++) for (int dz = -pool; dz <= pool; dz++) {
                        if (dx * dx + dz * dz > pool * pool + 1) continue;
                        BlockPos at = new BlockPos(px + dx, level, pz + dz);
                        if (!solid.containsKey(at)) continue;
                        solid.put(at, Blocks.WATER.defaultBlockState());
                        for (int k = 1; k <= 4; k++) solid.remove(at.above(k));
                        solid.put(at.below(), stone);
                    }
                    // the notch: a channel from the pool out past the rim, stepping down with the crown, so the water finds the drop
                    int channel = level;
                    for (int k = 2; k < c.radius + 2; k++) {
                        int x = px + (int) Math.round(Math.cos(angle) * k), z = pz + (int) Math.round(Math.sin(angle) * k);
                        Integer there = tops.get(BlockPos.asLong(x, 0, z));
                        if (there == null) break;
                        channel = Math.min(channel, there);
                        BlockPos at = new BlockPos(x, channel, z);
                        solid.put(at, Blocks.WATER.defaultBlockState());
                        for (int up = 1; up <= 4; up++) solid.remove(at.above(up));
                        solid.put(at.below(), stone);
                    }
                }
            }
        }

        /** A drifting rock: a lump of banded stone with moss, sometimes a strand trailing from it. */
        private void rock(BlockPos at, int radius, RandomSource r) {
            for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dy * dy * 1.3 + dz * dz > radius * radius + 0.5) continue;
                BlockPos pos = at.offset(dx, dy, dz);
                if (solid.containsKey(pos)) continue;
                solid.put(pos, dy == radius ? (r.nextBoolean() ? moss : grass) : r.nextFloat() < 0.2F ? cobble : stone);
            }
            if (r.nextFloat() < 0.5F) {
                BlockPos under = at.below(radius + 1);
                solid.putIfAbsent(under.above(), leaf);
                hang(under, 3 + r.nextInt(6), r);
            }
        }

        /** A giant vine or root of willow wood along a curve, sheathed in leaves, hung with strands. */
        private void vine(Vec3 start, Vec3 end, double r0, double r1, RandomSource r, double sag) {
            Vec3 mid = start.add(end).scale(0.5);
            Vec3 control = sag != 0 ? mid.add(0, -start.distanceTo(end) * sag, 0) : mid;
            int segments = Math.max(8, (int) (start.distanceTo(end) / 2));
            Vec3 previous = start;
            List<BlockPos> sheath = new ArrayList<>();
            for (int i = 1; i <= segments; i++) {
                double t = i / (double) segments, u = 1 - t;
                Vec3 at = start.scale(u * u).add(control.scale(2 * u * t)).add(end.scale(t * t));
                double rad = Mth.lerp(t, r0, r1) * (0.9 + 0.2 * Math.sin(i * 1.3));
                Vec3 dir = at.subtract(previous);
                Direction.Axis axis = Math.abs(dir.y) >= Math.max(Math.abs(dir.x), Math.abs(dir.z)) ? Direction.Axis.Y
                        : Math.abs(dir.x) >= Math.abs(dir.z) ? Direction.Axis.X : Direction.Axis.Z;
                int R = Mth.ceil(rad + 1);
                for (int dx = -R; dx <= R; dx++) for (int dy = -R; dy <= R; dy++) for (int dz = -R; dz <= R; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockPos pos = BlockPos.containing(at.x + dx, at.y + dy, at.z + dz);
                    if (dist <= rad) {
                        BlockState there = solid.get(pos);
                        if (there == null || there.getBlock() instanceof LeavesBlock) solid.put(pos, log.setValue(RotatedPillarBlock.AXIS, axis));
                    } else if (dist <= rad + 1 && r.nextFloat() < 0.5F && !solid.containsKey(pos)) {
                        solid.put(pos, leaf);
                        sheath.add(pos);
                    }
                }
                previous = at;
            }
            for (BlockPos pos : sheath) if (r.nextFloat() < 0.3F) hang(pos.below(), 2 + r.nextInt(7), r);
        }

        /** A strand chain of up to {@code length} from {@code from} down, stopping at anything solid in the plan. */
        void hang(BlockPos from, int length, RandomSource r) {
            BlockPos.MutableBlockPos cursor = from.mutable();
            int placed = 0;
            while (placed < length && !solid.containsKey(cursor)) {
                strands.put(cursor.immutable(), strand.setValue(WillowStrandBlock.TIP, false));
                placed++;
                cursor.move(Direction.DOWN);
            }
            if (placed > 0) {
                cursor.move(Direction.UP);
                // a glow here and there, and never more than a cluster's share: light is what costs a chunk, and a
                // cluster hung with long curtains has thousands of chains (the random draw stays, so the layout does not move)
                boolean glow = r.nextFloat() < 0.02F && glows < GLOW_BUDGET;
                if (glow) glows++;
                strands.put(cursor.immutable(), strand.setValue(WillowStrandBlock.TIP, true).setValue(WillowStrandBlock.GLOW, glow));
            }
        }

        public void write(WorldGenLevel level, java.util.function.Predicate<BlockPos> writable) {
            int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            for (var e : solid.entrySet()) {
                BlockPos pos = e.getKey();
                if (!writable.test(pos)) continue;
                // the rock overwrites whatever is there; a plant only stands in air
                if (e.getValue().getBlock() instanceof net.minecraft.world.level.block.BushBlock && !level.getBlockState(pos).isAir()) continue;
                level.setBlock(pos, e.getValue(), flags);
                if (e.getValue().is(Blocks.WATER)) level.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER, 5);
                if (e.getValue().is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                    chest.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tribalpower", "chests/ancestor_hall")), level.getSeed() ^ pos.asLong());
            }
            // strands from the top down, so each hangs from what holds it; only this chunk's share is sorted
            strands.entrySet().stream().filter(e -> writable.test(e.getKey()))
                    .sorted((a, b) -> Integer.compare(b.getKey().getY(), a.getKey().getY())).forEach(e -> {
                BlockPos pos = e.getKey();
                BlockState there = level.getBlockState(pos);
                if (!there.isAir() && !there.canBeReplaced()) return;
                if (!WillowStrandBlock.holds(level.getBlockState(pos.above()))) return;
                level.setBlock(pos, e.getValue(), flags);
            });
        }
    }

    // ------------------------------------------------------------------ the piece

    public static final class Island extends StructurePiece {
        private final BlockPos anchor;
        private final int centre, radius;
        private final long seed;
        private final List<BlockPos> targets;
        private final String biome;

        public Island(BlockPos anchor, int centre, int radius, long seed, List<BlockPos> targets, String biome) {
            super(MarchStructures.ISLAND.get(), 0, box(anchor, centre, targets));
            this.anchor = anchor; this.centre = centre; this.radius = radius; this.seed = seed; this.targets = targets; this.biome = biome;
        }

        private static BoundingBox box(BlockPos anchor, int centre, List<BlockPos> targets) {
            int low = targets.stream().mapToInt(BlockPos::getY).min().orElse(anchor.getY()) - 6;
            return new BoundingBox(anchor.getX() - REACH, Math.min(low, centre - 90), anchor.getZ() - REACH, anchor.getX() + REACH, centre + 90, anchor.getZ() + REACH);
        }

        public Island(CompoundTag tag) {
            super(MarchStructures.ISLAND.get(), tag);
            this.anchor = BlockPos.of(tag.getLong("Anchor"));
            this.centre = tag.getInt("Centre");
            this.radius = tag.getInt("Radius");
            this.seed = tag.getLong("Seed");
            this.targets = new ArrayList<>();
            for (long l : tag.getLongArray("Targets")) targets.add(BlockPos.of(l));
            this.biome = tag.contains("Biome") ? tag.getString("Biome") : "march_steppe";
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong("Anchor", anchor.asLong());
            tag.putInt("Centre", centre);
            tag.putInt("Radius", radius);
            tag.putLong("Seed", seed);
            tag.putLongArray("Targets", targets.stream().mapToLong(BlockPos::asLong).toArray());
            tag.putString("Biome", biome);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
            plan(anchor, centre, radius, seed, targets, biome).write(level, pos -> chunkBox.isInside(pos) && !level.isOutsideBuildHeight(pos));
        }
    }
}
