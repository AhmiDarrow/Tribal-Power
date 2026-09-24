package tk.darrow.tribalpower.client.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tk.darrow.tribalpower.TribalPower;

/**
 * The Spirit Codex, loaded from {@code assets/tribalpower/codex/<language>/}: {@code book.json}, one file per
 * category under {@code categories/}, and one per entry under {@code entries/<category>/<entry>.json}. English
 * is the base; another language's files replace English ones with the same path, so a translation can be
 * partial. An entry's id is its file name, and its category is its folder.
 */
public final class CodexBook {
    public record Book(String title, String landing, List<Category> categories, List<Entry> entries,
                       Map<String, Entry> byId, Map<String, String> itemEntries) {
        public List<Entry> in(String category) {
            return entries.stream().filter(e -> e.category().equals(category)).toList();
        }

        public Category category(String id) {
            return categories.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
        }
    }

    public record Category(String id, String name, String icon, String description, int order, boolean spoiler,
                           String progress) {}

    /** {@code unlock} is "tribe:&lt;id&gt;" or "tablet:&lt;n&gt;": a spoiler that opens itself once earned in the world. */
    public record Entry(String id, String category, String name, String icon, boolean spoiler, String unlock,
                        int order, List<String> items, List<Page> pages, String next) {}

    public sealed interface Page permits Text, Spotlight, Recipe, Image, Scene, Pattern, Quests, Events, NextStep {
        String title();
        String text();
    }

    public record Text(String title, String text) implements Page {}

    /** The player's live standing with one tribe's work: its open request and its story. */
    public record Quests(String tribe, String title, String text) implements Page {}

    /** What the March is doing now: its weather, its surge, whose festival it is. */
    public record Events(String title, String text) implements Page {}

    /** The one thing to do next on the guided path. */
    public record NextStep(String title, String text) implements Page {}

    /** A big item with its name, and text beneath. */
    public record Spotlight(String item, String title, String text) implements Page {}

    /** The live recipes that make {@code item}, one at a time. */
    public record Recipe(String item, String title, String text) implements Page {}

    public record Image(String image, String title, String text) implements Page {}

    /** A stepped 3D build: each step adds or removes blocks, highlights some, and says what is happening. */
    public record Scene(String title, String text, List<Step> steps) implements Page {}

    /** A ritual pattern drawn from its live definition, built up role by role. */
    public record Pattern(String pattern, int tier, String title, String text) implements Page {}

    public record Step(String caption, List<Placed> place, List<BlockPos> remove, List<BlockPos> highlight, Use use) {}

    public record Placed(BlockPos pos, String state) {}

    public record Use(BlockPos pos, String item) {}

    private static final Book EMPTY = new Book("Spirit Codex", "", List.of(), List.of(), Map.of(), Map.of());
    private static volatile Book book = EMPTY;

    private CodexBook() {}

    public static Book get() {
        return book;
    }

    public static final class Loader extends SimplePreparableReloadListener<Book> {
        @Override
        protected Book prepare(ResourceManager resources, ProfilerFiller profiler) {
            String language = Minecraft.getInstance().getLanguageManager().getSelected();
            Map<String, JsonObject> files = new LinkedHashMap<>();
            read(resources, "en_us", files);
            if (!language.equals("en_us")) read(resources, language, files);
            try {
                return build(files);
            } catch (RuntimeException error) {
                TribalPower.LOGGER.error("The Spirit Codex could not be read", error);
                return EMPTY;
            }
        }

        @Override
        protected void apply(Book loaded, ResourceManager resources, ProfilerFiller profiler) {
            book = loaded;
            TribalPower.LOGGER.info("Spirit Codex: {} categories, {} entries", loaded.categories().size(), loaded.entries().size());
        }
    }

