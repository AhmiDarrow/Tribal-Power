package tk.darrow.tribalpower.world;

import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;

/**
 * What grows in the March's water and caves: lily pads and moon lilies on its meres, ribbon weed under them and
 * silt on their beds; veil lichen, echo roots and lantern caps underground. Placed by the worldgen in
 * tools/generate_march_worldgen.py; art from tools/paint_march_decor.py.
 */
public final class MarchDecor {
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();

    public static final DeferredBlock<WaterlilyBlock> LILY_PAD = block("march_lily_pad", () -> new WaterlilyBlock(plant(MapColor.PLANT)
            .instabreak().sound(SoundType.LILY_PAD)), b -> new PlaceOnWaterBlockItem(b, new Item.Properties()));
    public static final DeferredBlock<WaterlilyBlock> MOON_LILY = block("moon_lily", () -> new WaterlilyBlock(plant(MapColor.COLOR_LIGHT_BLUE)
            .instabreak().sound(SoundType.LILY_PAD).lightLevel(s -> 6)), b -> new PlaceOnWaterBlockItem(b, new Item.Properties()));
    public static final DeferredBlock<RibbonWeedBlock> RIBBON_WEED = block("ribbon_weed", () -> new RibbonWeedBlock(plant(MapColor.WATER)
            .replaceable().noCollission().instabreak().sound(SoundType.WET_GRASS)), b -> new BlockItem(b, new Item.Properties()));
    public static final DeferredBlock<Block> SILT = block("march_silt", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_CYAN).strength(0.6F).sound(SoundType.MUD)), b -> new BlockItem(b, new Item.Properties()));
    public static final DeferredBlock<GlowLichenBlock> VEIL_LICHEN = block("veil_lichen", () -> new GlowLichenBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN).replaceable().noCollission().strength(0.2F).sound(SoundType.GLOW_LICHEN)
            .lightLevel(GlowLichenBlock.emission(4)).ignitedByLava().pushReaction(PushReaction.DESTROY)), b -> new BlockItem(b, new Item.Properties()));
    public static final DeferredBlock<HangingRootsBlock> ECHO_ROOTS = block("echo_roots", () -> new HangingRootsBlock(plant(MapColor.COLOR_PURPLE)
            .replaceable().noCollission().instabreak().sound(SoundType.HANGING_ROOTS).offsetType(BlockBehaviour.OffsetType.XZ)),
            b -> new BlockItem(b, new Item.Properties()));
    public static final DeferredBlock<LanternCapBlock> LANTERN_CAP = block("lantern_cap", () -> new LanternCapBlock(plant(MapColor.COLOR_ORANGE)
            .noCollission().instabreak().sound(SoundType.FUNGUS).lightLevel(s -> 6).offsetType(BlockBehaviour.OffsetType.XZ)),
            b -> new BlockItem(b, new Item.Properties()));

    private MarchDecor() {}

    public static void init() {}

    private static BlockBehaviour.Properties plant(MapColor colour) {
        return BlockBehaviour.Properties.of().mapColor(colour).pushReaction(PushReaction.DESTROY);
    }

    private static <B extends Block> DeferredBlock<B> block(String id, Supplier<B> factory, Function<B, Item> item) {
        DeferredBlock<B> block = ModBlocks.BLOCKS.register(id, factory);
        ITEMS.put(id, ModItems.ITEMS.register(id, () -> item.apply(block.get())));
        return block;
    }

    /** Tall underwater weed rooted in the bed of a mere; like seagrass, it only lives submerged. */
    public static class RibbonWeedBlock extends BushBlock implements LiquidBlockContainer {
        public static final MapCodec<RibbonWeedBlock> CODEC = simpleCodec(RibbonWeedBlock::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

        public RibbonWeedBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.isFaceSturdy(level, pos, Direction.UP) && !state.is(Blocks.MAGMA_BLOCK);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
            return fluid.is(FluidTags.WATER) && fluid.getAmount() == 8 ? super.getStateForPlacement(context) : null;
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
            BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
            if (!updated.isAir()) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return updated;
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return Fluids.WATER.getSource(false);
        }

        @Override
        public boolean canPlaceLiquid(net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
            return false;
        }

        @Override
        public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
            return false;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return super.canSurvive(state, level, pos) && level.getFluidState(pos).is(FluidTags.WATER);
        }
    }

    /** A small glowing mushroom of cave floors; it grows on any solid stone or soil, in the dark. */
    public static class LanternCapBlock extends BushBlock {
        public static final MapCodec<LanternCapBlock> CODEC = simpleCodec(LanternCapBlock::new);
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

        public LanternCapBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE.move(state.getOffset(level, pos).x, 0, state.getOffset(level, pos).z);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.isFaceSturdy(level, pos, Direction.UP);
        }
    }
}
