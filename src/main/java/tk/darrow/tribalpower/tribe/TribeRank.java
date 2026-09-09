package tk.darrow.tribalpower.tribe;

/** Standing ranks and their thresholds (design 3.0 §2 Standing). */
public enum TribeRank {
    STRANGER(0), GUEST(50), FRIEND(150), KIN(400), VOICE(800);

    private final int threshold;

    TribeRank(int threshold) { this.threshold = threshold; }

    public int threshold() { return threshold; }
    public String translationKey() { return "tribe.tribalpower.rank." + name().toLowerCase(java.util.Locale.ROOT); }

    public static TribeRank of(int standing) {
        TribeRank rank = STRANGER;
        for (TribeRank r : values()) if (standing >= r.threshold) rank = r;
        return rank;
    }

    public TribeRank next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : this;
    }
}
