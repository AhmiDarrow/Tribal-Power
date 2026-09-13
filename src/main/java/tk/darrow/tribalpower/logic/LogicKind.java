package tk.darrow.tribalpower.logic;

/** Face-mounted song plates. Names are tribal; each still does one clear logic job. */
public enum LogicKind {
    CHORUS("chorus_plate"),
    GATHERING("gathering_plate"),
    DISCORD("discord_plate"),
    HUSH("hush_plate"),
    INVERSE("inverse_plate"),
    ECHO("echo_plate"),
    MEMORY("memory_plate"),
    HEARTBEAT("heartbeat_plate"),
    DRIFT("drift_plate"),
    TALLY("tally_plate"),
    STRIKE("strike_plate"),
    CHANCE("chance_plate"),
    VERSE("verse_plate");

    public final String id;

    LogicKind(String id) { this.id = id; }

    public static LogicKind fromId(String id) {
        for (LogicKind kind : values()) if (kind.id.equals(id)) return kind;
        return CHORUS;
    }
}
