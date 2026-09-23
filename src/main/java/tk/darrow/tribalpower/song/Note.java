package tk.darrow.tribalpower.song;

import tk.darrow.tribalpower.entity.CreatureProfile;

/**
 * The family of a reagent, taken from the creature's own attack. Songs and verse arrows speak in
 * these notes. A fen hoof and a frost hoof are two pitches of the same quiet note, not two spells.
 */
public enum Note {
    EMBER,
    BOLT,
    CHILL,
    VENOM,
    GUST,
    ROOT,
    SHOVE,
    WEAVE,
    WEAKEN,
    HOUND,
    QUIET;

    public static Note of(CreatureProfile profile) {
        return switch (profile.attack) {
            case "ember" -> EMBER;
            case "bolt" -> BOLT;
            case "chill" -> CHILL;
            case "venom" -> VENOM;
            case "gust" -> GUST;
            case "root" -> ROOT;
            case "shove" -> SHOVE;
            case "weave" -> WEAVE;
            case "weaken" -> WEAKEN;
            case "hound" -> HOUND;
            default -> QUIET;
        };
    }

    /** The shape a song takes when this note is the first one seated. */
    public SongShape shape() {
        return switch (this) {
            case EMBER, BOLT, HOUND -> SongShape.BOLT;
            case VENOM, WEAKEN, CHILL, WEAVE -> SongShape.BIND;
            case GUST -> SongShape.STEP;
            case ROOT, SHOVE -> SongShape.CALL;
            case QUIET -> SongShape.WARD;
        };
    }
}
