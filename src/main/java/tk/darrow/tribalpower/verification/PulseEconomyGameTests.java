package tk.darrow.tribalpower.verification;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.PulseEconomy;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LatticeConverterBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseAdapterBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.grit.GritRegistry;
import tk.darrow.tribalpower.ley.LeyMath;

/**
 * Holds the Pulse economy to the pack it is played in.
 *
 * <p>Pulse was balanced against itself and drifted: the end-game Resonator made 20 a second while a
 * ranked Ember Kiln drew 58, and the FE adapter put out less than one Mekanism machine consumes.
 * Nothing caught it, because nothing here had ever compared a rate to anything outside the mod.
 * {@link PulseEconomy} carries the pack's real numbers; these tests hold the mod to them.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class PulseEconomyGameTests {

    /** Every generator must land inside the band, and the ladder must actually climb. */
    @GameTest(template="empty")
    public static void everyGeneratorSitsInThePacksBand(GameTestHelper h) {
        int collector = perSecondFromBeat(LeyCollectorBlockEntity.beatFor(LeyMath.MAX_GAIN),
                LeyCollectorBlockEntity.GAIN_INTERVAL);
        int oneDrum = DrumheartBlockEntity.ON_TEMPO;
        int zoneOfDrums = oneDrum * DrumheartBlockEntity.MAX_PER_ZONE;
        int entryResonator = PulseResonatorBlockEntity.gainFor(2, 1);
        int fullResonator = PulseResonatorBlockEntity.gainFor(6, 4);

        for (int rate : new int[]{collector, oneDrum, zoneOfDrums, entryResonator, fullResonator})
            h.assertTrue(PulseEconomy.inBand(rate),
                    rate + " Pulse/s is " + PulseEconomy.fePerTick(rate)
                            + " FE/t, past the " + PulseEconomy.PACK_CEILING + " FE/t ceiling");

        // The best build in the mod has to be worth building: at least a Powah hardened furnator,
        // which is 200 FE/t, or there is no reason to gather six voices.
        h.assertTrue(PulseEconomy.fePerTick(fullResonator) >= 200,
                "A full Resonator makes " + PulseEconomy.fePerTick(fullResonator)
                        + " FE/t, under a Powah hardened furnator at 200");

        // And the ladder has to climb: a perfect ley site, then a drum, then a zone of them, then
        // the ritual build. Equal rungs are allowed, going backwards is not.
        h.assertTrue(collector <= zoneOfDrums && zoneOfDrums < fullResonator,
                "The ladder must climb: collector " + collector + ", drums " + zoneOfDrums
                        + ", resonator " + fullResonator);
        h.assertTrue(entryResonator < fullResonator, "A bare Resonator must not match a built one");
        h.succeed();
    }

    /**
     * The adapter has to be able to run at least one machine from the mods around it.
     *
     * <p>It converted 20 Pulse a second into 100 FE/t. A Mekanism Enrichment Chamber draws 125 and
     * a Powah basic furnator makes 80, so the adapter could not turn over the cheapest machine in
     * the pack and was strictly worse than the generator you would otherwise have built.
     */
    @GameTest(template="empty")
    public static void theAdapterCanActuallyRunAMachine(GameTestHelper h) {
        int fePerTick = PulseAdapterBlockEntity.RATE * PulseAdapterBlockEntity.FE_PER_PULSE / 20;
        h.assertTrue(fePerTick >= 125,
                "The adapter makes " + fePerTick + " FE/t and the cheapest Mekanism machine draws 125");
        h.assertTrue(fePerTick <= PulseEconomy.PACK_CEILING,
                "The adapter makes " + fePerTick + " FE/t, past the " + PulseEconomy.PACK_CEILING
                        + " FE/t ceiling");
        // The exchange rate is quoted in the Codex, so it is not allowed to drift quietly.
        h.assertTrue(PulseAdapterBlockEntity.FE_PER_PULSE == PulseEconomy.FE_PER_PULSE,
                "The adapter and the economy must agree on what a Pulse is worth");
        // A buffer worth less than a few seconds of output stutters every time a machine idles.
        h.assertTrue(PulseAdapterBlockEntity.CAPACITY >= fePerTick * 20 * 4,
                "The adapter buffer holds " + PulseAdapterBlockEntity.CAPACITY
                        + " FE, under four seconds of its own output");
        h.succeed();
    }

    /**
     * A machine must cost less to run than the mod's own generators make, or the mod cannot power
     * itself -- which is what a ranked Ember Kiln at 58 a second against a 20 a second Resonator
     * meant in practice.
     */
    @GameTest(template="empty")
    public static void theModCanPowerItsOwnMachines(GameTestHelper h) {
        int fullResonator = PulseResonatorBlockEntity.gainFor(6, 4);
        int kiln = GritRegistry.KILN_PULSE;
        int rankedKiln = tk.darrow.tribalpower.item.MachineRank.scalePulse(null, kiln);

        h.assertTrue(fullResonator >= rankedKiln * 2,
                "A built Resonator makes " + fullResonator + " a second and a ranked kiln draws "
                        + rankedKiln + "; one generator should run at least two of them");
        // A zone of drums is the early-game answer and must at least turn over an unranked kiln.
        int zoneOfDrums = DrumheartBlockEntity.ON_TEMPO * DrumheartBlockEntity.MAX_PER_ZONE;
        h.assertTrue(zoneOfDrums >= kiln,
                "Two drums make " + zoneOfDrums + " and an Ember Kiln draws " + kiln);
        // And a machine must not cost more than the pack's own comparable: an Ember Kiln against a
        // Mekanism Energized Smelter at 125 FE/t, with headroom for it being a ritual device.
        h.assertTrue(PulseEconomy.fePerTick(kiln) <= 250,
                "An Ember Kiln costs " + PulseEconomy.fePerTick(kiln)
                        + " FE/t against a Mekanism smelter at 125");
        h.succeed();
    }

    /**
     * The Converter must never close a free-energy loop.
     *
     * <p>The Harmonic Energizer pays out {@value tk.darrow.tribalpower.api.pulse.PulseEconomy#FE_PER_PULSE}
     * FE a Pulse. If the Converter ever bought a Pulse back for less than that, a player could wire
     * the two together and print power. Its cheapest possible rate has to stay above the payout at
     * every voice count, including counts no build can actually reach.
     */
    @GameTest(template="empty")
    public static void theConverterCannotPrintEnergy(GameTestHelper h) {
        for (int voices = 0; voices <= 32; voices++) {
            int cost = LatticeConverterBlockEntity.costAt(voices);
            h.assertTrue(cost > PulseAdapterBlockEntity.FE_PER_PULSE,
                    "At " + voices + " voices a Pulse costs " + cost + " FE but the Energizer pays "
                            + PulseAdapterBlockEntity.FE_PER_PULSE + ": that is a free-energy loop");
        }
        h.assertTrue(LatticeConverterBlockEntity.COST_FLOOR > PulseAdapterBlockEntity.FE_PER_PULSE,
                "The cost floor must sit above the Energizer payout");
        h.succeed();
    }

    /** Voices are the whole point of the block: each one must make the conversion cheaper. */
    @GameTest(template="empty")
    public static void voicesMakeTheConverterCheaper(GameTestHelper h) {
        int previous = Integer.MAX_VALUE;
        for (int voices = 0; voices <= 5; voices++) {
            int cost = LatticeConverterBlockEntity.costAt(voices);
            h.assertTrue(cost < previous,
                    "Voice " + voices + " must cut the cost; stayed at " + cost);
            previous = cost;
        }
        h.assertTrue(LatticeConverterBlockEntity.costAt(0) == LatticeConverterBlockEntity.COST_BASE,
                "With no voices it runs at the base rate");
        h.assertTrue(LatticeConverterBlockEntity.costAt(99) == LatticeConverterBlockEntity.COST_FLOOR,
                "However many voices, it stops at the floor");
        // A ring of voices should be worth building: comfortably cheaper than bare ground.
        h.assertTrue(LatticeConverterBlockEntity.costAt(5) * 3 <= LatticeConverterBlockEntity.COST_BASE * 2,
                "A full ring must be appreciably cheaper than no voices at all");
        h.succeed();
    }

    /** What it costs to run, measured against the pack like everything else. */
    @GameTest(template="empty")
    public static void theConverterCostsAPackReasonableAmountOfFe(GameTestHelper h) {
        int best = LatticeConverterBlockEntity.RATE * LatticeConverterBlockEntity.COST_FLOOR / 20;
        int worst = LatticeConverterBlockEntity.RATE * LatticeConverterBlockEntity.COST_BASE / 20;
        h.assertTrue(best >= 125,
                "Running it flat out costs " + best + " FE/t, under one Mekanism machine: too cheap");
        h.assertTrue(worst <= PulseEconomy.PACK_CEILING,
                "At its worst it eats " + worst + " FE/t, past the " + PulseEconomy.PACK_CEILING
                        + " FE/t ceiling");
        h.assertTrue(PulseEconomy.inBand(LatticeConverterBlockEntity.RATE),
                "What it makes must sit in the band like any other generator");
        h.succeed();
    }

    private static int perSecondFromBeat(int beat, int intervalTicks) {
        return beat * 20 / intervalTicks;
    }
}
