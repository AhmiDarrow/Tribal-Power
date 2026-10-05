package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.ResonanceTotemBlock;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.ley.LeyMagnets;
import tk.darrow.tribalpower.ley.LeyMath;
import tk.darrow.tribalpower.leyheart.LeyHeartBlockEntity;
import tk.darrow.tribalpower.leyheart.LeyHeartRegistry;
import tk.darrow.tribalpower.rite.world.LeyLines;
import tk.darrow.tribalpower.rite.world.RiteSavedData;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Ley Heart: the Heart Star wakes it and raises six real ley lines, a broken piece silences it and lowers
 * them, water burns fastest of its three fuels, more kinds of fuel make more Pulse, and it reads the ley.
 *
 * <p>Every test drives the heart's beat by hand and pulls the heart down in a finally, so its threads never
 * outlive the test function: they run sixty-four blocks, across other tests' ground.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class LeyHeartGameTests {
    private static final BlockPos CORE = new BlockPos(8, 2, 8);
    /** The six spokes of the Heart Star as written, unrotated. */
    private static final Map<Attunement, BlockPos> SPOKES = new EnumMap<>(Map.of(
            Attunement.FIRE, new BlockPos(5, 0, 0),
            Attunement.WATER, new BlockPos(-5, 0, 0),
            Attunement.EARTH, new BlockPos(3, 0, 4),
            Attunement.AIR, new BlockPos(-3, 0, -4),
            Attunement.SPIRIT, new BlockPos(-3, 0, 4),
            Attunement.LOOM, new BlockPos(3, 0, -4)));

    private static void totem(GameTestHelper h, BlockPos rel, Attunement voice) {
        var block = ModBlocks.totemFor(voice).get();
        h.setBlock(rel, block.defaultBlockState());
        h.setBlock(rel.above(), block.defaultBlockState().setValue(ResonanceTotemBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Stone ground, the anchor-stone dais, the heart and a totem of every voice five out. */
    private static LeyHeartBlockEntity build(GameTestHelper h) {
        for (int x = 2; x <= 14; x++)
            for (int z = 3; z <= 13; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) h.setBlock(CORE.offset(dx, -1, dz), ModBlocks.ANCHOR_STONE.get());
        h.setBlock(CORE, LeyHeartRegistry.LEY_HEART.get());
        SPOKES.forEach((voice, offset) -> totem(h, CORE.offset(offset), voice));
        return (LeyHeartBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(CORE));
    }

    /** Pull the heart down so no thread of it outlives the test. */
    private static void clear(GameTestHelper h) {
        h.setBlock(CORE, Blocks.AIR);
        LeyLines.lower(h.getLevel(), h.absolutePos(CORE));
    }

    private static Vec3 along(BlockPos core, BlockPos spoke, double t) {
        double len = Math.sqrt(spoke.getX() * spoke.getX() + spoke.getZ() * spoke.getZ());
        return Vec3.atCenterOf(core).add(spoke.getX() / len * t, 0, spoke.getZ() / len * t);
    }

    private static final double[] STATIONS = {2.5, 9, 24, 40, 60};

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theStarRaisesSixLinesAndABrokenTotemLowersThem(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos core = h.absolutePos(CORE);
        try {
            LeyHeartBlockEntity heart = build(h);
            h.assertTrue(heart != null, "The Ley Heart must have its block entity");
            h.assertTrue(LeyLines.raisedBy(level, core).isEmpty(), "Nothing is raised before the heart has looked");
            heart.beat(level);
            h.assertTrue(heart.complete(), "A heart on its dais with all six voices five out is a whole Heart Star");
            var threads = LeyLines.raisedBy(level, core);
            h.assertTrue(threads.size() == 6, "A whole star raises six threads, raised " + threads.size());
            for (var spoke : SPOKES.entrySet()) {
                BlockPos totem = h.absolutePos(CORE.offset(spoke.getValue()));
                h.assertTrue(threads.stream().anyMatch(t -> t.voice() == spoke.getKey().ordinal() && t.totem().equals(totem)
                                && t.length() == LeyLines.HEART_REACH),
                        "The " + spoke.getKey() + " thread must run out through the " + spoke.getKey() + " totem");
                // Real lines: the planet's ley field carries them, all the way out past the totem.
                int gained = 0;
                for (double t : STATIONS) {
                    BlockPos at = BlockPos.containing(along(core, spoke.getValue(), t));
                    LeyField.Reading with = LeyField.sample(level, at);
                    LeyField.Reading without = LeyField.sample(level, at, LeyMagnets.near(level, at), core);
                    h.assertTrue(with.heard(spoke.getKey()), "The ley field must carry the " + spoke.getKey() + " thread " + t + " blocks out");
                    gained += with.points() - without.points();
                }
                h.assertTrue(gained > 0, "The " + spoke.getKey() + " thread must add ley strength along its length, added " + gained);
            }
            // The six meet at the heart itself: anything standing there reads all six.
            h.assertTrue(LeyField.sample(level, core).lines() == LeyField.MAX_LINES, "All six threads meet at the heart");
            // The lens draws them as straight threads no totem bends.
            var ropes = LeyField.ropes(level, core.offset(0, 0, 1));
            h.assertTrue(ropes.stream().filter(LeyField.Rope::pinned).count() == 6, "The lens must draw the six threads");
            for (var rope : ropes) if (rope.pinned())
                h.assertTrue(LeyMagnets.pulls(LeyMagnets.near(level, core), rope).isEmpty(), "No totem bends a heart's thread");

            // They are saved with the world, and an unloading chunk does not drop them.
            var saved = RiteSavedData.get(level.getServer()).save(new CompoundTag(), level.registryAccess());
            var loaded = RiteSavedData.load(saved, level.registryAccess());
            h.assertTrue(loaded.heartThreads(level).stream().filter(t -> t.heart().equals(core)).count() == 6,
                    "The six threads must survive a save and load");
            heart.onChunkUnloaded();
            h.assertTrue(LeyLines.raisedBy(level, core).size() == 6, "An unloading chunk must not lower the threads");

            // Break the Fire totem: every thread comes down at once, and the field is as if the heart were not there.
            BlockPos fire = CORE.offset(SPOKES.get(Attunement.FIRE));
            h.setBlock(fire, Blocks.AIR);
            h.setBlock(fire.above(), Blocks.AIR);
            h.assertTrue(LeyLines.raisedBy(level, core).isEmpty(), "Breaking a totem must lower all six threads");
            for (var spoke : SPOKES.entrySet())
                for (double t : STATIONS) {
                    BlockPos at = BlockPos.containing(along(core, spoke.getValue(), t));
                    var now = LeyField.sample(level, at);
                    var bare = LeyField.sample(level, at, LeyMagnets.near(level, at), core);
                    h.assertTrue(now.equals(bare), "No trace of the " + spoke.getKey() + " thread may remain " + t + " blocks out");
                }
            heart.beat(level);
            h.assertFalse(heart.complete(), "A star missing its Fire totem is not whole");
            h.assertTrue(LeyLines.raisedBy(level, core).isEmpty(), "A broken star must not raise its threads again");
            h.assertTrue(heart.currentOutput() == 0, "A broken star is inert");
            var match = heart.match(level);
            h.assertTrue(!match.found() && match.misses().size() == 1 && match.misses().getFirst().pos().equals(h.absolutePos(fire)),
                    "The Codex must name the missing Fire totem, misses " + match.misses().size());

            // Make it whole again and the threads rise.
            totem(h, fire, Attunement.FIRE);
            heart.invalidatePattern();
            heart.beat(level);
            h.assertTrue(LeyLines.raisedBy(level, core).size() == 6, "A star made whole again raises its threads again");
            // A totem of the wrong voice in a slot does not make a star.
            totem(h, fire, Attunement.EARTH);
            h.assertTrue(LeyLines.raisedBy(level, core).isEmpty(), "Swapping a totem lowers the threads");
            heart.invalidatePattern();
            heart.beat(level);
            h.assertFalse(heart.complete(), "Two Earth totems and no Fire is not a Heart Star");
            totem(h, fire, Attunement.FIRE);
            heart.invalidatePattern();
            heart.beat(level);
            h.assertTrue(LeyLines.raisedBy(level, core).size() == 6, "Whole once more");
            // Breaking the heart itself takes its threads with it.
            h.setBlock(CORE, Blocks.AIR);
            h.assertTrue(LeyLines.raisedBy(level, core).isEmpty(), "Breaking the heart must lower its threads");
            h.succeed();
        } finally {
            clear(h);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waterBurnsFastestOfTheThreeFuels(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        try {
            LeyHeartBlockEntity heart = build(h);
            var reagent = tk.darrow.tribalpower.song.Reagents.item(tk.darrow.tribalpower.entity.CreatureProfile.values()[0]);
            heart.setItem(LeyHeartBlockEntity.CRYSTAL, new ItemStack(ModItems.ECHO_SHARD.get()));
            heart.setItem(LeyHeartBlockEntity.REAGENT, new ItemStack(reagent));
            h.assertTrue(heart.canPlaceItem(LeyHeartBlockEntity.CRYSTAL, new ItemStack(ModItems.RESONANT_CORE.get())),
                    "The crystal slot takes what a Resonator seats");
            h.assertFalse(heart.canPlaceItem(LeyHeartBlockEntity.REAGENT, new ItemStack(ModItems.ECHO_SHARD.get())),
                    "The reagent slot takes only creature reagents");
            int filled = heart.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(filled == 1000, "The tank takes a bucket of water, took " + filled);
            int beats = 1000 / LeyHeartBlockEntity.WATER_PER_BEAT;
            for (int i = 0; i < beats; i++) {
                heart.beat(level);
                h.assertTrue(heart.currentOutput() > 0, "A whole star with fuel sings, beat " + i);
                heart.extractPulse(Integer.MAX_VALUE, false);
            }
            h.assertTrue(heart.tank.getFluidAmount() == 0, "A bucket of water is gone in five seconds, left " + heart.tank.getFluidAmount());
            h.assertTrue(heart.getItem(LeyHeartBlockEntity.CRYSTAL).isEmpty() && heart.crystalBurn() > 0,
                    "The crystal is lit and still burning");
            h.assertTrue(heart.getItem(LeyHeartBlockEntity.REAGENT).isEmpty() && heart.reagentBurn() > 0,
                    "The reagent is lit and still burning");
            double waterSpent = 1.0;   // the whole bucket
            double reagentSpent = 1.0 - heart.reagentBurn() / (double) LeyHeartBlockEntity.REAGENT_TICKS;
            double crystalSpent = 1.0 - heart.crystalBurn() / (double) LeyHeartBlockEntity.crystalTicks(1);
            h.assertTrue(waterSpent > reagentSpent && reagentSpent > crystalSpent && crystalSpent > 0,
                    "Water burns fastest, then the reagent, then the crystal: " + waterSpent + " " + reagentSpent + " " + crystalSpent);
            // With the water gone the heart sings on, a little quieter.
            int wet = heart.currentOutput();
            heart.beat(level);
            h.assertTrue(heart.currentOutput() > 0 && heart.currentOutput() < wet,
                    "Without water the heart makes less, " + heart.currentOutput() + " after " + wet);
            // A full heart rests and spends nothing.
            heart.insertPulse(Integer.MAX_VALUE, false);
            int reagentLeft = heart.reagentBurn();
            heart.beat(level);
            h.assertTrue(heart.reagentBurn() == reagentLeft, "A full heart must not burn its fuel");
            h.succeed();
        } finally {
            clear(h);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void moreKindsOfFuelMakeMorePulse(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        try {
            LeyHeartBlockEntity heart = build(h);
            heart.beat(level);
            h.assertTrue(heart.complete() && heart.currentOutput() == 0, "A whole star with nothing to burn makes nothing");
            heart.setItem(LeyHeartBlockEntity.CRYSTAL, new ItemStack(ModItems.ECHO_SHARD.get(), 4));
            heart.beat(level);
            int one = heart.currentOutput();
            int stored = heart.getPulseStored();
            h.assertTrue(one > 0 && stored == one, "A crystal alone sings, and what it makes is stored: " + one + " " + stored);
            var reagent = tk.darrow.tribalpower.song.Reagents.item(tk.darrow.tribalpower.entity.CreatureProfile.values()[0]);
            heart.setItem(LeyHeartBlockEntity.REAGENT, new ItemStack(reagent, 4));
            heart.beat(level);
            int two = heart.currentOutput();
            heart.tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
            heart.beat(level);
            int three = heart.currentOutput();
            h.assertTrue(one < two && two < three, "Each kind of fuel burning adds Pulse: " + one + " < " + two + " < " + three);
            // The numbers the Codex quotes.
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 4, true, true, LeyMath.MAX_GAIN) == 378,
                    "Six voices, a Resonant Core, a reagent and water on a perfect site make 378, made "
                            + LeyHeartBlockEntity.outputFor(6, 4, true, true, LeyMath.MAX_GAIN));
            int pair = PulseResonatorBlockEntity.gainFor(6, 4) + tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity.beatFor(LeyMath.MAX_GAIN) / 2;
            h.assertTrue(pair == 168, "A Resonant Core Resonator and a perfect Collector make 168, made " + pair);
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 4, false, false, LeyMath.MAX_GAIN) > pair,
                    "A heart burning a crystal alone still beats the Resonator and Collector it is made from");
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 4, true, true, LeyMath.MAX_GAIN) < 3 * pair,
                    "But not by so much that it buries every other generator");
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 0, false, false, LeyMath.MAX_GAIN) == 0, "Nothing burning, nothing made");
            h.assertTrue(LeyHeartBlockEntity.outputFor(3, 4, true, true, 8) < LeyHeartBlockEntity.outputFor(6, 4, true, true, 8),
                    "Only answered voices sing");
            h.succeed();
        } finally {
            clear(h);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theHeartReadsTheLeyLikeACollector(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos core = h.absolutePos(CORE);
        try {
            for (int g = 0; g < LeyMath.MAX_GAIN; g++)
                h.assertTrue(LeyHeartBlockEntity.outputFor(6, 1, false, false, g) < LeyHeartBlockEntity.outputFor(6, 1, false, false, g + 1),
                        "More ley strength must always make more Pulse, at " + g);
            LeyHeartBlockEntity heart = build(h);
            heart.setItem(LeyHeartBlockEntity.CRYSTAL, new ItemStack(ModItems.ECHO_SHARD.get(), 4));
            heart.resurvey();
            heart.beat(level);
            heart.extractPulse(Integer.MAX_VALUE, false);
            // It reads what a Ley Collector here would, less its own six threads: they leave it, they do not feed it.
            int open = LeyMath.factors(level, core, LeyField.sample(level, core, LeyMagnets.near(level, core), core)).gain();
            h.assertTrue(heart.leyGain() == open, "The heart reads the ley as a collector would, " + heart.leyGain() + " against " + open);
            int withOwn = LeyMath.factors(level, core, LeyField.sample(level, core)).gain();
            h.assertTrue(withOwn >= open, "Its own threads would only ever add, " + withOwn + " against " + open);
            int openOut = heart.currentOutput();
            // Roof the heart: the sky goes, and with it some of the land's share.
            h.setBlock(CORE.above(3), Blocks.STONE);
            heart.resurvey();
            heart.beat(level);
            int roofed = LeyMath.factors(level, core, LeyField.sample(level, core, LeyMagnets.near(level, core), core)).gain();
            h.assertTrue(heart.leyGain() == roofed, "The heart rereads the ley, " + heart.leyGain() + " against " + roofed);
            h.assertTrue(roofed <= open, "A roof never strengthens the ley, " + roofed + " against " + open);
            if (roofed < open)
                h.assertTrue(heart.currentOutput() < openOut, "Weaker ley must make less Pulse, " + heart.currentOutput() + " against " + openOut);
            h.succeed();
        } finally {
            clear(h);
        }
    }
}
