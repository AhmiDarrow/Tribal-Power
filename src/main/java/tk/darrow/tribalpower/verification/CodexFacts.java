package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import tk.darrow.tribalpower.blockentity.StoneFontBlockEntity;
import tk.darrow.tribalpower.client.codex.CodexBook;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.pit.OreBand;

/**
 * Numbers and rules the Codex states that must match the game. Each fact names the entry that teaches it and
 * a phrase that entry must contain; a few also name wording that would teach the old, wrong behaviour.
 * When a value changes in Java, change it here and in the entry together.
 */
final class CodexFacts {
    record Fact(String entry, List<String> says, List<String> never, String why) {
        static Fact of(String entry, String why, String... says) {
            return new Fact(entry, List.of(says), List.of(), why);
        }

        Fact never(String... phrases) {
            return new Fact(entry, says, List.of(phrases), why);
        }
    }

    static final List<Fact> FACTS = List.of(
            Fact.of("drumheart", "A Drumheart holds 1,000 Pulse and ranks at 15% per beat", "1,000 Pulse", "15% Pulse a beat"),
            // Stacking drums used to be the answer to every power problem. The cap is the rule
            // that stopped it, so the book has to keep saying so.
            Fact.of("drumheart", "Only two drums in a zone earn", "two drums in a zone"),
            Fact.of("pulse_resonator", "A Resonator holds 2,500 Pulse, ranks at 15%, and a heap caps at five voices including kinship", "2,500 Pulse", "15% Pulse a second", "five voices", "Kinship"),
            Fact.of("pulse_resonator", "Seating a Resonator uses an Echo Shard", "Echo Shard"),
            // The catalyst used to be permanent and the book said so. It is spent by use now,
            // and the book must not go back to promising otherwise.
            Fact.of("pulse_resonator", "The Resonator catalyst is spent by the Pulse it makes",
                    "spent").never("never wears out"),
            Fact.of("lattice_conductor", "A Conductor links chalked totems and drains any nearby generator, voice-crafts included", "Ritual Chalk", "250 Pulse", "generators within 8 blocks"),
            Fact.of("lattice_conductor", "Conductors within 8 of each other extend the zone a machine draws from", "within 8 of each other", "Pulse Cairns"),
            Fact.of("spirit_pulse", "Totems hold 250 Pulse; one use fills a cell as far as the source holds", "250 Pulse", "fills the cell as far as")
                    .never("up to 25 Pulse", "100 per use, and"),
            Fact.of("keeping", "Keeping: 40 minutes Answered, Dim stretches station time about 30%", "40 minutes", "30%"),
            Fact.of("ley_collector", "A Ley Collector holds 2,000 Pulse, beats every two seconds and ranks at 15%", "2,000 Pulse", "two seconds", "15%").never("base trickle", "hum harder"),
            Fact.of("ley_lens", "Ley Lens: a roof makes a collector weaker, not dead", "16").never("starves a collector"),
            // LeyHeartGameTests pins 378 and the burn rates in Java; the book must quote the same.
            Fact.of("ley_heart", "A Ley Heart holds 8,000 Pulse, drinks 200 mB a second, raises lines 64 blocks long, makes 378 at its best and ranks at 15%",
                    "8,000 Pulse", "200 mB a second", "64 blocks", "378 Pulse a second", "15%", "2 minutes"),
            Fact.of("pulse_adapter", "The Harmonic Energizer exports FE and cannot receive it", "FE"),
            Fact.of("six_voices", "Kinship Totems lend a tribe's element to stations", "Kinship"),
            Fact.of("ember_horn", "A ranked Ember Horn adds 15% Pulse a second per rank", "15%").never("the rate is the real cap"),
            Fact.of("wind_harp", "A ranked Wind Harp adds 15% Pulse a second per rank", "15%").never("the whole of the wind"),
            Fact.of("wave_drum", "A ranked Wave Drum adds 15% Pulse a second per rank", "15%"),
            Fact.of("wake_bell", "A ranked Wake Bell adds 15% Pulse a second per rank", "15%"),
            Fact.of("loom_anchor", "A Loom Anchor counts quiet totems and ranks at 15%", "15%").never("hum harder"),
            Fact.of("pulse_cairn", "A Pulse Cairn stone swallows 200 Pulse a second; touching stones are one store up to 64", "200 a second", "one pile", "64 stones", "256,000 Pulse")
                    .never("first five", "sixth stone"),
            // Station Pulse is quoted as the game spends it at the shipped settings: see spend().
            Fact.of("echo_shatter", "A shard is 2 seconds at 16 Pulse a second, grit 4 seconds at 20, both before the consumption multiplier",
                    "2 seconds at " + spend(16) + " Pulse a second", "4 seconds at " + spend(tk.darrow.tribalpower.grit.GritRegistry.SHATTER_PULSE) + " Pulse a second"),
            Fact.of("echo_attune", "Attune is 20 Pulse a second and Bind 24, before the consumption multiplier",
                    "3 seconds at " + spend(20) + " Pulse a second", "4 seconds at " + spend(24) + " Pulse a second"),
            Fact.of("echo_manifest", "Manifest is 32 and 40 Pulse a second before the consumption multiplier",
                    "5 seconds at " + spend(32) + " Pulse a second", "8 seconds at " + spend(40) + " Pulse a second"),
            Fact.of("crafting_components", "March Ore shatters at the grit rate", "at " + spend(tk.darrow.tribalpower.grit.GritRegistry.SHATTER_PULSE) + " Pulse a second"),
            Fact.of("metals_and_gems", "Grit shatter is 4 seconds at the grit rate", "4 seconds at " + spend(tk.darrow.tribalpower.grit.GritRegistry.SHATTER_PULSE) + " Pulse a second"),
            Fact.of("glimmer_ridge", "Lighting Quartz Glass is an Echo Attune job at its spent rate", "at " + spend(tk.darrow.tribalpower.block.QuartzGlass.PULSE) + " Pulse a second"),
            Fact.of("spirit_pulse", "The book says once that its numbers are for the default settings", "for the default settings"),
            Fact.of("ember_kiln", "Kiln smelting is 32 Pulse a second before the consumption multiplier; cobble becomes stone",
                    "10 seconds at " + spend(tk.darrow.tribalpower.grit.GritRegistry.KILN_PULSE) + " Pulse a second").never("Grit or cobble goes in"),
            Fact.of("song_bench", "Empowering spends 16 Pulse. A sheet is 3 to 7 empowered reagents", "16 Pulse", "3 to 7"),
            Fact.of("machine_ranks", "Machine ranks are 48, 64 and 80 Pulse a second before the consumption multiplier; generators rank at 15%",
                    "16 seconds at " + spend(48) + " Pulse a second", "20 seconds at " + spend(64), "24 seconds at " + spend(80), "15%").never("Ranked generators hum harder"),
            Fact.of("offering_table", "The Offering Table is 27-slot storage, not a hearth", "27").never("Standing still applies"),
            Fact.of("rain_chime", "A Rain Chime is a weather comparator", "8"),
            Fact.of("hush_totem", "Camp devices share a 2,400 Pulse reserve and refill up to 80 Pulse a second", "2,400", "80 Pulse a second"),
            Fact.of("wayanchor", "A Wayanchor refills up to 80 Pulse a second", "80 a second", "2,400 Pulse"),
            Fact.of("summoning_cradle", "A Summoning Cradle spends 80 Pulse per summon", "80 Pulse"),
            Fact.of("grove_tender", "A Grove Tender must be placed by a player to claim it", "yourself"),
            Fact.of("gate_drum", "The Gate Drum needs no Pulse; the Gate Rite on A, S, D, F at 60% opens it", "needs no Pulse", "60%", "rattle").never("costs 20", "Place and strike", "25 Pulse"),
            Fact.of("waystone_compass", "A compass checks the landing before it spends Pulse", "spends nothing").never("refunds Pulse"),
            Fact.of("way_gate", "Way Gates apply a one-second cooldown", "one-second"),
            Fact.of("far_gate", "Far Gate lighting is 1,200 Pulse", "1,200 Pulse"),
            Fact.of("wireless_cargo", "Relays cost 4, 8 or 16 Pulse a beat before the consumption multiplier; plates belong to their owner",
                    "costs " + spend(4) + " Pulse", spend(8) + " Pulse a beat", spend(16) + " Pulse a beat", "only you and your camp")
                    .never("open to everyone", "before relays had owners"),
            Fact.of("stone_font", "Stone and obsidian are scaled by the consumption multiplier, cobble is not; cobble is output only",
                    "at " + StoneFontBlockEntity.Ask.COBBLE.pulsePerSecond() + " Pulse a second",
                    "3 seconds at " + spend(StoneFontBlockEntity.Ask.STONE.pulsePerSecond()) + " Pulse a second",
                    "12 seconds at " + spend(StoneFontBlockEntity.Ask.OBSIDIAN.pulsePerSecond(false)) + " Pulse a second",
                    "only " + spend(StoneFontBlockEntity.Ask.OBSIDIAN.pulsePerSecond(true)) + " Pulse a second").never("Cobble in, stone out"),
            Fact.of("voice_ring", "The Voice Ring still requires Answered totems", "Answered"),
            Fact.of("listening_pit", "The Resonance Mesh holds a 200 Pulse buffer", "200 Pulse"),
            Fact.of("what_the_ground_offers", "A common cycle is 10 seconds at 28 Pulse a second before the consumption multiplier",
                    "10 seconds at " + spend(OreBand.COMMON.pulsePerSecond()) + " Pulse a second",
                    "iron cycle is " + 10 * Integer.parseInt(spend(OreBand.COMMON.pulsePerSecond())) + " Pulse",
                    "hot band at " + spend(OreBand.HOT.pulsePerSecond()) + " Pulse a second").never("lapis and quartz"),
            Fact.of("redstone_language", "A held signal pauses every generator other than the Drumheart", "every generator except the Drumheart"),
            Fact.of("pulse_gauge", "A Threshold sings at or above its Pulse mark; an Inverse plate flips it", "Inverse", "at or above"),
            Fact.of("verse_link", "A Verse Answer copies the Call's strength", "copies"),
            Fact.of("spring_calling", "Spring Calling needs its Rite Pedestals and does not need you standing there", "four pedestals", "do not have to stand").never("while you stand still"),
            Fact.of("sixfold_staff", "Staff ranges and rests", "12 blocks", "8 blocks", "4 seconds", "18 blocks", "1.5 seconds").never("nearby enemies"),
            Fact.of("resonance_maul", "The Resonance Maul rests one second after a break", "one second"),
            Fact.of("spiritgear", "Gear ranks are 48, 64 and 96 Pulse a second before the consumption multiplier; Manifest needs Loom Thread from The Unsung",
                    "45 seconds at " + spend(48) + " Pulse a second", "90 seconds at " + spend(64), "180 seconds at " + spend(96), "Loom Thread", "25% harder").never("8 seconds"),
            Fact.of("spiritweave", "A full Manifested set: 20% less damage taken", "20%", "8 extra hearts"),
            Fact.of("spiritweave", "Bound Spiritweave spends 3 Pulse upkeep, unlinked 2", "3 Pulse", "2 Pulse"),
            Fact.of("totem_bound_gear", "An Earth hood turns aside one projectile in five", "one projectile in five"),
            Fact.of("totem_bound_gear", "An Earth axe fells the whole tree and charges a swing's Pulse per batch of logs",
                    "fells the whole tree", "every " + tk.darrow.tribalpower.item.TreeFelling.LOGS_PER_CHARGE + " logs").never("sometimes fells a second log"),
            Fact.of("spirit_charms", "Charm voices cost 40 Pulse each; Ward turns aside one projectile in four", "40 Pulse", "one projectile in four"),
            Fact.of("ritual_brazier", "Camp blessings include the Loom's luck", "luck"),
            Fact.of("pulse_lights", "Pulse lights draw 1 Pulse a second", "1 Pulse a second"),
            Fact.of("wind_charm", "A Wind Charm spends no Pulse", "no Pulse"),
            Fact.of("standing", "Hearth offerings cap at 60 standing a day and at most 40 Pulse", "60", "40 Pulse").never("camp blocks"),
            // TribalKinEntity waits DRUM_INTERVAL (120 ticks) less up to 19 random ticks between beats.
            Fact.of("tribal_kin", "Elders restock once a Minecraft day; a Drummer beats every 5 to 6 seconds", "5 to 6 seconds").never("generators within 8 blocks"),
            Fact.of("kinship_totem", "Kinship lends an element, not a seventh voice", "seventh"),
            Fact.of("familiar_gifts", "A Cinder Imp refunds 4 Pulse; an Echo Weaver gathers within 4 blocks", "4 Pulse", "4 blocks").never("a little Pulse"),
            // FamiliarBoost: the numbers the familiar chapter teaches.
            Fact.of("familiar_attuning", "Attuning reaches 6 blocks; an attuned familiar keeps totems within 8",
                    tk.darrow.tribalpower.familiar.FamiliarBoost.ATTUNE_RANGE + " blocks", tk.darrow.tribalpower.familiar.FamiliarBoost.KEEP_RANGE + " blocks"),
            Fact.of("familiar_boons", "An attuned familiar within 16 blocks takes 40% off its voice's charm and Spiritgear Pulse; a 1-Pulse spend cannot round lower, so it is free 40% of the time; only the nearest sitting familiar counts",
                    tk.darrow.tribalpower.familiar.FamiliarBoost.RANGE + " blocks", Math.round(tk.darrow.tribalpower.familiar.FamiliarBoost.DISCOUNT * 100) + "% less Pulse",
                    "free " + Math.round(tk.darrow.tribalpower.familiar.FamiliarBoost.DISCOUNT * 100) + "% of the time", "nearest one").never("a swing's 2 becomes 1"),
            Fact.of("welcome", "The landing mentions JEI as optional", "JEI"),
            // LatticeNetwork.canLink is closerThan(LINK_RANGE): 16 blocks apart is already too far.
            Fact.of("lattice_conductor", "Chalk links need the totems less than 16 blocks apart", "less than 16 blocks").never("within 16 blocks", "up to 16 blocks"),
            Fact.of("moving_power", "Chalk links need the totems less than 16 blocks apart", "less than 16 blocks").never("up to 16 blocks"),
            Fact.of("ritual_mark", "Chalk links need the totems less than 16 blocks apart", "less than 16 blocks").never("within 16 blocks"),
            // A lush totem ticks its keeping every other tick; a roofed one runs at the ordinary rate, not faster.
            Fact.of("keeping", "Lush totems keep twice as long; a roofed one keeps the normal pace", "twice as long", "normal pace").never("forgets you faster"),
            Fact.of("keeping_totems", "Lush totems keep twice as long; a roofed one keeps the normal pace", "twice as long", "normal pace").never("forgets you faster"),
            Fact.of("wave_drum", "Two Wave Drums beside water within 8 blocks share its 3 a second, rounding down: 1 a second each", "1 a second each").never("a little under half"),
            Fact.of("echo_unweave", "Unweaving Spiritweave returns the one wool Echo Bind took", "Spiritweave: 1 wool").never("Spiritweave: 2 wool"),
            // Song Thread is crafted around Loom Thread, and every song plate and the Verse Call around Song Thread.
            Fact.of("song_thread", "Song Thread needs Loom Thread, which only the March gives", "Loom Thread", "only the March gives"),
            Fact.of("song_plates", "Song plates wait on Song Thread and so on Loom Thread", "Loom Thread"),
            // PlateLinks: a wrench sync is one way, capped and ranged by TribalConfig's logic section.
            Fact.of("song_plates", "Plates sync with the Totem Wrench, one way, up to the shipped cap and reach",
                    "Totem Wrench", "runs one way", "up to " + TribalConfig.PLATE_LINK_MAX.getDefault() + " others",
                    "within " + TribalConfig.PLATE_LINK_RANGE.getDefault() + " blocks"),
            // LogicPlateBlock / PlateLinks / VerseLinkBlock: Ownership.check gates every change, reading stays open.
            Fact.of("song_plates", "Song plates belong to their placer and camp; changing another's is refused",
                    "belongs to whoever set it down and their camp", "is refused"),
            Fact.of("verse_link", "Verse plates belong to their placer and camp; retuning another's is refused",
                    "belongs to whoever set it down and their camp", "is refused"),
            // CharmHooks grants flight for the Air voice on any worn charm, not for the Sky charm alone.
            Fact.of("spirit_charms", "Any worn charm carrying Air lends flight", "any worn charm that carries Air").never("Sky's flight"),
            // CharmHooks.gatherRadius: the Gathering Charm's reach by voices bound.
            Fact.of("spirit_charms", "Gathering reach is 5 blocks, 2 more per extra voice, capped at 11",
                    "from " + (int) tk.darrow.tribalpower.charm.CharmHooks.gatherRadius(1) + " blocks",
                    (int) (tk.darrow.tribalpower.charm.CharmHooks.gatherRadius(2) - tk.darrow.tribalpower.charm.CharmHooks.gatherRadius(1)) + " more per extra voice",
                    "up to " + (int) tk.darrow.tribalpower.charm.CharmHooks.gatherRadius(99)),
            Fact.of("colossus_warden", "The boss is the Colossus Warden, not the Weeping Colossus tree; 260 health, 14 armour, Heartwood Core",
                    "Colossus Warden", "260 health", "14 armour", "Heartwood Core", "Reed Fen"),
            // The water creatures swim (CreatureSwimming): the gentle ones are fish, the hunters amphibious.
            Fact.of("beasts_shallows", "Gentle swimmers keep to the water and dry out beached; a bonded one follows only by water; the Brine Lurker leaves the water after prey",
                    "never leave the water", "dries out", "only as far as its water goes", "out onto the bank"),
            Fact.of("shades", "The Drowned Shade swims and walks after its prey", "the shore is no refuge"));

