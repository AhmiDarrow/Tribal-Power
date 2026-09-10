package tk.darrow.tribalpower.grit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.item.ModItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Metals and gems are two different crafts (design 3.1 section 1).
 *
 * <p>A metal is crushed and then fired: raw item or silk-touched ore block to grit, grit to ingot. A gem
 * or mineral is only shattered: silk-touched ore block straight to the gem itself, at twice the ore's
 * base drop. That leaves "grit" meaning exactly one thing -- crushed metal waiting for the fire -- and
 * removes six intermediates that were dust made of dust.
 *
 * <p>Materials are discovered from the common tags on every tag reload, so a modded metal lights up with
 * no datapack work. The gem itself is never an input: silk-touched ore is the only doubling path, because
 * gem-to-gem would be a dupe.
 */
public final class GritRegistry {
    /** Which craft a material belongs to. */
    public enum Kind { METAL, GEM }

    public static final String TAG_RAW = "raw_materials/";
    public static final String TAG_ORES = "ores/";
    public static final String TAG_INGOTS = "ingots/";
    public static final String TAG_GEMS = "gems/";

    /** Shattering is Earth work, four seconds, ten Pulse a second -- the same terms as a written recipe. */
    public static final String STATION = "echo_shatter";
    public static final int SHATTER_SECONDS = 4;
    public static final int SHATTER_PULSE = 10;

    /**
     * Netherite must be earned in the Nether, so ancient debris is never a shattering input however its
     * tags read. Everything else is opt-out through the config deny list or the override file.
     */
    private static final List<String> HARD_DENY = List.of("netherite", "netherite_scrap");

    /**
     * Where a mineral's return item is written. The design says {@code c:gems/<m>}, and for diamond,
     * emerald, lapis and quartz that is true -- but coal and redstone wear no gem tag at all, and
     * redstone is a dust. So the scan looks in every place a return could reasonably live before
     * falling back to the vanilla names.
     */
    public static final String TAG_DUSTS = "dusts/";

    private static final Map<String, Item> VANILLA_MINERAL = Map.of(
            "coal", Items.COAL, "redstone", Items.REDSTONE, "lapis", Items.LAPIS_LAZULI,
            "diamond", Items.DIAMOND, "emerald", Items.EMERALD, "quartz", Items.QUARTZ);

    /** Gem yields are twice the ore's own drop, not a flat two. Modded gems default to two. */
    private static final Map<String, Integer> VANILLA_GEM_YIELD = Map.of(
            "coal", 2, "diamond", 2, "emerald", 2, "quartz", 2, "redstone", 8, "lapis", 8);

    /** One discovered material and everything the two crafts need to know about it. */
    public record Material(String name, Kind kind, Item raw, Item ore, Item deepslateOre,
                           Item gem, Item ingot, int gemCount) {
        public boolean metal() { return kind == Kind.METAL; }

        /** Display form of the material name: {@code raw_tin} never appears, {@code Tin} does. */
        public Component displayName() {
            StringBuilder out = new StringBuilder();
            for (String word : name.split("_")) {
                if (word.isEmpty()) continue;
                if (!out.isEmpty()) out.append(' ');
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return Component.literal(out.toString());
        }

        /** What one shatter of this material yields. */
        public ItemStack shatterResult() {
            return metal() ? stackFor(name) : new ItemStack(gem, gemCount);
        }

        /** What the Listening Pit calls up: the raw item for a metal, the ore block for a gem. */
        public ItemStack pitResult(boolean deepslateBand) {
            if (metal() && raw != null) return new ItemStack(raw);
            Item block = deepslateBand && deepslateOre != null ? deepslateOre : ore;
            return block == null ? ItemStack.EMPTY : new ItemStack(block);
        }
    }

    private static final Map<String, Material> MATERIALS = new LinkedHashMap<>();
    /** Every shattering input, mapped to the material it belongs to. */
    private static final Map<Item, Material> INPUTS = new HashMap<>();
    private static final Map<Item, Material> BY_GRIT = new HashMap<>();

    private GritRegistry() {}

    // ---- discovery -------------------------------------------------------------------------

    /** Rescan on every tag reload: a datapack or a newly loaded mod can change what exists. */
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        rebuild();
    }

