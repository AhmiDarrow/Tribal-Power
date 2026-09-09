package tk.darrow.tribalpower.rite.world;

import tk.darrow.tribalpower.api.pulse.Attunement;

/** The six world rites: which seal must be seated, how much Pulse the brazier draws, and how long the effect lasts. */
public enum WorldRite {
    RAIN_CALLING("rite_rain_calling", Attunement.WATER, 400, 20 * 60 * 20),
    SKY_CLEARING("rite_sky_clearing", Attunement.AIR, 400, 40 * 60 * 20),
    DAWN_CALLING("rite_dawn_calling", Attunement.FIRE, 800, 0),
    GREEN_BLESSING("rite_green_blessing", Attunement.EARTH, 600, 20 * 60 * 20),
    STILL_NIGHT("rite_still_night", Attunement.SPIRIT, 600, 15 * 60 * 20),
    LEY_BINDING("rite_ley_binding", Attunement.LOOM, 1200, 30 * 60 * 20);

    private final String tabletId;
    private final Attunement element;
    private final int cost;
    private final int durationTicks;

    WorldRite(String tabletId, Attunement element, int cost, int durationTicks) {
        this.tabletId = tabletId;
        this.element = element;
        this.cost = cost;
        this.durationTicks = durationTicks;
    }

    public String tabletId() { return tabletId; }
    public Attunement element() { return element; }
    public int cost() { return cost; }
    public int durationTicks() { return durationTicks; }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
}
