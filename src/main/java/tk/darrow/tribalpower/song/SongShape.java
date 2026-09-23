package tk.darrow.tribalpower.song;

/** The five things a written song can do. Extra reagents dress one of these. They do not add a second spell. */
public enum SongShape {
    BOLT,
    WARD,
    STEP,
    CALL,
    BIND;

    public String key() {
        return "song.tribalpower.shape." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
