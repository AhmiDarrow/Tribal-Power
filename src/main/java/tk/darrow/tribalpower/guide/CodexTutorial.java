package tk.darrow.tribalpower.guide;

import java.util.List;

/** Ordered reading path through the Spirit Codex. The reader can step Next along this chain. */
public final class CodexTutorial {
    public static final List<String> SEQUENCE = List.of(
            "chapter_1", "chapter_2", "chapter_3", "chapter_4",
            "walk_pulse_resonator", "walk_lattice_conductor",
            "chapter_11", "chapter_12",
            "camp_grove_tender", "workshop_hands", "workshop_seal_loom", "workshop_tide_pump",
            "workshop_wind_snare", "workshop_ward_drum",
            "song_thread", "song_vine", "verse_wireless", "logic_plates", "pulse_logic",
            "walk_grove_tender", "walk_seal_loom", "walk_song_clock"
    );

    private CodexTutorial() {}

    public static String next(String id) {
        int index = SEQUENCE.indexOf(id);
        if (index < 0 || index + 1 >= SEQUENCE.size()) return null;
        return SEQUENCE.get(index + 1);
    }
}
