package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.TribalBenchBlock;
import tk.darrow.tribalpower.blockentity.CampDisplayBlockEntity;
import tk.darrow.tribalpower.blockentity.TribalBenchBlockEntity;

/** The Tribal Bench: two halves, a grid that stays put, and a shelf along the back. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class BenchGameTests {

    /**
     * The whole reason the bench exists: what is left in the grid is still there later.
     *
     * <p>A vanilla crafting table throws its grid on the floor as the screen closes. The bench keeps
     * the nine places in the block entity, so they survive a save and a load.
     */
    @GameTest(template="empty")
    public static void theGridKeepsWhatIsLeftOnIt(GameTestHelper h) {
        BlockPos pos = place(h, new BlockPos(2, 2, 2));
        var bench = h.getBlockEntity(pos) instanceof TribalBenchBlockEntity b ? b : null;
        h.assertTrue(bench != null, "The left half must carry the bench entity");

        bench.grid().set(4, new ItemStack(Items.IRON_INGOT, 3));
        bench.grid().set(0, new ItemStack(Items.STICK));
        bench.gridChanged();

        // Round-trip it the way a chunk save and load would.
        var registries = h.getLevel().registryAccess();
        var tag = bench.saveWithFullMetadata(registries);
        var reloaded = new TribalBenchBlockEntity(h.absolutePos(pos), h.getBlockState(pos));
        reloaded.loadWithComponents(tag, registries);

        h.assertTrue(reloaded.grid().get(4).is(Items.IRON_INGOT) && reloaded.grid().get(4).getCount() == 3,
                "The middle place must come back with its three ingots");
        h.assertTrue(reloaded.grid().get(0).is(Items.STICK), "And the corner with its stick");
        h.assertTrue(reloaded.grid().get(8).isEmpty(), "Empty places stay empty");
        h.succeed();
    }

    /** Placing makes both halves, and they agree which one holds the inventory. */
    @GameTest(template="empty")
    public static void theBenchIsTwoHalvesThatShareOneInventory(GameTestHelper h) {
        BlockPos left = place(h, new BlockPos(2, 2, 2));
        var state = h.getBlockState(left);
        BlockPos right = TribalBenchBlock.otherHalf(state, left);
        h.assertBlockPresent(oak(), right);
        h.assertTrue(h.getBlockState(right).getValue(TribalBenchBlock.PART) == TribalBenchBlock.Part.RIGHT,
                "The second block must be the right half");
        h.assertTrue(h.getBlockState(right).getValue(TribalBenchBlock.FACING)
                        == state.getValue(TribalBenchBlock.FACING),
                "Both halves must face the same way");
        // Either half must point at the same inventory, or the two ends would hold different grids.
        h.assertTrue(TribalBenchBlock.head(state, left)
                        .equals(TribalBenchBlock.head(h.getBlockState(right), right)),
                "Both halves must open the same bench");
        h.succeed();
    }

    /** Break one half and the other goes too, rather than leaving half a bench standing. */
    @GameTest(template="empty")
    public static void breakingOneHalfTakesTheOther(GameTestHelper h) {
        BlockPos left = place(h, new BlockPos(2, 2, 2));
        BlockPos right = TribalBenchBlock.otherHalf(h.getBlockState(left), left);
        h.setBlock(right, Blocks.AIR);
        h.succeedWhen(() -> h.assertBlockNotPresent(oak(), left));
    }

    /**
     * The back board holds items like any other shelf, and the crafting grid is not part of the
     * container, so a hopper cannot reach into a half-built recipe.
     */
    @GameTest(template="empty")
    public static void theShelfHoldsItemsAndTheGridIsNotAContainer(GameTestHelper h) {
        BlockPos pos = place(h, new BlockPos(2, 2, 2));
        var bench = (TribalBenchBlockEntity) h.getBlockEntity(pos);
        var state = h.getBlockState(pos);

        h.assertTrue(CampDisplayBlockEntity.capacityOf(state) == 2,
                "Each half carries two of the bench's four shelf places");
        ItemStack left = bench.place(0, new ItemStack(Items.BRICK, 4));
        h.assertTrue(left.getCount() == 3, "A shelf takes one item at a time, left " + left.getCount());
        h.assertTrue(bench.at(0).is(Items.BRICK), "And holds it");

        // The container view is the shelf only: its size must not include the nine crafting places.
        h.assertTrue(bench.getContainerSize() == 2,
                "The container is the shelf, not the grid; saw " + bench.getContainerSize());
        for (Direction face : Direction.values())
            h.assertTrue(bench.getSlotsForFace(face).length == 0,
                    "No face may be open to a hopper, " + face + " was");
        h.succeed();
    }

    /** One representative wood; the family test below checks the rest are all there. */
    private static TribalBenchBlock oak() {
        return ModBlocks.TRIBAL_BENCHES.get("oak").get();
    }

    /**
     * Every wood must actually be registered, with its block, its item and its place in the family.
     *
     * <p>The bench comes in eleven vanilla woods and the seven the March grows. They are generated
     * from one list, so the way this breaks is a wood being added to the list and its assets never
     * being built -- which the asset gates catch -- or the list and the registry drifting apart.
     */
    @GameTest(template="empty")
    public static void everyWoodHasItsBench(GameTestHelper h) {
        var woods = tk.darrow.tribalpower.bench.BenchWoods.ALL;
        h.assertTrue(woods.size() == 18, "Eleven vanilla woods and seven March ones, found " + woods.size());
        for (var wood : woods) {
            var block = ModBlocks.TRIBAL_BENCHES.get(wood.id());
            h.assertTrue(block != null && block.get() instanceof TribalBenchBlock,
                    wood.id() + " has no bench block");
            h.assertTrue(tk.darrow.tribalpower.item.ModItems.TRIBAL_BENCHES.get(wood.id()) != null,
                    wood.id() + " has no bench item");
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    "tribalpower", wood.block())),
                    wood.block() + " is not in the block registry");
        }
        h.succeed();
    }

    /** Places a bench facing north and returns the left half. */
    private static BlockPos place(GameTestHelper h, BlockPos pos) {
        var state = oak().defaultBlockState()
                .setValue(TribalBenchBlock.FACING, Direction.NORTH)
                .setValue(TribalBenchBlock.PART, TribalBenchBlock.Part.LEFT);
        h.setBlock(pos, state);
        h.setBlock(TribalBenchBlock.otherHalf(state, pos),
                state.setValue(TribalBenchBlock.PART, TribalBenchBlock.Part.RIGHT));
        return pos;
    }
}
