package tk.darrow.tribalpower.bench;

import java.util.List;

/**
 * Every wood the Tribal Bench comes in.
 *
 * <p>Eleven vanilla woods and the seven the March grows, so a bench matches whatever the camp around
 * it is built from. They differ only in what they are made of: the same block, the same grid, the
 * same shelf, cut from a different tree.
 *
 * <p>The ids here drive the block registry, the recipes, the lang file and the textures, which are
 * all generated from this list by {@code tools/build_tribal_bench.py}. Adding a wood means adding it
 * here and running that script.
 */
public final class BenchWoods {
    private BenchWoods() {}

    /** A wood, and the planks a bench of it is made from. */
    public record Wood(String id, String planks, boolean vanilla) {
        /** The block id: {@code oak_tribal_bench}, {@code willow_tribal_bench}. */
        public String block() { return id + "_tribal_bench"; }

        /** The plank item this bench is crafted from, namespaced. */
        public String plankItem() {
            return (vanilla ? "minecraft:" : "tribalpower:") + planks;
        }
    }

    public static final List<Wood> VANILLA = List.of(
            new Wood("oak", "oak_planks", true),
            new Wood("spruce", "spruce_planks", true),
            new Wood("birch", "birch_planks", true),
            new Wood("jungle", "jungle_planks", true),
            new Wood("acacia", "acacia_planks", true),
            new Wood("dark_oak", "dark_oak_planks", true),
            new Wood("mangrove", "mangrove_planks", true),
            new Wood("cherry", "cherry_planks", true),
            new Wood("bamboo", "bamboo_planks", true),
            new Wood("crimson", "crimson_planks", true),
            new Wood("warped", "warped_planks", true));

    /** The March's own woods. {@code march} is the mixed grove; the rest are the named trees. */
    public static final List<Wood> MARCH = List.of(
            new Wood("march", "march_planks", false),
            new Wood("willow", "willow_planks", false),
            new Wood("hearthoak", "hearthoak_planks", false),
            new Wood("bellcap", "bellcap_planks", false),
            new Wood("frostpine", "frostpine_planks", false),
            new Wood("cinder", "cinder_planks", false),
            new Wood("strider", "strider_planks", false));

    public static final List<Wood> ALL =
            java.util.stream.Stream.concat(VANILLA.stream(), MARCH.stream()).toList();

    public static Wood of(String id) {
        for (Wood wood : ALL) if (wood.id().equals(id)) return wood;
        throw new IllegalArgumentException("No bench wood called " + id);
    }
}
