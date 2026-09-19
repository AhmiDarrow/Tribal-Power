package tk.darrow.tribalpower.world;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A grove of two to eight Weeping Colossi. Each tree is far too big for a single worldgen feature, so each is
 * a structure piece: its plan depends only on its origin and seed, and every chunk it overlaps writes its own
 * share. Trees stand at least 44 blocks apart and the grove stays within reach of its start chunk.
 */
public class WillowGroveStructure extends Structure {
    public static final MapCodec<WillowGroveStructure> CODEC = simpleCodec(WillowGroveStructure::new);
    private static final int REACH = 48, RISE = 175, SINK = 16;

    public WillowGroveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return MarchStructures.WILLOW_GROVE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        int wanted = 2 + random.nextInt(7);
        List<BlockPos> trees = new ArrayList<>();
        for (int attempt = 0; attempt < 48 && trees.size() < wanted; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI, distance = trees.isEmpty() ? 0 : 30 + random.nextDouble() * 40;
            int x = cx + (int) (Math.cos(angle) * distance), z = cz + (int) (Math.sin(angle) * distance);
            if (trees.stream().anyMatch(t -> Mth.square(t.getX() - x) + Mth.square(t.getZ() - z) < 44 * 44)) continue;
            int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            if (surface - floor > 3 || floor + RISE >= context.heightAccessor().getMaxBuildHeight()) continue;
            // Every tree, not only the first, must stand in a grove biome.
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x),
                    net.minecraft.core.QuartPos.fromBlock(floor), net.minecraft.core.QuartPos.fromBlock(z), context.randomState().sampler()))) continue;
            trees.add(new BlockPos(x, floor + 1, z));
        }
        if (trees.size() < 2) return Optional.empty();
        long[] seeds = trees.stream().mapToLong(t -> random.nextLong()).toArray();
        float[] scales = new float[trees.size()];
        for (int i = 0; i < scales.length; i++) scales[i] = 0.85F + random.nextFloat() * 0.25F;
        return Optional.of(new GenerationStub(trees.getFirst(), builder -> {
            for (int i = 0; i < trees.size(); i++) builder.addPiece(new Willow(trees.get(i), seeds[i], scales[i]));
        }));
    }

    /**
     * The few colossus plans in use, shared by every chunk that writes a share of them. A plan is several MB, so
     * only a handful are kept; a plan is built in full before it is published, so parallel chunks never see half.
     */
    private static final java.util.LinkedHashMap<String, MarchTreeFeature.Plan> PLANS = new java.util.LinkedHashMap<>(8, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, MarchTreeFeature.Plan> eldest) {
            return size() > 12;
        }
    };

    static MarchTreeFeature.Plan plan(BlockPos origin, long seed, float scale) {
        String key = origin.asLong() + ":" + seed + ":" + scale;
        synchronized (PLANS) {
            MarchTreeFeature.Plan plan = PLANS.get(key);
            if (plan != null) return plan;
        }
        MarchTreeFeature.Plan plan = MarchTreeFeature.grow(MarchTreeFeature.Shape.WEEPING_COLOSSUS, origin, seed, scale);
        synchronized (PLANS) {
            PLANS.putIfAbsent(key, plan);
            return PLANS.get(key);
        }
    }

    public static final class Willow extends StructurePiece {
        private final BlockPos origin;
        private final long seed;
        private final float scale;

        Willow(BlockPos origin, long seed, float scale) {
            super(MarchStructures.WILLOW.get(), 0, new BoundingBox(origin.getX() - REACH, origin.getY() - SINK, origin.getZ() - REACH,
                    origin.getX() + REACH, origin.getY() + RISE, origin.getZ() + REACH));
            this.origin = origin;
            this.seed = seed;
            this.scale = scale;
        }

        public Willow(CompoundTag tag) {
            super(MarchStructures.WILLOW.get(), tag);
            this.origin = BlockPos.of(tag.getLong("Origin"));
            this.seed = tag.getLong("Seed");
            this.scale = tag.getFloat("Scale");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong("Origin", origin.asLong());
            tag.putLong("Seed", seed);
            tag.putFloat("Scale", scale);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
            plan(origin, seed, scale).write(level, pos -> chunkBox.isInside(pos) && !level.isOutsideBuildHeight(pos));
        }
    }
}