    /** Wording that quotes Pulse before the pack's settings. The book quotes what the shipped settings really spend. */
    private static final List<String> UNSCALED = List.of("before pack settings", "shipped default", "consumption multiplier");

    private CodexFacts() {}

    /**
     * What a station really spends a second for a base cost, at the shipped consumption multiplier and rounded as
     * {@link TribalConfig#scaleConsumption} rounds. The pins use the default, not the live config, because the book
     * is written for the shipped settings.
     */
    static String spend(int base) {
        return String.valueOf(Math.max(1, (int) Math.round(base * TribalConfig.CONSUMPTION_MULTIPLIER.getDefault())));
    }

    static void check(GameTestHelper h, CodexBook.Book book) {
        for (Fact fact : FACTS) {
            CodexBook.Entry entry = book.byId().get(fact.entry());
            h.assertTrue(entry != null, "Codex fact names a missing entry " + fact.entry() + ": " + fact.why());
            String text = words(entry);
            for (String phrase : fact.says())
                h.assertTrue(text.contains(phrase), fact.entry() + " must say \"" + phrase + "\": " + fact.why());
            for (String phrase : fact.never())
                h.assertFalse(text.contains(phrase), fact.entry() + " must not say \"" + phrase + "\": " + fact.why());
        }
        for (CodexBook.Entry entry : book.entries()) {
            String text = words(entry);
            for (String phrase : UNSCALED)
                h.assertFalse(text.contains(phrase), entry.id() + " must not say \"" + phrase + "\": the book quotes real Pulse at the shipped settings");
        }
        // The bestiary: every creature the mod registers is described in one entry, found by its spawn egg.
        List<String> eggs = new java.util.ArrayList<>();
        for (var profile : tk.darrow.tribalpower.entity.CreatureProfile.values()) eggs.add("tribalpower:" + profile.id + "_spawn_egg");
        for (var egg : tk.darrow.tribalpower.wildlife.Wildlife.EGGS) eggs.add(egg.getId().toString());
        for (String egg : eggs)
            h.assertTrue(book.entries().stream().filter(e -> e.items().contains(egg)).count() == 1,
                    "Exactly one Codex entry must list " + egg + ": the one that describes its creature");
    }

    /** Every word the reader sees in an entry, markup removed, as one string. */
    static String words(CodexBook.Entry entry) {
        StringBuilder out = new StringBuilder(entry.name()).append(' ');
        for (CodexBook.Page page : entry.pages()) {
            out.append(page.title()).append(' ').append(strip(page.text())).append(' ');
            if (page instanceof CodexBook.Scene scene)
                for (CodexBook.Step step : scene.steps()) out.append(strip(step.caption())).append(' ');
        }
        return out.toString().replaceAll("\\s+", " ");
    }

    /** Markup removal without touching item registries (item references keep their id). */
    private static String strip(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1").replaceAll("\\[(.+?)\\]\\([a-z0-9_]+\\)", "$1");
    }
}
