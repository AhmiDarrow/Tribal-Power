package tk.darrow.tribalpower.gate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every song the Songkeeper Drum plays: the Tribal Power rites, the March and its guardians, the Ninjacat Skies
 * guardians and the Chocobos Reborn races. Charted from the real audio by tools/chart_songs.py (every note sits on
 * an attack in the recording) and read from the jar, so the client and the server hold the same notes. A go lasts
 * as long as the recording does.
 */
public final class Songbook {
    private static final String ROOT = "/data/tribalpower/songkeeper/";
    private static volatile List<Song> songs;
    private static final Map<String, Charts> CHARTS = new ConcurrentHashMap<>();

    private Songbook() {}

    public enum Difficulty {
        EASY, NORMAL, HARD, EXPERT;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Difficulty of(int ordinal) {
            return values()[Math.floorMod(ordinal, values().length)];
        }
    }

    /**
     * One song: what it is called, which record it is on, the sound that plays it (a full id: Tribal Power's own, Core's
     * guardian themes, or Chocobos Reborn's races), the mod that sound belongs to, and how long it runs.
     */
    public record Song(int index, String id, String title, String album, String sound, String requires, long lengthMs, int bpm) {
        /** Whether this song's recording is in the game: Tribal Power ships only its own, the others come with their mods. */
        public boolean available() {
            return requires.equals("tribalpower") || net.neoforged.fml.ModList.get().isLoaded(requires);
        }
    }

    /** One difficulty's notes, time-ordered: when each lands (ms from the first sound of the song) and on which drum. */
    public record Chart(long[] times, byte[] lanes) {
        public int size() {
            return times.length;
        }
    }

    /** A song's charts, the beats the highway draws, and the Spirit Surge windows (ms ranges). */
    public record Charts(long[] beats, long[][] surge, Map<Difficulty, Chart> byDifficulty) {
        public Chart chart(Difficulty difficulty) {
            return byDifficulty.get(difficulty);
        }

        /** Whether the note at this time is part of a Spirit Surge phrase. */
        public boolean surge(long time) {
            for (long[] window : surge) if (time >= window[0] && time < window[1]) return true;
            return false;
        }
    }

    public static List<Song> songs() {
        List<Song> loaded = songs;
        if (loaded == null) {
            synchronized (Songbook.class) {
                if (songs == null) songs = load();
                loaded = songs;
            }
        }
        return loaded;
    }

    public static Song song(String id) {
        for (Song song : songs()) if (song.id().equals(id)) return song;
        return null;
    }

    public static Song song(int index) {
        List<Song> all = songs();
        return index >= 0 && index < all.size() ? all.get(index) : null;
    }

    /** Every song whose recording is in the game, in list order. Indices stay those of {@link #songs}. */
    public static List<Song> available() {
        return songs().stream().filter(Song::available).toList();
    }

    /** The albums in the order the drum lists them, each with the songs that can play here. */
    public static Map<String, List<Song>> albums() {
        Map<String, List<Song>> out = new LinkedHashMap<>();
        for (Song song : available()) out.computeIfAbsent(song.album(), a -> new ArrayList<>()).add(song);
        return out;
    }

    public static Charts charts(Song song) {
        return CHARTS.computeIfAbsent(song.id(), id -> loadCharts(id));
    }

    public static Chart chart(Song song, Difficulty difficulty) {
        return charts(song).chart(difficulty);
    }

    private static JsonObject read(String path, boolean array) {
        try (InputStream in = Songbook.class.getResourceAsStream(ROOT + path)) {
            if (in == null) throw new IllegalStateException("Missing " + ROOT + path);
            var element = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (array) {
                JsonObject wrap = new JsonObject();
                wrap.add("songs", element);
                return wrap;
            }
            return element.getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read " + ROOT + path, e);
        }
    }

    private static List<Song> load() {
        JsonArray list = read("songs.json", true).getAsJsonArray("songs");
        List<Song> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            JsonObject row = list.get(i).getAsJsonObject();
            out.add(new Song(i, row.get("id").getAsString(), row.get("title").getAsString(), row.get("album").getAsString(),
                    row.get("sound").getAsString(), row.has("requires") ? row.get("requires").getAsString() : "tribalpower",
                    row.get("lengthMs").getAsLong(), row.get("bpm").getAsInt()));
        }
        return List.copyOf(out);
    }

    private static long[] longs(JsonArray array) {
        long[] out = new long[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsLong();
        return out;
    }

    private static Charts loadCharts(String id) {
        JsonObject root = read("charts/" + id + ".json", false);
        JsonArray surge = root.getAsJsonArray("surge");
        long[][] windows = new long[surge.size()][];
        for (int i = 0; i < windows.length; i++) windows[i] = longs(surge.get(i).getAsJsonArray());
        Map<Difficulty, Chart> charts = new java.util.EnumMap<>(Difficulty.class);
        JsonObject all = root.getAsJsonObject("charts");
        for (Difficulty difficulty : Difficulty.values()) {
            JsonObject chart = all.getAsJsonObject(difficulty.key());
            long[] times = longs(chart.getAsJsonArray("t"));
            JsonArray lanes = chart.getAsJsonArray("l");
            byte[] lane = new byte[lanes.size()];
            for (int i = 0; i < lane.length; i++) lane[i] = lanes.get(i).getAsByte();
            charts.put(difficulty, new Chart(times, lane));
        }
        return new Charts(longs(root.getAsJsonArray("beats")), windows, charts);
    }

    // ---- scoring, shared by the client that plays and the server that checks ---------------------------------

    public static final int PERFECT = 100, GREAT = 75, GOOD = 50;

    /** The streak multiplier: x1, then x2 at 10 in a row, x3 at 20, x4 at 30. */
    public static int multiplier(int streak) {
        return Math.min(4, 1 + streak / 10);
    }

    /**
     * The most a chart can score: every note perfect, the multiplier climbing as it would, and Spirit Surge doubling
     * its phrases' worth once. The server refuses anything above it.
     */
    public static long maxScore(Song song, Difficulty difficulty) {
        Chart chart = chart(song, difficulty);
        long total = 0;
        for (int i = 0; i < chart.size(); i++) total += (long) PERFECT * multiplier(i) * 2;
        return total;
    }

    /** Stars for a score: out of five, against a clean perfect run with no Surge. */
    public static int stars(Song song, Difficulty difficulty, long score) {
        Chart chart = chart(song, difficulty);
        long clean = 0;
        for (int i = 0; i < chart.size(); i++) clean += (long) PERFECT * multiplier(i);
        if (clean <= 0) return 0;
        double share = (double) score / clean;
        return share >= 0.95 ? 5 : share >= 0.8 ? 4 : share >= 0.6 ? 3 : share >= 0.4 ? 2 : share >= 0.2 ? 1 : 0;
    }
}
