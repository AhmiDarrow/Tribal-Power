package tk.darrow.tribalpower.cuisine;

import java.util.Locale;

/**
 * The March's five crops: what grows wild in each country, and what a hoe can bring home. Each is its own seed,
 * the way potatoes are, and each is edible raw, if not much of a meal.
 */
public enum MarchCrop {
    EMBERROOT("march_ember_wastes", 0x6E2A1A, 0xE8703A, 2, 0.3F),
    FEN_RICE("march_reed_fen", 0x4A6A2E, 0xD8D0A0, 2, 0.2F),
    FROSTBERRY("march_snow_fields", 0x2E4A6E, 0x9AD8F0, 2, 0.3F),
    GLIMMER_BEAN("march_glimmer_ridge", 0x3A5A5E, 0x9AE8D8, 2, 0.3F),
    STEPPE_GRAIN("march_steppe", 0x6A5A2A, 0xE8D080, 1, 0.2F);

    public final String biome;
    public final int leaf, fruit;
    public final int nutrition;
    public final float saturation;

    MarchCrop(String biome, int leaf, int fruit, int nutrition, float saturation) {
        this.biome = biome;
        this.leaf = leaf;
        this.fruit = fruit;
        this.nutrition = nutrition;
        this.saturation = saturation;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
