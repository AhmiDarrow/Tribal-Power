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
            double reagentSpent = 1.0 - heart.reagentBurn() / (double) LeyHeartBlockEntity.reagentTicks(0);
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
            int bare = LeyHeartBlockEntity.outputFor(6, 1, 0, true, 0), plainBest = LeyHeartBlockEntity.outputFor(6, 1, 0, true, LeyMath.MAX_GAIN);
            h.assertTrue(bare == 1080 && plainBest == 1368,
                    "An Echo Shard, a plain reagent and water make 1,080 on a bare site and 1,368 on a perfect one, made " + bare + " and " + plainBest);
            int top = LeyHeartBlockEntity.outputFor(6, 4, 3, true, LeyMath.MAX_GAIN);
            h.assertTrue(top == 3096, "A Resonant Core, a Thread III reagent and water on a perfect site make 3,096, made " + top);
            int manifested = top + Math.round(top * 0.15F * tk.darrow.tribalpower.item.MachineRank.MAX);
            h.assertTrue(manifested == 4489, "A Manifested heart at its best makes 4,489, made " + manifested);
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 3, 2, true, 8) == 2196, "A Bound Echo, Thread II and water on ley 8 make about 2,200, made "
                    + LeyHeartBlockEntity.outputFor(6, 3, 2, true, 8));
            int pair = PulseResonatorBlockEntity.gainFor(6, 4) + tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity.beatFor(LeyMath.MAX_GAIN) / 2;
            h.assertTrue(pair == 168, "A Resonant Core Resonator and a perfect Collector make 168, made " + pair);
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 4, LeyHeartBlockEntity.NO_REAGENT, false, LeyMath.MAX_GAIN) > pair,
                    "A heart burning a crystal alone still beats the Resonator and Collector it is made from");
            // Its buffer holds between half a minute and a minute of its very best.
            h.assertTrue(LeyHeartBlockEntity.CAPACITY >= 30 * manifested && LeyHeartBlockEntity.CAPACITY <= 60 * manifested,
                    "The heart's buffer must hold 30 to 60 seconds of its best, holds " + LeyHeartBlockEntity.CAPACITY);
            h.assertTrue(LeyHeartBlockEntity.outputFor(6, 0, LeyHeartBlockEntity.NO_REAGENT, false, LeyMath.MAX_GAIN) == 0, "Nothing burning, nothing made");
            h.assertTrue(LeyHeartBlockEntity.outputFor(3, 4, 0, true, 8) < LeyHeartBlockEntity.outputFor(6, 4, 0, true, 8),
                    "Only answered voices sing");
            // Quality is the lever: every step of crystal or Thread is worth more, and burns faster for it.
            for (int thread = 0; thread < tk.darrow.tribalpower.song.ReagentThread.MAX; thread++) {
                h.assertTrue(LeyHeartBlockEntity.outputFor(6, 1, thread, true, 8) < LeyHeartBlockEntity.outputFor(6, 1, thread + 1, true, 8),
                        "A stronger Thread must make more Pulse, at " + thread);
                h.assertTrue(LeyHeartBlockEntity.reagentTicks(thread + 1) < LeyHeartBlockEntity.reagentTicks(thread),
                        "A stronger Thread must burn faster, at " + thread);
            }
            for (int rank = 1; rank < 4; rank++) {
                h.assertTrue(LeyHeartBlockEntity.outputFor(6, rank, 0, true, 8) < LeyHeartBlockEntity.outputFor(6, rank + 1, 0, true, 8),
                        "A better crystal must make more Pulse, at " + rank);
                h.assertTrue(LeyHeartBlockEntity.crystalTicks(rank + 1) < LeyHeartBlockEntity.crystalTicks(rank),
                        "A better crystal must burn faster, at " + rank);
            }
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
                h.assertTrue(LeyHeartBlockEntity.outputFor(6, 1, LeyHeartBlockEntity.NO_REAGENT, false, g)
                                < LeyHeartBlockEntity.outputFor(6, 1, LeyHeartBlockEntity.NO_REAGENT, false, g + 1),
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

    private static ItemStack reagent(int count, int thread) {
        var item = tk.darrow.tribalpower.song.Reagents.item(tk.darrow.tribalpower.entity.CreatureProfile.values()[0]);
        return tk.darrow.tribalpower.song.ReagentThread.with(new ItemStack(item, count), thread);
    }

    /** A whole heart with water, a crystal of {@code rank} and reagents of {@code thread}, one beat sung. */
    private static LeyHeartBlockEntity fuelled(GameTestHelper h, int rank, int thread, int machineRank) {
        LeyHeartBlockEntity heart = build(h);
        var crystal = switch (rank) {
            case 1 -> ModItems.ECHO_SHARD.get();
            case 2 -> ModItems.ATTUNED_ECHO.get();
            case 3 -> ModItems.BOUND_ECHO.get();
            default -> ModItems.RESONANT_CORE.get();
        };
        tk.darrow.tribalpower.item.MachineRank.apply(heart, machineRank);
        heart.setItem(LeyHeartBlockEntity.CRYSTAL, new ItemStack(crystal, 2));
        heart.setItem(LeyHeartBlockEntity.REAGENT, reagent(4, thread));
        heart.tank.fill(new FluidStack(Fluids.WATER, LeyHeartBlockEntity.TANK_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        heart.resurvey();
        heart.beat(h.getLevel());
        return heart;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void reagentThreadScalesTheHeartFromAThousandToFourThousand(GameTestHelper h) {
        try {
            // A complete heart on ordinary fuel: an Echo Shard, a plain reagent and water.
            LeyHeartBlockEntity plain = fuelled(h, 1, 0, 0);
            int ordinary = plain.currentOutput();
            h.assertTrue(ordinary >= 1000 && ordinary <= 1500,
                    "A complete heart on an Echo Shard, a plain reagent and water makes 1,000 to 1,500, made " + ordinary + " at ley " + plain.leyGain());
            h.assertTrue(plain.reagentThread() == 0 && plain.reagentBurn() == LeyHeartBlockEntity.reagentTicks(0) - LeyHeartBlockEntity.BEAT,
                    "A plain reagent lights for two minutes");
            clear(h);
            // The same heart and crystal on Thread I, II and III reagents makes more at every step.
            int last = ordinary;
            for (int thread = 1; thread <= tk.darrow.tribalpower.song.ReagentThread.MAX; thread++) {
                LeyHeartBlockEntity heart = fuelled(h, 1, thread, 0);
                int made = heart.currentOutput();
                h.assertTrue(made > last, "A Thread " + thread + " reagent must make more than the step below, " + made + " after " + last);
                h.assertTrue(heart.reagentThread() == thread
                                && heart.reagentBurn() == LeyHeartBlockEntity.reagentTicks(thread) - LeyHeartBlockEntity.BEAT,
                        "The lit reagent keeps its Thread and burns for that Thread's time, burn " + heart.reagentBurn());
                last = made;
                clear(h);
            }
            // The top: a Manifested heart on a Resonant Core, Thread III and water.
            LeyHeartBlockEntity top = fuelled(h, 4, 3, tk.darrow.tribalpower.item.MachineRank.MAX);
            h.assertTrue(top.currentOutput() >= 4000, "A Manifested heart at its best makes 4,000 or more, made " + top.currentOutput()
                    + " at ley " + top.leyGain());
            h.succeed();
        } finally {
            clear(h);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theTopHeartFillsItsBufferAtTheRateItSings(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        try {
            LeyHeartBlockEntity heart = fuelled(h, 4, 3, tk.darrow.tribalpower.item.MachineRank.MAX);
            long made = heart.currentOutput();
            h.assertTrue(heart.getPulseStored() == made, "The first beat's song is all in the buffer, " + heart.getPulseStored() + " of " + made);
            // Ten seconds more: every beat's Pulse lands, none is lost to a cap, and the buffer has room for it all.
            for (int i = 0; i < 10; i++) {
                heart.beat(level);
                h.assertTrue(heart.currentOutput() >= 4000, "Every beat sings 4,000 or more, beat " + i + " made " + heart.currentOutput());
                made += heart.currentOutput();
                h.assertTrue(heart.getPulseStored() == made, "The buffer must fill at the rate the heart sings: "
                        + heart.getPulseStored() + " stored after " + made + " made, beat " + i);
            }
            h.assertTrue(made >= 44000 && made < LeyHeartBlockEntity.CAPACITY, "Eleven seconds at its best fit the buffer, made " + made);
            // And it gives it all back to a draw: a Grand Pulse Cell takes 19,200 at a touch.
            ItemStack cell = new ItemStack(ModItems.GRAND_PULSE_CELL.get());
            int filled = tk.darrow.tribalpower.item.PulseCellItem.fillFrom(cell, heart);
            h.assertTrue(filled == tk.darrow.tribalpower.item.PulseCellItem.GRAND_CAPACITY && heart.getPulseStored() == made - filled,
                    "A Grand Pulse Cell fills to the brim from the heart, took " + filled);
            // The rest leaves along the lattice: one Manifested conductor beside the heart carries 4,096 a second.
            Weaving.conductor(h, CORE.above().east());
            int rest = heart.getPulseStored();
            int drawn = tk.darrow.tribalpower.lattice.LatticeNetwork.extractPulseNearby(level, h.absolutePos(CORE.above()), 8, rest);
            int carried = Math.min(rest, 4096);
            h.assertTrue(drawn == carried && heart.getPulseStored() == rest - carried,
                    "A draw through a Manifested conductor takes " + carried + " of the rest in a second, drew " + drawn + " of " + rest);
            h.succeed();
        } finally {
            clear(h);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aReagentCarriesTheThreadOfTheCreatureItCameOff(GameTestHelper h) {
        var animal = h.spawn(tk.darrow.tribalpower.entity.CreatureEntities.ANIMALS.get(tk.darrow.tribalpower.entity.CreatureProfile.DAWN_STAG).get(),
                new BlockPos(3, 2, 3));
        animal.setNoAi(true);
        var lattice = animal.lattice();
        int[][] cases = {{1, 0}, {2, 0}, {3, 1}, {4, 2}, {5, 3}};
        for (int[] c : cases) {
            lattice.fill(1);
            lattice.setAlleles(tk.darrow.tribalpower.familiar.FamiliarData.Thread.HUM, c[0], c[0]);
            int thread = tk.darrow.tribalpower.song.ReagentThread.of(animal);
            h.assertTrue(thread == c[1], "A creature whose best thread is " + c[0] + " sheds Thread " + c[1] + ", shed " + thread);
        }
        // A gold-named elite sheds one step stronger.
        lattice.fill(3);
        animal.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).addPermanentModifier(
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(tk.darrow.tribalpower.entity.MarchThreat.ELITE, 1.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(tk.darrow.tribalpower.song.ReagentThread.of(animal) == 2, "An elite with a thread at 3 sheds Thread II");
        lattice.fill(5);
        h.assertTrue(tk.darrow.tribalpower.song.ReagentThread.of(animal) == 3, "Thread III is the most there is");
        ItemStack shed = tk.darrow.tribalpower.song.ReagentThread.shed(animal, reagent(2, 0));
        h.assertTrue(tk.darrow.tribalpower.song.ReagentThread.get(shed) == 3, "A brushed reagent carries the creature's Thread");
        // Threads never stack with one another or with plain reagents.
        h.assertFalse(ItemStack.isSameItemSameComponents(reagent(1, 1), reagent(1, 2)), "Thread I and II must not stack");
        h.assertFalse(ItemStack.isSameItemSameComponents(reagent(1, 0), reagent(1, 1)), "A plain reagent and Thread I must not stack");
        h.assertTrue(ItemStack.isSameItemSameComponents(reagent(1, 2), reagent(1, 2)), "Two Thread II reagents stack");
        // The pouch takes plain reagents and leaves threaded ones in the bags.
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModItems.REAGENT_POUCH.get()));
        h.assertTrue(tk.darrow.tribalpower.song.ReagentPouchHooks.absorb(player, reagent(3, 2)) == 3, "The pouch must leave a Thread II reagent out");
        h.assertTrue(tk.darrow.tribalpower.song.ReagentPouchHooks.absorb(player, reagent(3, 0)) == 0, "The pouch takes plain reagents");
        // A threaded reagent still feeds the heart's reagent slot.
        h.assertTrue(tk.darrow.tribalpower.song.Reagents.isReagent(reagent(1, 3)), "A threaded reagent is still a reagent");
        h.succeed();
    }
}
