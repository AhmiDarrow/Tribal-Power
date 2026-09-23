package tk.darrow.tribalpower.verification;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.TribalPower;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The client half of the mod, checked from the server.
 *
 * <p>Every other test here runs headless, which never loads a model, so a blockstate can name a
 * property the block does not have or point at a model file that was never written and nothing fails
 * until someone starts the game and reads the log. Both of those shipped once. This walks the
 * blockstate files the way the client would and refuses them.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class AssetGameTests {

    /** Every blockstate must name real properties, real values, and models that exist. */
    @GameTest(template="empty")
    public static void everyBlockstateNamesRealPropertiesAndModels(GameTestHelper h) {
        ClassLoader loader = AssetGameTests.class.getClassLoader();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !TribalPower.MOD_ID.equals(id.getNamespace())) continue;
            URL url = loader.getResource("assets/" + TribalPower.MOD_ID + "/blockstates/" + id.getPath() + ".json");
            if (url == null) {
                problems.add(id.getPath() + ": no blockstate file");
                continue;
            }
            checked++;
            JsonObject root = read(url);
            if (root == null) {
                problems.add(id.getPath() + ": blockstate is not readable JSON");
                continue;
            }
            Set<String> models = new LinkedHashSet<>();
            if (root.has("variants")) {
                for (var entry : root.getAsJsonObject("variants").entrySet()) {
                    checkKey(block, id, entry.getKey(), problems);
                    collect(entry.getValue(), models);
                }
            } else if (root.has("multipart")) {
                for (JsonElement part : root.getAsJsonArray("multipart"))
                    collect(part.getAsJsonObject().get("apply"), models);
            } else {
                problems.add(id.getPath() + ": blockstate has neither variants nor multipart");
            }
            for (String model : models) {
                if (!model.startsWith(TribalPower.MOD_ID + ":")) continue;
                String path = model.substring(TribalPower.MOD_ID.length() + 1);
                URL modelUrl = loader.getResource("assets/" + TribalPower.MOD_ID + "/models/" + path + ".json");
                if (modelUrl == null) {
                    problems.add(id.getPath() + ": points at a model that is not there, " + model);
                    continue;
                }
                // And the textures that model names: a model can load perfectly while pointing at a
                // png nobody ever wrote, which the client draws as the missing-texture checker.
                JsonObject m = read(modelUrl);
                if (m == null || !m.has("textures")) continue;
                for (var t : m.getAsJsonObject("textures").entrySet()) {
                    if (!t.getValue().isJsonPrimitive()) continue;
                    String texture = t.getValue().getAsString();
                    if (!texture.startsWith(TribalPower.MOD_ID + ":")) continue;
                    String tp = texture.substring(TribalPower.MOD_ID.length() + 1);
                    if (loader.getResource("assets/" + TribalPower.MOD_ID + "/textures/" + tp + ".png") == null)
                        problems.add(id.getPath() + ": model " + path + " names a texture that is not there, " + texture);
                }
            }
        }
        h.assertTrue(checked > 200, "Expected the whole block list, walked only " + checked);
        h.assertTrue(problems.isEmpty(), problems.size() + " blockstate problem(s): "
                + String.join(" | ", problems.subList(0, Math.min(8, problems.size()))));
        h.succeed();
    }

    /**
     * Every creature must have a body to be drawn with and a skin to wear.
     *
     * <p>Adding a creature without a rig threw on the render thread the instant the game loaded, and
     * nothing headless noticed, because the geometry lives in a client class this test cannot touch.
     * The mapping was moved into common code so it can be held to the roster from here.
     */
    @GameTest(template="empty")
    public static void everyCreatureHasARigAndASkin(GameTestHelper h) {
        ClassLoader loader = AssetGameTests.class.getClassLoader();
        List<String> problems = new ArrayList<>();
        for (var profile : tk.darrow.tribalpower.entity.CreatureProfile.values()) {
            String rig = tk.darrow.tribalpower.entity.CreatureRigs.rig(profile.id);
            if (!tk.darrow.tribalpower.entity.CreatureRigs.BUILT.contains(rig))
                problems.add(profile.id + ": drawn with '" + rig + "', which has no built geometry");
            for (String suffix : new String[]{"", "_glow"}) {
                if (loader.getResource("assets/" + TribalPower.MOD_ID + "/textures/entity/"
                        + profile.id + suffix + ".png") == null && suffix.isEmpty())
                    problems.add(profile.id + ": no entity texture");
            }
        }
        h.assertTrue(problems.isEmpty(), problems.size() + " creature problem(s): "
                + String.join(" | ", problems.subList(0, Math.min(6, problems.size()))));
        h.succeed();
    }

    /**
     * Every item the mod registers must have a model, and that model's textures must be on disk.
     *
     * <p>The blockstate walk above never looked at items, so a reagent could register, drop, and be
     * fed to a familiar while rendering as the missing-texture checker in everyone's inventory.
     */
    @GameTest(template="empty")
    public static void everyItemHasAModelAndItsTextures(GameTestHelper h) {
        ClassLoader loader = AssetGameTests.class.getClassLoader();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !TribalPower.MOD_ID.equals(id.getNamespace())) continue;
            if (BuiltInRegistries.BLOCK.containsKey(id)) continue;   // block items use the block model
            checked++;
            URL url = loader.getResource("assets/" + TribalPower.MOD_ID + "/models/item/"
                    + id.getPath() + ".json");
            if (url == null) {
                problems.add(id.getPath() + ": no item model");
                continue;
            }
            JsonObject model = read(url);
            if (model == null || !model.has("textures")) continue;   // template models carry none
            for (var texture : model.getAsJsonObject("textures").entrySet()) {
                if (!texture.getValue().isJsonPrimitive()) continue;
                String name = texture.getValue().getAsString();
                if (!name.startsWith(TribalPower.MOD_ID + ":")) continue;
                String path = name.substring(TribalPower.MOD_ID.length() + 1);
                if (loader.getResource("assets/" + TribalPower.MOD_ID + "/textures/" + path + ".png") == null)
                    problems.add(id.getPath() + ": model names a texture that is not there, " + name);
            }
        }
        h.assertTrue(checked > 100, "Expected the whole item list, walked only " + checked);
        h.assertTrue(problems.isEmpty(), problems.size() + " item asset problem(s): "
                + String.join(" | ", problems.subList(0, Math.min(8, problems.size()))));
        h.succeed();
    }

    /** "facing=up,waterlogged=false" — every name and every value has to exist on the block. */
    private static void checkKey(Block block, ResourceLocation id, String key, List<String> problems) {
        if (key.isEmpty()) return;
        for (String pair : key.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                problems.add(id.getPath() + ": malformed variant key '" + key + "'");
                continue;
            }
            var property = block.getStateDefinition().getProperty(kv[0]);
            if (property == null) {
                problems.add(id.getPath() + ": blockstate names unknown property '" + kv[0] + "'");
                continue;
            }
            if (property.getValue(kv[1]).isEmpty())
                problems.add(id.getPath() + ": property " + kv[0] + " has no value '" + kv[1] + "'");
        }
    }

    /** A variant is a model object or a weighted list of them. */
    private static void collect(JsonElement element, Set<String> models) {
        if (element == null) return;
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) collect(e, models);
        } else if (element.isJsonObject() && element.getAsJsonObject().has("model")) {
            models.add(element.getAsJsonObject().get("model").getAsString());
        }
    }

    private static JsonObject read(URL url) {
        try (var stream = url.openStream();
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