    private static void read(ResourceManager resources, String language, Map<String, JsonObject> files) {
        String root = "codex/" + language;
        resources.listResources(root, id -> id.getNamespace().equals(TribalPower.MOD_ID) && id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        files.put(id.getPath().substring(root.length() + 1), JsonParser.parseReader(reader).getAsJsonObject());
                    } catch (Exception error) {
                        TribalPower.LOGGER.error("Spirit Codex file {} is broken", id, error);
                    }
                });
    }

    /** Builds the book from paths relative to the language folder. Public so tests can read the same files. */
    public static Book build(Map<String, JsonObject> files) {
        JsonObject meta = files.getOrDefault("book.json", new JsonObject());
        List<Category> categories = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        files.forEach((path, json) -> {
            String[] parts = path.split("/");
            if (parts[0].equals("categories") && parts.length == 2) {
                categories.add(new Category(stem(parts[1]), str(json, "name"), str(json, "icon"), text(json, "description"),
                        json.has("order") ? json.get("order").getAsInt() : 100, bool(json, "spoiler"), str(json, "progress")));
            } else if (parts[0].equals("entries") && parts.length == 3) {
                // One malformed entry (no pages, bad scene) must not take the whole book down with it.
                try {
                    entries.add(entry(parts[1], stem(parts[2]), json));
                } catch (RuntimeException error) {
                    TribalPower.LOGGER.warn("Spirit Codex entry {} skipped: {}", path, error.toString());
                }
            }
        });
        categories.sort(Comparator.comparingInt(Category::order).thenComparing(Category::id));
        Map<String, Integer> categoryOrder = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) categoryOrder.put(categories.get(i).id(), i);
        entries.sort(Comparator.comparingInt((Entry e) -> categoryOrder.getOrDefault(e.category(), 999))
                .thenComparingInt(Entry::order).thenComparing(Entry::name));
        Map<String, Entry> byId = new LinkedHashMap<>();
        for (Entry e : entries) byId.put(e.id(), e);
        // Clicking an item opens the entry that is most about it: the one it is the icon of, then one that
        // spotlights it, then one that merely lists it. The beginner path only claims what nothing else covers.
        Map<String, String> itemEntries = new HashMap<>();
        Map<String, Integer> claim = new HashMap<>();
        for (Entry e : entries) {
            boolean beginner = categories.stream().findFirst().map(c -> c.id().equals(e.category())).orElse(false);
            for (String item : e.items()) {
                int strength = item.equals(e.icon()) ? 3 : 1;
                for (Page page : e.pages())
                    if (page instanceof Spotlight s && s.item().equals(item)) strength = Math.max(strength, 2);
                if (beginner) strength -= 10;
                if (strength > claim.getOrDefault(item, Integer.MIN_VALUE)) {
                    claim.put(item, strength);
                    itemEntries.put(item, e.id());
                }
            }
        }
        return new Book(meta.has("title") ? str(meta, "title") : "Spirit Codex", text(meta, "landing"),
                List.copyOf(categories), List.copyOf(entries), byId, itemEntries);
    }

    private static Entry entry(String category, String id, JsonObject json) {
        List<Page> pages = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("pages")) pages.add(page(element.getAsJsonObject()));
        List<String> items = new ArrayList<>();
        if (json.has("items")) for (JsonElement item : json.getAsJsonArray("items")) items.add(item.getAsString());
        String icon = str(json, "icon");
        if (!icon.isEmpty() && !items.contains(icon)) items.add(icon);
        // What an entry spotlights or shows the recipe of, it teaches.
        for (Page page : pages) {
            String shown = page instanceof Spotlight s ? s.item() : page instanceof Recipe r ? r.item() : "";
            if (!shown.isEmpty() && !items.contains(shown)) items.add(shown);
        }
        return new Entry(id, category, str(json, "name"), icon, bool(json, "spoiler"), str(json, "unlock"),
                json.has("order") ? json.get("order").getAsInt() : 100, List.copyOf(items), List.copyOf(pages), str(json, "next"));
    }

    private static Page page(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "text";
        String title = str(json, "title"), text = text(json, "text");
        return switch (type) {
            case "spotlight" -> new Spotlight(str(json, "item"), title, text);
            case "recipe" -> new Recipe(str(json, "item"), title, text);
            case "image" -> new Image(str(json, "image"), title, text);
            case "pattern" -> new Pattern(str(json, "pattern"), json.has("tier") ? json.get("tier").getAsInt() : 1, title, text);
            case "scene" -> new Scene(title, text, steps(json.getAsJsonArray("steps")));
            case "quests" -> new Quests(str(json, "tribe"), title, text);
            case "events" -> new Events(title, text);
            case "next_step" -> new NextStep(title, text);
            default -> new Text(title, text);
        };
    }

    private static List<Step> steps(JsonArray array) {
        List<Step> steps = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            List<Placed> place = new ArrayList<>();
            if (json.has("place")) for (JsonElement p : json.getAsJsonArray("place")) {
                JsonObject placed = p.getAsJsonObject();
                place.add(new Placed(pos(placed.getAsJsonArray("pos")), placed.get("block").getAsString()));
            }
            Use use = null;
            if (json.has("use")) {
                JsonObject u = json.getAsJsonObject("use");
                use = new Use(pos(u.getAsJsonArray("pos")), str(u, "item"));
            }
            steps.add(new Step(text(json, "caption"), List.copyOf(place), positions(json, "remove"), positions(json, "highlight"), use));
        }
        return List.copyOf(steps);
    }

    private static List<BlockPos> positions(JsonObject json, String key) {
        List<BlockPos> out = new ArrayList<>();
        if (json.has(key)) for (JsonElement p : json.getAsJsonArray(key)) out.add(pos(p.getAsJsonArray()));
        return List.copyOf(out);
    }

    private static BlockPos pos(JsonArray array) {
        return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    /** Text may be one string or an array of paragraphs, joined by blank lines. */
    private static String text(JsonObject json, String key) {
        if (!json.has(key)) return "";
        JsonElement value = json.get(key);
        if (!value.isJsonArray()) return value.getAsString();
        List<String> parts = new ArrayList<>();
        for (JsonElement part : value.getAsJsonArray()) parts.add(part.getAsString());
        return String.join("\n\n", parts);
    }

    private static String str(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }

    private static String stem(String file) {
        return file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
    }

    /** Called while rendering, so a malformed id resolves to an unregistered one instead of throwing. */
    public static ResourceLocation itemId(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id.contains(":") ? id : TribalPower.MOD_ID + ":" + id);
        return key != null ? key : ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "missing");
    }
}
