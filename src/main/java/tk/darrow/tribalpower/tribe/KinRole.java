package tk.darrow.tribalpower.tribe;

/** Tribal Kin roles. Serialised by name in the {@code Role} NBT key. */
public enum KinRole {
    ELDER, DRUMMER, HUNTER, WEAVER;

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public String translationKey() { return "tribe.tribalpower.role." + id(); }

    public static KinRole byName(String name) {
        for (KinRole r : values()) if (r.name().equalsIgnoreCase(name)) return r;
        return WEAVER;
    }

    /** {@code values()} copies the array on every call; Kin read their role several times a tick. */
    private static final KinRole[] ALL = values();

    public static KinRole byOrdinal(int ordinal) {
        return ALL[Math.floorMod(ordinal, ALL.length)];
    }
}
