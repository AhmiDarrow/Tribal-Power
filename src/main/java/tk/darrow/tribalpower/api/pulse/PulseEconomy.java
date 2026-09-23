package tk.darrow.tribalpower.api.pulse;

/**
 * Where Tribal Power's Pulse sits among the tech mods it is played beside.
 *
 * <p>Pulse was balanced against itself and drifted out of the pack. The Resonator, the mod's
 * end-game generator, made 20 Pulse a second while an Ember Kiln drew 32 and a ranked one drew 58;
 * the Harmonic Energizer put out 100 FE/t, which is less than a single Mekanism Enrichment Chamber needs
 * to turn over. Every number below is now derived from one anchor and checked against the pack
 * rather than chosen by feel.
 *
 * <h2>The anchor</h2>
 * <p><b>1 Pulse = {@value #FE_PER_PULSE} FE</b>, the rate the Harmonic Energizer has always converted at
 * and the Codex has always quoted. So a rate in Pulse a second is {@code pulse * 100 / 20}, or
 * {@link #fePerTick(int) pulse * 5}, in FE a tick.
 *
 * <h2>What it is measured against</h2>
 * <p>Read out of the Ninjacat Skies instance's own configs, in FE a tick:
 * <pre>
 *   Powah furnator / magmator   20    80   200    800   2,000   8,000  40,000
 *   Powah solar panel           20    60   100    200     400     800   2,000
 *   Powah reactor              250 1,000 2,500 10,000  25,000 100,000 500,000
 *   Solar Flux panel             1     8    32    128     512   2,048  32,768
 *   Mekanism solar 125 · advanced solar 750 · heat generator 500 · bio generator 875
 *   createaddition motor 480 at max RPM · alternator 5,000 cap
 *
 *   Mekanism machine draw: 125 enrichment / crusher / smelter / sawmill
 *                          250 osmium compressor, electric pump
 *                          500 purification, chemical oxidiser / infuser / washer
 *                        1,000 chemical injection / dissolution / crystalliser
 *                        2,500 digital miner
 * </pre>
 *
 * <h2>Where Tribal Power lands</h2>
 * <p>The mod is the pack's primitive tech, so it aims at the low-to-middle of that spread: from
 * about a Powah starter at the bottom to about a Powah blazing at the very top, and it never
 * reaches reactors. In FE a tick equivalent:
 * <pre>
 *   Ley Collector, poor site        5     under everything, which is the point
 *   Ley Collector, perfect site   120     ~ Mekanism solar 125
 *   Drumheart, one, on tempo      120     ~ Mekanism solar 125
 *   Drumheart, two (a zone)       240     ~ Powah hardened 200
 *   Resonator, 2 voices + shard    40     ~ Powah starter 20
 *   Resonator, 4 voices + bound   240     ~ Powah hardened 200
 *   Resonator, 6 ringed + core    720     ~ Mekanism advanced solar 750, Powah blazing 800
 *   Harmonic Energizer out        300     between Powah hardened 200 and Mekanism heat gen 500
 *   Lattice Converter in      240-440     the FE it eats to make 40 Pulse a second back
 * </pre>
 *
 * <p>And the draws, which were already in band and are left alone:
 * <pre>
 *   Echo Shatter            20-100     under Mekanism's cheapest machine
 *   Echo Attune / Bind      80-120     ~ Mekanism enrichment 125
 *   Ember Kiln                 160     a little over a Mekanism smelter
 *   Ember Kiln, rank 3         290     ~ Mekanism osmium compressor 250
 *   Echo Manifest          160-200     ~ Powah hardened 200
 *   Machine / gear ranking 240-400     under Mekanism purification 500
 * </pre>
 *
 * <p>{@code PulseEconomyGameTests} holds every one of these to the band, so the next change to a
 * rate has to argue with the pack rather than with nothing.
 */
public final class PulseEconomy {
    private PulseEconomy() {}

    /** The one exchange rate, and the only place it is stated as a number. */
    public static final int FE_PER_PULSE = 100;

    /** The weakest thing in the pack worth comparing to: a Powah starter furnator, in FE a tick. */
    public static final int PACK_FLOOR = 20;

    /**
     * The strongest Tribal Power is allowed to reach, in FE a tick.
     *
     * <p>A Powah blazing furnator is 800 and a Mekanism advanced solar is 750. Above that the pack
     * has niotic, spirited, nitro and reactors, and a ritual generator has no business there.
     */
    public static final int PACK_CEILING = 900;

    /** A rate in Pulse a second, as FE a tick. */
    public static int fePerTick(int pulsePerSecond) {
        return pulsePerSecond * FE_PER_PULSE / 20;
    }

    /** A rate in FE a tick, as Pulse a second. */
    public static int pulsePerSecond(int fePerTick) {
        return fePerTick * 20 / FE_PER_PULSE;
    }

    /** Whether a generator's rate sits in the band this mod aims at. */
    public static boolean inBand(int pulsePerSecond) {
        int fe = fePerTick(pulsePerSecond);
        return fe >= 0 && fe <= PACK_CEILING;
    }
}
