package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.EchoStationBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.lattice.LatticeNotice;
import tk.darrow.tribalpower.lattice.Weave;
import tk.darrow.tribalpower.leyheart.LeyHeartBlockEntity;
import tk.darrow.tribalpower.leyheart.LeyHeartRegistry;

/**
 * The Pulse lattice (lattice/Weave): machines draw only through Lattice Conductors, conductors link within 8 into
 * networks, each conductor carries its rank's rate, cairns store by rank, and a totem's buffer is its voice.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class LatticeWeaveGameTests {
    private LatticeWeaveGameTests() {}

    private static <T> T at(GameTestHelper h, BlockPos pos, Class<T> type) {
        return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos)));
    }

    private static String state(EchoStationBlockEntity station) {
        return station.status().getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    private static LeyHeartBlockEntity heart(GameTestHelper h, BlockPos pos, int pulse) {
        h.setBlock(pos, LeyHeartRegistry.LEY_HEART.get());
        LeyHeartBlockEntity heart = at(h, pos, LeyHeartBlockEntity.class);
        heart.insertPulse(pulse, false);
        return heart;
    }

    /** The hard switch: a station right beside a full drum starves until a conductor weaves them together. */
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void noConductorMeansAMachineStarvesBesideAGenerator(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(3, 2, 2, ModBlocks.DRUMHEART.get());
        var drum = at(h, new BlockPos(3, 2, 2), DrumheartBlockEntity.class);
        drum.insertPulse(1000, false);
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.ECHO_SHATTER.get());
        h.setBlock(2, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var station = at(h, pos, EchoStationBlockEntity.class);
        station.setItem(0, new ItemStack(Items.STONE, 4));
        h.assertTrue(LatticeNetwork.extractPulseNearby(level, h.absolutePos(pos), 8, 10, true) == 0,
                "Off the lattice nothing can be drawn, even beside a full drum");
        h.runAfterDelay(30, () -> {
            h.assertTrue(state(station).endsWith(".lattice"), "The station says it is off the lattice, said " + state(station));
            h.assertTrue(drum.getPulseStored() == 1000, "The drum must not be touched, holds " + drum.getPulseStored());
            Weaving.conductor(h, new BlockPos(4, 2, 2), 0);
            h.succeedWhen(() -> {
                h.assertTrue(drum.getPulseStored() < 1000, "With a conductor in reach the station draws the drum");
                h.assertTrue(state(station).endsWith(".working"), "And works, said " + state(station));
            });
        });
    }

    /** One conductor within 8 of a generator and a machine puts both on one network. */
    @GameTest(template = "empty")
    public static void oneConductorWithinEightConnectsBoth(GameTestHelper h) {
        var level = h.getLevel();
        var horn = new BlockPos(1, 2, 1);
        h.setBlock(horn, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        at(h, horn, tk.darrow.tribalpower.generator.EmberHornBlockEntity.class).insertPulse(100, false);
        var machine = h.absolutePos(new BlockPos(14, 2, 14));
        Weaving.conductor(h, new BlockPos(8, 2, 8), 0);
        h.assertTrue(Weave.onLattice(level, machine) && Weave.onLattice(level, h.absolutePos(horn)),
                "Both ends stand within 8 of the conductor");
        h.assertTrue(LatticeNetwork.extractPulseNearby(level, machine, 8, 30, false) == 30,
                "Thirteen blocks apart, one conductor between them carries the Pulse");
        var reading = Weave.read(level, machine);
        h.assertTrue(reading.conductors() == 1 && reading.generators() == 1 && reading.tapRate() == 64,
                "One Woven conductor, one generator, read " + reading);
        h.succeed();
    }

    /** Two groups of conductors out of reach of each other are two networks, and keep their Pulse apart. */
    @GameTest(template = "empty")
    public static void twoNetworksOutOfReachStaySeparate(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(1, 2, 1, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        var hornA = at(h, new BlockPos(1, 2, 1), tk.darrow.tribalpower.generator.EmberHornBlockEntity.class);
        hornA.insertPulse(100, false);
        Weaving.conductor(h, new BlockPos(2, 2, 2));
        var machineA = h.absolutePos(new BlockPos(3, 2, 1));
        h.setBlock(14, 2, 14, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        var hornB = at(h, new BlockPos(14, 2, 14), tk.darrow.tribalpower.generator.EmberHornBlockEntity.class);
        hornB.insertPulse(100, false);
        Weaving.conductor(h, new BlockPos(13, 2, 13));
        var machineB = h.absolutePos(new BlockPos(12, 2, 14));
        var a = Weave.tapsAt(level, machineA).first();
        var b = Weave.tapsAt(level, machineB).first();
        h.assertTrue(a != null && b != null && a.net() != b.net(), "Conductors 11 apart weave two networks");
        h.assertTrue(LatticeNetwork.extractPulseNearby(level, machineA, 8, 1000, false) == 100,
                "A machine draws only its own network's horn");
        h.assertTrue(hornA.getPulseStored() == 0 && hornB.getPulseStored() == 100, "The other network's horn is untouched");
        // A conductor placed out on the far network re-weaves only that one: this network keeps its cached weave.
        var kept = Weave.tapsAt(level, machineA).first().net();
        Weaving.conductor(h, new BlockPos(14, 2, 11));
        h.assertTrue(Weave.tapsAt(level, machineA).first().net() == kept, "A far conductor leaves this network's weave alone");
        h.assertTrue(Weave.tapsAt(level, machineB).first().net().conductors().size() == 2, "The far network takes it in");
        Weaving.conductor(h, new BlockPos(8, 2, 8));
        h.assertTrue(Weave.tapsAt(level, machineA).first().net() == Weave.tapsAt(level, machineB).first().net(),
                "A conductor between them links the two into one");
        h.assertTrue(LatticeNetwork.extractPulseNearby(level, machineA, 8, 1000, false) == 100,
                "Linked, the far horn is reachable");
        h.succeed();
    }

    /**
     * A Woven conductor carries 64 Pulse a second, whatever stands behind it; a Manifested one carries 4,096, enough
     * for a top Ley Heart. The budget renews each second.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void conductorRankCapsThroughput(GameTestHelper h) {
        var level = h.getLevel();
        var heart = heart(h, new BlockPos(2, 2, 2), LeyHeartBlockEntity.CAPACITY);
        var conductor = Weaving.conductor(h, new BlockPos(4, 2, 2), 0);
        var machine = h.absolutePos(new BlockPos(6, 2, 2));
        int woven = LatticeNetwork.extractPulseNearby(level, machine, 8, 5000, false);
        h.assertTrue(woven == Weave.rate(0) && woven == 64, "A Woven conductor caps the second at 64, drew " + woven);
        h.assertTrue(LatticeNetwork.extractPulseNearby(level, machine, 8, 10, true) == 0, "Nothing more this second");
        h.assertTrue(!LatticeNetwork.tryExtractPulseNearby(level, machine, 8, 1), "All-or-nothing sees the cap too");
        MachineRank.apply(conductor, MachineRank.MAX);
        int manifested = LatticeNetwork.extractPulseNearby(level, machine, 8, 5000, false);
        h.assertTrue(manifested == Weave.rate(3) - 64 && Weave.rate(3) == 4096,
                "Manifested, the same second carries the rest of 4,096, drew " + manifested);
        h.assertTrue(Weave.rate(1) == 256 && Weave.rate(2) == 1024, "Attuned 256, Bound 1,024");
        long second = level.getGameTime() / 20;
        h.succeedWhen(() -> {
            h.assertTrue(level.getGameTime() / 20 > second, "Wait for the next second");
            heart.insertPulse(LeyHeartBlockEntity.CAPACITY, false);
            int next = LatticeNetwork.extractPulseNearby(level, machine, 8, 5000, true);
            h.assertTrue(next == 4096, "A new second renews the whole 4,096, saw " + next);
        });
    }

    /**
     * Four Bound conductors around one generator are four lanes out of it: Pulse that leaves through one and arrives
     * through another counts against both, and the machine's own conductor is the narrow end.
     */
    @GameTest(template = "empty")
    public static void severalConductorsAddTheirLanes(GameTestHelper h) {
        var level = h.getLevel();
        heart(h, new BlockPos(2, 2, 2), LeyHeartBlockEntity.CAPACITY);
        Weaving.conductor(h, new BlockPos(1, 2, 2), 2);
        Weaving.conductor(h, new BlockPos(3, 2, 2), 2);
        Weaving.conductor(h, new BlockPos(2, 2, 1), 2);
        Weaving.conductor(h, new BlockPos(2, 2, 3), 2);
        // Every machine spot sits in reach of all four conductors: Pulse leaves and arrives through the same one.
        var machine = h.absolutePos(new BlockPos(4, 2, 4));
        int drawn = LatticeNetwork.extractPulseNearby(level, machine, 8, 8000, false);
        h.assertTrue(drawn == 4 * 1024, "Four Bound conductors carry 4,096 together, drew " + drawn);
        h.succeed();
    }

    /** A Pulse Cairn stores and moves Pulse by rank: 4,000 a Woven stone up to 256,000 Manifested, 64 to 4,096 a second. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void rankedCairnsHoldAndMoveMore(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(2, 2, 2, ModBlocks.PULSE_CAIRN.get());
        var cairn = at(h, new BlockPos(2, 2, 2), PulseCairnBlockEntity.class);
        h.assertTrue(cairn.getPulseCapacity() == 4000 && cairn.pile().rate() == 64, "An unranked stone is Woven");
        MachineRank.apply(cairn, 3);
        h.assertTrue(cairn.getPulseCapacity() == 256000 && cairn.pile().rate() == 4096,
                "A Manifested stone holds 256,000 and moves 4,096, read " + cairn.getPulseCapacity() + " / " + cairn.pile().rate());
        MachineRank.apply(cairn, 1);
        h.assertTrue(cairn.getPulseCapacity() == 16000 && cairn.pile().rate() == 256, "Attuned: 16,000 and 256");
        h.setBlock(2, 2, 3, ModBlocks.PULSE_CAIRN.get());
        var second = at(h, new BlockPos(2, 2, 3), PulseCairnBlockEntity.class);
        MachineRank.apply(second, 2);
        h.assertTrue(cairn.getPulseCapacity() == 16000 + 64000 && cairn.pile().rate() == 256 + 1024,
                "A pile sums its stones by rank, read " + cairn.getPulseCapacity() + " / " + cairn.pile().rate());
        cairn.insertPulse(70000, false);
        h.assertTrue(cairn.ownPulse() <= 16000 && second.ownPulse() <= 64000 && cairn.getPulseStored() == 70000,
                "Each stone keeps no more than its own rank holds");

        // Out through a Manifested conductor: the pile's own rate is the narrow end.
        Weaving.conductor(h, new BlockPos(4, 2, 2));
        var machine = h.absolutePos(new BlockPos(6, 2, 2));
        int out = LatticeNetwork.extractPulseNearby(level, machine, 8, 100000, false);
        h.assertTrue(out == 256 + 1024, "The pile lends its summed rate a second, lent " + out);

        // In: a fresh Woven stone drinks a generator's surplus at 64 a second.
        h.setBlock(10, 2, 10, ModBlocks.PULSE_CAIRN.get());
        var drinker = at(h, new BlockPos(10, 2, 10), PulseCairnBlockEntity.class);
        heart(h, new BlockPos(12, 2, 12), LeyHeartBlockEntity.CAPACITY);
        Weaving.conductor(h, new BlockPos(11, 2, 11));
        int got = Weave.fillPile(level, drinker.pile());
        h.assertTrue(got == 64 && drinker.getPulseStored() == 64, "A Woven stone drinks 64 a second, took " + got);
        h.assertTrue(Weave.fillPile(level, drinker.pile()) == 0, "And no more in the same second");
        h.succeed();
    }

    /** A cairn drinks only a generator's surplus: a generator at half or less is left for its machines. */
    @GameTest(template = "empty")
    public static void aCairnLeavesAGeneratorItsWorkingHalf(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(2, 2, 2, ModBlocks.PULSE_CAIRN.get());
        var cairn = at(h, new BlockPos(2, 2, 2), PulseCairnBlockEntity.class);
        MachineRank.apply(cairn, 3);
        var heart = heart(h, new BlockPos(4, 2, 4), LeyHeartBlockEntity.CAPACITY / 2);
        Weaving.conductor(h, new BlockPos(3, 2, 3));
        h.assertTrue(Weave.fillPile(level, cairn.pile()) == 0, "Half full is all working room");
        heart.insertPulse(1000, false);
        int got = Weave.fillPile(level, cairn.pile());
        h.assertTrue(got == 1000, "Only what sits above half moves, took " + got);
        h.succeed();
    }

    /**
     * A totem on an active network fills and sits full; off the lattice it drains and falls silent, and a silent totem
     * lends no voice. Its keeping clock is a separate thing: a quiet totem with a full buffer still has its voice,
     * and an answered one with an empty buffer does not.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aTotemBufferIsItsVoiceAndKeepingStaysSeparate(GameTestHelper h) {
        var level = h.getLevel();
        var totemPos = new BlockPos(4, 2, 2);
        h.setBlock(totemPos, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var totem = at(h, totemPos, ResonanceTotemBlockEntity.class);
        var station = h.absolutePos(new BlockPos(6, 2, 2));
        h.assertTrue(totem.getPulseStored() == ResonanceTotemBlockEntity.BUFFER && totem.voiced(), "A new totem starts full");
        totem.extractPulse(ResonanceTotemBlockEntity.BUFFER - 3, false);
        h.setBlock(2, 2, 4, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        var horn = at(h, new BlockPos(2, 2, 4), tk.darrow.tribalpower.generator.EmberHornBlockEntity.class);
        horn.insertPulse(400, false);
        // Off the lattice: 2 a second drains 3 Pulse in two seconds.
        h.runAfterDelay(45, () -> {
            h.assertTrue(totem.getPulseStored() == 0 && !totem.voiced(), "Off the lattice the buffer drains, holds " + totem.getPulseStored());
            h.assertTrue(totem.keeping() == Keeping.State.ANSWERED, "Its keeping clock is untouched");
            h.assertTrue(!LatticeNetwork.hasAttunement(level, station, 8, Attunement.EARTH), "An empty totem is silent: no voice");
            var near = LatticeNetwork.TotemsNear.of(level, station, 8);
            h.assertTrue(near.silent(Attunement.EARTH), "Silent, not missing");
            h.assertTrue(horn.getPulseStored() == 400, "Draining costs the camp nothing");
            Weaving.conductor(h, new BlockPos(2, 2, 2));
            h.runAfterDelay(45, () -> {
                h.assertTrue(totem.getPulseStored() == ResonanceTotemBlockEntity.BUFFER, "On an active network it fills, holds " + totem.getPulseStored());
                h.assertTrue(LatticeNetwork.hasAttunement(level, station, 8, Attunement.EARTH), "Full again, its voice is back");
                int hornNow = horn.getPulseStored();
                totem.setAttention(0);
                h.assertTrue(totem.keeping() == Keeping.State.QUIET, "Keeping can go quiet on its own");
                h.assertTrue(LatticeNetwork.hasAttunement(level, station, 8, Attunement.EARTH)
                                && Keeping.voice(level, station, Attunement.EARTH) == Keeping.State.QUIET,
                        "A quiet totem with a full buffer has its voice, and is quiet: two separate conditions");
                h.runAfterDelay(25, () -> {
                    h.assertTrue(horn.getPulseStored() == hornNow, "Sitting full costs nothing, horn " + horn.getPulseStored());
                    h.setBlock(2, 2, 2, Blocks.AIR);
                    horn.extractPulse(Integer.MAX_VALUE, false);
                    h.succeed();
                });
            });
        });
    }

    /** A dry network is not an active one: a totem on it drains too. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aDryNetworkLetsATotemDrain(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var totem = at(h, new BlockPos(4, 2, 2), ResonanceTotemBlockEntity.class);
        h.setBlock(2, 2, 4, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        Weaving.conductor(h, new BlockPos(2, 2, 2));
        h.assertTrue(Weave.onLattice(level, h.absolutePos(new BlockPos(4, 2, 2))) && !Weave.active(level, h.absolutePos(new BlockPos(4, 2, 2))),
                "On the lattice, but nothing in its sources");
        int before = totem.getPulseStored();
        h.runAfterDelay(45, () -> {
            h.assertTrue(totem.getPulseStored() < before, "A dry lattice lets the voice fade, holds " + totem.getPulseStored());
            h.succeed();
        });
    }

    /** The lattice change is told once per player who played before it, never to a brand new one. */
    @GameTest(template = "empty")
    public static void theLatticeNoticeShowsOnce(GameTestHelper h) {
        // Joining the level already ran the login hook: a mock player has no play time, so it was marked, not told.
        var newcomer = VerificationPlayers.inLevel(h);
        h.assertTrue(persisted(newcomer).getInt(LatticeNotice.KEY) == LatticeNotice.VERSION,
                "The login hook marks a brand new player at once");
        h.assertTrue(!LatticeNotice.greet(newcomer), "A brand new player had no old base to lose");
        newcomer.awardStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME), 1200);
        h.assertTrue(!LatticeNotice.greet(newcomer), "And is not told later either");

        // A player from before the change: play time, and no mark yet.
        var veteran = VerificationPlayers.inLevel(h);
        var data = persisted(veteran);
        data.remove(LatticeNotice.KEY);
        veteran.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, data);
        veteran.awardStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME), 1200);
        h.assertTrue(LatticeNotice.greet(veteran), "A player from before the change is told");
        h.assertTrue(!LatticeNotice.greet(veteran), "Only once");
        h.assertTrue(persisted(veteran).getInt(LatticeNotice.KEY) == LatticeNotice.VERSION, "Remembered where death keeps it");
        h.succeed();
    }

    private static net.minecraft.nbt.CompoundTag persisted(net.minecraft.server.level.ServerPlayer player) {
        return player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
    }

    /**
     * MSPT: a big base. 200 conductors woven into one network, 20 generators and 100 machines on it, every machine
     * drawing every tick for two seconds (machines really draw about once a second, so this is twenty times the
     * load). Timed: the cold weave, the draws per tick, and a re-weave after a conductor is placed or broken, warm.
     */
    @GameTest(template = "empty", batch = "lattice_weave_perf", timeoutTicks = 200)
    public static void aLargeLatticeStaysCheap(GameTestHelper h) {
        var level = h.getLevel();
        // Two layers of 10 x 10 conductors, inside the template so no neighbouring test is in reach.
        for (int layer = 0; layer < 2; layer++)
            for (int x = 3; x <= 12; x++)
                for (int z = 3; z <= 12; z++)
                    h.setBlock(x, 3 + 2 * layer, z, ModBlocks.LATTICE_CONDUCTOR.get());
        for (int i = 0; i < 20; i++) {
            BlockPos pos = new BlockPos(3 + (i % 10), 1, 3 + (i / 10) * 9);
            h.setBlock(pos, tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
            at(h, pos, tk.darrow.tribalpower.generator.EmberHornBlockEntity.class).insertPulse(1000, false);
        }
        BlockPos[] machines = new BlockPos[100];
        for (int i = 0; i < 100; i++) machines[i] = h.absolutePos(new BlockPos(3 + (i % 10), 2 + (i / 50) * 2, 3 + (i / 10) % 10));
        long t0 = System.nanoTime();
        h.assertTrue(Weave.draw(level, machines[0], 1, true) == 1, "The big network carries");
        long cold = System.nanoTime() - t0;
        var reading = Weave.read(level, machines[0]);
        h.assertTrue(reading.conductors() == 200 && reading.generators() == 20, "One network of 200 conductors, read " + reading);
        long[] drawNanos = new long[1];
        int[] drawn = new int[1], ticks = new int[1];
        h.onEachTick(() -> {
            if (ticks[0] >= 40) return;
            ticks[0]++;
            long t = System.nanoTime();
            for (BlockPos machine : machines) drawn[0] += Weave.draw(level, machine, 2, false);
            drawNanos[0] += System.nanoTime() - t;
        });
        h.succeedWhen(() -> {
            h.assertTrue(ticks[0] >= 40, "Draw for two seconds");
            h.assertTrue(drawn[0] == 8000, "Every draw is served, drew " + drawn[0]);
            // Re-weaving, warm: a conductor placed, then broken, five times, each followed by a draw.
            long reweave = 0, weaveOnly = 0;
            for (int i = 0; i < 5; i++) {
                h.setBlock(1, 3, 1, ModBlocks.LATTICE_CONDUCTOR.get());
                long t = System.nanoTime();
                Weave.tapsAt(level, machines[0]);
                weaveOnly += System.nanoTime() - t;
                Weave.draw(level, machines[0], 1, true);
                reweave += System.nanoTime() - t;
                h.setBlock(1, 3, 1, Blocks.AIR);
            }
            double perTick = drawNanos[0] / 40.0 / 1e6;
            tk.darrow.tribalpower.TribalPower.LOGGER.info(String.format(java.util.Locale.ROOT,
                    "[lattice-perf] 200 conductors, 20 generators, 100 machines: cold weave %.3f ms, 100 draws %.3f ms a tick (%.1f us a draw), warm re-weave %.3f ms (%.3f ms of it the conductor walk)",
                    cold / 1e6, perTick, perTick * 10.0, reweave / 5.0 / 1e6, weaveOnly / 5.0 / 1e6));
            h.assertTrue(perTick < 5.0, "100 draws must stay well under a tick's budget, took " + perTick + " ms");
        });
    }
}
