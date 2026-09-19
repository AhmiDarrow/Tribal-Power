package tk.darrow.tribalpower.world;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;

/**
 * The March building set: stairs, slabs and walls for its stones, cut and chiseled stone bricks, and a full
 * wood set for March planks. Models, recipes, loot and tags come from tools/generate_march_building.py.
 */
public final class MarchBuilding {
    /** Every building block's item, in creative-tab order. */
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();

    public static final DeferredBlock<Block> STONE_BRICKS = cube("march_stone_bricks", ModBlocks.MARCH_STONE);
    public static final DeferredBlock<Block> CHISELED_STONE_BRICKS = cube("chiseled_march_stone_bricks", ModBlocks.MARCH_STONE);
    public static final DeferredBlock<Block> POLISHED_STONE = cube("polished_march_stone", ModBlocks.MARCH_STONE);

    static {
        stoneSet("march_stone", ModBlocks.MARCH_STONE, true);
        stoneSet("march_cobble", ModBlocks.MARCH_COBBLE, true);
        stoneSet("march_stone_brick", STONE_BRICKS, true);
        stoneSet("polished_march_stone", POLISHED_STONE, false);

        Supplier<BlockBehaviour.Properties> wood = () -> BlockBehaviour.Properties.ofFullCopy(ModBlocks.MARCH_PLANKS.get());
        stairsAndSlab("march_planks", ModBlocks.MARCH_PLANKS);
        block("march_fence", () -> new FenceBlock(wood.get()));
        block("march_fence_gate", () -> new FenceGateBlock(WoodType.OAK, wood.get().forceSolidOn()));
        door();
        block("march_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, wood.get().noOcclusion()
                .isValidSpawn((s, l, p, e) -> false)));
        block("march_pressure_plate", () -> new PressurePlateBlock(BlockSetType.OAK, wood.get().forceSolidOn()
                .noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
        block("march_button", () -> new ButtonBlock(BlockSetType.OAK, 30, wood.get().noCollission().strength(0.5F)
                .pushReaction(PushReaction.DESTROY)));
    }

    private MarchBuilding() {}

    public static void init() {}

    private static void stoneSet(String prefix, DeferredBlock<? extends Block> base, boolean wall) {
        stairsAndSlab(prefix, base);
        if (wall) block(prefix + "_wall", () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(base.get()).forceSolidOn()));
    }

    private static void stairsAndSlab(String prefix, DeferredBlock<? extends Block> base) {
        block(prefix + "_stairs", () -> new StairBlock(base.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base.get())));
        block(prefix + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base.get())));
    }

    private static DeferredBlock<Block> cube(String id, DeferredBlock<? extends Block> like) {
        return block(id, () -> new Block(BlockBehaviour.Properties.ofFullCopy(like.get())));
    }

    private static <B extends Block> DeferredBlock<B> block(String id, Supplier<B> factory) {
        DeferredBlock<B> block = ModBlocks.BLOCKS.register(id, factory);
        ITEMS.put(id, ModItems.ITEMS.registerSimpleBlockItem(id, block));
        return block;
    }

    private static void door() {
        String id = "march_door";
        DeferredBlock<DoorBlock> block = ModBlocks.BLOCKS.register(id, () -> new DoorBlock(BlockSetType.OAK,
                BlockBehaviour.Properties.ofFullCopy(ModBlocks.MARCH_PLANKS.get()).noOcclusion().pushReaction(PushReaction.DESTROY)));
        ITEMS.put(id, ModItems.ITEMS.register(id, () -> new DoubleHighBlockItem(block.get(), new Item.Properties())));
    }
}
