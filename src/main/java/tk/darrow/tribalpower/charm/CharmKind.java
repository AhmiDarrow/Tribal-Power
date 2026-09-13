package tk.darrow.tribalpower.charm;

import tk.darrow.tribalpower.api.pulse.Attunement;

/**
 * Ten Spirit Charms. Each is a power object: craft it, then bind one or more totem voices.
 * More voices cost more Pulse and do more. Sky with Air is creative flight.
 */
public enum CharmKind {
    SKY("sky_charm", Attunement.AIR),
    EMBER("ember_charm", Attunement.FIRE),
    TIDE("tide_charm", Attunement.WATER),
    ROOT("root_charm", Attunement.EARTH),
    LANTERN("lantern_charm", Attunement.SPIRIT),
    SPINDLE("spindle_charm", Attunement.LOOM),
    WARD("ward_charm", Attunement.EARTH),
    HEARTH("hearth_charm", Attunement.FIRE),
    VEIL("veil_charm", Attunement.SPIRIT),
    CHORUS("chorus_charm", null);

    public final String id;
    public final Attunement nativeVoice;

    CharmKind(String id, Attunement nativeVoice) {
        this.id = id;
        this.nativeVoice = nativeVoice;
    }
}
