package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * The voice an automated device answers to. Every hand that works on its own asks for a Resonance Totem of its
 * own voice, kept (not quiet) within the lattice reach: the Grove Tender the Earth, the Ward Drum the Spirit,
 * the Tide Pump the Water, the Wind Snare the Air, the Seal Loom the Loom, relays the Air (Astral ones the Loom).
 * Without it the device stands idle and says which totem it wants. {@code automationNeedsVoices} in the config
 * lets a pack waive it.
 */
public final class Voices {
    private Voices() {}

    /** The voice a camp or workshop device of that kind answers to, or null when it asks for none. */
    public static @Nullable Attunement required(String kind) {
        return switch (kind) {
            case "grove_tender", "wayanchor" -> Attunement.EARTH;
            case "hush_totem", "summoning_cradle", "ward_drum" -> Attunement.SPIRIT;
            case "tide_pump" -> Attunement.WATER;
            case "wind_snare" -> Attunement.AIR;
            case "seal_loom" -> Attunement.LOOM;
            default -> null;
        };
    }

    /** A relay's voice: through the air, or along the Loom's threads when it crosses worlds. */
    public static Attunement relay(int tier) {
        return tier >= 3 ? Attunement.LOOM : Attunement.AIR;
    }

    /** Whether a totem of that voice keeps (is not quiet) within the lattice reach of a device. */
    public static boolean kept(Level level, BlockPos pos, @Nullable Attunement voice) {
        if (voice == null || !TribalConfig.automationNeedsVoices()) return true;
        for (var totem : LatticeNetwork.findNearbyTotems(level, pos, LatticeNetwork.DEFAULT_RADIUS))
            if (totem.getAttunement() == voice && totem.keeping() != Keeping.State.QUIET) return true;
        return false;
    }

    /** Whether a device of that kind may work here: its voice kept nearby, or no voice asked for. */
    public static boolean answers(Level level, BlockPos pos, String kind) {
        return kept(level, pos, required(kind));
    }

    public static Component name(Attunement voice) {
        return Component.translatable("attunement.tribalpower." + voice.getSerializedName());
    }

    /** The name from a saved serialized voice (what a device keeps in its status), translated on the reader's side. */
    public static Component name(String serialized) {
        for (Attunement voice : Attunement.values()) if (voice.getSerializedName().equals(serialized)) return name(voice);
        return Component.literal(serialized);
    }
}
