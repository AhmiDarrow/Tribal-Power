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

    public static KinRole byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
