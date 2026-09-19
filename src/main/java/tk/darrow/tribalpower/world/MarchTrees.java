package tk.darrow.tribalpower.world;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.WillowStrandBlock;
import tk.darrow.tribalpower.item.ModItems;

/**
 * The feature that grows the March's own trees (see {@link MarchTreeFeature}) and the willow's hanging strand.
 * Each tree's wood, leaves and sapling live in {@link MarchWoods}.
 */
public final class MarchTrees {
    public static final DeferredBlock<WillowStrandBlock> WILLOW_STRAND = ModBlocks.BLOCKS.register("willow_strand", () -> new WillowStrandBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).noCollission().noOcclusion().replaceable()
                    .strength(0.2F).sound(SoundType.CAVE_VINES).pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(WillowStrandBlock.GLOW) ? 7 : 0)));
    public static final DeferredItem<BlockItem> WILLOW_STRAND_ITEM = ModItems.ITEMS.registerSimpleBlockItem("willow_strand", WILLOW_STRAND);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, MarchTreeFeature> FEATURE =
            MarchFeatures.FEATURES.register("march_tree", MarchTreeFeature::new);

    private MarchTrees() {}

    public static void init() {}
}