    public static synchronized void rebuild() {
        MATERIALS.clear();
        INPUTS.clear();
        BY_GRIT.clear();

        Map<String, List<Item>> raws = new HashMap<>();
        Map<String, List<Item>> ores = new HashMap<>();
        Map<String, List<Item>> ingots = new HashMap<>();
        Map<String, List<Item>> gems = new HashMap<>();
        Map<String, List<Item>> dusts = new HashMap<>();

        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            TagKey<Item> key = pair.getFirst();
            ResourceLocation id = key.location();
            if (!id.getNamespace().equals("c")) return;
            String path = id.getPath();
            List<Item> items = new ArrayList<>();
            pair.getSecond().forEach(holder -> items.add(holder.value()));
            if (items.isEmpty()) return;
            if (path.startsWith(TAG_RAW)) raws.put(path.substring(TAG_RAW.length()), items);
            else if (path.startsWith(TAG_ORES)) ores.put(path.substring(TAG_ORES.length()), items);
            else if (path.startsWith(TAG_INGOTS)) ingots.put(path.substring(TAG_INGOTS.length()), items);
            else if (path.startsWith(TAG_GEMS)) gems.put(path.substring(TAG_GEMS.length()), items);
            else if (path.startsWith(TAG_DUSTS)) dusts.put(path.substring(TAG_DUSTS.length()), items);
        });

        List<String> names = new ArrayList<>();
        names.addAll(ores.keySet());
        for (String name : raws.keySet()) if (!names.contains(name)) names.add(name);
        names.sort(String::compareTo);

        for (String name : names) {
            if (denied(name)) continue;
            List<Item> ingotItems = ingots.getOrDefault(name, List.of());
            List<Item> rawItems = clean(raws.getOrDefault(name, List.of()));
            List<Item> oreItems = clean(ores.getOrDefault(name, List.of()));
            Item gemItem = mineralReturn(name, gems, dusts);

            Material material;
            if (!ingotItems.isEmpty() && (!rawItems.isEmpty() || !oreItems.isEmpty())) {
                material = new Material(name, Kind.METAL, first(rawItems), stoneOre(oreItems), deepslateOre(oreItems),
                        null, ingotItems.getFirst(), 0);
            } else if (gemItem != null && !oreItems.isEmpty() && ingotItems.isEmpty()) {
                material = new Material(name, Kind.GEM, null, stoneOre(oreItems), deepslateOre(oreItems),
                        gemItem, null, VANILLA_GEM_YIELD.getOrDefault(name, 2));
            } else {
                continue;
            }
            // A material whose return would be nothing is not a material: skip rather than synthesise a void recipe.
            if (material.shatterResult().isEmpty()) continue;

            MATERIALS.put(name, material);
            // Metals accept the raw item and the ore block; gems accept the ore block only, never the gem.
            if (material.metal()) for (Item item : rawItems) INPUTS.putIfAbsent(item, material);
            for (Item item : oreItems) INPUTS.putIfAbsent(item, material);
            if (material.metal()) BY_GRIT.putIfAbsent(stackFor(name).getItem(), material);
        }
        TribalPower.LOGGER.debug("Grit scan: {} materials ({} metal)", MATERIALS.size(),
                MATERIALS.values().stream().filter(Material::metal).count());
    }

    /** What one shatter of a mineral gives back, or null when nothing knows. */
    private static Item mineralReturn(String name, Map<String, List<Item>> gems, Map<String, List<Item>> dusts) {
        List<Item> gem = gems.get(name);
        if (gem != null && !gem.isEmpty()) return gem.getFirst();
        List<Item> dust = dusts.get(name);
        if (dust != null && !dust.isEmpty()) return dust.getFirst();
        return VANILLA_MINERAL.get(name);
    }

    private static boolean denied(String name) {
        if (HARD_DENY.contains(name)) return true;
        for (String entry : TribalConfig.gritDenyList()) if (name.equalsIgnoreCase(entry)) return true;
        return false;
    }

    /** Ancient debris never becomes a shattering input, whatever tag it wears. */
    private static List<Item> clean(Collection<Item> items) {
        List<Item> out = new ArrayList<>(items.size());
        for (Item item : items) if (item != Items.ANCIENT_DEBRIS && item != Items.AIR) out.add(item);
        return out;
    }

    private static Item first(List<Item> items) { return items.isEmpty() ? null : items.getFirst(); }

    private static Item stoneOre(List<Item> ores) {
        for (Item ore : ores) if (!isDeepslate(ore)) return ore;
        return first(ores);
    }

    private static Item deepslateOre(List<Item> ores) {
        for (Item ore : ores) if (isDeepslate(ore)) return ore;
        return null;
    }

    private static boolean isDeepslate(Item ore) {
        return BuiltInRegistries.ITEM.getKey(ore).getPath().contains("deepslate");
    }

    // ---- lookup ----------------------------------------------------------------------------

    public static Collection<Material> materials() {
        if (MATERIALS.isEmpty()) rebuild();
        return MATERIALS.values();
    }

    public static Material material(String name) {
        if (MATERIALS.isEmpty()) rebuild();
        return MATERIALS.get(name);
    }

    /** The material a shattering input belongs to, or null when it is not one. */
    public static Material inputMaterial(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (MATERIALS.isEmpty()) rebuild();
        return INPUTS.get(stack.getItem());
    }

    /**
     * The grit item for a material. The three metals that shipped before 3.1 keep their own items so old
     * saves and old recipes still mean what they meant; everything else rides the one component item.
     */
    public static ItemStack stackFor(String material) {
        return switch (material) {
            case "iron" -> new ItemStack(ModItems.IRON_GRIT.get());
            case "copper" -> new ItemStack(ModItems.COPPER_GRIT.get());
            case "gold" -> new ItemStack(ModItems.GOLD_GRIT.get());
            default -> MineralGritItem.of(material);
        };
    }

    /** The material a grit stack holds, or null when the stack is not grit. */
    public static String materialOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModItems.IRON_GRIT.get())) return "iron";
        if (stack.is(ModItems.COPPER_GRIT.get())) return "copper";
        if (stack.is(ModItems.GOLD_GRIT.get())) return "gold";
        return MineralGritItem.materialOf(stack);
    }

    /** The ingot a grit stack fires into, or empty when nothing knows how. */
    public static ItemStack ingotFor(ItemStack grit) {
        String name = materialOf(grit);
        if (name == null) return ItemStack.EMPTY;
        Material material = material(name);
        return material == null || material.ingot() == null ? ItemStack.EMPTY : new ItemStack(material.ingot());
    }

    // ---- synthesised recipes ---------------------------------------------------------------

    /**
     * The shattering a discovered material implies, for inputs no datapack recipe covers.
     * Written recipes are found first by {@link ProcessingRecipes#find}, so a pack can always override.
     */
    public static ProcessingRecipes.Formula formula(String station, ItemStack stack) {
        if (!STATION.equals(station)) return null;
        Material material = inputMaterial(stack);
        if (material == null) return null;
        ItemStack result = material.shatterResult();
        if (result.isEmpty()) return null;
        LatticeRecipe recipe = new LatticeRecipe(STATION, Ingredient.of(stack.getItem()), result,
                SHATTER_SECONDS, SHATTER_PULSE, Attunement.EARTH);
        return new ProcessingRecipes.Formula(syntheticId(material, stack), recipe);
    }

    /** Every shattering the scan implies, for JEI: synthesised recipes are invisible otherwise. */
    public static List<ProcessingRecipes.Formula> allFormulae() {
        List<ProcessingRecipes.Formula> out = new ArrayList<>();
        for (Map.Entry<Item, Material> entry : INPUTS.entrySet()) {
            ItemStack input = new ItemStack(entry.getKey());
            ProcessingRecipes.Formula formula = formula(STATION, input);
            if (formula != null) out.add(formula);
        }
        out.sort(java.util.Comparator.comparing(formula -> formula.id().toString()));
        return out;
    }

    private static ResourceLocation syntheticId(Material material, ItemStack input) {
        String source = BuiltInRegistries.ITEM.getKey(input.getItem()).toString()
                .replace(':', '.').toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "grit/" + material.name() + "/" + source);
    }

    /** True when {@code level} has a written recipe for this input, which always outranks the scan. */
    public static boolean hasWrittenRecipe(Level level, String station, ItemStack stack) {
        return ProcessingRecipes.findWritten(level, station, stack) != null;
    }
}
