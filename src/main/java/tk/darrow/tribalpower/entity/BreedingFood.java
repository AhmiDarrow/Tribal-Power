package tk.darrow.tribalpower.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * What every gentle March creature eats: the food that tempts it, breeds it, tames it and heals it.
 *
 * <p>Most creatures used to want their own reagent, which only brushing that same creature gives, so a herd
 * could never be started from nothing. Each now eats something a player can actually carry in: the wild crop of
 * its own land where it has one (Steppe Grain, Fen Rice, Frostberries, Glimmer Beans, Emberroot), and a common
 * item where it does not. Remnants are not here: they still take their own reagent (see LatticeMonster).
 */
public final class BreedingFood {
    /** Any of these items, or anything in the tag. The first item is the one tests and the codex hand over. */
    public record Food(List<ResourceLocation> items, TagKey<Item> tag) {
        public boolean test(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (tag != null && stack.is(tag)) return true;
            for (var id : items) if (stack.is(BuiltInRegistries.ITEM.get(id))) return true;
            return false;
        }

        public ItemStack sample() {
            return new ItemStack(BuiltInRegistries.ITEM.get(items.get(0)));
        }
    }

    private static Food food(TagKey<Item> tag, String... ids) {
        return new Food(java.util.Arrays.stream(ids).map(ResourceLocation::parse).toList(), tag);
    }

    private static Food food(String... ids) {
        return food((TagKey<Item>) null, ids);
    }

    private static final String STEPPE = "tribalpower:steppe_grain", FEN = "tribalpower:fen_rice",
            FROST = "tribalpower:frostberry", BEAN = "tribalpower:glimmer_bean", EMBER = "tribalpower:emberroot";
    // The cave grubs eat the March's own lantern caps, or any mushroom; the shallows eat its ribbon weed or seagrass.
    private static final Food GRUB = food(Tags.Items.MUSHROOMS, "tribalpower:lantern_cap", "minecraft:brown_mushroom");
    private static final Food WEED = food("tribalpower:ribbon_weed", "minecraft:seagrass");
    private static final Food BLOOM = food(ItemTags.SMALL_FLOWERS, "tribalpower:echo_bloom", "minecraft:dandelion");
    private static final Food SEED = food("minecraft:wheat_seeds", STEPPE);

    private static final Map<String, Food> BY_ID = Map.ofEntries(
            // the first three keep the foods they always had
            Map.entry("dawn_stag", food("minecraft:wheat")),
            Map.entry("lantern_fox", food("minecraft:sweet_berries")),
            Map.entry("mossback", food("minecraft:seagrass", "tribalpower:ribbon_weed")),
            // wanderers that live in many lands eat what any farm grows
            Map.entry("ash_hopper", food("minecraft:carrot", STEPPE)),
            Map.entry("ridge_grazer", food("minecraft:wheat")),
            Map.entry("crag_bounder", food("minecraft:wheat")),
            Map.entry("glimmer_moth", BLOOM),
            Map.entry("dust_flitter", SEED),
            Map.entry("stone_grub", GRUB),
            Map.entry("ember_drifter", food(ItemTags.COALS, "minecraft:charcoal")),
            Map.entry("magma_creeper", food(ItemTags.COALS, "minecraft:coal")),
            // Steppe
            Map.entry("tuftback", food(STEPPE)),
            Map.entry("longshank", food(STEPPE)),
            Map.entry("palewing", food(STEPPE, "minecraft:wheat_seeds")),
            // Highlands: no wild crop, so farm food and the ley thistle that grows there
            Map.entry("cragcoat", food("minecraft:wheat")),
            Map.entry("bouldersnout", food("minecraft:potato", "minecraft:carrot")),
            Map.entry("updrifter", food("tribalpower:ley_thistle")),
            // Glimmer Ridge
            Map.entry("prism_grazer", food(BEAN)),
            Map.entry("shardmoth", food(BEAN)),
            Map.entry("glowgrub", GRUB),
            // Snow Fields
            Map.entry("driftpelt", food(FROST)),
            Map.entry("frost_strider", food(FROST)),
            Map.entry("snowveil", food(FROST)),
            // Ember Wastes
            Map.entry("cinderhide", food(EMBER)),
            Map.entry("ashmoth", food(EMBER)),
            Map.entry("slaglump", food(EMBER)),
            // Reed Fen
            Map.entry("marsh_hopper", food(FEN)),
            Map.entry("fen_strider", food(FEN)),
            Map.entry("bog_floater", food(FEN)),
            // Crystal Fields: no wild crop either
            Map.entry("chime_grazer", food("minecraft:wheat")),
            Map.entry("glass_flitter", BLOOM),
            Map.entry("geode_grub", GRUB),
            // Shallows
            Map.entry("silt_glider", WEED),
            Map.entry("pale_drifter", WEED),
            Map.entry("shoal_darter", WEED),
            // the walking trees take bone meal, the fays anything sweet
            Map.entry("hearth_warden", food("minecraft:bone_meal")),
            Map.entry("grove_elder", food("minecraft:bone_meal")),
            Map.entry("frostpine_sentinel", food("minecraft:bone_meal")),
            Map.entry("snagwalker", food("minecraft:bone_meal")),
            Map.entry("weeping_warden", food("minecraft:bone_meal")),
            Map.entry("bellcap_elder", food("minecraft:bone_meal")),
            Map.entry("stiltwood", food("minecraft:bone_meal")),
            Map.entry("glimmer_fay", food("minecraft:sugar", "minecraft:honey_bottle")),
            Map.entry("prism_fay", food("minecraft:sugar", "minecraft:honey_bottle")),
            Map.entry("marsh_fay", food("minecraft:sugar", "minecraft:honey_bottle")),
            // the March's older and smaller life
            Map.entry("march_walker", food("minecraft:wheat", STEPPE)),
            Map.entry("spirit_wisp", food("tribalpower:spirit_shard")),
            Map.entry("glimmerfin", WEED),
            Map.entry("drift_bell", WEED),
            Map.entry("veil_ray", WEED),
            Map.entry("loom_swift", SEED));

    private BreedingFood() {}

    /** Null for anything that is not bred: the remnants, the hunters, the Silt Eel and the bosses. */
    public static Food of(String id) {
        return BY_ID.get(id);
    }

    public static Food of(CreatureProfile profile) {
        return of(profile.id);
    }

    public static Food of(EntityType<?> type) {
        return of(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }

    public static boolean isFood(EntityType<?> type, ItemStack stack) {
        Food food = of(type);
        return food != null && food.test(stack);
    }
}
