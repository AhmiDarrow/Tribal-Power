package tk.darrow.tribalpower.verification;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.gate.DrumPractice;
import tk.darrow.tribalpower.gate.Songbook;
import tk.darrow.tribalpower.gate.Songbook.Difficulty;
import tk.darrow.tribalpower.kit.KitRegistry;

/**
 * The Songkeeper Drum: every song is there with its music and a sound chart at four difficulties, a go is held to the
 * song's real length and capped at what its chart allows, old scores carry over, and two drums side by side duel.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class SongkeeperGameTests {
    private SongkeeperGameTests() {}

    @GameTest(template = "empty")
    public static void everySongHasItsMusicAndCharts(GameTestHelper h) {
        var songs = Songbook.songs();
        h.assertTrue(songs.size() == 46, "Tribal Power's 26, the Ninjacat guardians' 12 and the Chocobo races' 8, got " + songs.size());
        var mods = net.neoforged.fml.ModList.get();
        int albums = 3 + (mods.isLoaded("guardians") ? 1 : 0) + (mods.isLoaded("chocobosreborn") ? 1 : 0);
        h.assertTrue(Songbook.albums().size() == albums, "Tribal Power's three albums, plus one per music mod here, got " + Songbook.albums().keySet());
        var sounds = JsonParser.parseReader(new InputStreamReader(
                SongkeeperGameTests.class.getResourceAsStream("/assets/tribalpower/sounds.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        int own = 0;
        for (var song : songs) {
            h.assertTrue(song.sound().startsWith(song.requires() + ":"), song.id() + " plays a sound of the mod it requires, " + song.sound());
            h.assertTrue(java.util.Set.of("tribalpower", "guardians", "chocobosreborn").contains(song.requires()), song.id() + " requires a known mod");
            h.assertTrue(song.available() == net.neoforged.fml.ModList.get().isLoaded(song.requires()), song.id() + " is on the drum exactly when its mod is");
            h.assertTrue(song.lengthMs() > 20_000, song.id() + " is a whole song, " + song.lengthMs() + " ms");
            if (!song.requires().equals("tribalpower")) continue;
            own++;
            String event = song.sound().substring("tribalpower:".length());
            h.assertTrue(sounds.has(event), song.id() + " plays sounds.json event " + event);
            var entry = sounds.getAsJsonObject(event).getAsJsonArray("sounds").get(0);
            String file = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString() : entry.getAsString();
            h.assertTrue(entry.isJsonObject() && entry.getAsJsonObject().get("stream").getAsBoolean(), song.id() + " streams");
            String path = "/assets/tribalpower/sounds/" + file.substring(file.indexOf(':') + 1) + ".ogg";
            h.assertTrue(SongkeeperGameTests.class.getResource(path) != null, song.id() + " ships its recording " + path);
        }
        h.assertTrue(own == 26, "Tribal Power carries its own 26 recordings and no others, got " + own);
        h.assertTrue(SongkeeperGameTests.class.getResource("/assets/tribalpower/sounds/songkeeper") == null, "No other mod's music rides in the jar");
        for (var song : songs) {
            var charts = Songbook.charts(song);
            h.assertTrue(charts.beats().length > 8, song.id() + " has its beats for the highway");
            int previous = 0;
            for (Difficulty difficulty : Difficulty.values()) {
                var chart = charts.chart(difficulty);
                h.assertTrue(chart.size() >= previous, song.id() + " " + difficulty + " has at least as many notes as the difficulty below");
                previous = chart.size();
                h.assertTrue(chart.size() > 0, song.id() + " " + difficulty + " has notes");
                long last = -1;
                for (int i = 0; i < chart.size(); i++) {
                    long t = chart.times()[i];
                    h.assertTrue(t >= last && t >= 0 && t <= song.lengthMs(), song.id() + " " + difficulty + " note " + i + " in order inside the song");
                    int lane = chart.lanes()[i];
                    h.assertTrue(lane >= 0 && lane <= (difficulty == Difficulty.EASY ? 2 : 3), song.id() + " " + difficulty + " note " + i + " on a drum");
                    last = t;
                }
            }
        }
        h.succeed();
    }

    /**
     * The Gate Drum plays its rite on the Songkeeper's game: every rite track is a Songbook song with the same recording,
     * and that song's Hard chart is exactly the rite's composed pattern, the notes the server judges the gate by.
     */
    @GameTest(template = "empty")
    public static void everyGateRiteIsASongkeeperSong(GameTestHelper h) {
        for (int track = 0; track < tk.darrow.tribalpower.gate.DrumRite.trackCount(); track++) {
            var pattern = tk.darrow.tribalpower.gate.DrumRite.pattern(track);
            Songbook.Song song = null;
            for (var candidate : Songbook.songs()) if (candidate.sound().equals("tribalpower:" + pattern.sound())) song = candidate;
            h.assertTrue(song != null, "Rite track " + pattern.sound() + " is in the Songbook");
            var hard = Songbook.chart(song, Difficulty.HARD);
            h.assertTrue(hard.size() == pattern.notes().size(), song.id() + ": Hard has the rite's " + pattern.notes().size() + " notes, got " + hard.size());
            for (int i = 0; i < hard.size(); i++)
                h.assertTrue(hard.times()[i] == pattern.notes().get(i).timeMs() && hard.lanes()[i] == pattern.notes().get(i).lane(),
                        song.id() + " note " + i + " matches the rite");
            h.assertTrue(song.lengthMs() >= pattern.endMs(), song.id() + " runs at least as long as the rite the server holds it to");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aGoIsScoredHeldToItsLengthAndCapped(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            BlockPos drum = h.absolutePos(new BlockPos(2, 2, 2));
            h.setBlock(new BlockPos(2, 2, 2), KitRegistry.SONGKEEPER_DRUM.get());
            player.teleportTo(drum.getX() + 0.5, drum.getY(), drum.getZ() + 1.5);
            var song = Songbook.song("waking_beat");
            int total = Songbook.chart(song, Difficulty.HARD).size();
            long ticks = song.lengthMs() / 50 + 5;
            DrumPractice.beginAt(player, drum, song.index(), Difficulty.HARD, ticks);
            long points = DrumPractice.finish(player, new DrumPractice.Result(drum, song.index(), Difficulty.HARD.ordinal(), 12345, total, total, total, 0, 0, false, false));
            h.assertTrue(points == 12345, "The go is scored as played, got " + points);
            var scores = DrumPractice.Scores.get(player);
            var best = scores.best(song, Difficulty.HARD, player.getUUID());
            h.assertTrue(best != null && best.points() == 12345 && best.fullCombo() && best.accuracy() == 100, "It is the player's best, a full combo");
            DrumPractice.beginAt(player, drum, song.index(), Difficulty.HARD, ticks);
            long capped = DrumPractice.finish(player, new DrumPractice.Result(drum, song.index(), Difficulty.HARD.ordinal(), Long.MAX_VALUE / 4, total, total, total, 0, 0, false, false));
            h.assertTrue(capped == Songbook.maxScore(song, Difficulty.HARD), "A score past what the chart allows is capped, got " + capped);
            DrumPractice.beginAt(player, drum, song.index(), Difficulty.EASY, 0);
            h.assertTrue(DrumPractice.finish(player, new DrumPractice.Result(drum, song.index(), Difficulty.EASY.ordinal(), 999, 1, 1, 1, 0, 0, false, false)) < 0,
                    "A go cut short is refused");
            DrumPractice.beginAt(player, drum, song.index(), Difficulty.NORMAL, 20);
            long broke = DrumPractice.finish(player, new DrumPractice.Result(drum, song.index(), Difficulty.NORMAL.ordinal(), 500, 5, 5, 5, 20, 0, true, false));
            h.assertTrue(broke == 500 && scores.best(song, Difficulty.NORMAL, player.getUUID()) == null, "A broken song counts in a duel but keeps no best");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** Scores from before the Songbook were for the seven old tracks, whose charts are now those tracks' Hard. */
    @GameTest(template = "empty")
    public static void oldScoresCarryOverAsHard(GameTestHelper h) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag old = new CompoundTag();
        var someone = java.util.UUID.randomUUID();
        old.putInt("Track", 6);
        old.putUUID("Player", someone);
        old.putString("Name", "Drummer");
        old.putInt("Points", 4321);
        old.putInt("Accuracy", 88);
        list.add(old);
        tag.put("Scores", list);
        var scores = DrumPractice.Scores.load(tag, h.getLevel().registryAccess());
        var best = scores.best(Songbook.song("drum_circle"), Difficulty.HARD, someone);
        h.assertTrue(best != null && best.points() == 4321 && best.name().equals("Drummer"), "The old Drum Circle best is the new Drum Circle Hard best");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void twoDrumsSideBySideDuel(GameTestHelper h) {
        var challenger = VerificationPlayers.inLevel(h);
        var rival = VerificationPlayers.inLevel(h);
        try {
            BlockPos left = h.absolutePos(new BlockPos(2, 2, 2)), right = h.absolutePos(new BlockPos(4, 2, 2));
            h.setBlock(new BlockPos(2, 2, 2), KitRegistry.SONGKEEPER_DRUM.get());
            h.setBlock(new BlockPos(4, 2, 2), KitRegistry.SONGKEEPER_DRUM.get());
            challenger.teleportTo(left.getX() + 0.5, left.getY(), left.getZ() - 1.5);
            rival.teleportTo(right.getX() + 1.5, right.getY(), right.getZ() - 1.5);
            h.assertTrue(DrumPractice.partners(h.getLevel(), left).contains(right), "The drums are a pair");
            h.assertTrue(DrumPractice.rivals(challenger, left).contains(rival), "The player at the other drum can be challenged");
            h.assertFalse(DrumPractice.rivals(challenger, left).contains(challenger), "Not yourself");
            var song = Songbook.song("storm_call");
            var before = DrumPractice.duels();
            DrumPractice.challenge(challenger, left, rival.getGameProfile().getName(), song.index(), Difficulty.NORMAL);
            var after = new java.util.HashSet<>(DrumPractice.duels());
            after.removeAll(before);
            h.assertTrue(after.size() == 1, "The challenge is on the table");
            int duel = after.iterator().next();
            DrumPractice.answer(challenger, duel, true);
            h.assertTrue(DrumPractice.duels().contains(duel), "Only the challenged player can accept");
            DrumPractice.answer(rival, duel, true);
            h.assertFalse(DrumPractice.duels().contains(duel) && DrumPractice.rivals(challenger, left).contains(rival),
                    "Once the duel starts the rival is busy, not challengeable again");
            // both drums play the song through; the challenger scores higher
            long ticks = song.lengthMs() / 50 + 5;
            DrumPractice.rewind(challenger, ticks);
            DrumPractice.rewind(rival, ticks);
            var scores = DrumPractice.Scores.get(challenger);
            int won = scores.record(challenger.getUUID())[0], lost = scores.record(rival.getUUID())[1];
            int total = Songbook.chart(song, Difficulty.NORMAL).size();
            DrumPractice.finish(challenger, new DrumPractice.Result(left, song.index(), Difficulty.NORMAL.ordinal(), 9000, total, total, total, 0, 0, false, false));
            h.assertTrue(DrumPractice.duels().contains(duel), "The duel waits for the other drum");
            DrumPractice.finish(rival, new DrumPractice.Result(right, song.index(), Difficulty.NORMAL.ordinal(), 7000, total - 3, total - 5, 20, 3, 1, false, false));
            h.assertFalse(DrumPractice.duels().contains(duel), "Both are in: the duel is settled");
            h.assertTrue(scores.record(challenger.getUUID())[0] == won + 1, "The higher score wins the duel");
            h.assertTrue(scores.record(rival.getUUID())[1] == lost + 1, "and the lower loses it");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(challenger);
            h.getLevel().getServer().getPlayerList().remove(rival);
        }
    }
}
