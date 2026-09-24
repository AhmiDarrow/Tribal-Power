package tk.darrow.tribalpower.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * What a tribe's Elder says. Data-driven: every file under {@code data/tribalpower/dialogue/<tribe>/} is a set
 * of nodes for that tribe, and they are merged. A node is a few lines and the choices under them; a choice may
 * be gated by conditions and may do things when taken. Texts are translation keys, so the words live in the
 * lang file with everything else the player reads.
 *
 * <pre>
 * { "roots": [ { "node": "greet_kin", "conditions": [ { "rank": "kin" } ] }, { "node": "greet" } ],
 *   "nodes": { "greet": { "text": [ "dialogue.tribalpower.soil.greet" ],
 *                         "choices": [ { "text": "...", "next": "lore", "conditions": [...], "actions": [...] } ] } } }
 * </pre>
 */
public final class Dialogue {
    public static final String DIRECTORY = "dialogue";

    public record Condition(String kind, String value) {}
    public record Action(String kind, JsonElement value) {}
    public record Choice(String text, String next, List<Condition> conditions, List<Action> actions) {}
    public record Node(String id, List<String> text, List<Choice> choices) {}
    public record Root(String node, List<Condition> conditions) {}
    public record Tree(TribeDefinition tribe, List<Root> roots, Map<String, Node> nodes) {
        public Node node(String id) { return nodes.get(id); }
    }

    private static Map<TribeDefinition, Tree> TREES = Map.of();

    private Dialogue() {}

    public static Tree tree(TribeDefinition tribe) {
        return TREES.get(tribe);
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Reloader());
    }

    /** Replaces every tree with those parsed from the given files. */
    public static void load(Map<ResourceLocation, JsonElement> files) {
        TREES = parse(files);
    }

    /** Parses trees from JSON without a resource manager and without touching the loaded ones (for tests). */
    public static Map<TribeDefinition, Tree> parse(Map<ResourceLocation, JsonElement> files) {
        Map<TribeDefinition, List<Root>> roots = new HashMap<>();
        Map<TribeDefinition, Map<String, Node>> nodes = new HashMap<>();
        for (var entry : files.entrySet()) {
            String path = entry.getKey().getPath();
            String tribeId = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
            TribeDefinition tribe = byId(tribeId);
            if (tribe == null) {
                TribalPower.LOGGER.warn("Dialogue {} names no tribe", entry.getKey());
                continue;
            }
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (json.has("roots")) for (JsonElement r : json.getAsJsonArray("roots")) {
                    JsonObject root = r.getAsJsonObject();
                    roots.computeIfAbsent(tribe, t -> new ArrayList<>()).add(new Root(root.get("node").getAsString(), conditions(root)));
                }
                if (json.has("nodes")) for (var n : json.getAsJsonObject("nodes").entrySet()) {
                    JsonObject node = n.getValue().getAsJsonObject();
                    List<Choice> choices = new ArrayList<>();
                    if (node.has("choices")) for (JsonElement c : node.getAsJsonArray("choices")) {
                        JsonObject choice = c.getAsJsonObject();
                        choices.add(new Choice(choice.get("text").getAsString(), choice.has("next") ? choice.get("next").getAsString() : "",
                                conditions(choice), actions(choice)));
                    }
                    nodes.computeIfAbsent(tribe, t -> new HashMap<>()).put(n.getKey(), new Node(n.getKey(), strings(node.get("text")), List.copyOf(choices)));
                }
            } catch (RuntimeException e) {
                TribalPower.LOGGER.warn("Bad dialogue {}: {}", entry.getKey(), e.toString());
            }
        }
        Map<TribeDefinition, Tree> trees = new HashMap<>();
        for (TribeDefinition tribe : TribeDefinition.values()) {
            if (!nodes.containsKey(tribe)) continue;
            trees.put(tribe, new Tree(tribe, List.copyOf(roots.getOrDefault(tribe, List.of())), Map.copyOf(nodes.get(tribe))));
        }
        return Map.copyOf(trees);
    }

    private static TribeDefinition byId(String id) {
        for (TribeDefinition tribe : TribeDefinition.values()) if (tribe.id().equals(id)) return tribe;
        return null;
    }

    private static List<String> strings(JsonElement element) {
        List<String> out = new ArrayList<>();
        if (element == null) return out;
        if (element.isJsonArray()) for (JsonElement e : element.getAsJsonArray()) out.add(e.getAsString());
        else out.add(element.getAsString());
        return List.copyOf(out);
    }

    private static List<Condition> conditions(JsonObject json) {
        List<Condition> out = new ArrayList<>();
        if (json.has("conditions")) for (JsonElement c : json.getAsJsonArray("conditions")) {
            JsonObject condition = c.getAsJsonObject();
            for (var field : condition.entrySet()) out.add(new Condition(field.getKey(), field.getValue().isJsonPrimitive() ? field.getValue().getAsString() : field.getValue().toString()));
        }
        return List.copyOf(out);
    }

    private static List<Action> actions(JsonObject json) {
        List<Action> out = new ArrayList<>();
        if (json.has("actions")) for (JsonElement a : json.getAsJsonArray("actions")) {
            if (a.isJsonPrimitive()) out.add(new Action(a.getAsString(), null));
            else for (var field : a.getAsJsonObject().entrySet()) out.add(new Action(field.getKey(), field.getValue()));
        }
        return List.copyOf(out);
    }

    private static final class Reloader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().setLenient().create();

        Reloader() { super(GSON, DIRECTORY); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            load(files);
            TribalPower.LOGGER.info("Loaded dialogue for {} tribes", TREES.size());
        }
    }

    /** A hand-made array of conditions, for tests. */
    public static JsonArray array(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement e : elements) array.add(e);
        return array;
    }
}
