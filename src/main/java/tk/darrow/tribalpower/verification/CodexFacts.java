package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import tk.darrow.tribalpower.client.codex.CodexBook;

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
            Fact.of("spirit_pulse", "Totems hold 250 Pulse; a cell carries Pulse for tools, spells and travel", "250 Pulse", "25 Pulse"),
            Fact.of("keeping", "Keeping: 40 minutes Answered, Dim stretches station time about 30%", "40 minutes", "30%"),
            Fact.of("ley_collector", "A Ley Collector holds 2,000 Pulse, beats every two seconds and ranks at 15%", "2,000 Pulse", "two seconds", "15%").never("base trickle", "hum harder"),
            Fact.of("ley_lens", "Ley Lens: a roof makes a collector weaker, not dead", "16").never("starves a collector"),
            Fact.of("pulse_adapter", "The Harmonic Energizer exports FE and cannot receive it", "FE"),
            Fact.of("six_voices", "Kinship Totems lend a tribe's element to stations", "Kinship"),
            Fact.of("ember_horn", "A ranked Ember Horn adds 15% Pulse a second per rank", "15%").never("the rate is the real cap"),
            Fact.of("wind_harp", "A ranked Wind Harp adds 15% Pulse a second per rank", "15%").never("the whole of the wind"),
            Fact.of("wave_drum", "A ranked Wave Drum adds 15% Pulse a second per rank", "15%"),
            Fact.of("wake_bell", "A ranked Wake Bell adds 15% Pulse a second per rank", "15%"),
            Fact.of("loom_anchor", "A Loom Anchor counts quiet totems and ranks at 15%", "15%").never("hum harder"),
            Fact.of("pulse_cairn", "A Pulse Cairn swallows 200 Pulse a second", "200 a second"),
            Fact.of("echo_shatter", "Grit shatter is 4 seconds at 20 Pulse a second before pack settings", "20 Pulse a second"),
            Fact.of("ember_kiln", "Kiln smelting spends 32 Pulse a second; cobble becomes stone", "32 Pulse a second").never("Grit or cobble goes in"),
            Fact.of("song_bench", "The Song Bench spends 8 Pulse a tick", "8 Pulse a tick"),
            Fact.of("machine_ranks", "Machine Attune is 16 seconds at 48 Pulse a second; generators rank at 15%", "48 Pulse a second", "15%").never("Ranked generators hum harder"),
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
            Fact.of("wireless_cargo", "Relay Pulse is quoted before pack settings", "consumption multiplier"),
            Fact.of("stone_font", "The Stone Font makes stone at 24 Pulse a second and obsidian at 6; cobble is output only", "24 Pulse a second").never("Cobble in, stone out"),
            Fact.of("voice_ring", "The Voice Ring still requires Answered totems", "Answered"),
            Fact.of("listening_pit", "The Resonance Mesh holds a 200 Pulse buffer", "200 Pulse"),
            Fact.of("what_the_ground_offers", "Pit Pulse is 28 a second before pack settings; an iron cycle is 280, 560 at the default", "28 Pulse", "280 Pulse", "560").never("lapis and quartz"),
            Fact.of("redstone_language", "A held signal pauses every generator other than the Drumheart", "every generator except the Drumheart"),
            Fact.of("pulse_gauge", "A Threshold sings at or above its Pulse mark; an Inverse plate flips it", "Inverse", "at or above"),
            Fact.of("verse_link", "A Verse Answer copies the Call's strength", "copies"),
            Fact.of("spring_calling", "Spring Calling needs its Rite Pedestals and does not need you standing there", "four pedestals", "do not have to stand").never("while you stand still"),
            Fact.of("sixfold_staff", "Staff ranges and rests", "12 blocks", "8 blocks", "4 seconds", "18 blocks", "1.5 seconds").never("nearby enemies"),
            Fact.of("resonance_maul", "The Resonance Maul rests one second after a break", "one second"),
            Fact.of("spiritgear", "Gear ranks: Attune 45 seconds at 48 Pulse a second; Manifest needs Loom Thread from The Unsung", "45 seconds", "48 Pulse a second", "180 seconds", "Loom Thread", "25% harder").never("8 seconds"),
            Fact.of("spiritweave", "A full Manifested set: 20% less damage taken", "20%", "8 extra hearts"),
            Fact.of("spiritweave", "Bound Spiritweave spends 3 Pulse upkeep, unlinked 2", "3 Pulse", "2 Pulse"),
            Fact.of("totem_bound_gear", "An Earth hood turns aside one projectile in five", "one projectile in five"),
            Fact.of("spirit_charms", "Charm voices cost 40 Pulse each; Ward turns aside one projectile in four", "40 Pulse", "one projectile in four"),
            Fact.of("ritual_brazier", "Camp blessings include the Loom's luck", "luck"),
            Fact.of("pulse_lights", "Pulse lights draw 1 Pulse a second", "1 Pulse a second"),
            Fact.of("wind_charm", "A Wind Charm spends no Pulse", "no Pulse"),
            Fact.of("standing", "Hearth offerings cap at 60 standing a day and at most 40 Pulse", "60", "40 Pulse").never("camp blocks"),
            Fact.of("tribal_kin", "Elders restock once a Minecraft day; a Drummer beats every 6 seconds", "6 seconds").never("generators within 8 blocks"),
            Fact.of("kinship_totem", "Kinship lends an element, not a seventh voice", "seventh"),
            Fact.of("familiar_gifts", "A Cinder Imp refunds 4 Pulse; an Echo Weaver gathers within 4 blocks", "4 Pulse", "4 blocks").never("a little Pulse"),
            Fact.of("welcome", "The landing mentions JEI as optional", "JEI"));

    private CodexFacts() {}

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
