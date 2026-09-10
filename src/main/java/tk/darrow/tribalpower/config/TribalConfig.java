package tk.darrow.tribalpower.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Balance knobs pack makers need (design 3.1 section 14). Hard-coded balance is a support burden, so the
 * numbers a pack is most likely to want to move live in {@code tribalpower-common.toml}.
 *
 * <p>Every accessor tolerates being read before the config loads -- game tests and datagen both do -- and
 * falls back to the shipped default rather than throwing.
 */
public final class TribalConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue GENERATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PIT_SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue GATE_TRAVEL_COST;
    public static final ModConfigSpec.IntValue FAR_GATE_TRAVEL_COST;
    public static final ModConfigSpec.BooleanValue ENABLE_FAR_GATES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GRIT_DENY_LIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Tribal Power balance. Every value is server-authoritative.").push("balance");
        GENERATION_MULTIPLIER = b
                .comment("Scales the Pulse every generator produces. 1.0 is the shipped balance.")
                .defineInRange("generationMultiplier", 1.0, 0.0, 16.0);
        PIT_SPEED_MULTIPLIER = b
                .comment("Scales how fast the Listening Pit and Stone Font cycle. Higher is faster; Pulse per second is unchanged, so a faster cycle is also a cheaper one.")
                .defineInRange("pitSpeedMultiplier", 1.0, 0.05, 20.0);
        GATE_TRAVEL_COST = b
                .comment("Pulse a Way Gate spends per entity that steps through.")
                .defineInRange("gateTravelCost", 20, 0, 100000);
        FAR_GATE_TRAVEL_COST = b
                .comment("Pulse a Far Gate spends per entity that steps through.")
                .defineInRange("farGateTravelCost", 120, 0, 100000);
        ENABLE_FAR_GATES = b
                .comment("When false, Far Gate keystones refuse to link across dimensions. Way Gates are unaffected.")
                .define("enableFarGates", true);
        GRIT_DENY_LIST = b
                .comment("Materials the grit scan must ignore, by material name (the part after the tag prefix), e.g. \"netherite\".",
                        "Ancient debris is already excluded: netherite should require the Nether.")
                .defineListAllowEmpty("gritDenyList", List.of(), () -> "", o -> o instanceof String s && !s.isBlank());
        b.pop();
        SPEC = b.build();
    }

    private TribalConfig() {}

    public static double generationMultiplier() { return SPEC.isLoaded() ? GENERATION_MULTIPLIER.get() : 1.0; }

    public static double pitSpeedMultiplier() { return SPEC.isLoaded() ? PIT_SPEED_MULTIPLIER.get() : 1.0; }

    public static int gateTravelCost() { return SPEC.isLoaded() ? GATE_TRAVEL_COST.get() : 20; }

    public static int farGateTravelCost() { return SPEC.isLoaded() ? FAR_GATE_TRAVEL_COST.get() : 120; }

    public static boolean farGatesEnabled() { return !SPEC.isLoaded() || ENABLE_FAR_GATES.get(); }

    public static List<? extends String> gritDenyList() { return SPEC.isLoaded() ? GRIT_DENY_LIST.get() : List.of(); }

    /** Applies {@link #GENERATION_MULTIPLIER} without ever rounding a working generator down to nothing. */
    public static int scaleGeneration(int pulse) {
        if (pulse <= 0) return 0;
        double scaled = pulse * generationMultiplier();
        return scaled <= 0 ? 0 : Math.max(1, (int) Math.round(scaled));
    }
}
