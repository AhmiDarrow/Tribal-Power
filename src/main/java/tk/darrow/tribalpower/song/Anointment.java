package tk.darrow.tribalpower.song;

import java.util.List;
import java.util.Locale;

/**
 * What a reagent does once it is worked into a weapon at the Song Bench. The reagent's Note decides it, so every
 * reagent of one Note gives the same anointment. Each one's numbers are declared here with their shipped values
 * and read live from the {@code anointing} section of the common config.
 */
public enum Anointment {
    SEARING(Note.EMBER,
            new Param("burnSeconds", 4, 0, 60, "Seconds the target burns."),
            new Param("bonusDamage", 2, 0, 100, "Extra fire damage on each hit.")),
    STORMCALL(Note.BOLT,
            new Param("chance", 0.25, 0, 1, "Chance a hit arcs to nearby hostiles."),
            new Param("arcDamage", 4, 0, 100, "Damage each arc deals."),
            new Param("arcTargets", 2, 0, 16, "Most hostiles one arc reaches."),
            new Param("arcRange", 4, 1, 16, "Blocks an arc can jump.")),
    FROSTBITE(Note.CHILL,
            new Param("seconds", 3, 0, 60, "Seconds the target is slowed and frozen."),
            new Param("slownessLevel", 2, 1, 10, "Level of the Slowness applied.")),
    VENOM(Note.VENOM,
            new Param("seconds", 5, 0, 60, "Seconds of Poison."),
            new Param("poisonLevel", 1, 1, 10, "Level of the Poison applied.")),
    GALE(Note.GUST,
            new Param("knockback", 0.8, 0, 5, "Extra knockback strength."),
            new Param("lift", 0.25, 0, 2, "Upward push on each hit.")),
    ROOTING(Note.ROOT,
            new Param("chance", 0.2, 0, 1, "Chance a hit roots the target in place."),
            new Param("seconds", 2, 0, 30, "Seconds a rooted target cannot move.")),
    SUNDERING(Note.SHOVE,
            new Param("armorFraction", 0.25, 0, 5, "Fraction of the target's armour points dealt as bonus damage.")),
    ECHO(Note.WEAVE,
            new Param("chance", 0.25, 0, 1, "Chance a hit strikes a second time."),
            new Param("fraction", 0.5, 0, 5, "The second strike's damage, as a fraction of the first.")),
    SAPPING(Note.WEAKEN,
            new Param("seconds", 5, 0, 60, "Seconds of Weakness."),
            new Param("weaknessLevel", 1, 1, 10, "Level of the Weakness applied.")),
    BLOODTHIRST(Note.HOUND,
            new Param("lifeSteal", 0.15, 0, 5, "Fraction of the damage dealt returned to the wielder as health.")),
    MENDING(Note.QUIET,
            new Param("repairPerHit", 2, 0, 100, "Durability restored to the weapon on each hit."));

    public record Param(String key, double value, double min, double max, String comment) {}

    public final Note note;
    public final List<Param> params;

    Anointment(Note note, Param... params) {
        this.note = note;
        this.params = List.of(params);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Anointment of(Note note) {
        for (Anointment anointment : values()) if (anointment.note == note) return anointment;
        return MENDING;
    }

    public double shipped(String key) {
        for (Param param : params) if (param.key.equals(key)) return param.value;
        throw new IllegalArgumentException(name() + " has no " + key);
    }
}
