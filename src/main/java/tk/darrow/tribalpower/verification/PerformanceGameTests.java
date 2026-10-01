package tk.darrow.tribalpower.verification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.TribalBenchBlock;
import tk.darrow.tribalpower.blockentity.LatticeConductorBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.familiar.FamiliarData;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * The caches added for tick time must never change an answer: each is checked against the work it saves.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class PerformanceGameTests {

    // ---- Tribal Bench shapes ----------------------------------------------------------------------------

    /** The bench outline is looked up, not rebuilt, and is still the table, its legs and the shelf at the back. */
    @GameTest(template = "empty")
    public static void benchShapesAreBuiltOnceAndUnchanged(GameTestHelper h) {
        TribalBenchBlock bench = null;
        for (Block block : BuiltInRegistries.BLOCK) if (block instanceof TribalBenchBlock found) { bench = found; break; }
        h.assertTrue(bench != null, "A Tribal Bench must be registered");
        VoxelShape table = Shapes.or(Block.box(0, 6, 0, 16, 10, 16), Block.box(1, 0, 1, 4, 6, 4), Block.box(12, 0, 1, 15, 6, 4),
                Block.box(1, 0, 12, 4, 6, 15), Block.box(12, 0, 12, 15, 6, 15));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shelf = switch (facing) {
                case SOUTH -> Shapes.or(Block.box(0, 10, 2, 16, 16, 4), Block.box(0, 14, 1, 16, 16, 5));
                case WEST -> Shapes.or(Block.box(12, 10, 0, 14, 16, 16), Block.box(11, 14, 0, 15, 16, 16));
                case EAST -> Shapes.or(Block.box(2, 10, 0, 4, 16, 16), Block.box(1, 14, 0, 5, 16, 16));
                default -> Shapes.or(Block.box(0, 10, 12, 16, 16, 14), Block.box(0, 14, 11, 16, 16, 15));
            };
            VoxelShape expected = Shapes.or(table, shelf);
            for (TribalBenchBlock.Part part : TribalBenchBlock.Part.values()) {
                BlockState state = bench.defaultBlockState().setValue(TribalBenchBlock.FACING, facing).setValue(TribalBenchBlock.PART, part);
                VoxelShape first = state.getShape(h.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                VoxelShape again = state.getShape(h.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                h.assertTrue(first == again, "The " + facing + " " + part + " outline must be the same object each call");
                h.assertTrue(first == TribalBenchBlock.shapeFor(facing), "Both halves share the " + facing + " outline");
                h.assertFalse(Shapes.joinIsNotEmpty(first, expected, BooleanOp.NOT_SAME),
                        "The " + facing + " outline must be the table, legs and shelf it always was");
                h.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(h.getLevel(), BlockPos.ZERO), expected, BooleanOp.NOT_SAME),
                        "Things walking into the " + facing + " bench meet the same outline");
            }
        }
        h.succeed();
    }

    // ---- station recipe index ---------------------------------------------------------------------------

    /** Every station, every input any written recipe names (plus a spread of other items): the index agrees with the full scan. */
    @GameTest(template = "empty")
    public static void recipeIndexAgreesWithTheFullScan(GameTestHelper h) {
        Level level = h.getLevel();
        ProcessingRecipes.invalidate();
        var recipes = level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get());
        h.assertTrue(!recipes.isEmpty(), "The station recipes must be loaded");
        Set<String> stations = new LinkedHashSet<>();
        for (var holder : recipes) stations.add(holder.value().station());
        stations.add("no_such_station");
        List<ItemStack> probes = probes(recipes.stream().map(holder -> holder.value().ingredient()).toList());
        int checked = 0, found = 0;
        for (String station : stations) {
            for (ItemStack probe : probes) {
                var indexed = ProcessingRecipes.findWritten(level, station, probe);
                var scanned = ProcessingRecipes.findWrittenUncached(level, station, probe);
                h.assertTrue(Objects.equals(id(indexed), id(scanned)),
                        station + " with " + probe + ": index gave " + id(indexed) + ", the full scan " + id(scanned));
                checked++;
                if (scanned != null) found++;
            }
        }
        h.assertTrue(found > 0, "Some written recipe must have matched, checked " + checked);
        h.assertTrue(ProcessingRecipes.indexed(level), "The lookups must have built the index");
        h.succeed();
    }

    /**
     * A recipe set that changes (a reload, a datapack, KubeJS) is seen by the very next lookup: a new recipe that
     * sorts first wins at once, and taking it away hands the input back to the recipe it had before.
     */
    @GameTest(template = "empty")
    public static void recipeIndexFollowsARecipeReload(GameTestHelper h) {
        Level level = h.getLevel();
        var manager = level.getRecipeManager();
        var recipes = manager.getAllRecipesFor(LatticeRecipe.TYPE.get());
        RecipeHolder<LatticeRecipe> victim = null;
        ItemStack input = ItemStack.EMPTY;
        for (var holder : recipes) {
            ItemStack[] items = holder.value().ingredient().getItems();
            if (items.length == 0 || items[0].isEmpty()) continue;
            var before = ProcessingRecipes.findWritten(level, holder.value().station(), items[0]);
            if (before != null && before.id().equals(holder.id())) { victim = holder; input = items[0].copy(); break; }
        }
        h.assertTrue(victim != null, "Some written recipe must own its first input");
        String station = victim.value().station();
        h.assertTrue(ProcessingRecipes.indexed(level), "The index is warm before the reload");

        // The whole swap happens inside this one call on the server thread, so no other check ever sees it.
        List<RecipeHolder<?>> original = List.copyOf(manager.getRecipes());
        var usurper = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("a", "index_check"),
                new LatticeRecipe(station, Ingredient.of(input.getItem()), new ItemStack(Items.DIAMOND, 7), 3, 1, Attunement.SPIRIT));
        String during, scannedDuring, after;
        boolean stale;
        try {
            List<RecipeHolder<?>> grown = new ArrayList<>(original);
            grown.add(usurper);
            manager.replaceRecipes(grown);
            stale = ProcessingRecipes.indexed(level);
            during = id(ProcessingRecipes.findWritten(level, station, input));
            scannedDuring = id(ProcessingRecipes.findWrittenUncached(level, station, input));
        } finally {
            manager.replaceRecipes(original);
        }
        after = id(ProcessingRecipes.findWritten(level, station, input));
        h.assertFalse(stale, "A replaced recipe set must not read as indexed");
        h.assertTrue(usurper.id().toString().equals(during), "The new first recipe must win at once, got " + during);
        h.assertTrue(Objects.equals(during, scannedDuring), "Index and scan agree after the reload: " + during + " / " + scannedDuring);
        h.assertTrue(victim.id().toString().equals(after), "Removing it hands the input back, got " + after);

        ProcessingRecipes.invalidate();
        h.assertFalse(ProcessingRecipes.indexed(level), "A tag reload drops the index");
        h.assertTrue(victim.id().toString().equals(id(ProcessingRecipes.findWritten(level, station, input))),
                "and the next lookup builds it again with the same answer");
        h.succeed();
    }

    /** The Ember Kiln's furnace lookup answers exactly as walking every smelting recipe did. */
    @GameTest(template = "empty")
    public static void kilnIndexAgreesWithTheFullScan(GameTestHelper h) {
        Level level = h.getLevel();
        ProcessingRecipes.invalidate();
        var smelting = level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        List<Ingredient> ingredients = new ArrayList<>();
        for (var holder : smelting) ingredients.addAll(holder.value().getIngredients());
        int found = 0;
        for (ItemStack probe : probes(ingredients)) {
            var indexed = ProcessingRecipes.kiln(level, probe);
            String scanned = null;
            var input = new SingleRecipeInput(probe);
            for (var holder : smelting) {
                if (!holder.value().matches(input, level)) continue;
                ItemStack output = holder.value().assemble(input, level.registryAccess());
                if (output.isEmpty()) output = holder.value().getResultItem(level.registryAccess()).copy();
                if (output.isEmpty()) continue;
                scanned = holder.id().toString();
                break;
            }
            h.assertTrue(Objects.equals(id(indexed), scanned), "Kiln with " + probe + ": index " + id(indexed) + ", scan " + scanned);
            if (scanned != null) found++;
        }
        h.assertTrue(found > 0, "Some smelting recipe must have matched");
        h.succeed();
    }

    /** Each ingredient's items (a damaged copy too, for ingredients that read components) and a spread of others. */
    private static List<ItemStack> probes(List<Ingredient> ingredients) {
        List<ItemStack> out = new ArrayList<>();
        Set<Item> seen = new java.util.HashSet<>();
        for (Ingredient ingredient : ingredients) {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.isEmpty()) continue;
                out.add(stack.copy());
                if (stack.isDamageableItem()) {
                    ItemStack worn = stack.copy();
                    worn.setDamageValue(Math.max(1, worn.getMaxDamage() / 2));
                    out.add(worn);
                }
                seen.add(stack.getItem());
            }
        }
        int step = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || seen.contains(item) || step++ % 7 != 0) continue;
            out.add(new ItemStack(item));
        }
        return out;
    }

    private static String id(ProcessingRecipes.Formula formula) {
        return formula == null ? null : formula.id().toString();
    }

    // ---- Lattice Conductor --------------------------------------------------------------------------------

    /** The conductor keeps its chalk network between beats, but a new link or a broken totem shows on the next one. */
    @GameTest(template = "empty")
    public static void conductorSeesChalkChangesOnTheNextBeat(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(4, 2, 4, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth = at(h, new BlockPos(2, 2, 4), ResonanceTotemBlockEntity.class);
        var fire = at(h, new BlockPos(4, 2, 4), ResonanceTotemBlockEntity.class);
        LatticeNetwork.linkTotems(earth, fire);
        var conductor = at(h, pos, LatticeConductorBlockEntity.class);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 2, "Two linked totems, saw " + conductor.getNetworkSize());

        // A third totem out of the conductor's own reach joins only through chalk.
        h.setBlock(14, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var far = at(h, new BlockPos(14, 2, 4), ResonanceTotemBlockEntity.class);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 2, "An unlinked totem out of reach is not in the network, saw " + conductor.getNetworkSize());
        LatticeNetwork.linkTotems(fire, far);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 3, "A new chalk link shows on the next beat, saw " + conductor.getNetworkSize());

        h.setBlock(14, 2, 4, Blocks.AIR);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 2, "A broken totem leaves the network on the next beat, saw " + conductor.getNetworkSize());
        h.succeed();
    }

    /** A redstone signal still stops the conductor's beat, and taking it away starts it again. */
    @GameTest(template = "empty")
    public static void conductorRedstoneLockStillHolds(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(4, 2, 4, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth = at(h, new BlockPos(2, 2, 4), ResonanceTotemBlockEntity.class);
        var fire = at(h, new BlockPos(4, 2, 4), ResonanceTotemBlockEntity.class);
        var conductor = at(h, pos, LatticeConductorBlockEntity.class);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 2, "Both totems in reach, saw " + conductor.getNetworkSize());

        h.setBlock(2, 3, 2, Blocks.REDSTONE_BLOCK);
        h.setBlock(4, 2, 4, Blocks.AIR);
        beat(h, pos, conductor);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 2, "A locked conductor does not beat, saw " + conductor.getNetworkSize());

        h.setBlock(2, 3, 2, Blocks.AIR);
        beat(h, pos, conductor);
        h.assertTrue(conductor.getNetworkSize() == 1, "Unlocked, it beats again and sees the totem gone, saw " + conductor.getNetworkSize());
        h.succeed();
    }

    private static void beat(GameTestHelper h, BlockPos pos, LatticeConductorBlockEntity conductor) {
        // The level ticks it too; driving a full interval by hand guarantees at least one beat whatever its phase.
        for (int i = 0; i < LatticeConductorBlockEntity.TICK_INTERVAL; i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), conductor);
    }

    // ---- Ley Collector -------------------------------------------------------------------------------------

    /** What a beat puts in is what a fresh survey says it should, before and after a totem comes to stand near it. */
    @GameTest(template = "empty")
    public static void collectorBeatMatchesAFreshSurvey(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.LEY_COLLECTOR.get());
        var collector = at(h, pos, LeyCollectorBlockEntity.class);
        int first = collectorBeat(h, pos, collector);
        h.assertTrue(first > 0, "A collector under open sky makes Pulse, made " + first);

        // A totem nearby changes both the land survey and the ley threads the collector kept from its last beat.
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        collectorBeat(h, pos, collector);
        h.setBlock(4, 2, 2, Blocks.AIR);
        int third = collectorBeat(h, pos, collector);
        h.assertTrue(third == first, "With the totem gone the beat is what it was, " + first + " then " + third);
        h.succeed();
    }

    private static int collectorBeat(GameTestHelper h, BlockPos pos, LeyCollectorBlockEntity collector) {
        var level = h.getLevel();
        BlockPos at = h.absolutePos(pos);
        int expected = collector.currentBeat(level, at);
        collector.extractPulse(collector.getPulseStored(), false);
        for (int i = 0; i < LeyCollectorBlockEntity.GAIN_INTERVAL; i++) {
            int before = collector.getPulseStored();
            LeyCollectorBlockEntity.serverTick(level, at, h.getBlockState(pos), collector);
            int made = collector.getPulseStored() - before;
            if (made > 0) {
                h.assertTrue(made == expected, "The beat must put in what a fresh survey gives, " + made + " against " + expected);
                return made;
            }
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("The collector never beat");
    }

    // ---- familiar marks ------------------------------------------------------------------------------------

    /** The Marks a creature shows follow its carried Marks however they are written. */
    @GameTest(template = "empty")
    public static void expressedMarksFollowEveryWrite(GameTestHelper h) {
        FamiliarData data = new FamiliarData();
        data.fill(1);
        h.assertTrue(data.expressedMarks().isEmpty(), "No Marks carried, none shown");
        data.setMarks(FamiliarData.Mark.KEEN, FamiliarData.Mark.NONE);
        h.assertFalse(data.expressed(FamiliarData.Mark.KEEN), "One recessive Keen does not show");
        data.setMarks(FamiliarData.Mark.KEEN, FamiliarData.Mark.KEEN);
        h.assertTrue(data.expressed(FamiliarData.Mark.KEEN), "A pair of Keen shows");
        data.setMarks(FamiliarData.Mark.STONEHIDE, FamiliarData.Mark.DRIFT);
        h.assertTrue(data.expressedMarks().equals(List.of(FamiliarData.Mark.STONEHIDE)), "Stonehide hides Drift, saw " + data.expressedMarks());

        FamiliarData copy = new FamiliarData();
        copy.fill(1);
        h.assertTrue(copy.expressedMarks().isEmpty(), "A blank copy shows nothing yet");
        copy.load(data.save());
        h.assertTrue(copy.expressedMarks().equals(data.expressedMarks()), "Loading carries the shown Marks, saw " + copy.expressedMarks());
        FamiliarData cloned = new FamiliarData();
        cloned.fill(1);
        cloned.expressedMarks();
        cloned.copyFrom(data);
        h.assertTrue(cloned.expressed(FamiliarData.Mark.STONEHIDE), "Copying carries the shown Marks");
        cloned.fill(2);
        h.assertTrue(cloned.expressedMarks().isEmpty(), "Refilling clears them");
        h.succeed();
    }

    private static <T> T at(GameTestHelper h, BlockPos pos, Class<T> type) {
        return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos)));
    }
}
