package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceMeshBlockEntity;
import tk.darrow.tribalpower.grit.GritRegistry;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.pattern.PatternMatcher;
import tk.darrow.tribalpower.pit.OreBand;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeStandingSavedData;

import java.util.UUID;

/** The Listening Pit (design 3.1 section 7). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class PitGameTests {
    private static final BlockPos MESH = new BlockPos(5, 2, 5);

    /**
     * A complete tier 1 pit around {@link #MESH}: stone beneath, four anchor stones at the corners, chalk
     * to the mesh, an Earth totem in reach and an Ancestral Cache beside it.
     */
    private static ResonanceMeshBlockEntity pit(GameTestHelper h) {
        for (int x = 3; x <= 7; x++)
            for (int z = 3; z <= 7; z++) h.setBlock(x, 1, z, Blocks.STONE);
        h.setBlock(MESH, ModBlocks.RESONANCE_MESH.get());
        for (int dx : new int[]{-2, 2})
            for (int dz : new int[]{-2, 2}) h.setBlock(MESH.offset(dx, 0, dz), ModBlocks.ANCHOR_STONE.get());
        for (int d = 1; d <= 2; d++) {
            h.setBlock(MESH.offset(d, 0, 0), ModBlocks.RITUAL_MARK.get());
            h.setBlock(MESH.offset(-d, 0, 0), ModBlocks.RITUAL_MARK.get());
            h.setBlock(MESH.offset(0, 0, d), ModBlocks.RITUAL_MARK.get());
            h.setBlock(MESH.offset(0, 0, -d), ModBlocks.RITUAL_MARK.get());
        }
        h.setBlock(MESH.above(), ModBlocks.ANCESTRAL_CACHE.get());
        // Clear of the pattern: the corners belong to the anchor stones.
        h.setBlock(9, 2, 5, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        ResonanceMeshBlockEntity mesh = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
        // A pit listens on its owner's standing with the Grit-singers, and no owner means no standing --
        // so a test pit has to be owned by somebody who has earned the band it is asking for.
        stand(h, mesh, TribeRank.FRIEND);
        return mesh;
    }

    /** Gives the mesh an owner standing at {@code rank} with the Grit-singers. */
    private static UUID stand(GameTestHelper h, ResonanceMeshBlockEntity mesh, TribeRank rank) {
        UUID owner = UUID.nameUUIDFromBytes(("pit-test-" + rank.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TribeStandingSavedData.get(h.getLevel().getServer()).set(owner, TribeDefinition.STONE, rank.threshold());
        mesh.setOwner(owner);
        return owner;
    }

    /** Pulse from outside the pattern: the five by five belongs to the pit. */
    private static DrumheartBlockEntity feed(GameTestHelper h, int pulse) {
        BlockPos outside = new BlockPos(9, 2, 3);
        h.setBlock(outside, ModBlocks.DRUMHEART.get());
        DrumheartBlockEntity drum = (DrumheartBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(outside));
        drum.insertPulse(pulse, false);
        return drum;
    }

    @GameTest(template = "empty")
    public static void tierOnePitCompletesAndNamesItsBand(GameTestHelper h) {
        ResonanceMeshBlockEntity mesh = pit(h);
        PatternMatcher.Match match = mesh.patternState().get(h.getLevel(), h.absolutePos(MESH));
        h.assertTrue(match.found(), "The written tier 1 shape must match: " + match.report(3));
        h.assertTrue(match.tier() == 1, "Five by five is tier 1, got " + match.tier());
        h.assertTrue(mesh.cache(h.getLevel(), h.absolutePos(MESH)) != null, "The cache beside the mesh must be found");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pitConsumesSubstrateAndEmitsTheSampledMaterial(GameTestHelper h) {
        GritRegistry.rebuild();
        ResonanceMeshBlockEntity mesh = pit(h);
        feed(h, 1000);
        mesh.setItem(ResonanceMeshBlockEntity.SUBSTRATE_A, new ItemStack(Items.STONE, 64));
        mesh.setItem(ResonanceMeshBlockEntity.SAMPLE, new ItemStack(Items.RAW_IRON));

        h.assertTrue("iron".equals(mesh.sampleMaterial()), "Raw iron must read as a sample of iron");
        h.assertTrue(OreBand.COMMON.biasedBy("iron"), "Iron lives in the common band");

        int before = mesh.getItem(ResonanceMeshBlockEntity.SUBSTRATE_A).getCount();
        h.succeedWhen(() -> {
            ResonanceMeshBlockEntity live = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
            h.assertTrue(live.getItem(ResonanceMeshBlockEntity.SUBSTRATE_A).getCount() < before,
                    "The pit must eat its substrate, state is " + live.state());
            // The sample is a filter, not an ingredient: it is still there afterwards.
            h.assertTrue(!live.getItem(ResonanceMeshBlockEntity.SAMPLE).isEmpty(), "The sample must never be consumed");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pitStallsRatherThanVoidingWhenTheCacheIsFull(GameTestHelper h) {
        GritRegistry.rebuild();
        ResonanceMeshBlockEntity mesh = pit(h);
        feed(h, 4000);
        mesh.setItem(ResonanceMeshBlockEntity.SUBSTRATE_A, new ItemStack(Items.STONE, 64));

        AncestralCacheBlockEntity cache = (AncestralCacheBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH.above()));
        for (int slot = 0; slot < cache.getContainerSize(); slot++)
            cache.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        for (int slot = ResonanceMeshBlockEntity.OUTPUT_FIRST; slot < ResonanceMeshBlockEntity.SLOTS; slot++)
            mesh.setItem(slot, new ItemStack(Items.BEDROCK, 64));

        h.runAfterDelay(60, () -> {
            ResonanceMeshBlockEntity live = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
            h.assertTrue("full".equals(live.state()), "A full pit must stall, not void; state is " + live.state());
            for (int slot = ResonanceMeshBlockEntity.OUTPUT_FIRST; slot < ResonanceMeshBlockEntity.SLOTS; slot++)
                h.assertTrue(live.getItem(slot).is(Items.BEDROCK), "Nothing already in the output may be overwritten");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anUnownedPitEarnsNothingRatherThanEverything(GameTestHelper h) {
        // Failing open here handed the rare band to a mesh a dispenser placed. No owner is no standing.
        GritRegistry.rebuild();
        ResonanceMeshBlockEntity mesh = pit(h);
        feed(h, 4000);
        mesh.setOwner(null);
        mesh.setItem(ResonanceMeshBlockEntity.SUBSTRATE_A, new ItemStack(Items.STONE, 64));
        h.runAfterDelay(60, () -> {
            ResonanceMeshBlockEntity live = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
            h.assertTrue("standing".equals(live.state()),
                    "An unowned pit must stall on standing, not run; state is " + live.state());
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void theDeepBandNeverHandsOutTheNethersQuartz(GameTestHelper h) {
        // c:ores/quartz holds only the Nether's ore, so a quartz row in the deep band gave the hot band's
        // bargain away for free -- no Fire, and at deep's price.
        GritRegistry.rebuild();
        for (OreBand.Entry entry : OreBand.DEEP.entries())
            h.assertFalse(entry.result(false).is(Items.NETHER_QUARTZ_ORE),
                    "The deep band must not call up Nether Quartz Ore");
        h.assertTrue(OreBand.bandsFor("quartz").contains(OreBand.HOT),
                "Quartz belongs to the hot band, or Fire's bargain is unreachable");
        h.assertTrue(OreBand.HOT.descent().getFirst() == OreBand.HOT, "The hot band must be selectable");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void washingTakesNoWaterItCannotUse(GameTestHelper h) {
        GritRegistry.rebuild();
        ResonanceMeshBlockEntity mesh = pit(h);
        feed(h, 4000);
        mesh.setItem(ResonanceMeshBlockEntity.SUBSTRATE_A, new ItemStack(Items.STONE, 64));
        h.setBlock(9, 2, 7, ModBlocks.RESONANCE_TOTEM_WATER.get());
        // Less than one wash costs: the tank must still hold it after several cycles.
        int short_ = ResonanceMeshBlockEntity.WASH_COST - 50;
        mesh.tank.fill(new FluidStack(Fluids.WATER, short_), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(300, () -> {
            ResonanceMeshBlockEntity live = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
            h.assertTrue(live.tank.getFluidAmount() == short_,
                    "A tank too shallow to wash must be left alone, held " + live.tank.getFluidAmount());
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void theMeshIsReachableAsATankAndAsAnInventory(GameTestHelper h) {
        // Washing is unreachable and no relay can feed the pit unless these capabilities are registered.
        ResonanceMeshBlockEntity mesh = pit(h);
        BlockPos at = h.absolutePos(MESH);
        h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                at, null) != null, "A bucket or a cistern must find the mesh's tank");
        h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                at, net.minecraft.core.Direction.NORTH) != null, "A relay must find the mesh's substrate slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heldSignalStillsTheMeshAndClosesItToHoppers(GameTestHelper h) {
        ResonanceMeshBlockEntity mesh = pit(h);
        h.assertFalse(mesh.stilled(), "The mesh starts unstilled");
        h.assertTrue(mesh.canPlaceItemThroughFace(ResonanceMeshBlockEntity.SUBSTRATE_A,
                new ItemStack(Items.STONE), net.minecraft.core.Direction.NORTH), "Sides feed substrate");

        h.setBlock(5, 2, 4, Blocks.REDSTONE_BLOCK);
        h.assertTrue(mesh.stilled(), "A held signal must still the mesh");
        h.assertFalse(mesh.canPlaceItemThroughFace(ResonanceMeshBlockEntity.SUBSTRATE_A,
                new ItemStack(Items.STONE), net.minecraft.core.Direction.NORTH), "A stilled mesh takes nothing in");
        h.assertFalse(mesh.canTakeItemThroughFace(ResonanceMeshBlockEntity.OUTPUT_FIRST,
                ItemStack.EMPTY, net.minecraft.core.Direction.DOWN), "A stilled mesh gives nothing out");
        h.assertTrue(mesh.progressSignal() == 0, "A stilled mesh reports no progress");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void meshFacesAreTheDocumentedOnes(GameTestHelper h) {
        ResonanceMeshBlockEntity mesh = pit(h);
        h.assertTrue(mesh.getSlotsForFace(net.minecraft.core.Direction.UP).length == 1
                && mesh.getSlotsForFace(net.minecraft.core.Direction.UP)[0] == ResonanceMeshBlockEntity.SAMPLE,
                "The top is the sample slot");
        h.assertTrue(mesh.getSlotsForFace(net.minecraft.core.Direction.DOWN).length == 4,
                "The bottom is the output");
        h.assertTrue(mesh.getSlotsForFace(net.minecraft.core.Direction.EAST).length == 2,
                "The sides are substrate");
        h.assertFalse(mesh.canPlaceItem(ResonanceMeshBlockEntity.OUTPUT_FIRST, new ItemStack(Items.STONE)),
                "Nothing is ever inserted into an output slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void meshDrinksWaterForWashing(GameTestHelper h) {
        ResonanceMeshBlockEntity mesh = pit(h);
        int filled = mesh.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 1000, "The mesh must accept water, took " + filled);
        h.assertTrue(mesh.tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "The mesh drinks water and nothing else");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aPoweredCacheDoesNotStallAPaidCycle(GameTestHelper h) {
        // The cache is the pit's output, not a camp device. Redstone next to it used to make cache()
        // return null, which stalled the mesh and wiped the Pulse already spent on that cycle.
        GritRegistry.rebuild();
        ResonanceMeshBlockEntity mesh = pit(h);
        feed(h, 4000);
        mesh.setItem(ResonanceMeshBlockEntity.SUBSTRATE_A, new ItemStack(Items.STONE, 64));
        mesh.setItem(ResonanceMeshBlockEntity.SAMPLE, new ItemStack(Items.RAW_IRON));
        int before = mesh.getItem(ResonanceMeshBlockEntity.SUBSTRATE_A).getCount();
        h.setBlock(MESH.above().east(), Blocks.REDSTONE_BLOCK);
        h.assertFalse(mesh.stilled(), "Power on the cache must not still the mesh");
        h.succeedWhen(() -> {
            ResonanceMeshBlockEntity live = (ResonanceMeshBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MESH));
            h.assertTrue(live.getItem(ResonanceMeshBlockEntity.SUBSTRATE_A).getCount() < before,
                    "A powered cache must not stall a paid cycle, state is " + live.state());
        });
    }

    @GameTest(template = "empty")
    public static void theComparatorReachesFifteenOnTheLastBeat(GameTestHelper h) {
        ResonanceMeshBlockEntity mesh = pit(h);
        h.assertTrue(mesh.progressSignal() == 0, "Idle is 0");
        // 1 + 14 * (work-1) / max(1, seconds-2). Common is 10s, so work 9 must read 15, not 13.
        try {
            var field = ResonanceMeshBlockEntity.class.getDeclaredField("work");
            field.setAccessible(true);
            var state = ResonanceMeshBlockEntity.class.getDeclaredField("state");
            state.setAccessible(true);
            state.set(mesh, "working");
            field.setInt(mesh, 9);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        h.assertTrue(mesh.progressSignal() == 15,
                "The last observed beat must read 15, got " + mesh.progressSignal());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void substratesAreRenewableAndRareCostsSpiritToo(GameTestHelper h) {
        // No deepslate and no March Crystal anywhere in the substrate table: both are worldgen-only and
        // would throttle the tier behind a pickaxe (design 3.1 section 7.5).
        for (OreBand band : OreBand.values())
            for (ItemStack stack : band.substrate()) {
                h.assertFalse(stack.is(Items.DEEPSLATE), band + " must not ask for deepslate");
                h.assertFalse(stack.is(ModItems.MARCH_CRYSTAL.get()), band + " must not ask for March Crystal");
            }
        h.assertTrue(OreBand.COMMON.substrate().getFirst().is(Items.STONE), "The common band runs on Stone Font stone");
        h.assertTrue(OreBand.RARE.substrate().size() == 2, "The rare band costs March Stone and a Spirit Shard");
        h.succeed();
    }
}
