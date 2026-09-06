package tk.darrow.tribalpower.api.pulse;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Harmonic identity held by a Resonance Totem within a Totem Lattice.
 */
public enum Attunement implements StringRepresentable {
    EARTH("earth"),
    FIRE("fire"),
    WATER("water"),
    AIR("air"),
    SPIRIT("spirit");

    public static final Codec<Attunement> CODEC = StringRepresentable.fromEnum(Attunement::values);

    private final String name;

    Attunement(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Attunement byName(String name) {
        for (Attunement a : values()) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return SPIRIT;
    }
}
