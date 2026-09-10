package tk.darrow.tribalpower.gate;

import net.minecraft.resources.ResourceLocation;

/**
 * What colour a destination is (design 3.1 section 8). Common, not client-only: the keystone computes the
 * tint on the server from its partner's dimension and syncs it, so the plane shows where it goes rather
 * than where it stands.
 */
public final class GateTint {
    public static final int OVERWORLD = 0x5FB871;
    public static final int MARCH = 0x3FBFB4;
    public static final int NETHER = 0xC7503A;
    public static final int END = 0xB9A4D6;
    /** A gate that answers nothing, or answers somewhere this build has never heard of. */
    public static final int UNKNOWN = 0x8FA0A8;

    private GateTint() {}

    public static int of(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        return id == null ? UNKNOWN : of(id);
    }

    public static int of(ResourceLocation dimension) {
        return switch (dimension.toString()) {
            case "minecraft:overworld" -> OVERWORLD;
            case "minecraft:the_nether" -> NETHER;
            case "minecraft:the_end" -> END;
            case "tribalpower:the_march" -> MARCH;
            default -> UNKNOWN;
        };
    }
}
