package tk.darrow.tribalpower.entity;

import java.util.Set;

/**
 * Which body each creature is drawn with.
 *
 * <p>The hand-built cuboid rigs come out of {@code art/creatures/tribal_bestiary.blend} through
 * {@code tools/blender_bestiary.py}, and the client switches on this id to pick one. A creature with
 * no rig used to throw on the render thread the moment the game loaded, which nothing on the server
 * could see — so the mapping lives here, in common code, where a test can hold it to the roster.
 *
 * <p>The 3.9 creatures borrow the nearest existing body and wear their own colours. Give them their
 * own rigs by adding them to the Blender source and running the exporter; then drop their alias.
 */
public final class CreatureRigs {
    /** Every id the exporter has actually built geometry for. */
    public static final Set<String> BUILT = Set.of(
            "dawn_stag", "lantern_fox", "mossback", "ashbound", "rootbound", "reed_stalker", "shardback",
            "hollow_sentinel", "storm_moth", "cinder_imp", "mourning_bell", "rift_hound", "echo_weaver",
            "tuftback", "longshank", "palewing", "burrow_gnasher", "cragcoat", "bouldersnout", "updrifter",
            "scree_lurcher", "prism_grazer", "shardmoth", "glowgrub", "facet_stalker", "driftpelt",
            "frost_strider", "snowveil", "rime_crawler", "cinderhide", "ashmoth", "slaglump", "emberjaw",
            "marsh_hopper", "fen_strider", "bog_floater", "reed_coil", "chime_grazer", "glass_flitter",
            "geode_grub", "shard_stalker", "silt_glider", "pale_drifter", "shoal_darter", "brine_lurker",
            "hearth_warden", "grove_elder", "frostpine_sentinel", "snagwalker", "weeping_warden",
            "bellcap_elder", "stiltwood", "colossus_warden", "crag_troll", "rime_troll", "slag_troll",
            "fen_goblin", "tunnel_goblin", "ridge_kobold", "geode_kobold", "glimmer_fay", "prism_fay",
            "marsh_fay", "barrow_shade", "pale_wraith", "cinder_shade", "drowned_shade");

    private CreatureRigs() {}

    /** The rig a creature is drawn with: its own where one exists, otherwise its nearest cousin. */
    public static String rig(String id) {
        return switch (id) {
            case "ash_hopper" -> "lantern_fox";
            case "ridge_grazer" -> "mossback";
            case "glimmer_moth" -> "storm_moth";
            case "deep_lurker" -> "rift_hound";
            case "pale_stalker" -> "reed_stalker";
            case "dust_flitter" -> "storm_moth";
            case "veil_drifter" -> "mourning_bell";
            case "crag_bounder" -> "dawn_stag";
            case "stone_grub" -> "mossback";
            case "gloom_crawler" -> "echo_weaver";
            case "ember_drifter" -> "storm_moth";
            case "magma_creeper" -> "mossback";
            default -> id;
        };
    }
}
