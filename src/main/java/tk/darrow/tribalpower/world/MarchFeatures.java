package tk.darrow.tribalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;

/** Code-driven worldgen features for The March. */
public final class MarchFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TribalPower.MOD_ID);

    public static final DeferredHolder<Feature<?>, AmethystPocket> AMETHYST_POCKET =
            FEATURES.register("amethyst_pocket", () -> new AmethystPocket(NoneFeatureConfiguration.CODEC));

    private MarchFeatures() {}

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }

    /**
     * A small amethyst seam in a cave wall: a few amethyst blocks set into the rock around an air pocket, with
     * buds and clusters growing from whichever faces open onto the cave. No budding amethyst, so nothing ticks.
     */
    public static final class AmethystPocket extends Feature<NoneFeatureConfiguration> {
        private static final Block[] GROWTH = {Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD,
                Blocks.LARGE_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER};

        public AmethystPocket(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            var random = context.random();
            if (!level.getBlockState(origin).isAir()) return false;
            int radius = 2 + random.nextInt(2);
            var seam = new java.util.ArrayList<BlockPos>();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
                if (pos.distSqr(origin) > radius * radius + 0.5 || !isHost(level.getBlockState(pos))) continue;
                if (random.nextFloat() < 0.35F || !touchesAir(level, pos)) continue;
                level.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                seam.add(pos.immutable());
            }
            if (seam.size() < 3) return !seam.isEmpty();
            for (BlockPos pos : seam) {
                for (Direction face : Direction.values()) {
                    BlockPos out = pos.relative(face);
                    if (!level.getBlockState(out).isAir() || random.nextFloat() > 0.4F) continue;
                    // Mostly buds; a full cluster is the find.
                    int size = random.nextFloat() < 0.15F ? 3 : random.nextInt(3);
                    BlockState growth = GROWTH[size].defaultBlockState().setValue(AmethystClusterBlock.FACING, face);
                    level.setBlock(out, growth, Block.UPDATE_CLIENTS);
                }
            }
            return true;
        }

        private static boolean isHost(BlockState state) {
            return state.is(ModBlocks.MARCH_STONE.get()) || state.is(ModBlocks.MARCH_COBBLE.get())
                    || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF);
        }

        private static boolean touchesAir(WorldGenLevel level, BlockPos pos) {
            for (Direction face : Direction.values()) if (level.getBlockState(pos.relative(face)).isAir()) return true;
            return false;
        }
    }
}
