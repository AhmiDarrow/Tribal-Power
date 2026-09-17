package tk.darrow.tribalpower.guide;

import java.util.List;

/** Ordered reading path through the Spirit Codex. The reader can step Next along this chain. */
public final class CodexTutorial {
    public static final List<String> SEQUENCE = List.of(
            "chapter_1", "chapter_2", "chapter_3", "chapter_4",
            "chapter_11", "chapter_12",
            "walk_ember_kiln",
            "walk_pulse_resonator", "walk_lattice_conductor",
            "walk_echo_chain",
            "song_thread", "walk_song_clock", "walk_seal_loom",
            "walk_ley_collector", "walk_ancestral_cache",
            "walk_snap_relay", "walk_stone_font",
            "walk_first_camp",
            "walk_gate_drum", "walk_waystone", "walk_way_gate",
            "walk_listening_pit",
            "walk_found_a_camp", "walk_bond_a_familiar",
            "walk_totem_bound_gear", "walk_first_rite",
            "walk_wake_the_unsung", "walk_sixth_voice", "walk_echo_unweave"
    );

    private CodexTutorial() {}

    public static String next(String id) {
        int index = SEQUENCE.indexOf(id);
        if (index < 0 || index + 1 >= SEQUENCE.size()) return null;
        return SEQUENCE.get(index + 1);
    }
}
