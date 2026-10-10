package tk.darrow.tribalpower.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Balance knobs pack makers need (design 3.1 section 14). Hard-coded balance is a support burden, so the
 * numbers a pack is most likely to want to move live in {@code tribalpower-common.toml}.
 *
 * <p>Every accessor tolerates being read before the config loads -- game tests and datagen both do -- and
 * falls back to the shipped default rather than throwing.
 */
public final class TribalConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue GENERATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CONSUMPTION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PIT_SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue GATE_TRAVEL_COST;
    public static final ModConfigSpec.IntValue FAR_GATE_TRAVEL_COST;
    public static final ModConfigSpec.BooleanValue ENABLE_FAR_GATES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GRIT_DENY_LIST;
    public static final ModConfigSpec.BooleanValue RETROGEN_MARCH;

    public static final ModConfigSpec.IntValue DAY_SPAWN_ONE_IN;
    public static final ModConfigSpec.IntValue NIGHT_CROWD_CAP;
    public static final ModConfigSpec.IntValue EVENT_WARNING_SECONDS;
    public static final ModConfigSpec.IntValue DAY_CROWD_CAP;
    public static final ModConfigSpec.IntValue CROWD_RADIUS;
    public static final ModConfigSpec.IntValue MAX_GROUP_SIZE;
    public static final ModConfigSpec.BooleanValue DAWN_FADE;
    public static final ModConfigSpec.IntValue DAWN_FADE_ONE_IN;
    public static final ModConfigSpec.DoubleValue NIGHT_HEALTH_BONUS;
    public static final ModConfigSpec.DoubleValue NIGHT_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue NIGHT_ELITE_CHANCE;
    public static final ModConfigSpec.DoubleValue DAY_ELITE_CHANCE;
    public static final ModConfigSpec.DoubleValue HARD_ELITE_BONUS;
    public static final ModConfigSpec.DoubleValue LOCAL_DIFFICULTY_ELITE_BONUS;
    public static final ModConfigSpec.DoubleValue ELITE_HEALTH_BONUS;
    public static final ModConfigSpec.DoubleValue ELITE_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue ELITE_ARMOR;
    public static final ModConfigSpec.IntValue ELITE_XP_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ONLY_HUNTERS_BLOCK_SLEEP;
    public static final ModConfigSpec.BooleanValue SLEEP_SKIPS_NIGHT;
    public static final ModConfigSpec.BooleanValue MARCH_REPOPULATE;
    public static final ModConfigSpec.IntValue MARCH_REPOPULATE_SECONDS;
    public static final ModConfigSpec.IntValue MARCH_REPOPULATE_MIN_DISTANCE;
    public static final ModConfigSpec.IntValue MARCH_REPOPULATE_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue MARCH_REPOPULATE_RADIUS;
    public static final ModConfigSpec.IntValue MARCH_REPOPULATE_CAP;
    public static final ModConfigSpec.IntValue EEL_ONE_IN;

    /** Per weapon kind: attack damage, attacks per second, extra reach, and its trait number. */
    public record WeaponValues(ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue speed,
                               ModConfigSpec.DoubleValue reach, ModConfigSpec.DoubleValue trait, ModConfigSpec.DoubleValue weight) {}
    private static final java.util.Map<tk.darrow.tribalpower.item.WeaponKind, WeaponValues> WEAPONS =
            new java.util.EnumMap<>(tk.darrow.tribalpower.item.WeaponKind.class);
    public static final ModConfigSpec.IntValue ANOINT_REAGENT_COST;
    public static final ModConfigSpec.DoubleValue REQUEST_SCALE;
    public static final ModConfigSpec.IntValue REQUESTS_PER_MARK;
    public static final ModConfigSpec.IntValue REQUESTS_PER_DAY;
    public static final ModConfigSpec.BooleanValue ELDER_DIALOGUE;
    public static final ModConfigSpec.BooleanValue GUARDIANS_ENABLED;
    public static final ModConfigSpec.DoubleValue GUARDIAN_HEALTH_SCALE;
    public static final ModConfigSpec.DoubleValue GUARDIAN_DAMAGE_SCALE;
    public static final ModConfigSpec.IntValue GUARDIAN_CALL_COST;
    public static final ModConfigSpec.IntValue GUARDIAN_COOLDOWN_MINUTES;
    public static final ModConfigSpec.IntValue SILENT_DRUM_COOLDOWN_MINUTES;
    public static final ModConfigSpec.IntValue GUARDIAN_ABILITY_INTERVAL;
    public static final ModConfigSpec.IntValue GUARDIAN_ADDS_INTERVAL;
    public static final ModConfigSpec.IntValue GUARDIAN_ADDS_PER_WAVE;
    public static final ModConfigSpec.IntValue GUARDIAN_MAX_ADDS;
    public static final ModConfigSpec.IntValue GUARDIAN_RESET_SECONDS;
    public static final ModConfigSpec.IntValue NINTH_AGREEMENT_COST;
    public static final ModConfigSpec.IntValue NINTH_AGREEMENT_BOON_MINUTES;
    public static final ModConfigSpec.BooleanValue WEATHER_ENABLED;
    public static final ModConfigSpec.DoubleValue WEATHER_CHANCE_PER_HOUR;
    public static final ModConfigSpec.IntValue WEATHER_MINUTES_MIN;
    public static final ModConfigSpec.IntValue WEATHER_MINUTES_MAX;
    public static final ModConfigSpec.DoubleValue WEATHER_GENERATOR_BONUS;
    public static final ModConfigSpec.DoubleValue WEATHER_GENERATOR_PENALTY;
    public static final ModConfigSpec.BooleanValue WEATHER_SPAWN_SHIFT;
    public static final ModConfigSpec.BooleanValue SURGES_ENABLED;
    public static final ModConfigSpec.IntValue SURGE_EVERY_MINUTES;
    public static final ModConfigSpec.IntValue SURGE_MINUTES;
    public static final ModConfigSpec.DoubleValue SURGE_YIELD;
    public static final ModConfigSpec.IntValue SURGE_SICKNESS_SECONDS;
    public static final ModConfigSpec.BooleanValue FESTIVALS_ENABLED;
    public static final ModConfigSpec.IntValue FESTIVAL_CYCLE_DAYS;
    public static final ModConfigSpec.IntValue FESTIVAL_STANDING;
    public static final ModConfigSpec.IntValue FESTIVAL_RITE_STANDING;
    public static final ModConfigSpec.IntValue FESTIVAL_FEAST_STANDING;
    public static final ModConfigSpec.BooleanValue WANDERING_SPIRITS_ENABLED;
    public static final ModConfigSpec.IntValue WANDERING_SPIRIT_ONE_IN;
    public static final ModConfigSpec.IntValue WANDERING_SPIRIT_LIFE;
    public static final ModConfigSpec.IntValue DISH_BOON;
    public static final ModConfigSpec.DoubleValue HEARTH_SCALE;
    public static final ModConfigSpec.BooleanValue HEARTH_HEAT;
    public static final ModConfigSpec.IntValue FEAST_NUTRITION;
    public static final ModConfigSpec.DoubleValue FEAST_SATURATION;
    public static final ModConfigSpec.IntValue FEAST_BLESSING;
    public static final ModConfigSpec.IntValue DISH_STANDING;
    public static final ModConfigSpec.IntValue GROVE_WATER;
    public static final ModConfigSpec.BooleanValue AUTOMATION_VOICES;
    public static final ModConfigSpec.IntValue TIDE_PUMP_COST;
    public static final ModConfigSpec.IntValue WIND_SNARE_COST;
    public static final ModConfigSpec.IntValue WARD_DRUM_COST;
    public static final ModConfigSpec.DoubleValue WARD_DRUM_DAMAGE;
    public static final ModConfigSpec.IntValue SEAL_LOOM_COST;
    public static final ModConfigSpec.IntValue RELAY_COST;
    public static final ModConfigSpec.IntValue LONGREACH_RELAY_COST;
    public static final ModConfigSpec.IntValue ASTRAL_RELAY_COST;
    public static final ModConfigSpec.IntValue BRAZIER_COST;
    public static final ModConfigSpec.IntValue PLATE_LINK_RANGE;
    public static final ModConfigSpec.IntValue PLATE_LINK_MAX;
    public static final ModConfigSpec.DoubleValue EARTH_KNOCKBACK;
    public static final ModConfigSpec.DoubleValue EARTH_MINING;
    public static final ModConfigSpec.DoubleValue FIRE_BURN;
    public static final ModConfigSpec.DoubleValue WATER_SWIM;
    public static final ModConfigSpec.DoubleValue WATER_REGEN;
    public static final ModConfigSpec.DoubleValue AIR_FALL;
    public static final ModConfigSpec.DoubleValue AIR_JUMP;
    public static final ModConfigSpec.IntValue SPIRIT_SIGHT;
    public static final ModConfigSpec.DoubleValue LOOM_REFUND;
    public static final ModConfigSpec.BooleanValue BOONS_STANDING;
    public static final ModConfigSpec.DoubleValue SOIL_SAT;
    public static final ModConfigSpec.DoubleValue STONE_CHANCE;
    public static final ModConfigSpec.IntValue SPROUT_REACH;
    public static final ModConfigSpec.IntValue SPROUT_TICKS;
    public static final ModConfigSpec.DoubleValue SPARK_SPEED;
    public static final ModConfigSpec.IntValue CLOCK_REACH;
    public static final ModConfigSpec.DoubleValue SIGIL_DISCOUNT;
    public static final ModConfigSpec.DoubleValue SPINDLE_DISCOUNT;
    public static final ModConfigSpec.IntValue HUSH_SECONDS;
    public static final ModConfigSpec.DoubleValue FRAYED_HEALTH;
    public static final ModConfigSpec.IntValue FRAYED_SECONDS;
    public static final ModConfigSpec.IntValue LEY_DRAIN;
    public static final ModConfigSpec.IntValue WELL_PULSE;
    public static final ModConfigSpec.DoubleValue LANTERN_STRETCH;
    public static final ModConfigSpec.IntValue SONG_BLESSING_SECONDS;
    public static final ModConfigSpec.IntValue RITE_BLESSING_MINUTES;
    public static final ModConfigSpec.BooleanValue SPIRIT_DOOR_BLOCKS;
    public static final ModConfigSpec.IntValue LIFT_RANGE;
    public static final ModConfigSpec.IntValue CROSSBOW_LOAD;
    public static final ModConfigSpec.DoubleValue CROSSBOW_DAMAGE;
    public static final ModConfigSpec.DoubleValue CROSSBOW_SPEED;
    public static final ModConfigSpec.IntValue CROSSBOW_PULSE;
    public static final ModConfigSpec.DoubleValue CROSSBOW_SPREAD;
    public static final ModConfigSpec.DoubleValue BOW_DAMAGE;
    public static final ModConfigSpec.DoubleValue BOW_SPEED;
    public static final ModConfigSpec.DoubleValue BOW_SPREAD;
    public static final ModConfigSpec.IntValue BOW_PULSE;
    public static final ModConfigSpec.IntValue VERSE_PULSE;
    public static final ModConfigSpec.DoubleValue VERSE_DAMAGE;
    public static final ModConfigSpec.DoubleValue VERSE_DAMAGE_CAP;
    public static final ModConfigSpec.IntValue BOLT_LIFETIME;
    public static final ModConfigSpec.IntValue URN_WOVEN;
    public static final ModConfigSpec.IntValue URN_COPPER;
    public static final ModConfigSpec.IntValue URN_MANIFESTED;
    public static final ModConfigSpec.IntValue DYE_YIELD;
    // songs
    public static final ModConfigSpec.IntValue SONG_CAST_PULSE_BASE;
    public static final ModConfigSpec.IntValue SONG_CAST_PULSE_PER_REAGENT;
    public static final ModConfigSpec.DoubleValue SONG_BOLT_DAMAGE;
    public static final ModConfigSpec.DoubleValue SONG_BOLT_DAMAGE_PER_POWER;
    public static final ModConfigSpec.DoubleValue SONG_CALL_DAMAGE;
    public static final ModConfigSpec.DoubleValue SONG_CALL_DAMAGE_PER_POWER;
    public static final ModConfigSpec.IntValue SONGBOOK_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SICKNESS_CHANCE;
    public static final ModConfigSpec.BooleanValue SICKNESS_FROM_ELITES;
    public static final ModConfigSpec.BooleanValue SICKNESS_FROM_NIGHT;
    public static final ModConfigSpec.IntValue SICKNESS_SECONDS;
    public static final ModConfigSpec.IntValue SICKNESS_MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue SICKNESS_HEALTH;
    public static final ModConfigSpec.DoubleValue SICKNESS_SLOW;
    public static final ModConfigSpec.DoubleValue BLESSING_HEALTH;
    public static final ModConfigSpec.BooleanValue REMEDIES_CURE_SICKNESS;
    public static final ModConfigSpec.IntValue KETTLE_SECONDS;
    public static final ModConfigSpec.IntValue KETTLE_PULSE;
    public static final ModConfigSpec.BooleanValue KETTLE_NEEDS_HEAT;
    public static final ModConfigSpec.IntValue TINCTURE_YIELD;
    public static final ModConfigSpec.IntValue SALVE_YIELD;
    public static final ModConfigSpec.IntValue INCENSE_YIELD;
    public static final ModConfigSpec.IntValue TINCTURE_SECONDS;
    public static final ModConfigSpec.DoubleValue SALVE_HEAL;
    public static final ModConfigSpec.DoubleValue SALVE_FRACTION;
    public static final ModConfigSpec.IntValue INCENSE_BEATS;
    public static final ModConfigSpec.IntValue INCENSE_RADIUS;
    public static final ModConfigSpec.IntValue INCENSE_SECONDS;
    public static final ModConfigSpec.DoubleValue INCENSE_FAMILIAR_HEAL;
    public static final ModConfigSpec.IntValue VOICE_WATER;
    public static final ModConfigSpec.DoubleValue VOICE_FIRE;
    public static final ModConfigSpec.DoubleValue VOICE_EARTH;
    public static final ModConfigSpec.DoubleValue VOICE_AIR;
    public static final ModConfigSpec.IntValue VOICE_SPIRIT;
    public static final ModConfigSpec.DoubleValue VOICE_LOOM;
    public static final ModConfigSpec.DoubleValue RATTLE_HEAL;
    public static final ModConfigSpec.DoubleValue RATTLE_RANK_BONUS;
    public static final ModConfigSpec.IntValue RATTLE_COST;
    public static final ModConfigSpec.DoubleValue RATTLE_RANGE;
    public static final ModConfigSpec.IntValue CIRCLE_RADIUS;
    public static final ModConfigSpec.DoubleValue CIRCLE_CAST_HEAL;
    public static final ModConfigSpec.DoubleValue CIRCLE_BEAT_HEAL;
    public static final ModConfigSpec.IntValue CIRCLE_BLESSING;
    public static final ModConfigSpec.BooleanValue CIRCLE_REVIVES;
    public static final ModConfigSpec.BooleanValue REMNANTS;
    public static final ModConfigSpec.IntValue LODGE_RANGE;
    public static final ModConfigSpec.BooleanValue LODGE_ROOF;
    public static final ModConfigSpec.IntValue LODGE_BLESSING;
    public static final ModConfigSpec.IntValue ANOINT_PULSE_COST;
    // charms
    public static final ModConfigSpec.IntValue CHARM_UPKEEP_PER_VOICE;
    public static final ModConfigSpec.IntValue CHARM_UPKEEP_BEAT;
    public static final ModConfigSpec.DoubleValue CHARM_LOOM_REFUND_CHANCE;
    public static final ModConfigSpec.IntValue CHARM_LOOM_REFUND_CAP;
    public static final ModConfigSpec.IntValue GATHER_REACH;
    public static final ModConfigSpec.IntValue GATHER_REACH_PER_VOICE;
    public static final ModConfigSpec.IntValue GATHER_REACH_CAP;
    public static final ModConfigSpec.IntValue HEARTH_CHARM_FOOD;
    public static final ModConfigSpec.DoubleValue HEARTH_CHARM_SATURATION;
    public static final ModConfigSpec.IntValue CHARM_FIRE_TICKS;
    public static final ModConfigSpec.IntValue CHARM_WATER_BREATHING_TICKS;
    public static final ModConfigSpec.IntValue CHARM_DOLPHINS_GRACE_TICKS;
    public static final ModConfigSpec.IntValue CHARM_EARTH_TICKS;
    public static final ModConfigSpec.IntValue CHARM_SPIRIT_TICKS;
    public static final ModConfigSpec.IntValue CHARM_LOOM_TICKS;
    public static final ModConfigSpec.IntValue CHARM_AIR_TICKS;
    public static final ModConfigSpec.DoubleValue WARD_BLOCK_CHANCE;
    public static final ModConfigSpec.DoubleValue WARD_BLOCK_CHANCE_SPIRIT;
    public static final ModConfigSpec.IntValue CHARM_EMBER_SECONDS;
    public static final ModConfigSpec.IntValue CHARM_LINK_COST;
    public static final ModConfigSpec.IntValue CHARM_CHORUS_COST;
    // spiritgear
    public static final ModConfigSpec.IntValue GEAR_MINE_COST;
    public static final ModConfigSpec.IntValue GEAR_HIT_COST;
    public static final ModConfigSpec.IntValue GEAR_USE_COST;
    public static final ModConfigSpec.IntValue GEAR_LINK_COST;
    public static final ModConfigSpec.IntValue ARMOR_COST_LINKED;
    public static final ModConfigSpec.IntValue ARMOR_UPKEEP_TICKS;
    public static final ModConfigSpec.DoubleValue LOOM_ROBE_REFUND;
    public static final ModConfigSpec.DoubleValue LOOM_ROBE_REFUND_MANIFESTED;
    public static final ModConfigSpec.IntValue WATER_BOOTS_FREEZE;
    public static final ModConfigSpec.IntValue WATER_BOOTS_FREEZE_MANIFESTED;
    public static final ModConfigSpec.IntValue BOOTS_SLOW_FALL_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_NIGHT_VISION_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_RESISTANCE_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_SPEED_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_FIRE_RESISTANCE_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_WATER_BREATHING_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_DOLPHINS_GRACE_TICKS;
    public static final ModConfigSpec.IntValue ARMOR_LUCK_TICKS;
    public static final ModConfigSpec.IntValue SPIRIT_HOOD_GLOW_RANGE;
    public static final ModConfigSpec.IntValue LOOM_BOOTS_STITCH_REACH;
    public static final ModConfigSpec.IntValue LOOM_BOOTS_STITCH_REACH_MANIFESTED;
    public static final ModConfigSpec.IntValue LOOM_BOOTS_STITCH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SET_MEND_HEALTH;
    public static final ModConfigSpec.IntValue SET_MEND_PULSE;
    public static final ModConfigSpec.IntValue SET_MEND_TICKS;
    public static final ModConfigSpec.IntValue SPIRIT_PICK_GLINT;
    public static final ModConfigSpec.IntValue SPIRIT_PICK_GLINT_MANIFESTED;
    public static final ModConfigSpec.IntValue SPIRIT_SHOVEL_GLINT;
    public static final ModConfigSpec.DoubleValue EARTH_HOOD_BLOCK;
    public static final ModConfigSpec.DoubleValue EARTH_HOOD_BLOCK_MANIFESTED;
    public static final ModConfigSpec.IntValue FIRE_ROBE_IGNITE_SECONDS;
    public static final ModConfigSpec.IntValue SPIRIT_ROBE_GLOW_TICKS;
    public static final ModConfigSpec.IntValue WATER_ROBE_REGEN_TICKS;
    public static final ModConfigSpec.IntValue WATER_ROBE_REGEN_TICKS_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_ECHO_DAMAGE;
    public static final ModConfigSpec.IntValue BLADE_GLOW_TICKS;
    public static final ModConfigSpec.DoubleValue BLADE_SPIRIT_ECHO;
    public static final ModConfigSpec.DoubleValue BLADE_SPIRIT_ECHO_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_EARTH_KNOCKBACK;
    public static final ModConfigSpec.IntValue BLADE_EARTH_SLOWNESS_LEVEL;
    public static final ModConfigSpec.IntValue BLADE_EARTH_SLOWNESS_LEVEL_MANIFESTED;
    public static final ModConfigSpec.IntValue BLADE_EARTH_SLOWNESS_TICKS;
    public static final ModConfigSpec.IntValue BLADE_FIRE_SECONDS;
    public static final ModConfigSpec.IntValue BLADE_FIRE_SECONDS_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_FIRE_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLADE_FIRE_DAMAGE_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_WATER_HEAL;
    public static final ModConfigSpec.DoubleValue BLADE_WATER_HEAL_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_AIR_SWEEP;
    public static final ModConfigSpec.DoubleValue BLADE_AIR_SWEEP_MANIFESTED;
    public static final ModConfigSpec.DoubleValue BLADE_LOOM_PULL;
    public static final ModConfigSpec.DoubleValue BLADE_LOOM_PULL_MANIFESTED;
    public static final ModConfigSpec.DoubleValue AXE_WATER_SAPLING_CHANCE;
    public static final ModConfigSpec.IntValue AXE_SPIRIT_GLOW_RANGE;
    public static final ModConfigSpec.IntValue AXE_SPIRIT_GLOW_TICKS;
    public static final ModConfigSpec.IntValue PICK_AIR_HASTE_TICKS;
    public static final ModConfigSpec.DoubleValue SHOVEL_WATER_CLAY_CHANCE;
    public static final ModConfigSpec.DoubleValue HOE_SPIRIT_BOUNTY_CHANCE;
    public static final ModConfigSpec.IntValue HOE_WATER_MOISTEN_RADIUS;
    public static final ModConfigSpec.IntValue HOE_WATER_MOISTEN_RADIUS_MANIFESTED;
    public static final ModConfigSpec.DoubleValue SHEARS_WATER_REGROW_CHANCE;
    public static final ModConfigSpec.IntValue SHEARS_SPIRIT_REGEN_TICKS;
    public static final ModConfigSpec.IntValue SHEARS_SPIRIT_GLOW_RANGE;
    public static final ModConfigSpec.IntValue SHEARS_SPIRIT_GLOW_RANGE_MANIFESTED;
    public static final ModConfigSpec.IntValue RATTLE_VOICE_TICKS;
    public static final ModConfigSpec.IntValue RATTLE_FIRE_TICKS;
    public static final ModConfigSpec.DoubleValue RATTLE_WATER_BONUS;
    public static final ModConfigSpec.DoubleValue BOUND_SET_DAMAGE;
    public static final ModConfigSpec.DoubleValue MANIFESTED_SET_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLADE_BOSS_BONUS;
    public static final ModConfigSpec.DoubleValue BLADE_LIFESTEAL;
    public static final ModConfigSpec.DoubleValue MANIFESTED_SPARE_CHANCE;
    public static final ModConfigSpec.DoubleValue EARTH_BOOTS_KNOCKBACK;
    public static final ModConfigSpec.DoubleValue SPIRIT_BOOTS_BOUNCE_FALL;
    public static final ModConfigSpec.DoubleValue SPIRIT_BOOTS_BOUNCE_SPEED;
    public static final ModConfigSpec.IntValue SPIRIT_HOOD_GLOW_TICKS;
    public static final ModConfigSpec.IntValue SHEARS_SPIRIT_GLOW_TICKS;
    // staff
    public static final ModConfigSpec.IntValue STAFF_EARTH_COST;
    public static final ModConfigSpec.IntValue STAFF_FIRE_COST;
    public static final ModConfigSpec.IntValue STAFF_WATER_COST;
    public static final ModConfigSpec.IntValue STAFF_AIR_COST;
    public static final ModConfigSpec.IntValue STAFF_SPIRIT_COST;
    public static final ModConfigSpec.IntValue STAFF_TETHER_COST;
    public static final ModConfigSpec.IntValue STAFF_STITCH_COST;
    public static final ModConfigSpec.DoubleValue STAFF_EARTH_DAMAGE;
    public static final ModConfigSpec.DoubleValue STAFF_FIRE_DAMAGE;
    public static final ModConfigSpec.IntValue STAFF_EARTH_SLOW_TICKS;
    public static final ModConfigSpec.IntValue STAFF_WATER_REGEN_TICKS;
    public static final ModConfigSpec.IntValue STAFF_WATER_BREATHING_TICKS;
    public static final ModConfigSpec.IntValue STAFF_AIR_SLOW_FALL_TICKS;
    public static final ModConfigSpec.IntValue STAFF_SPIRIT_SIGHT_TICKS;
    public static final ModConfigSpec.IntValue STAFF_SPIRIT_GLOW_TICKS;
    public static final ModConfigSpec.IntValue STAFF_COOLDOWN;
    public static final ModConfigSpec.IntValue STAFF_WATER_COOLDOWN;
    public static final ModConfigSpec.IntValue STAFF_STITCH_COOLDOWN;
    public static final ModConfigSpec.IntValue STAFF_FIRE_IGNITE_SECONDS;
    public static final ModConfigSpec.DoubleValue STAFF_TETHER_PULL;
    public static final ModConfigSpec.IntValue STAFF_SPIRIT_RADIUS;
    public static final ModConfigSpec.IntValue STAFF_RANGE;
    public static final ModConfigSpec.IntValue STAFF_STITCH_RANGE;
    // tools and rites
    public static final ModConfigSpec.IntValue WAYSTONE_TIER1_PULSE;
    public static final ModConfigSpec.IntValue WAYSTONE_TIER2_PULSE;
    public static final ModConfigSpec.IntValue WAYSTONE_TIER3_PULSE;
    public static final ModConfigSpec.IntValue WAYSTONE_COOLDOWN;
    public static final ModConfigSpec.IntValue WAYSTONE_TIER1_RANGE;
    public static final ModConfigSpec.IntValue MAUL_BLOCK_PULSE;
    public static final ModConfigSpec.IntValue MAUL_COOLDOWN;
    public static final ModConfigSpec.IntValue WAND_PULSE_PER_BLOCK;
    public static final ModConfigSpec.IntValue WAND_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue WRENCH_USE_PULSE;
    public static final ModConfigSpec.IntValue RITE_AMPLIFY_PULSE;
    // tribes
    public static final ModConfigSpec.IntValue STANDING_FAVOURED;
    public static final ModConfigSpec.IntValue STANDING_REAGENT;
    public static final ModConfigSpec.IntValue STANDING_FOOD;
    public static final ModConfigSpec.IntValue STANDING_PULSE_PER_10;
    public static final ModConfigSpec.IntValue HEARTH_CELL_DRAIN;
    public static final ModConfigSpec.IntValue STANDING_KILL;
    public static final ModConfigSpec.IntValue KILL_CAP_PER_DAY;
    public static final ModConfigSpec.IntValue OFFER_CAP_PER_DAY;
    public static final ModConfigSpec.IntValue KILL_RADIUS;
    public static final ModConfigSpec.IntValue STANDING_TRADE;
    public static final ModConfigSpec.IntValue STANDING_HURT_KIN;
    public static final ModConfigSpec.IntValue STANDING_CAMP_BLOCK;
    public static final ModConfigSpec.IntValue STANDING_HEARTH;
    public static final ModConfigSpec.IntValue HUNTER_ANGER_TICKS;
    public static final ModConfigSpec.IntValue KIN_DRUM_INTERVAL;
    public static final ModConfigSpec.IntValue KIN_DRUM_RADIUS;
    public static final ModConfigSpec.IntValue KIN_DRUM_PULSE;
    // familiars
    public static final ModConfigSpec.IntValue FAMILIAR_RANGE;
    public static final ModConfigSpec.IntValue FAMILIAR_ATTUNE_RANGE;
    public static final ModConfigSpec.DoubleValue FAMILIAR_DISCOUNT;
    public static final ModConfigSpec.DoubleValue FAMILIAR_GEAR_PERK;
    public static final ModConfigSpec.DoubleValue FAMILIAR_SONG_DAMAGE;
    public static final ModConfigSpec.DoubleValue FAMILIAR_SONG_REACH;
    public static final ModConfigSpec.DoubleValue FAMILIAR_RITE_BLESSING;
    public static final ModConfigSpec.IntValue FAMILIAR_BOON_TICKS;
    public static final ModConfigSpec.IntValue FOX_SIGHT_RANGE;
    public static final ModConfigSpec.IntValue FOX_ORE_RANGE;
    public static final ModConfigSpec.IntValue FOX_LIGHT_PERIOD;
    public static final ModConfigSpec.IntValue IMP_RANGE;
    public static final ModConfigSpec.IntValue IMP_REFUND;
    public static final ModConfigSpec.IntValue IMP_TEMPO_BONUS;
    public static final ModConfigSpec.IntValue BELL_RANGE;
    public static final ModConfigSpec.IntValue MOTH_CLICK_PERIOD;
    public static final ModConfigSpec.IntValue MOTH_CLICK_TICKS;
    public static final ModConfigSpec.IntValue MOTH_CLICK_TRUE_TICKS;
    public static final ModConfigSpec.IntValue WEAVER_RANGE;
    public static final ModConfigSpec.IntValue WEAVER_GATHER_RANGE;
    public static final ModConfigSpec.IntValue HOUND_TRACK_RANGE;
    public static final ModConfigSpec.DoubleValue MARK_SPECIES_CHANCE;
    public static final ModConfigSpec.DoubleValue MARK_KIN_CHANCE;
    public static final ModConfigSpec.IntValue BRUSH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BOND_CHANCE;
    public static final ModConfigSpec.DoubleValue BOND_REMNANT_CHANCE;
    // echo
    public static final ModConfigSpec.IntValue KILN_SECONDS;
    public static final ModConfigSpec.IntValue KILN_PULSE;
    public static final ModConfigSpec.IntValue GRIT_SHATTER_SECONDS;
    public static final ModConfigSpec.IntValue GRIT_SHATTER_PULSE;
    public static final ModConfigSpec.IntValue GEAR_ATTUNE_SECONDS;
    public static final ModConfigSpec.IntValue GEAR_ATTUNE_PULSE;
    public static final ModConfigSpec.IntValue GEAR_BIND_SECONDS;
    public static final ModConfigSpec.IntValue GEAR_BIND_PULSE;
    public static final ModConfigSpec.IntValue GEAR_MANIFEST_SECONDS;
    public static final ModConfigSpec.IntValue GEAR_MANIFEST_PULSE;
    public static final ModConfigSpec.IntValue MACHINE_ATTUNE_SECONDS;
    public static final ModConfigSpec.IntValue MACHINE_ATTUNE_PULSE;
    public static final ModConfigSpec.IntValue MACHINE_BIND_SECONDS;
    public static final ModConfigSpec.IntValue MACHINE_BIND_PULSE;
    public static final ModConfigSpec.IntValue MACHINE_MANIFEST_SECONDS;
    public static final ModConfigSpec.IntValue MACHINE_MANIFEST_PULSE;
    public static final ModConfigSpec.DoubleValue MACHINE_RANK1_TIME;
    public static final ModConfigSpec.DoubleValue MACHINE_RANK2_TIME;
    public static final ModConfigSpec.DoubleValue MACHINE_RANK3_TIME;
    public static final ModConfigSpec.DoubleValue MACHINE_RANK_GAIN;
    private static final java.util.Map<tk.darrow.tribalpower.song.Anointment, java.util.Map<String, ModConfigSpec.DoubleValue>> ANOINTING =
            new java.util.EnumMap<>(tk.darrow.tribalpower.song.Anointment.class);
    private static final java.util.Map<tk.darrow.tribalpower.cuisine.Fare, ModConfigSpec.IntValue> FARE_SECONDS =
            new java.util.EnumMap<>(tk.darrow.tribalpower.cuisine.Fare.class);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Tribal Power balance. Every value is server-authoritative.").push("balance");
        GENERATION_MULTIPLIER = b
                .comment("Scales the Pulse every generator produces. 1.0 is the shipped balance.")
                .defineInRange("generationMultiplier", 1.0, 0.0, 16.0);
        CONSUMPTION_MULTIPLIER = b
                .comment("Scales Pulse machines spend. 2.0 is the shipped balance: a hand Drumheart still runs Stone Font cobble, not Echo stations or the pit. Stone Font cobble is exempt.")
                .defineInRange("consumptionMultiplier", 2.0, 0.25, 16.0);
        PIT_SPEED_MULTIPLIER = b
                .comment("Scales how fast the Listening Pit and Stone Font cycle. Higher is faster; Pulse per second is unchanged, so a faster cycle is also a cheaper one.")
                .defineInRange("pitSpeedMultiplier", 1.0, 0.05, 20.0);
        GATE_TRAVEL_COST = b
                .comment("Pulse a Way Gate spends per entity that steps through.")
                .defineInRange("gateTravelCost", 20, 0, 100000);
        FAR_GATE_TRAVEL_COST = b
                .comment("Pulse a Far Gate spends per entity that steps through.")
                .defineInRange("farGateTravelCost", 120, 0, 100000);
        ENABLE_FAR_GATES = b
                .comment("When false, Far Gate keystones refuse to link across dimensions. Way Gates are unaffected.")
                .define("enableFarGates", true);
        GRIT_DENY_LIST = b
                .comment("Materials the grit scan must ignore, by material name (the part after the tag prefix), e.g. \"netherite\".",
                        "Ancient debris is already excluded: netherite should require the Nether.")
                .defineListAllowEmpty("gritDenyList", List.of(), () -> "", o -> o instanceof String s && !s.isBlank());
        b.pop();
        b.comment("World upkeep.").push("world");
        RETROGEN_MARCH = b
                .comment("When a save holds a March generated by an older Tribal Power, set that March aside once and let it",
                        "regenerate with the current terrain. The old chunks are moved to tribalpower_backups inside the save,",
                        "never deleted. Set false to keep an existing March exactly as it is (new chunks still use new terrain).")
                .define("retrogenMarch", true);
        b.pop();
        b.comment("The March's spirits: when they rise, how many, and how hard they hit. Night is meant to be the danger",
                "and day the time to work; these are the dials for that.").push("march");
        DAY_SPAWN_ONE_IN = b
                .comment("By day in the March, one spawn attempt in this many may raise a spirit in sunlight (never in torch",
                        "or lantern light). 1 lets every attempt through; 0 means spirits only rise in the dark.")
                .defineInRange("daySpawnOneIn", 12, 0, 1000);
        NIGHT_CROWD_CAP = b
                .comment("No new spirit rises in the dark once this many wild spirits are within crowdRadius. 0 disables the cap.")
                .defineInRange("nightCrowdCap", 4, 0, 256);
        DAY_CROWD_CAP = b
                .comment("The same cap for spirits rising in daylight. 0 disables the cap.")
                .defineInRange("dayCrowdCap", 1, 0, 256);
        CROWD_RADIUS = b
                .comment("Blocks around a spawn attempt that the crowd caps count within.")
                .defineInRange("crowdRadius", 32, 4, 128);
        MAX_GROUP_SIZE = b
                .comment("Most spirits that rise together in one spawn attempt, whatever pack size a spawn table asks for.",
                        "Applies to every Tribal Power monster, in any dimension.")
                .defineInRange("maxGroupSize", 2, 1, 16);
        DAWN_FADE = b
                .comment("When true, wild spirits under open sky in the March fade away by day unless they are chasing someone.",
                        "Bosses and bonded spirits never fade.")
                .define("dawnFade", true);
        DAWN_FADE_ONE_IN = b
                .comment("Each tick of daylight, a spirit that can fade does so with a chance of one in this many. 300 is",
                        "about fifteen seconds on average.")
                .defineInRange("dawnFadeOneIn", 300, 1, 72000);
        NIGHT_HEALTH_BONUS = b
                .comment("Extra max health for spirits that rise at night, as a fraction of base (0.4 = +40%).")
                .defineInRange("nightHealthBonus", 0.4, 0.0, 10.0);
        NIGHT_DAMAGE_BONUS = b
                .comment("Extra attack damage for spirits that rise at night, as a fraction of base.")
                .defineInRange("nightDamageBonus", 0.25, 0.0, 10.0);
        NIGHT_ELITE_CHANCE = b
                .comment("Chance a spirit rising at night is an elite.")
                .defineInRange("nightEliteChance", 0.08, 0.0, 1.0);
        DAY_ELITE_CHANCE = b
                .comment("Chance a spirit rising by day is an elite.")
                .defineInRange("dayEliteChance", 0.0, 0.0, 1.0);
        HARD_ELITE_BONUS = b
                .comment("Added to the night elite chance on Hard difficulty.")
                .defineInRange("hardEliteBonus", 0.04, 0.0, 1.0);
        LOCAL_DIFFICULTY_ELITE_BONUS = b
                .comment("Added to the night elite chance, scaled by local difficulty (how long the area has been played in).")
                .defineInRange("localDifficultyEliteBonus", 0.04, 0.0, 1.0);
        ELITE_HEALTH_BONUS = b
                .comment("Extra max health for elites, as a fraction of base (1.0 = double).")
                .defineInRange("eliteHealthBonus", 1.0, 0.0, 20.0);
        ELITE_DAMAGE_BONUS = b
                .comment("Extra attack damage for elites, as a fraction of base.")
                .defineInRange("eliteDamageBonus", 0.5, 0.0, 20.0);
        ELITE_ARMOR = b
                .comment("Armor points elites gain.")
                .defineInRange("eliteArmor", 4.0, 0.0, 30.0);
        ELITE_XP_MULTIPLIER = b
                .comment("Elites drop this many times the usual experience.")
                .defineInRange("eliteXpMultiplier", 3, 1, 100);
        ONLY_HUNTERS_BLOCK_SLEEP = b
                .comment("When true, only a spirit already hunting you (or a boss) keeps you from sleeping. When false, any",
                        "nearby spirit does, as with vanilla monsters.")
                .define("onlyHuntersBlockSleep", true);
        SLEEP_SKIPS_NIGHT = b
                .comment("When true, sleeping through the night in the March moves the shared clock on to morning. The March",
                        "keeps the overworld's time, so this skips the overworld's night as well.")
                .define("sleepSkipsNight", true);
        b.pop();
        b.comment("Wildlife returning to the March. Animals mostly appear when land is first generated, so land that has",
                "been hunted or emptied would otherwise stay empty. Now and then, near each player in the March, a small group",
                "from the biome's own creature list may wander back in where few animals live. Monsters are not affected.").push("wildlife");
        MARCH_REPOPULATE = b
                .comment("Whether animals slowly return to emptied land in the March. The doMobSpawning game rule and the",
                        "server's spawn-animals setting are honoured as well.")
                .define("marchRepopulate", true);
        MARCH_REPOPULATE_SECONDS = b
                .comment("Seconds between attempts. Each player in the March gets one attempt per interval, and many attempts",
                        "find nothing that fits the spot, so emptied land fills over tens of minutes rather than at once.")
                .defineInRange("marchRepopulateSeconds", 60, 5, 3600);
        MARCH_REPOPULATE_MIN_DISTANCE = b
                .comment("Closest an attempt lands to its player, in blocks horizontally. Nothing ever appears within 24 blocks",
                        "of a player, as with natural spawning.")
                .defineInRange("marchRepopulateMinDistance", 32, 24, 128);
        MARCH_REPOPULATE_MAX_DISTANCE = b
                .comment("Farthest an attempt lands from its player. Only chunks already loaded and ticking are used; none is",
                        "ever loaded or generated for this. 128 is where wandering animals would count as far away.")
                .defineInRange("marchRepopulateMaxDistance", 96, 24, 128);
        MARCH_REPOPULATE_RADIUS = b
                .comment("Blocks around an attempt in which animals are counted against marchRepopulateCap.")
                .defineInRange("marchRepopulateRadius", 32, 8, 128);
        MARCH_REPOPULATE_CAP = b
                .comment("No group arrives while this many animals (any creature, tamed or wild, from any mod) already live within",
                        "marchRepopulateRadius, and a group never takes the count past it. 6 is a little above how many a",
                        "stretch of new March land starts with, so land refills to about its first state and no further.")
                .defineInRange("marchRepopulateCap", 6, 1, 64);
        EEL_ONE_IN = b.comment("One fish in this many a rod lands in the March is a Raw Silt Eel; the rest are glimmerfin.").defineInRange("eelOneIn", 4, 1, 100);
        b.pop();
        b.comment("Spiritgear weapons. Damage and speed are the rank 0 values the tooltip shows (attack damage, attacks",
                "per second); ranks add the Blade's bonuses on top. Reach is added to the wielder's reach in blocks.").push("weapons");
        for (var kind : tk.darrow.tribalpower.item.WeaponKind.values()) {
            b.push(kind.id());
            WEAPONS.put(kind, new WeaponValues(
                    b.comment("Attack damage at rank 0.").defineInRange("damage", kind.damage, 1.0, 100.0),
                    b.comment("Attacks per second at rank 0.").defineInRange("speed", kind.speed, 0.1, 4.0),
                    b.comment("Extra reach in blocks (negative is shorter).").defineInRange("reach", kind.reach, -2.0, 4.0),
                    b.comment(kind.traitComment()).defineInRange("trait", kind.trait, 0.0, 10.0),
                    b.comment("Movement speed while it is in hand, as a fraction: negative is heavy, positive is light.")
                            .defineInRange("weight", kind.weight, -0.9, 0.9)));
            b.pop();
        }
        b.pop();
        b.comment("Anointing: a weapon worked with an empowered reagent at the Song Bench keeps that reagent's",
                "anointment until another replaces it. One at a time. The reagent's Note decides which anointment.").push("anointing");
        ANOINT_REAGENT_COST = b
                .comment("Empowered reagents one anointing spends.")
                .defineInRange("reagentCost", 4, 1, 999);
        ANOINT_PULSE_COST = b
                .comment("Pulse one anointing spends, before the bench's rank discount.")
                .defineInRange("pulseCost", 32, 0, 100000);
        for (var anointment : tk.darrow.tribalpower.song.Anointment.values()) {
            b.push(anointment.id());
            var values = new java.util.HashMap<String, ModConfigSpec.DoubleValue>();
            for (var param : anointment.params)
                values.put(param.key(), b.comment(param.comment()).defineInRange(param.key(), param.value(), param.min(), param.max()));
            ANOINTING.put(anointment, java.util.Map.copyOf(values));
            b.pop();
        }
        b.pop();
        b.comment("Shamanic healing: Spirit Sickness and Blessing, the Spirit Kettle and its remedies, the Healer's Rattle,",
                "the Healing Circle, Spirit Remnants and the Sweat Lodge.").push("healing");
        SICKNESS_CHANCE = b.comment("Chance a hit from a March elite or night spirit leaves a player with Spirit Sickness.").defineInRange("sicknessChance", 0.25, 0.0, 1.0);
        SICKNESS_FROM_ELITES = b.comment("Whether March elites inflict Spirit Sickness.").define("sicknessFromElites", true);
        SICKNESS_FROM_NIGHT = b.comment("Whether spirits that rose at night inflict Spirit Sickness.").define("sicknessFromNight", true);
        SICKNESS_SECONDS = b.comment("Seconds Spirit Sickness lasts; a fresh hit renews it.").defineInRange("sicknessSeconds", 300, 1, 72000);
        SICKNESS_MAX_LEVEL = b.comment("Highest level Spirit Sickness stacks to.").defineInRange("sicknessMaxLevel", 3, 1, 10);
        SICKNESS_HEALTH = b.comment("Max health lost per level of Spirit Sickness.").defineInRange("sicknessHealthPerLevel", 2.0, 0.0, 20.0);
        SICKNESS_SLOW = b.comment("Movement speed lost per level of Spirit Sickness, as a fraction.").defineInRange("sicknessSlowPerLevel", 0.05, 0.0, 0.5);
        BLESSING_HEALTH = b.comment("Max health Spirit Blessing adds. A blessed player cannot catch Spirit Sickness.").defineInRange("blessingHealth", 4.0, 0.0, 40.0);
        REMEDIES_CURE_SICKNESS = b.comment("Whether every remedy also cures Spirit Sickness.").define("remediesCureSickness", true);
        KETTLE_SECONDS = b.comment("Seconds the Spirit Kettle takes to brew one batch.").defineInRange("kettleSeconds", 10, 1, 600);
        KETTLE_PULSE = b.comment("Pulse one batch costs.").defineInRange("kettlePulse", 24, 0, 100000);
        KETTLE_NEEDS_HEAT = b.comment("Whether the kettle must sit over a lit campfire, fire, magma, lava or an Ember Bowl.").define("kettleNeedsHeat", true);
        TINCTURE_YIELD = b.comment("Tinctures one batch makes.").defineInRange("tinctureYield", 2, 1, 16);
        SALVE_YIELD = b.comment("Salves one batch makes.").defineInRange("salveYield", 2, 1, 16);
        INCENSE_YIELD = b.comment("Incense sticks one batch makes.").defineInRange("incenseYield", 4, 1, 16);
        TINCTURE_SECONDS = b.comment("Seconds a tincture's effect lasts.").defineInRange("tinctureSeconds", 180, 1, 72000);
        SALVE_HEAL = b.comment("Health a salve restores at once.").defineInRange("salveHeal", 6.0, 0.0, 100.0);
        SALVE_FRACTION = b.comment("A salve's effect lasts this fraction of a tincture's.").defineInRange("salveEffectFraction", 0.5, 0.0, 4.0);
        INCENSE_BEATS = b.comment("Brazier beats (two seconds each) one incense stick burns for.").defineInRange("incenseBeats", 15, 1, 1000);
        INCENSE_RADIUS = b.comment("Blocks around a brazier that burning incense reaches.").defineInRange("incenseRadius", 8, 1, 48);
        INCENSE_SECONDS = b.comment("Seconds each beat of incense grants its effect for (renewed every beat).").defineInRange("incenseSeconds", 12, 1, 600);
        INCENSE_FAMILIAR_HEAL = b.comment("Health burning incense restores to bonded familiars every beat.").defineInRange("incenseFamiliarHeal", 2.0, 0.0, 40.0);
        VOICE_WATER = b.comment("Water voice: extra effect levels on a remedy.").defineInRange("voiceWaterLevels", 1, 0, 5);
        VOICE_FIRE = b.comment("Fire voice: remedy duration multiplier.").defineInRange("voiceFireDuration", 1.5, 1.0, 10.0);
        VOICE_EARTH = b.comment("Earth voice: absorption a remedy grants.").defineInRange("voiceEarthAbsorption", 4.0, 0.0, 40.0);
        VOICE_AIR = b.comment("Air voice: health a remedy restores at once.").defineInRange("voiceAirHeal", 4.0, 0.0, 40.0);
        VOICE_SPIRIT = b.comment("Spirit voice: seconds of Regeneration a remedy adds.").defineInRange("voiceSpiritRegen", 10, 0, 600);
        VOICE_LOOM = b.comment("Loom voice: remedy duration multiplier (the effect stays at level I).").defineInRange("voiceLoomDuration", 2.0, 1.0, 10.0);
        RATTLE_HEAL = b.comment("Health the Healer's Rattle restores each shake (every half second).").defineInRange("rattleHeal", 1.5, 0.0, 40.0);
        RATTLE_RANK_BONUS = b.comment("Extra healing per rank, as a fraction of rattleHeal.").defineInRange("rattleRankBonus", 0.5, 0.0, 10.0);
        RATTLE_COST = b.comment("Pulse each shake of the rattle spends.").defineInRange("rattleCost", 2, 0, 1000);
        RATTLE_RANGE = b.comment("Blocks the rattle reaches to heal what you look at.").defineInRange("rattleRange", 6.0, 1.0, 32.0);
        CIRCLE_RADIUS = b.comment("Blocks around the brazier the Healing Circle reaches.").defineInRange("circleRadius", 12, 1, 64);
        CIRCLE_CAST_HEAL = b.comment("Health the Healing Circle restores when it is cast.").defineInRange("circleCastHeal", 20.0, 0.0, 1000.0);
        CIRCLE_BEAT_HEAL = b.comment("Health the circle restores every two seconds while it lasts.").defineInRange("circleBeatHeal", 2.0, 0.0, 100.0);
        CIRCLE_BLESSING = b.comment("Minutes of Spirit Blessing the Healing Circle grants.").defineInRange("circleBlessingMinutes", 10, 0, 600);
        CIRCLE_REVIVES = b.comment("Whether the Healing Circle revives Spirit Remnants seated on its pedestals.").define("circleRevives", true);
        REMNANTS = b.comment("Whether a bonded familiar leaves a Spirit Remnant when it dies.").define("familiarRemnants", true);
        LODGE_RANGE = b.comment("Blocks from the bed that heated Sweat Stones must be within.").defineInRange("lodgeRange", 5, 1, 32);
        LODGE_ROOF = b.comment("Whether the bed must be under a roof for the lodge to work.").define("lodgeNeedsRoof", true);
        LODGE_BLESSING = b.comment("Minutes of Spirit Blessing and Regeneration a night in the lodge grants.").defineInRange("lodgeBlessingMinutes", 10, 0, 600);
        b.pop();
        b.comment("Camp kit: the Spirit Door, Vine Lifts, the Pulse Bow and Crossbow, Soul Urns and reagent dyes.").push("kit");
        SPIRIT_DOOR_BLOCKS = b.comment("Whether a Spirit Door stops hostile monsters and spirits while letting everyone else through.").define("spiritDoorBlocksHostiles", true);
        LIFT_RANGE = b.comment("Blocks up or down a Vine Lift looks for the next lift in its column.").defineInRange("liftRange", 48, 2, 384);
        BOW_DAMAGE = b.comment("Damage of a fully drawn Pulse Bow bolt, before a verse arrow's bonus. A partial draw hits for its share of this, as a vanilla bow does (a vanilla full draw is 6, up to 10 on a critical).").defineInRange("bowDamage", 7.0, 0.5, 100.0);
        BOW_SPEED = b.comment("Speed a fully drawn Pulse Bow bolt leaves at; a weak draw leaves at three quarters of it. Bolts fly straight, without falling.").defineInRange("bowVelocity", 3.2, 0.5, 10.0);
        BOW_SPREAD = b.comment("Inaccuracy of a Pulse Bow bolt (a vanilla arrow is 1.0).").defineInRange("bowSpread", 0.3, 0.0, 10.0);
        BOW_PULSE = b.comment("Pulse a full Pulse Bow draw spends; a partial draw spends its share, at least 1.").defineInRange("bowPulse", 6, 0, 1000);
        VERSE_PULSE = b.comment("Pulse a verse arrow adds to a Pulse Bow draw or a Pulse Crossbow load.").defineInRange("versePulse", 6, 0, 1000);
        VERSE_DAMAGE = b.comment("Damage a verse arrow adds to its bolt for each time its lead reagent is repeated (its power), scaled by the draw.").defineInRange("verseDamagePerPower", 1.0, 0.0, 100.0);
        VERSE_DAMAGE_CAP = b.comment("Most damage a verse arrow can add to a full-draw bolt, however strong its verse.").defineInRange("verseDamageCap", 3.0, 0.0, 100.0);
        BOLT_LIFETIME = b.comment("Ticks a sonic bolt flies before it fades: its range, since it does not fall (20 ticks is about 58 blocks from a full bow draw).").defineInRange("boltLifetimeTicks", 20, 2, 200);
        CROSSBOW_LOAD = b.comment("Ticks the Pulse Crossbow takes to load (a Pulse Bow reaches full draw in 20).").defineInRange("crossbowLoadTicks", 25, 1, 200);
        CROSSBOW_DAMAGE = b.comment("Damage of a crossbow bolt against a fully drawn Pulse Bow bolt, verse bonus included (a vanilla crossbow arrow is 7 to 11).").defineInRange("crossbowDamageScale", 1.3, 0.1, 10.0);
        CROSSBOW_SPEED = b.comment("Speed a crossbow bolt leaves at (a full bow draw is 3.2).").defineInRange("crossbowVelocity", 3.6, 0.5, 10.0);
        CROSSBOW_SPREAD = b.comment("Inaccuracy of a crossbow bolt (a vanilla arrow is 1.0).").defineInRange("crossbowSpread", 0.05, 0.0, 10.0);
        CROSSBOW_PULSE = b.comment("Pulse loading the crossbow spends; a verse arrow adds versePulse.").defineInRange("crossbowPulse", 10, 0, 1000);
        URN_WOVEN = b.comment("Uses a Woven Soul Urn has. Capturing and releasing are one use each.").defineInRange("wovenUrnUses", 2, 2, 10000);
        URN_COPPER = b.comment("Uses a Copper Soul Urn has.").defineInRange("copperUrnUses", 10, 2, 10000);
        URN_MANIFESTED = b.comment("Uses a Manifested Soul Urn has. A Resonant Soul Urn never wears out.").defineInRange("manifestedUrnUses", 20, 2, 10000);
        DYE_YIELD = b.comment("Dye one reagent ground with chalk in a bowl makes.").defineInRange("dyeYield", 2, 1, 64);
        b.pop();
        b.comment("Songs: what a sung verse costs and strikes for.").push("songs");
        SONG_CAST_PULSE_BASE = b.comment("Pulse singing a sheet or a songbook page costs, before the per-reagent part and the Spindle boon.").defineInRange("songCastPulseBase", 8, 0, 1000);
        SONG_CAST_PULSE_PER_REAGENT = b.comment("Pulse each reagent of the verse adds to that cost.").defineInRange("songCastPulsePerReagent", 4, 0, 1000);
        SONG_BOLT_DAMAGE = b.comment("Damage a bolt-shaped song deals, plus songBoltDamagePerPower for each copy of its lead reagent.").defineInRange("songBoltDamage", 3.0, 0.0, 100.0);
        SONG_BOLT_DAMAGE_PER_POWER = b.comment("Extra bolt damage per copy of the lead reagent (its power).").defineInRange("songBoltDamagePerPower", 1.0, 0.0, 100.0);
        SONG_CALL_DAMAGE = b.comment("Damage a call-shaped song deals to every hostile in its ring, plus songCallDamagePerPower per copy of its lead reagent.").defineInRange("songCallDamage", 2.0, 0.0, 100.0);
        SONG_CALL_DAMAGE_PER_POWER = b.comment("Extra call damage per copy of the lead reagent.").defineInRange("songCallDamagePerPower", 0.5, 0.0, 100.0);
        SONGBOOK_COOLDOWN = b.comment("Ticks a songbook rests after a verse.").defineInRange("songbookCooldownTicks", 30, 0, 72000);
        b.pop();
        b.comment("The spirit layer: the six voice blessings, the nine tribe boons and the March's afflictions.").push("effects");
        EARTH_KNOCKBACK = b.comment("Earth blessing: knockback resistance per level.").defineInRange("earthBlessingKnockback", 0.3, 0.0, 1.0);
        EARTH_MINING = b.comment("Earth blessing: extra mining speed per level, as a fraction.").defineInRange("earthBlessingMining", 0.25, 0.0, 5.0);
        FIRE_BURN = b.comment("Fire blessing: seconds a blow sets the target alight, per level. Level II also grants fire resistance.").defineInRange("fireBlessingBurn", 3.0, 0.0, 60.0);
        WATER_SWIM = b.comment("Water blessing: extra swim speed per level, as a fraction.").defineInRange("waterBlessingSwim", 0.4, 0.0, 5.0);
        WATER_REGEN = b.comment("Water blessing: health mended each second while wet.").defineInRange("waterBlessingRegen", 1.0, 0.0, 20.0);
        AIR_FALL = b.comment("Air blessing: blocks of fall taken off every fall, per level.").defineInRange("airBlessingFall", 3.0, 0.0, 50.0);
        AIR_JUMP = b.comment("Air blessing: extra jump strength per level.").defineInRange("airBlessingJump", 0.1, 0.0, 1.0);
        SPIRIT_SIGHT = b.comment("Spirit blessing: blocks around you within which hostiles show through walls.").defineInRange("spiritBlessingSight", 16, 1, 64);
        LOOM_REFUND = b.comment("Loom blessing: fraction of the Pulse gear spends that comes back.").defineInRange("loomBlessingRefund", 0.1, 0.0, 1.0);
        BOONS_STANDING = b.comment("Whether Kin standing with a tribe carries its boon for as long as the standing lasts.").define("boonsFromStanding", true);
        SOIL_SAT = b.comment("Soil boon: chance every two seconds to top a hungry belly up by one.").defineInRange("soilBoonSaturation", 0.35, 0.0, 1.0);
        STONE_CHANCE = b.comment("Stone boon: chance a broken ore drops one more.").defineInRange("stoneBoonChance", 0.2, 0.0, 1.0);
        SPROUT_REACH = b.comment("Sprout boon: blocks around you that crops and saplings hurry within.").defineInRange("sproutBoonReach", 4, 1, 16);
        SPROUT_TICKS = b.comment("Sprout boon: growth ticks handed out every two seconds.").defineInRange("sproutBoonTicks", 4, 0, 64);
        SPARK_SPEED = b.comment("Spark boon: extra attack speed, as a fraction.").defineInRange("sparkBoonAttackSpeed", 0.1, 0.0, 2.0);
        CLOCK_REACH = b.comment("Clock boon: blocks around you within which Echo stations work at double speed.").defineInRange("clockBoonReach", 8, 1, 32);
        SIGIL_DISCOUNT = b.comment("Sigil boon: fraction taken off the Pulse charms ask.").defineInRange("sigilBoonDiscount", 0.5, 0.0, 1.0);
        SPINDLE_DISCOUNT = b.comment("Spindle boon: fraction taken off the Pulse a song costs.").defineInRange("spindleBoonDiscount", 0.25, 0.0, 1.0);
        HUSH_SECONDS = b.comment("Seconds an Unsung Silence bolt hushes you for: no song and no staff voice until it lifts.").defineInRange("hushSeconds", 12, 1, 600);
        FRAYED_HEALTH = b.comment("Frayed: max health lost per level.").defineInRange("frayedHealth", 4.0, 0.0, 20.0);
        FRAYED_SECONDS = b.comment("Seconds a Loom-torn creature's blow leaves you Frayed.").defineInRange("frayedSeconds", 120, 1, 72000);
        LEY_DRAIN = b.comment("Ley Sickness: Pulse drained from carried cells every two seconds, per level.").defineInRange("leySicknessDrain", 2, 0, 1000);
        WELL_PULSE = b.comment("Pulse a wash in a Spirit Well asks to lift every affliction.").defineInRange("spiritWellPulse", 40, 0, 100000);
        LANTERN_STRETCH = b.comment("A worn Lantern charm keeps blessings and boons this many times longer.").defineInRange("lanternCharmStretch", 1.5, 1.0, 5.0);
        SONG_BLESSING_SECONDS = b.comment("Seconds a Ward song grants its voice's blessing for, per note of power.").defineInRange("songBlessingSeconds", 45, 0, 3600);
        RITE_BLESSING_MINUTES = b.comment("Minutes a seal rite at a pedestal grants its voice's blessing to everyone nearby.").defineInRange("riteBlessingMinutes", 5, 0, 600);
        b.pop();
        b.comment("Tribal cuisine: the Hearth Pot, tribe dishes and voice feasts.").push("cuisine");
        DISH_BOON = b.comment("Minutes a tribe dish carries its tribe's boon for.").defineInRange("dishBoonMinutes", 5, 0, 600);
        HEARTH_SCALE = b.comment("Scales how long every Hearth Pot meal takes; 1.0 is the recipe's own time.").defineInRange("hearthCookScale", 1.0, 0.05, 20.0);
        HEARTH_HEAT = b.comment("Whether the Hearth Pot must sit over a lit campfire, fire, magma, lava or an Ember Bowl.").define("hearthNeedsHeat", true);
        FEAST_NUTRITION = b.comment("Hunger one serving of a feast restores.").defineInRange("feastNutrition", 6, 0, 20);
        FEAST_SATURATION = b.comment("Saturation modifier of one serving of a feast.").defineInRange("feastSaturation", 0.8, 0.0, 2.0);
        FEAST_BLESSING = b.comment("Minutes of the voice's blessing one serving of its feast grants.").defineInRange("feastBlessingMinutes", 8, 0, 600);
        DISH_STANDING = b.comment("Standing a tribe grants when offered its own dish at its hearth.").defineInRange("dishStanding", 6, 0, 100);
        b.comment("Camp fare: the Hearth Pot meals that carry no tribe boon. Seconds each one's side effect (always level I) lasts; 0 turns it off.").push("fare");
        for (var fare : tk.darrow.tribalpower.cuisine.Fare.values())
            if (fare.effect != null)
                FARE_SECONDS.put(fare, b.defineInRange(fare.id() + "Seconds", fare.defaultSeconds, 0, 3600));
        b.pop();
        b.pop();
        b.comment("Camp and workshop hands: what the automated devices ask for.").push("camp");
        AUTOMATION_VOICES = b.comment("Whether every automated device needs a kept Resonance Totem of its own voice within 8 blocks: Earth for the Grove Tender and Wayanchor, Spirit for the Ward Drum, Hush Totem and Summoning Cradle, Water for the Tide Pump, Air for the Wind Snare and relays, Loom for the Seal Loom and Astral relays.").define("automationNeedsVoices", true);
        GROVE_WATER = b.comment("Pulse a Grove Tender spends on a beat that re-wets its bed's farmland, when a Water totem keeps within 8 blocks of it.").defineInRange("groveWaterCost", 4, 0, 200);
        TIDE_PUMP_COST = b.comment("Pulse a Tide Pump spends on a beat that moves fluid.").defineInRange("tidePumpCost", 4, 0, 1000);
        WIND_SNARE_COST = b.comment("Pulse a Wind Snare spends per item stack it catches.").defineInRange("windSnareCost", 2, 0, 1000);
        WARD_DRUM_COST = b.comment("Pulse a Ward Drum spends on a strike.").defineInRange("wardDrumCost", 8, 0, 1000);
        WARD_DRUM_DAMAGE = b.comment("Magic damage a Ward Drum strike deals to a hostile within 8 blocks.").defineInRange("wardDrumDamage", 4.0, 0.0, 1000.0);
        SEAL_LOOM_COST = b.comment("Pulse a Seal Loom spends on a craft.").defineInRange("sealLoomCost", 6, 0, 1000);
        RELAY_COST = b.comment("Pulse a relay plate spends on a transfer, before rank and the consumption multiplier.").defineInRange("relayCost", 4, 0, 1000);
        LONGREACH_RELAY_COST = b.comment("Pulse a Longreach relay plate spends on a transfer, before rank and the consumption multiplier.").defineInRange("longreachRelayCost", 8, 0, 1000);
        ASTRAL_RELAY_COST = b.comment("Pulse an Astral relay plate spends on a transfer, before rank and the consumption multiplier.").defineInRange("astralRelayCost", 16, 0, 1000);
        BRAZIER_COST = b.comment("Pulse a Ritual Brazier draws every two seconds to sustain its blessing.").defineInRange("brazierBlessingCost", 8, 0, 1000);
        b.pop();
        b.comment("Song plates synced with the Totem Wrench: one plate hears another with no wire between them.").push("logic");
        PLATE_LINK_RANGE = b.comment("Blocks a synced song plate can hear another across, in the same world. A link beyond this reads 0.").defineInRange("plateLinkRange", 32, 1, 256);
        PLATE_LINK_MAX = b.comment("Plates one song plate can be synced to hear.").defineInRange("plateLinkMax", 8, 1, 64);
        b.pop();
        b.comment("Tribes that talk: requests and questlines.").push("quests");
        REQUEST_SCALE = b.comment("Scales the standing a finished tribe request pays.").defineInRange("requestStandingScale", 1.0, 0.0, 10.0);
        REQUESTS_PER_DAY = b.comment("Requests a player may finish in one day, across every tribe.").defineInRange("requestsPerDay", 3, 1, 100);
        REQUESTS_PER_MARK = b.comment("Every this many finished requests for one tribe pays a Tribe Mark.").defineInRange("requestsPerMark", 3, 1, 100);
        ELDER_DIALOGUE = b.comment("Whether an Elder opens a conversation. When false the Elder trades at once, as before.").define("elderDialogue", true);
        b.pop();
        b.comment("The guardians of the March and the last rite.").push("guardians");
        GUARDIANS_ENABLED = b.comment("Whether Guardian Altars answer at all. Off, the guardians never rise and the stories stop at their trial.").define("guardiansEnabled", true);
        GUARDIAN_HEALTH_SCALE = b.comment("Scales every guardian's health.").defineInRange("guardianHealthScale", 1.0, 0.1, 10.0);
        GUARDIAN_DAMAGE_SCALE = b.comment("Scales every guardian's damage.").defineInRange("guardianDamageScale", 1.0, 0.1, 10.0);
        GUARDIAN_CALL_COST = b.comment("Reagents laid on a Guardian Altar to call its guardian.").defineInRange("guardianCallCost", 8, 1, 64);
        GUARDIAN_COOLDOWN_MINUTES = b.comment("Game minutes an altar rests between calls.").defineInRange("guardianCooldownMinutes", 20, 0, 1440);
        SILENT_DRUM_COOLDOWN_MINUTES = b.comment("Game minutes a Silent Drum rests after waking the Unsung.").defineInRange("silentDrumCooldownMinutes", 20, 0, 1440);
        GUARDIAN_ABILITY_INTERVAL = b.comment("Ticks between a guardian's signature attacks (shorter when roused).").defineInRange("guardianAbilityInterval", 80, 20, 600);
        GUARDIAN_ADDS_INTERVAL = b.comment("Ticks between waves of creatures a roused guardian calls.").defineInRange("guardianAddsInterval", 300, 40, 2400);
        GUARDIAN_ADDS_PER_WAVE = b.comment("Creatures a guardian calls per wave.").defineInRange("guardianAddsPerWave", 2, 0, 10);
        GUARDIAN_MAX_ADDS = b.comment("Most creatures a guardian keeps at its side.").defineInRange("guardianMaxAdds", 6, 0, 30);
        GUARDIAN_RESET_SECONDS = b.comment("Seconds without a player near before a guardian goes back to sleep.").defineInRange("guardianResetSeconds", 30, 5, 600);
        NINTH_AGREEMENT_COST = b.comment("Pulse the Ninth Agreement rite draws.").defineInRange("ninthAgreementCost", 2000, 0, 100000);
        NINTH_AGREEMENT_BOON_MINUTES = b.comment("Minutes all nine boons last after the Ninth Agreement.").defineInRange("ninthAgreementBoonMinutes", 30, 1, 1440);
        b.pop();
        b.comment("The living March: weather, surges, festivals and wandering spirits.").push("events");
        WEATHER_ENABLED = b.comment("Whether the March has its own weather (ashfall, glimmer storms, whiteouts, fen mist).").define("weatherEnabled", true);
        WEATHER_CHANCE_PER_HOUR = b.comment("Chance per game hour (1000 ticks) that each idle weather begins; 0.06 is about one onset per kind every game day.").defineInRange("weatherChancePerHour", 0.06, 0.0, 20.0);
        WEATHER_MINUTES_MIN = b.comment("Shortest a March weather lasts, in game minutes.").defineInRange("weatherMinutesMin", 8, 1, 600);
        WEATHER_MINUTES_MAX = b.comment("Longest a March weather lasts, in game minutes.").defineInRange("weatherMinutesMax", 20, 1, 1200);
        WEATHER_GENERATOR_BONUS = b.comment("What a weather's favoured generators make, as a multiplier.").defineInRange("weatherGeneratorBonus", 1.5, 0.0, 10.0);
        WEATHER_GENERATOR_PENALTY = b.comment("What a weather's hampered generators make, as a multiplier.").defineInRange("weatherGeneratorPenalty", 0.5, 0.0, 1.0);
        WEATHER_SPAWN_SHIFT = b.comment("Whether spirits rise by day under thick weather as they would at night.").define("weatherSpawnShift", true);
        SURGES_ENABLED = b.comment("Whether ley surges happen.").define("surgesEnabled", true);
        SURGE_EVERY_MINUTES = b.comment("Game minutes between the end of one surge and the start of the next.").defineInRange("surgeEveryMinutes", 90, 1, 10000);
        SURGE_MINUTES = b.comment("How long a surge lasts, in game minutes.").defineInRange("surgeMinutes", 15, 1, 600);
        SURGE_YIELD = b.comment("What a collector on a surging thread yields, as a multiplier.").defineInRange("surgeYieldMultiplier", 2.0, 1.0, 10.0);
        SURGE_SICKNESS_SECONDS = b.comment("Seconds of Ley Sickness for standing on a bare crossing in a surge.").defineInRange("surgeSicknessSeconds", 30, 1, 600);
        FESTIVALS_ENABLED = b.comment("Whether the tribes keep festival days.").define("festivalsEnabled", true);
        FESTIVAL_CYCLE_DAYS = b.comment("Days in the festival cycle; each tribe's festival falls once in it.").defineInRange("festivalCycleDays", 27, 9, 900);
        FESTIVAL_STANDING = b.comment("Standing the Elder's festival gift pays.").defineInRange("festivalStanding", 40, 0, 1000);
        FESTIVAL_RITE_STANDING = b.comment("Standing a rite in a festival camp pays on top of the usual.").defineInRange("festivalRiteStanding", 30, 0, 1000);
        FESTIVAL_FEAST_STANDING = b.comment("Standing for eating a feast by a festival hearth.").defineInRange("festivalFeastStanding", 20, 0, 1000);
        EVENT_WARNING_SECONDS = b.comment("Seconds of warning before a March weather or a ley surge sets in, so players can reach shelter (0 for none).").defineInRange("eventWarningSeconds", 120, 0, 900);
        WANDERING_SPIRITS_ENABLED = b.comment("Whether wandering spirits appear.").define("wanderingSpiritsEnabled", true);
        WANDERING_SPIRIT_ONE_IN = b.comment("A spirit appears near a March player one time in this many five-second checks (360 is about once a game day).").defineInRange("wanderingSpiritOneIn", 360, 1, 100000);
        WANDERING_SPIRIT_LIFE = b.comment("Seconds a wandering spirit lingers before it fades.").defineInRange("wanderingSpiritLifeSeconds", 120, 5, 3600);
        b.pop();
        b.comment("Spirit Charms: what worn charms ask in Pulse and what their voices give.").push("charms");
        CHARM_UPKEEP_PER_VOICE = b.comment("Pulse a worn charm asks each upkeep beat, per voice bound to it (a charm with no voice still asks for one).").defineInRange("charmUpkeepPerVoice", 2, 0, 1000);
        CHARM_UPKEEP_BEAT = b.comment("Ticks between charm upkeep payments (40 is two seconds).").defineInRange("charmUpkeepBeatTicks", 40, 1, 72000);
        CHARM_LOOM_REFUND_CHANCE = b.comment("Chance, each upkeep beat, that a worn Loom voice threads some of the upkeep back into a carried cell.").defineInRange("charmLoomRefundChance", 0.30, 0.0, 1.0);
        CHARM_LOOM_REFUND_CAP = b.comment("Most Pulse one Loom refund gives back.").defineInRange("charmLoomRefundCap", 2, 0, 1000);
        GATHER_REACH = b.comment("Blocks a Gathering Charm with one voice pulls drops and experience from.").defineInRange("gatherReach", 5, 0, 64);
        GATHER_REACH_PER_VOICE = b.comment("Blocks each extra voice bound to a Gathering Charm adds to its reach.").defineInRange("gatherReachPerVoice", 2, 0, 64);
        GATHER_REACH_CAP = b.comment("Farthest a Gathering Charm ever reaches, however many voices it carries.").defineInRange("gatherReachCap", 11, 0, 64);
        HEARTH_CHARM_FOOD = b.comment("Hunger a worn Hearth Charm restores every four seconds.").defineInRange("hearthCharmNutrition", 1, 0, 20);
        HEARTH_CHARM_SATURATION = b.comment("Saturation modifier of what the Hearth Charm restores.").defineInRange("hearthCharmSaturation", 0.4, 0.0, 2.0);
        CHARM_FIRE_TICKS = b.comment("Ticks of Fire Resistance a worn Fire voice grants, renewed every four seconds.").defineInRange("charmFireResistanceTicks", 120, 0, 72000);
        CHARM_WATER_BREATHING_TICKS = b.comment("Ticks of Water Breathing a worn Water voice grants, renewed every four seconds.").defineInRange("charmWaterBreathingTicks", 220, 0, 72000);
        CHARM_DOLPHINS_GRACE_TICKS = b.comment("Ticks of Dolphin's Grace a worn Water voice grants, renewed every four seconds.").defineInRange("charmDolphinsGraceTicks", 100, 0, 72000);
        CHARM_EARTH_TICKS = b.comment("Ticks of Resistance a worn Earth voice grants, renewed every four seconds.").defineInRange("charmResistanceTicks", 100, 0, 72000);
        CHARM_SPIRIT_TICKS = b.comment("Ticks of Night Vision a worn Spirit voice grants, renewed every four seconds.").defineInRange("charmNightVisionTicks", 300, 0, 72000);
        CHARM_LOOM_TICKS = b.comment("Ticks of Luck a worn Loom voice grants, renewed every four seconds.").defineInRange("charmLuckTicks", 120, 0, 72000);
        CHARM_AIR_TICKS = b.comment("Ticks of Slow Falling a worn Air voice grants (not when Fire is worn too), renewed every four seconds.").defineInRange("charmSlowFallingTicks", 80, 0, 72000);
        WARD_BLOCK_CHANCE = b.comment("Chance a worn Ward Charm with Earth turns a projectile aside.").defineInRange("wardBlockChance", 0.25, 0.0, 1.0);
        WARD_BLOCK_CHANCE_SPIRIT = b.comment("The same chance when Spirit is worn as well.").defineInRange("wardBlockChanceSpirit", 0.50, 0.0, 1.0);
        CHARM_EMBER_SECONDS = b.comment("Seconds a worn Fire voice sets an attacker alight for (a Fire familiar near doubles it).").defineInRange("charmEmberSeconds", 3, 0, 600);
        CHARM_LINK_COST = b.comment("Pulse binding one more voice onto a Spirit Charm at a Resonance Totem spends.").defineInRange("charmLinkCost", 40, 0, 100000);
        CHARM_CHORUS_COST = b.comment("Pulse imprinting a Chorus Charm with the voices of three or more totems spends.").defineInRange("charmChorusCost", 200, 0, 100000);
        b.pop();
        b.comment("Spiritgear and Spiritweave: what the tools, weapons and armour pay in Pulse and what their voices give.").push("spiritgear");
        GEAR_MINE_COST = b.comment("Pulse a Spiritgear tool spends on each block it breaks, before the rank discount.").defineInRange("gearMineCost", 2, 0, 1000);
        GEAR_HIT_COST = b.comment("Pulse a Spiritgear blow spends, before the rank discount.").defineInRange("gearHitCost", 3, 0, 1000);
        GEAR_USE_COST = b.comment("Pulse a Spiritgear use (stripping, pathing, tilling, shearing) spends, before the rank discount.").defineInRange("gearUseCost", 1, 0, 1000);
        GEAR_LINK_COST = b.comment("Pulse linking a piece to a totem's voice spends.").defineInRange("gearLinkCost", 40, 0, 100000);
        ARMOR_COST_LINKED = b.comment("Pulse one upkeep payment of a bound Spiritweave piece spends. An unlinked piece has no boon and spends nothing.").defineInRange("armorCostLinked", 3, 0, 1000);
        ARMOR_UPKEEP_TICKS = b.comment("Ticks one upkeep payment keeps a Spiritweave piece's perks going (80 is four seconds, the gap between its timed boons).").defineInRange("armorUpkeepTicks", 80, 1, 72000);
        LOOM_ROBE_REFUND = b.comment("Chance a Loom robe threads an upkeep payment back into its cell.").defineInRange("loomRobeRefundChance", 0.30, 0.0, 1.0);
        LOOM_ROBE_REFUND_MANIFESTED = b.comment("The same chance for a Manifested Loom robe.").defineInRange("loomRobeRefundChanceManifested", 0.50, 0.0, 1.0);
        WATER_BOOTS_FREEZE = b.comment("Blocks around Water boots that still water freezes within.").defineInRange("waterBootsFreezeRadius", 2, 0, 16);
        WATER_BOOTS_FREEZE_MANIFESTED = b.comment("The same radius for Manifested Water boots.").defineInRange("waterBootsFreezeRadiusManifested", 3, 0, 16);
        BOOTS_SLOW_FALL_TICKS = b.comment("Ticks of Slow Falling unlinked or Air boots catch a fall with.").defineInRange("bootsSlowFallTicks", 80, 0, 72000);
        ARMOR_NIGHT_VISION_TICKS = b.comment("Ticks of Night Vision an unlinked, Air or Spirit hood grants, renewed every armorUpkeepTicks.").defineInRange("armorNightVisionTicks", 300, 0, 72000);
        ARMOR_RESISTANCE_TICKS = b.comment("Ticks of Resistance an unlinked, Earth or Spirit robe grants, renewed every armorUpkeepTicks.").defineInRange("armorResistanceTicks", 100, 0, 72000);
        ARMOR_SPEED_TICKS = b.comment("Ticks of Speed unlinked, Fire (in heat), Air or Spirit leggings grant, renewed every armorUpkeepTicks.").defineInRange("armorSpeedTicks", 100, 0, 72000);
        ARMOR_FIRE_RESISTANCE_TICKS = b.comment("Ticks of Fire Resistance a Fire hood or robe grants, renewed every armorUpkeepTicks.").defineInRange("armorFireResistanceTicks", 120, 0, 72000);
        ARMOR_WATER_BREATHING_TICKS = b.comment("Ticks of Water Breathing a Water hood grants, renewed every armorUpkeepTicks.").defineInRange("armorWaterBreathingTicks", 220, 0, 72000);
        ARMOR_DOLPHINS_GRACE_TICKS = b.comment("Ticks of Dolphin's Grace Water leggings grant, renewed every armorUpkeepTicks.").defineInRange("armorDolphinsGraceTicks", 100, 0, 72000);
        ARMOR_LUCK_TICKS = b.comment("Ticks of Luck a Loom hood grants, renewed every armorUpkeepTicks.").defineInRange("armorLuckTicks", 120, 0, 72000);
        SPIRIT_HOOD_GLOW_RANGE = b.comment("Blocks around a Spirit hood within which hostiles glow through walls.").defineInRange("spiritHoodGlowRange", 12, 0, 64);
        LOOM_BOOTS_STITCH_REACH = b.comment("Blocks Loom boots stitch forward when their wearer sneaks ahead.").defineInRange("loomBootsStitchReach", 4, 2, 32);
        LOOM_BOOTS_STITCH_REACH_MANIFESTED = b.comment("The same reach for Manifested Loom boots.").defineInRange("loomBootsStitchReachManifested", 6, 2, 32);
        LOOM_BOOTS_STITCH_COOLDOWN = b.comment("Ticks Loom boots rest between stitches.").defineInRange("loomBootsStitchCooldownTicks", 160, 0, 72000);
        SET_MEND_HEALTH = b.comment("Health a whole Manifested set mends its wearer every setMendTicks.").defineInRange("setMendHealth", 2.0, 0.0, 100.0);
        SET_MEND_PULSE = b.comment("Pulse the robe pays for each mend.").defineInRange("setMendPulse", 4, 0, 1000);
        SET_MEND_TICKS = b.comment("Ticks between mends.").defineInRange("setMendTicks", 80, 1, 72000);
        SPIRIT_PICK_GLINT = b.comment("Blocks around a held Spirit pickaxe within which ores glint for its holder.").defineInRange("spiritPickGlintRange", 6, 0, 32);
        SPIRIT_PICK_GLINT_MANIFESTED = b.comment("The same range for a Manifested Spirit pickaxe.").defineInRange("spiritPickGlintRangeManifested", 10, 0, 32);
        SPIRIT_SHOVEL_GLINT = b.comment("Blocks around a held Spirit shovel within which buried chests and spawners glint.").defineInRange("spiritShovelGlintRange", 8, 0, 32);
        EARTH_HOOD_BLOCK = b.comment("Chance an Earth hood turns a projectile aside.").defineInRange("earthHoodBlockChance", 0.20, 0.0, 1.0);
        EARTH_HOOD_BLOCK_MANIFESTED = b.comment("The same chance for a Manifested Earth hood.").defineInRange("earthHoodBlockChanceManifested", 0.35, 0.0, 1.0);
        FIRE_ROBE_IGNITE_SECONDS = b.comment("Seconds a Fire robe sets whoever strikes its wearer alight for.").defineInRange("fireRobeIgniteSeconds", 3, 0, 600);
        SPIRIT_ROBE_GLOW_TICKS = b.comment("Ticks whoever strikes a Spirit robe's wearer glows for.").defineInRange("spiritRobeGlowTicks", 80, 0, 72000);
        WATER_ROBE_REGEN_TICKS = b.comment("Ticks of Regeneration a Water robe grants its wearer when struck.").defineInRange("waterRobeRegenTicks", 100, 0, 72000);
        WATER_ROBE_REGEN_TICKS_MANIFESTED = b.comment("The same for a Manifested Water robe.").defineInRange("waterRobeRegenTicksManifested", 160, 0, 72000);
        BLADE_ECHO_DAMAGE = b.comment("Echo damage every fuelled Spiritgear blow adds.").defineInRange("bladeEchoDamage", 2.0, 0.0, 100.0);
        BLADE_GLOW_TICKS = b.comment("Ticks a struck target glows.").defineInRange("bladeGlowTicks", 80, 0, 72000);
        BLADE_SPIRIT_ECHO = b.comment("Extra echo damage a Spirit blade adds.").defineInRange("bladeSpiritEcho", 2.0, 0.0, 100.0);
        BLADE_SPIRIT_ECHO_MANIFESTED = b.comment("The same for a Manifested Spirit blade.").defineInRange("bladeSpiritEchoManifested", 4.0, 0.0, 100.0);
        BLADE_EARTH_KNOCKBACK = b.comment("Knockback an Earth blade adds to every blow.").defineInRange("bladeEarthKnockback", 1.2, 0.0, 10.0);
        BLADE_EARTH_SLOWNESS_LEVEL = b.comment("Slowness level an Earth blade applies.").defineInRange("bladeEarthSlownessLevel", 1, 1, 10);
        BLADE_EARTH_SLOWNESS_LEVEL_MANIFESTED = b.comment("The same for a Manifested Earth blade.").defineInRange("bladeEarthSlownessLevelManifested", 2, 1, 10);
        BLADE_EARTH_SLOWNESS_TICKS = b.comment("Ticks the Slowness lasts.").defineInRange("bladeEarthSlownessTicks", 60, 0, 72000);
        BLADE_FIRE_SECONDS = b.comment("Seconds a Fire blade sets the target alight for.").defineInRange("bladeFireSeconds", 4, 0, 600);
        BLADE_FIRE_SECONDS_MANIFESTED = b.comment("The same for a Manifested Fire blade.").defineInRange("bladeFireSecondsManifested", 6, 0, 600);
        BLADE_FIRE_DAMAGE = b.comment("Fire damage a Fire blade adds to every blow.").defineInRange("bladeFireDamage", 3.0, 0.0, 100.0);
        BLADE_FIRE_DAMAGE_MANIFESTED = b.comment("The same for a Manifested Fire blade.").defineInRange("bladeFireDamageManifested", 5.0, 0.0, 100.0);
        BLADE_WATER_HEAL = b.comment("Health a Water blade heals its wielder on every blow.").defineInRange("bladeWaterHeal", 2.0, 0.0, 100.0);
        BLADE_WATER_HEAL_MANIFESTED = b.comment("The same for a Manifested Water blade.").defineInRange("bladeWaterHealManifested", 4.0, 0.0, 100.0);
        BLADE_AIR_SWEEP = b.comment("Damage an Air blade sweeps into hostiles beside the target.").defineInRange("bladeAirSweep", 2.0, 0.0, 100.0);
        BLADE_AIR_SWEEP_MANIFESTED = b.comment("The same for a Manifested Air blade.").defineInRange("bladeAirSweepManifested", 4.0, 0.0, 100.0);
        BLADE_LOOM_PULL = b.comment("Blocks a Loom blade pulls the target toward its wielder.").defineInRange("bladeLoomPull", 4.0, 0.0, 32.0);
        BLADE_LOOM_PULL_MANIFESTED = b.comment("The same for a Manifested Loom blade.").defineInRange("bladeLoomPullManifested", 6.0, 0.0, 32.0);
        AXE_WATER_SAPLING_CHANCE = b.comment("Chance a log cut by a Water axe drops a sapling (doubled when Manifested).").defineInRange("axeWaterSaplingChance", 0.15, 0.0, 1.0);
        AXE_SPIRIT_GLOW_RANGE = b.comment("Blocks around a Spirit axe within which hostiles glow when it cuts a log.").defineInRange("axeSpiritGlowRange", 8, 0, 64);
        AXE_SPIRIT_GLOW_TICKS = b.comment("Ticks that glow lasts.").defineInRange("axeSpiritGlowTicks", 80, 0, 72000);
        PICK_AIR_HASTE_TICKS = b.comment("Ticks of Haste an Air pickaxe renews while held (Haste II when Manifested).").defineInRange("pickAirHasteTicks", 60, 0, 72000);
        SHOVEL_WATER_CLAY_CHANCE = b.comment("Chance a dirt block dug by a Water shovel turns up clay (doubled when Manifested).").defineInRange("shovelWaterClayChance", 0.1, 0.0, 1.0);
        HOE_SPIRIT_BOUNTY_CHANCE = b.comment("Chance a Spirit hoe's reap doubles the harvest (doubled when Manifested).").defineInRange("hoeSpiritBountyChance", 0.25, 0.0, 1.0);
        HOE_WATER_MOISTEN_RADIUS = b.comment("Blocks around a held Water hoe within which farmland stays wet.").defineInRange("hoeWaterMoistenRadius", 3, 0, 16);
        HOE_WATER_MOISTEN_RADIUS_MANIFESTED = b.comment("The same for a Manifested Water hoe.").defineInRange("hoeWaterMoistenRadiusManifested", 5, 0, 16);
        SHEARS_WATER_REGROW_CHANCE = b.comment("Chance a sheep shorn by Water shears keeps its fleece (always when Manifested).").defineInRange("shearsWaterRegrowChance", 0.5, 0.0, 1.0);
        SHEARS_SPIRIT_REGEN_TICKS = b.comment("Ticks of Regeneration an animal shorn by Spirit shears gets.").defineInRange("shearsSpiritRegenTicks", 100, 0, 72000);
        SHEARS_SPIRIT_GLOW_RANGE = b.comment("Blocks around held Spirit shears within which shearable animals glow.").defineInRange("shearsSpiritGlowRange", 10, 0, 64);
        SHEARS_SPIRIT_GLOW_RANGE_MANIFESTED = b.comment("The same for Manifested Spirit shears.").defineInRange("shearsSpiritGlowRangeManifested", 16, 0, 64);
        RATTLE_VOICE_TICKS = b.comment("Ticks of the Earth (Resistance), Air (Speed) and Loom (Regeneration) rattle's boon on each shake.").defineInRange("rattleVoiceTicks", 40, 0, 72000);
        RATTLE_FIRE_TICKS = b.comment("Ticks of Fire Resistance a Fire rattle gives on each shake.").defineInRange("rattleFireTicks", 60, 0, 72000);
        RATTLE_WATER_BONUS = b.comment("What a Water rattle's healing is multiplied by.").defineInRange("rattleWaterBonus", 1.5, 0.0, 10.0);
        BOUND_SET_DAMAGE = b.comment("What damage taken is multiplied by for a wearer of a whole Spiritweave set of Bound or better.").defineInRange("boundSetDamageTaken", 0.9, 0.0, 1.0);
        MANIFESTED_SET_DAMAGE = b.comment("The same for a whole Manifested set.").defineInRange("manifestedSetDamageTaken", 0.8, 0.0, 1.0);
        BLADE_BOSS_BONUS = b.comment("What a Manifested blade's blows against bosses are multiplied by.").defineInRange("bladeBossBonusManifested", 1.25, 1.0, 10.0);
        BLADE_LIFESTEAL = b.comment("Fraction of every blow a Manifested blade lands that comes back to its wielder as health.").defineInRange("bladeLifestealManifested", 0.1, 0.0, 1.0);
        MANIFESTED_SPARE_CHANCE = b.comment("Chance a Manifested tool's starved action wears it nothing at all.").defineInRange("manifestedSpareChance", 0.5, 0.0, 1.0);
        EARTH_BOOTS_KNOCKBACK = b.comment("Fraction of a blow's knockback Earth boots let through.").defineInRange("earthBootsKnockbackKept", 0.4, 0.0, 1.0);
        SPIRIT_BOOTS_BOUNCE_FALL = b.comment("Blocks a Manifested Spirit boots' wearer must fall before the landing bounces them back up.").defineInRange("spiritBootsBounceFall", 4.0, 0.0, 64.0);
        SPIRIT_BOOTS_BOUNCE_SPEED = b.comment("Upward speed of that bounce.").defineInRange("spiritBootsBounceSpeed", 0.55, 0.0, 4.0);
        SPIRIT_HOOD_GLOW_TICKS = b.comment("Ticks hostiles a Spirit hood lights up glow for, renewed every armorUpkeepTicks.").defineInRange("spiritHoodGlowTicks", 100, 0, 72000);
        SHEARS_SPIRIT_GLOW_TICKS = b.comment("Ticks shearable animals near held Spirit shears glow for, renewed every four seconds.").defineInRange("shearsSpiritGlowTicks", 100, 0, 72000);
        b.pop();
        b.comment("The Sixfold Staff: what each voice costs and does.").push("staff");
        STAFF_EARTH_COST = b.comment("Pulse the Earth voice (a bolt that slows) spends.").defineInRange("staffEarthCost", 12, 0, 1000);
        STAFF_FIRE_COST = b.comment("Pulse the Fire voice (a bolt that burns) spends.").defineInRange("staffFireCost", 18, 0, 1000);
        STAFF_WATER_COST = b.comment("Pulse the Water voice (cleansing and breath) spends.").defineInRange("staffWaterCost", 24, 0, 1000);
        STAFF_AIR_COST = b.comment("Pulse the Air voice (a leap) spends.").defineInRange("staffAirCost", 16, 0, 1000);
        STAFF_SPIRIT_COST = b.comment("Pulse the Spirit voice (sight) spends.").defineInRange("staffSpiritCost", 20, 0, 1000);
        STAFF_TETHER_COST = b.comment("Pulse the Loom voice's Tether spends.").defineInRange("staffTetherCost", 6, 0, 1000);
        STAFF_STITCH_COST = b.comment("Pulse the Loom voice's Stitch (a blink forward) spends.").defineInRange("staffStitchCost", 10, 0, 1000);
        STAFF_EARTH_DAMAGE = b.comment("Damage the Earth bolt deals.").defineInRange("staffEarthDamage", 4.0, 0.0, 100.0);
        STAFF_FIRE_DAMAGE = b.comment("Damage the Fire bolt deals.").defineInRange("staffFireDamage", 8.0, 0.0, 100.0);
        STAFF_EARTH_SLOW_TICKS = b.comment("Ticks of Slowness IV the Earth bolt leaves.").defineInRange("staffEarthSlowTicks", 100, 0, 72000);
        STAFF_WATER_REGEN_TICKS = b.comment("Ticks of Regeneration the Water voice grants.").defineInRange("staffWaterRegenTicks", 100, 0, 72000);
        STAFF_WATER_BREATHING_TICKS = b.comment("Ticks of Water Breathing the Water voice grants.").defineInRange("staffWaterBreathingTicks", 600, 0, 72000);
        STAFF_AIR_SLOW_FALL_TICKS = b.comment("Ticks of Slow Falling the Air leap grants.").defineInRange("staffAirSlowFallTicks", 120, 0, 72000);
        STAFF_SPIRIT_SIGHT_TICKS = b.comment("Ticks of Night Vision the Spirit voice grants.").defineInRange("staffSpiritSightTicks", 600, 0, 72000);
        STAFF_SPIRIT_GLOW_TICKS = b.comment("Ticks nearby hostiles glow after the Spirit voice.").defineInRange("staffSpiritGlowTicks", 240, 0, 72000);
        STAFF_COOLDOWN = b.comment("Ticks the staff rests after any voice but Water.").defineInRange("staffCooldownTicks", 40, 0, 72000);
        STAFF_WATER_COOLDOWN = b.comment("Ticks the staff rests after the Water voice.").defineInRange("staffWaterCooldownTicks", 160, 0, 72000);
        STAFF_STITCH_COOLDOWN = b.comment("Ticks the staff rests after a Stitch.").defineInRange("staffStitchCooldownTicks", 30, 0, 72000);
        STAFF_FIRE_IGNITE_SECONDS = b.comment("Seconds the Fire bolt sets its target alight for.").defineInRange("staffFireIgniteSeconds", 4, 0, 600);
        STAFF_TETHER_PULL = b.comment("Farthest, in blocks, a Tether draws its target along the thread toward the caster.").defineInRange("staffTetherPull", 8.0, 0.0, 64.0);
        STAFF_SPIRIT_RADIUS = b.comment("Blocks around the caster within which hostiles glow after the Spirit voice.").defineInRange("staffSpiritRadius", 12, 0, 64);
        STAFF_RANGE = b.comment("Blocks along the look within which the Earth, Fire and Loom voices find their target.").defineInRange("staffRange", 18, 1, 64);
        STAFF_STITCH_RANGE = b.comment("Farthest, in blocks, a Stitch blinks forward; it falls back to shorter blinks when the air is not clear.").defineInRange("staffStitchRange", 6, 2, 32);
        b.pop();
        b.comment("Travel and digging tools.").push("tools");
        WAYSTONE_TIER1_PULSE = b.comment("Pulse the Waystone Compass (bound within waystoneTier1Range blocks, same world) spends on a jump.").defineInRange("waystoneTier1Pulse", 20, 0, 100000);
        WAYSTONE_TIER2_PULSE = b.comment("Pulse the second compass (any distance, same world) spends on a jump.").defineInRange("waystoneTier2Pulse", 40, 0, 100000);
        WAYSTONE_TIER3_PULSE = b.comment("Pulse the third compass (across worlds) spends on a jump.").defineInRange("waystoneTier3Pulse", 100, 0, 100000);
        WAYSTONE_COOLDOWN = b.comment("Ticks a compass rests after a jump.").defineInRange("waystoneCooldownTicks", 100, 0, 72000);
        WAYSTONE_TIER1_RANGE = b.comment("Blocks the Waystone Compass can jump from its bound spot.").defineInRange("waystoneTier1Range", 128, 1, 100000);
        MAUL_BLOCK_PULSE = b.comment("Pulse the Resonance Maul spends on each block of its 3x3 swing.").defineInRange("maulBlockPulse", 8, 0, 1000);
        MAUL_COOLDOWN = b.comment("Ticks the Resonance Maul rests after a swing that broke something.").defineInRange("maulCooldownTicks", 20, 0, 72000);
        WAND_PULSE_PER_BLOCK = b.comment("Pulse the Weaver's Wand spends on each block it lays.").defineInRange("wandPulsePerBlock", 2, 0, 1000);
        WAND_MAX_BLOCKS = b.comment("Most blocks one sweep of the Weaver's Wand lays.").defineInRange("wandMaxBlocks", 32, 1, 512);
        WRENCH_USE_PULSE = b.comment("Pulse the Totem Wrench spends on a turn in place of a point of wear; with no Pulse to hand it wears instead. 0 makes every turn free.").defineInRange("wrenchUsePulse", 1, 0, 1000);
        b.pop();
        b.comment("Seal rites at the Rite Pedestal.").push("rites");
        RITE_AMPLIFY_PULSE = b.comment("Pulse a seal rite draws from carried cells to amplify itself; without it the rite still works, unamplified.").defineInRange("riteAmplifyPulse", 8, 0, 100000);
        b.pop();
        b.comment("Standing with the tribes: what each deed is worth.").push("tribes");
        STANDING_FAVOURED = b.comment("Standing a hearth offering of a tribe's favoured item pays.").defineInRange("standingFavoured", 3, 0, 1000);
        STANDING_REAGENT = b.comment("Standing a hearth offering of a tribe's reagent pays.").defineInRange("standingReagent", 8, 0, 1000);
        STANDING_FOOD = b.comment("Standing a hearth offering of any food pays.").defineInRange("standingFood", 1, 0, 1000);
        STANDING_PULSE_PER_10 = b.comment("Standing every 10 Pulse poured into a hearth from a cell pays.").defineInRange("standingPulsePer10", 2, 0, 1000);
        HEARTH_CELL_DRAIN = b.comment("Most Pulse a hearth draws from a cell in one offering.").defineInRange("hearthCellDrain", 40, 10, 100000);
        STANDING_KILL = b.comment("Standing a hostile kill near a tribe's hearth pays.").defineInRange("standingKill", 1, 0, 1000);
        KILL_CAP_PER_DAY = b.comment("Most kills near one tribe's hearth that pay standing in a Minecraft day.").defineInRange("killCapPerDay", 20, 0, 10000);
        OFFER_CAP_PER_DAY = b.comment("Most standing one tribe's hearth grants for offerings in a Minecraft day: the Listening Pit makes ore renewable, and this keeps it from feeding the standing that unlocked it.").defineInRange("offerCapPerDay", 60, 0, 100000);
        KILL_RADIUS = b.comment("Blocks from a hearth within which a kill counts for its tribe.").defineInRange("killRadius", 24, 1, 128);
        STANDING_TRADE = b.comment("Standing a trade with a tribe's Kin pays.").defineInRange("standingTrade", 2, 0, 1000);
        STANDING_HURT_KIN = b.comment("Standing lost for striking a tribe's Kin (negative).").defineInRange("standingHurtKin", -25, -100000, 0);
        STANDING_CAMP_BLOCK = b.comment("Standing lost for breaking a tribe's banner (negative).").defineInRange("standingCampBlock", -5, -100000, 0);
        STANDING_HEARTH = b.comment("Standing lost for breaking a tribe's hearth (negative).").defineInRange("standingHearth", -40, -100000, 0);
        HUNTER_ANGER_TICKS = b.comment("Ticks a tribe's Hunters stay angry at whoever struck their Kin or broke their hearth.").defineInRange("hunterAngerTicks", 1200, 0, 72000);
        KIN_DRUM_INTERVAL = b.comment("Ticks between a Drummer's beats (less up to a second of drift).").defineInRange("kinDrumIntervalTicks", 120, 20, 12000);
        KIN_DRUM_RADIUS = b.comment("Blocks from a Drummer within which its beat feeds Drumhearts, Ley Collectors and Pulse Resonators.").defineInRange("kinDrumRadius", 8, 1, 32);
        KIN_DRUM_PULSE = b.comment("Pulse a Drummer's beat puts into each generator it reaches.").defineInRange("kinDrumPulse", 2, 0, 1000);
        b.pop();
        b.comment("Familiars: how far their boosts reach and what their species gifts give.").push("familiars");
        FAMILIAR_RANGE = b.comment("Blocks from its keeper within which an attuned familiar lends its voice's boost.").defineInRange("familiarBoostRange", 16, 1, 64);
        FAMILIAR_ATTUNE_RANGE = b.comment("Blocks from a Resonance Totem within which a familiar can be attuned to it.").defineInRange("familiarAttuneRange", 6, 1, 32);
        FAMILIAR_DISCOUNT = b.comment("Share of the Pulse an attuned familiar takes off its voice's charms and Spiritgear.").defineInRange("familiarDiscount", 0.4, 0.0, 1.0);
        FAMILIAR_GEAR_PERK = b.comment("Spiritgear voice perks of the familiar's voice fire this many times as often.").defineInRange("familiarGearPerk", 1.5, 1.0, 10.0);
        FAMILIAR_SONG_DAMAGE = b.comment("Songs of the familiar's voice strike this many times as hard.").defineInRange("familiarSongDamage", 1.3, 1.0, 10.0);
        FAMILIAR_SONG_REACH = b.comment("Blocks a call of the familiar's voice reaches further.").defineInRange("familiarSongReach", 1.5, 0.0, 32.0);
        FAMILIAR_RITE_BLESSING = b.comment("Rite blessings of the familiar's voice last this many times as long.").defineInRange("familiarRiteBlessing", 1.5, 1.0, 10.0);
        FAMILIAR_BOON_TICKS = b.comment("Ticks a charm's familiar-boosted extra effect lasts; longer than the four-second refresh, so it lingers after you part.").defineInRange("familiarBoonTicks", 200, 0, 72000);
        FOX_SIGHT_RANGE = b.comment("Blocks from a Lantern Fox within which its keeper is given Night Vision.").defineInRange("foxSightRange", 8, 1, 64);
        FOX_ORE_RANGE = b.comment("Blocks around a Lantern Fox within which ores glint for its keeper.").defineInRange("foxOreRange", 6, 1, 16);
        FOX_LIGHT_PERIOD = b.comment("Ticks between a Lantern Fox moving its travelling light.").defineInRange("foxLightPeriodTicks", 10, 1, 1200);
        IMP_RANGE = b.comment("Blocks from a Drumheart within which a Cinder Imp refunds its keeper's hand beats.").defineInRange("impRange", 8, 1, 64);
        IMP_REFUND = b.comment("Pulse a Cinder Imp refunds per hand beat.").defineInRange("impRefund", 4, 0, 1000);
        IMP_TEMPO_BONUS = b.comment("Extra Pulse an Imp with the Tempo mark refunds.").defineInRange("impTempoBonus", 2, 0, 1000);
        BELL_RANGE = b.comment("Blocks from a Mourning Bell within which it cleanses its keeper.").defineInRange("bellRange", 8, 1, 64);
        MOTH_CLICK_PERIOD = b.comment("Ticks between a sitting Storm Moth's redstone clicks.").defineInRange("mothClickPeriodTicks", 20, 1, 1200);
        MOTH_CLICK_TICKS = b.comment("Ticks a Storm Moth's click holds.").defineInRange("mothClickTicks", 2, 1, 1200);
        MOTH_CLICK_TRUE_TICKS = b.comment("Ticks the click holds for a moth with the Click-true mark.").defineInRange("mothClickTrueTicks", 4, 1, 1200);
        WEAVER_RANGE = b.comment("Blocks around an Echo Weaver within which it gathers drops.").defineInRange("weaverRange", 4, 1, 64);
        WEAVER_GATHER_RANGE = b.comment("The same reach for a weaver with the Gather mark.").defineInRange("weaverGatherRange", 8, 1, 64);
        HOUND_TRACK_RANGE = b.comment("Blocks from a Rift Hound within which the foe that last hurt its keeper glints.").defineInRange("houndTrackRange", 48, 1, 128);
        MARK_SPECIES_CHANCE = b.comment("When a bred child's Mark mutates, chance the new Mark is its species' own rather than a common one.").defineInRange("markSpeciesChance", 0.35, 0.0, 1.0);
        MARK_KIN_CHANCE = b.comment("When both parents share a keeper, chance a mutated Mark becomes the Kin Mark instead.").defineInRange("markKinChance", 0.2, 0.0, 1.0);
        BRUSH_COOLDOWN = b.comment("Ticks a creature rests after a brush before it sheds reagent again.").defineInRange("brushCooldownTicks", 1200, 0, 72000);
        BOND_CHANCE = b.comment("Chance a Bonding Charm bonds a gentle animal (the charm is kept on failure).").defineInRange("bondChance", 0.6, 0.0, 1.0);
        BOND_REMNANT_CHANCE = b.comment("Chance a Bonding Charm bonds a remnant at Voice standing (the charm is spent on failure).").defineInRange("bondRemnantChance", 0.4, 0.0, 1.0);
        b.pop();
        b.comment("Echo stations.").push("echo");
        KILN_SECONDS = b.comment("Seconds the Ember Kiln takes on a furnace recipe no written kiln recipe covers.").defineInRange("kilnSeconds", 10, 1, 600);
        KILN_PULSE = b.comment("Pulse that kiln job costs, before the consumption multiplier.").defineInRange("kilnPulse", 32, 0, 100000);
        GRIT_SHATTER_SECONDS = b.comment("Seconds the Echo Shatter takes on a raw metal or ore the tag scan found, with no written recipe for it.").defineInRange("gritShatterSeconds", 4, 1, 600);
        GRIT_SHATTER_PULSE = b.comment("Pulse a second that shattering costs, before the consumption multiplier.").defineInRange("gritShatterPulse", 20, 0, 100000);
        GEAR_ATTUNE_SECONDS = b.comment("Seconds Echo Attune takes to rank a Spiritgear piece.").defineInRange("gearAttuneSeconds", 45, 1, 3600);
        GEAR_ATTUNE_PULSE = b.comment("Pulse a second that attuning costs, before the consumption multiplier.").defineInRange("gearAttunePulse", 48, 0, 100000);
        GEAR_BIND_SECONDS = b.comment("Seconds Echo Bind takes to rank a Spiritgear piece.").defineInRange("gearBindSeconds", 90, 1, 3600);
        GEAR_BIND_PULSE = b.comment("Pulse a second that binding costs, before the consumption multiplier.").defineInRange("gearBindPulse", 64, 0, 100000);
        GEAR_MANIFEST_SECONDS = b.comment("Seconds Echo Manifest takes to rank a Spiritgear piece.").defineInRange("gearManifestSeconds", 180, 1, 3600);
        GEAR_MANIFEST_PULSE = b.comment("Pulse a second that manifesting costs, before the consumption multiplier.").defineInRange("gearManifestPulse", 96, 0, 100000);
        MACHINE_ATTUNE_SECONDS = b.comment("Seconds Echo Attune takes to rank a workshop machine.").defineInRange("machineAttuneSeconds", 16, 1, 3600);
        MACHINE_ATTUNE_PULSE = b.comment("Pulse a second that costs, before the consumption multiplier.").defineInRange("machineAttunePulse", 48, 0, 100000);
        MACHINE_BIND_SECONDS = b.comment("Seconds Echo Bind takes to rank a workshop machine.").defineInRange("machineBindSeconds", 20, 1, 3600);
        MACHINE_BIND_PULSE = b.comment("Pulse a second that costs, before the consumption multiplier.").defineInRange("machineBindPulse", 64, 0, 100000);
        MACHINE_MANIFEST_SECONDS = b.comment("Seconds Echo Manifest takes to rank a workshop machine.").defineInRange("machineManifestSeconds", 24, 1, 3600);
        MACHINE_MANIFEST_PULSE = b.comment("Pulse a second that costs, before the consumption multiplier.").defineInRange("machineManifestPulse", 80, 0, 100000);
        MACHINE_RANK1_TIME = b.comment("What an Attuned machine's job time is multiplied by; its Pulse draw and cargo scale up by the inverse.").defineInRange("machineRank1Time", 0.85, 0.05, 1.0);
        MACHINE_RANK2_TIME = b.comment("The same for a Bound machine.").defineInRange("machineRank2Time", 0.70, 0.05, 1.0);
        MACHINE_RANK3_TIME = b.comment("The same for a Manifested machine.").defineInRange("machineRank3Time", 0.55, 0.05, 1.0);
        MACHINE_RANK_GAIN = b.comment("Extra Pulse a ranked generator makes per rank, as a fraction of its beat.").defineInRange("machineRankGainBonus", 0.15, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private TribalConfig() {}

    public static double generationMultiplier() { return SPEC.isLoaded() ? GENERATION_MULTIPLIER.get() : 1.0; }

    public static double consumptionMultiplier() { return SPEC.isLoaded() ? CONSUMPTION_MULTIPLIER.get() : 2.0; }

    public static double pitSpeedMultiplier() { return SPEC.isLoaded() ? PIT_SPEED_MULTIPLIER.get() : 1.0; }

    public static int gateTravelCost() { return SPEC.isLoaded() ? GATE_TRAVEL_COST.get() : 20; }

    public static int farGateTravelCost() { return SPEC.isLoaded() ? FAR_GATE_TRAVEL_COST.get() : 120; }

    public static boolean farGatesEnabled() { return !SPEC.isLoaded() || ENABLE_FAR_GATES.get(); }

    public static boolean retrogenMarch() { return !SPEC.isLoaded() || RETROGEN_MARCH.get(); }

    /** Reads a value, or its shipped default before the config loads. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> value) { return SPEC.isLoaded() ? value.get() : value.getDefault(); }

    public static int daySpawnOneIn() { return get(DAY_SPAWN_ONE_IN); }
    public static int nightCrowdCap() { return get(NIGHT_CROWD_CAP); }
    public static int dayCrowdCap() { return get(DAY_CROWD_CAP); }
    public static int crowdRadius() { return get(CROWD_RADIUS); }
    public static int maxGroupSize() { return get(MAX_GROUP_SIZE); }
    public static boolean dawnFade() { return get(DAWN_FADE); }
    public static int dawnFadeOneIn() { return get(DAWN_FADE_ONE_IN); }
    public static double nightHealthBonus() { return get(NIGHT_HEALTH_BONUS); }
    public static double nightDamageBonus() { return get(NIGHT_DAMAGE_BONUS); }
    public static double nightEliteChance() { return get(NIGHT_ELITE_CHANCE); }
    public static double dayEliteChance() { return get(DAY_ELITE_CHANCE); }
    public static double hardEliteBonus() { return get(HARD_ELITE_BONUS); }
    public static double localDifficultyEliteBonus() { return get(LOCAL_DIFFICULTY_ELITE_BONUS); }
    public static double eliteHealthBonus() { return get(ELITE_HEALTH_BONUS); }
    public static double eliteDamageBonus() { return get(ELITE_DAMAGE_BONUS); }
    public static double eliteArmor() { return get(ELITE_ARMOR); }
    public static int eliteXpMultiplier() { return get(ELITE_XP_MULTIPLIER); }
    public static boolean onlyHuntersBlockSleep() { return get(ONLY_HUNTERS_BLOCK_SLEEP); }
    public static boolean sleepSkipsNight() { return get(SLEEP_SKIPS_NIGHT); }
    public static boolean marchRepopulate() { return get(MARCH_REPOPULATE); }
    public static int marchRepopulateSeconds() { return get(MARCH_REPOPULATE_SECONDS); }
    public static int marchRepopulateMinDistance() { return get(MARCH_REPOPULATE_MIN_DISTANCE); }
    public static int marchRepopulateMaxDistance() { return get(MARCH_REPOPULATE_MAX_DISTANCE); }
    public static int marchRepopulateRadius() { return get(MARCH_REPOPULATE_RADIUS); }
    public static int marchRepopulateCap() { return get(MARCH_REPOPULATE_CAP); }
    public static int eelOneIn() { return get(EEL_ONE_IN); }

    public static double weaponDamage(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).damage()); }
    public static double weaponSpeed(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).speed()); }
    public static double weaponReach(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).reach()); }
    public static double weaponTrait(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).trait()); }
    public static double weaponWeight(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).weight()); }
    public static int anointReagentCost() { return get(ANOINT_REAGENT_COST); }
    public static double requestStandingScale() { return get(REQUEST_SCALE); }
    public static int requestsPerMark() { return get(REQUESTS_PER_MARK); }
    public static int requestsPerDay() { return get(REQUESTS_PER_DAY); }
    public static boolean elderDialogue() { return get(ELDER_DIALOGUE); }
    public static boolean guardiansEnabled() { return get(GUARDIANS_ENABLED); }
    public static double guardianHealthScale() { return get(GUARDIAN_HEALTH_SCALE); }
    public static double guardianDamageScale() { return get(GUARDIAN_DAMAGE_SCALE); }
    public static int guardianCallCost() { return get(GUARDIAN_CALL_COST); }
    public static int guardianCooldownMinutes() { return get(GUARDIAN_COOLDOWN_MINUTES); }
    public static int silentDrumCooldownMinutes() { return get(SILENT_DRUM_COOLDOWN_MINUTES); }
    public static int guardianAbilityInterval() { return get(GUARDIAN_ABILITY_INTERVAL); }
    public static int guardianAddsInterval() { return get(GUARDIAN_ADDS_INTERVAL); }
    public static int guardianAddsPerWave() { return get(GUARDIAN_ADDS_PER_WAVE); }
    public static int guardianMaxAdds() { return get(GUARDIAN_MAX_ADDS); }
    public static int guardianResetSeconds() { return get(GUARDIAN_RESET_SECONDS); }
    public static int ninthAgreementCost() { return get(NINTH_AGREEMENT_COST); }
    public static int ninthAgreementBoonMinutes() { return get(NINTH_AGREEMENT_BOON_MINUTES); }
    public static boolean weatherEnabled() { return get(WEATHER_ENABLED); }
    public static double weatherChancePerHour() { return get(WEATHER_CHANCE_PER_HOUR); }
    public static int weatherMinutesMin() { return get(WEATHER_MINUTES_MIN); }
    public static int weatherMinutesMax() { return get(WEATHER_MINUTES_MAX); }
    public static double weatherGeneratorBonus() { return get(WEATHER_GENERATOR_BONUS); }
    public static double weatherGeneratorPenalty() { return get(WEATHER_GENERATOR_PENALTY); }
    public static boolean weatherSpawnShift() { return get(WEATHER_SPAWN_SHIFT); }
    public static boolean surgesEnabled() { return get(SURGES_ENABLED); }
    public static int surgeEveryMinutes() { return get(SURGE_EVERY_MINUTES); }
    public static int surgeMinutes() { return get(SURGE_MINUTES); }
    public static double surgeYieldMultiplier() { return get(SURGE_YIELD); }
    public static int surgeSicknessSeconds() { return get(SURGE_SICKNESS_SECONDS); }
    public static boolean festivalsEnabled() { return get(FESTIVALS_ENABLED); }
    public static int festivalCycleDays() { return get(FESTIVAL_CYCLE_DAYS); }
    public static int festivalStanding() { return get(FESTIVAL_STANDING); }
    public static int festivalRiteStanding() { return get(FESTIVAL_RITE_STANDING); }
    public static int festivalFeastStanding() { return get(FESTIVAL_FEAST_STANDING); }
    public static boolean wanderingSpiritsEnabled() { return get(WANDERING_SPIRITS_ENABLED); }
    public static int wanderingSpiritOneIn() { return get(WANDERING_SPIRIT_ONE_IN); }
    public static int wanderingSpiritLifeSeconds() { return get(WANDERING_SPIRIT_LIFE); }
    public static int eventWarningSeconds() { return get(EVENT_WARNING_SECONDS); }
    public static int dishBoonMinutes() { return get(DISH_BOON); }
    /** Seconds a camp fare's side effect lasts; 0 for a fare that has none. */
    public static int fareEffectSeconds(tk.darrow.tribalpower.cuisine.Fare fare) {
        var value = FARE_SECONDS.get(fare);
        return value == null ? 0 : get(value);
    }
    public static double hearthCookScale() { return get(HEARTH_SCALE); }
    public static boolean hearthNeedsHeat() { return get(HEARTH_HEAT); }
    public static int feastNutrition() { return get(FEAST_NUTRITION); }
    public static double feastSaturation() { return get(FEAST_SATURATION); }
    public static int feastBlessingMinutes() { return get(FEAST_BLESSING); }
    public static int dishStanding() { return get(DISH_STANDING); }
    public static int groveWaterCost() { return get(GROVE_WATER); }
    public static boolean automationNeedsVoices() { return get(AUTOMATION_VOICES); }
    public static int tidePumpCost() { return get(TIDE_PUMP_COST); }
    public static int windSnareCost() { return get(WIND_SNARE_COST); }
    public static int wardDrumCost() { return get(WARD_DRUM_COST); }
    public static float wardDrumDamage() { return get(WARD_DRUM_DAMAGE).floatValue(); }
    public static int sealLoomCost() { return get(SEAL_LOOM_COST); }
    /** A relay plate's cost by tier (1 plain, 2 Longreach, 3 Astral), before rank and the consumption multiplier. */
    public static int relayCost(int tier) { return get(tier >= 3 ? ASTRAL_RELAY_COST : tier == 2 ? LONGREACH_RELAY_COST : RELAY_COST); }
    public static int brazierBlessingCost() { return get(BRAZIER_COST); }
    public static double earthBlessingKnockback() { return get(EARTH_KNOCKBACK); }
    public static double earthBlessingMining() { return get(EARTH_MINING); }
    public static double fireBlessingBurn() { return get(FIRE_BURN); }
    public static double waterBlessingSwim() { return get(WATER_SWIM); }
    public static double waterBlessingRegen() { return get(WATER_REGEN); }
    public static double airBlessingFall() { return get(AIR_FALL); }
    public static double airBlessingJump() { return get(AIR_JUMP); }
    public static int spiritBlessingSight() { return get(SPIRIT_SIGHT); }
    public static double loomBlessingRefund() { return get(LOOM_REFUND); }
    public static boolean boonsFromStanding() { return get(BOONS_STANDING); }
    public static double soilBoonSaturation() { return get(SOIL_SAT); }
    public static double stoneBoonChance() { return get(STONE_CHANCE); }
    public static int sproutBoonReach() { return get(SPROUT_REACH); }
    public static int sproutBoonTicks() { return get(SPROUT_TICKS); }
    public static double sparkBoonAttackSpeed() { return get(SPARK_SPEED); }
    public static int clockBoonReach() { return get(CLOCK_REACH); }
    public static double sigilBoonDiscount() { return get(SIGIL_DISCOUNT); }
    public static double spindleBoonDiscount() { return get(SPINDLE_DISCOUNT); }
    public static int hushSeconds() { return get(HUSH_SECONDS); }
    public static double frayedHealth() { return get(FRAYED_HEALTH); }
    public static int frayedSeconds() { return get(FRAYED_SECONDS); }
    public static int leySicknessDrain() { return get(LEY_DRAIN); }
    public static int spiritWellPulse() { return get(WELL_PULSE); }
    public static double lanternCharmStretch() { return get(LANTERN_STRETCH); }
    public static int songBlessingSeconds() { return get(SONG_BLESSING_SECONDS); }
    public static int riteBlessingMinutes() { return get(RITE_BLESSING_MINUTES); }
    public static boolean spiritDoorBlocksHostiles() { return get(SPIRIT_DOOR_BLOCKS); }
    public static int liftRange() { return get(LIFT_RANGE); }
    public static int crossbowLoadTicks() { return get(CROSSBOW_LOAD); }
    public static double crossbowDamageMultiplier() { return get(CROSSBOW_DAMAGE); }
    public static double crossbowVelocity() { return get(CROSSBOW_SPEED); }
    public static int crossbowPulse() { return get(CROSSBOW_PULSE); }
    public static double crossbowSpread() { return get(CROSSBOW_SPREAD); }
    public static double bowDamage() { return get(BOW_DAMAGE); }
    public static double bowVelocity() { return get(BOW_SPEED); }
    public static double bowSpread() { return get(BOW_SPREAD); }
    public static int bowPulse() { return get(BOW_PULSE); }
    public static int versePulse() { return get(VERSE_PULSE); }
    public static double verseDamagePerPower() { return get(VERSE_DAMAGE); }
    public static double verseDamageCap() { return get(VERSE_DAMAGE_CAP); }
    public static int boltLifetimeTicks() { return get(BOLT_LIFETIME); }
    public static int wovenUrnUses() { return get(URN_WOVEN); }
    public static int copperUrnUses() { return get(URN_COPPER); }
    public static int manifestedUrnUses() { return get(URN_MANIFESTED); }
    public static int dyeYield() { return get(DYE_YIELD); }
    // songs
    public static int songCastPulseBase() { return get(SONG_CAST_PULSE_BASE); }
    public static int songCastPulsePerReagent() { return get(SONG_CAST_PULSE_PER_REAGENT); }
    public static double songBoltDamage() { return get(SONG_BOLT_DAMAGE); }
    public static double songBoltDamagePerPower() { return get(SONG_BOLT_DAMAGE_PER_POWER); }
    public static double songCallDamage() { return get(SONG_CALL_DAMAGE); }
    public static double songCallDamagePerPower() { return get(SONG_CALL_DAMAGE_PER_POWER); }
    public static int songbookCooldownTicks() { return get(SONGBOOK_COOLDOWN); }
    public static double sicknessChance() { return get(SICKNESS_CHANCE); }
    public static boolean sicknessFromElites() { return get(SICKNESS_FROM_ELITES); }
    public static boolean sicknessFromNight() { return get(SICKNESS_FROM_NIGHT); }
    public static int sicknessSeconds() { return get(SICKNESS_SECONDS); }
    public static int sicknessMaxLevel() { return get(SICKNESS_MAX_LEVEL); }
    public static double sicknessHealthPerLevel() { return get(SICKNESS_HEALTH); }
    public static double sicknessSlowPerLevel() { return get(SICKNESS_SLOW); }
    public static double blessingHealth() { return get(BLESSING_HEALTH); }
    public static boolean remediesCureSickness() { return get(REMEDIES_CURE_SICKNESS); }
    public static int kettleSeconds() { return get(KETTLE_SECONDS); }
    public static int kettlePulse() { return get(KETTLE_PULSE); }
    public static boolean kettleNeedsHeat() { return get(KETTLE_NEEDS_HEAT); }
    public static int tinctureYield() { return get(TINCTURE_YIELD); }
    public static int salveYield() { return get(SALVE_YIELD); }
    public static int incenseYield() { return get(INCENSE_YIELD); }
    public static int tinctureSeconds() { return get(TINCTURE_SECONDS); }
    public static double salveHeal() { return get(SALVE_HEAL); }
    public static double salveEffectFraction() { return get(SALVE_FRACTION); }
    public static int incenseBeats() { return get(INCENSE_BEATS); }
    public static int incenseRadius() { return get(INCENSE_RADIUS); }
    public static int incenseSeconds() { return get(INCENSE_SECONDS); }
    public static double incenseFamiliarHeal() { return get(INCENSE_FAMILIAR_HEAL); }
    public static int voiceWaterLevels() { return get(VOICE_WATER); }
    public static double voiceFireDuration() { return get(VOICE_FIRE); }
    public static double voiceEarthAbsorption() { return get(VOICE_EARTH); }
    public static double voiceAirHeal() { return get(VOICE_AIR); }
    public static int voiceSpiritRegen() { return get(VOICE_SPIRIT); }
    public static double voiceLoomDuration() { return get(VOICE_LOOM); }
    public static double rattleHeal() { return get(RATTLE_HEAL); }
    public static double rattleRankBonus() { return get(RATTLE_RANK_BONUS); }
    public static int rattleCost() { return get(RATTLE_COST); }
    public static double rattleRange() { return get(RATTLE_RANGE); }
    public static int circleRadius() { return get(CIRCLE_RADIUS); }
    public static double circleCastHeal() { return get(CIRCLE_CAST_HEAL); }
    public static double circleBeatHeal() { return get(CIRCLE_BEAT_HEAL); }
    public static int circleBlessingMinutes() { return get(CIRCLE_BLESSING); }
    public static boolean circleRevives() { return get(CIRCLE_REVIVES); }
    public static boolean familiarRemnants() { return get(REMNANTS); }
    public static int lodgeRange() { return get(LODGE_RANGE); }
    public static boolean lodgeNeedsRoof() { return get(LODGE_ROOF); }
    public static int lodgeBlessingMinutes() { return get(LODGE_BLESSING); }
    public static int anointPulseCost() { return get(ANOINT_PULSE_COST); }
    public static int plateLinkRange() { return get(PLATE_LINK_RANGE); }
    public static int plateLinkMax() { return get(PLATE_LINK_MAX); }
    // charms
    public static int charmUpkeepPerVoice() { return get(CHARM_UPKEEP_PER_VOICE); }
    public static int charmUpkeepBeatTicks() { return get(CHARM_UPKEEP_BEAT); }
    public static double charmLoomRefundChance() { return get(CHARM_LOOM_REFUND_CHANCE); }
    public static int charmLoomRefundCap() { return get(CHARM_LOOM_REFUND_CAP); }
    public static int gatherReach() { return get(GATHER_REACH); }
    public static int gatherReachPerVoice() { return get(GATHER_REACH_PER_VOICE); }
    public static int gatherReachCap() { return get(GATHER_REACH_CAP); }
    public static int hearthCharmNutrition() { return get(HEARTH_CHARM_FOOD); }
    public static double hearthCharmSaturation() { return get(HEARTH_CHARM_SATURATION); }
    public static int charmFireResistanceTicks() { return get(CHARM_FIRE_TICKS); }
    public static int charmWaterBreathingTicks() { return get(CHARM_WATER_BREATHING_TICKS); }
    public static int charmDolphinsGraceTicks() { return get(CHARM_DOLPHINS_GRACE_TICKS); }
    public static int charmResistanceTicks() { return get(CHARM_EARTH_TICKS); }
    public static int charmNightVisionTicks() { return get(CHARM_SPIRIT_TICKS); }
    public static int charmLuckTicks() { return get(CHARM_LOOM_TICKS); }
    public static int charmSlowFallingTicks() { return get(CHARM_AIR_TICKS); }
    public static double wardBlockChance() { return get(WARD_BLOCK_CHANCE); }
    public static double wardBlockChanceSpirit() { return get(WARD_BLOCK_CHANCE_SPIRIT); }
    public static int charmEmberSeconds() { return get(CHARM_EMBER_SECONDS); }
    public static int charmLinkCost() { return get(CHARM_LINK_COST); }
    public static int charmChorusCost() { return get(CHARM_CHORUS_COST); }
    // spiritgear
    public static int gearMineCost() { return get(GEAR_MINE_COST); }
    public static int gearHitCost() { return get(GEAR_HIT_COST); }
    public static int gearUseCost() { return get(GEAR_USE_COST); }
    public static int gearLinkCost() { return get(GEAR_LINK_COST); }
    public static int armorCostLinked() { return get(ARMOR_COST_LINKED); }
    public static int armorUpkeepTicks() { return get(ARMOR_UPKEEP_TICKS); }
    public static double loomRobeRefundChance() { return get(LOOM_ROBE_REFUND); }
    public static double loomRobeRefundChanceManifested() { return get(LOOM_ROBE_REFUND_MANIFESTED); }
    public static int waterBootsFreezeRadius() { return get(WATER_BOOTS_FREEZE); }
    public static int waterBootsFreezeRadiusManifested() { return get(WATER_BOOTS_FREEZE_MANIFESTED); }
    public static int bootsSlowFallTicks() { return get(BOOTS_SLOW_FALL_TICKS); }
    public static int armorNightVisionTicks() { return get(ARMOR_NIGHT_VISION_TICKS); }
    public static int armorResistanceTicks() { return get(ARMOR_RESISTANCE_TICKS); }
    public static int armorSpeedTicks() { return get(ARMOR_SPEED_TICKS); }
    public static int armorFireResistanceTicks() { return get(ARMOR_FIRE_RESISTANCE_TICKS); }
    public static int armorWaterBreathingTicks() { return get(ARMOR_WATER_BREATHING_TICKS); }
    public static int armorDolphinsGraceTicks() { return get(ARMOR_DOLPHINS_GRACE_TICKS); }
    public static int armorLuckTicks() { return get(ARMOR_LUCK_TICKS); }
    public static int spiritHoodGlowRange() { return get(SPIRIT_HOOD_GLOW_RANGE); }
    public static int loomBootsStitchReach() { return get(LOOM_BOOTS_STITCH_REACH); }
    public static int loomBootsStitchReachManifested() { return get(LOOM_BOOTS_STITCH_REACH_MANIFESTED); }
    public static int loomBootsStitchCooldownTicks() { return get(LOOM_BOOTS_STITCH_COOLDOWN); }
    public static double setMendHealth() { return get(SET_MEND_HEALTH); }
    public static int setMendPulse() { return get(SET_MEND_PULSE); }
    public static int setMendTicks() { return get(SET_MEND_TICKS); }
    public static int spiritPickGlintRange() { return get(SPIRIT_PICK_GLINT); }
    public static int spiritPickGlintRangeManifested() { return get(SPIRIT_PICK_GLINT_MANIFESTED); }
    public static int spiritShovelGlintRange() { return get(SPIRIT_SHOVEL_GLINT); }
    public static double earthHoodBlockChance() { return get(EARTH_HOOD_BLOCK); }
    public static double earthHoodBlockChanceManifested() { return get(EARTH_HOOD_BLOCK_MANIFESTED); }
    public static int fireRobeIgniteSeconds() { return get(FIRE_ROBE_IGNITE_SECONDS); }
    public static int spiritRobeGlowTicks() { return get(SPIRIT_ROBE_GLOW_TICKS); }
    public static int waterRobeRegenTicks() { return get(WATER_ROBE_REGEN_TICKS); }
    public static int waterRobeRegenTicksManifested() { return get(WATER_ROBE_REGEN_TICKS_MANIFESTED); }
    public static double bladeEchoDamage() { return get(BLADE_ECHO_DAMAGE); }
    public static int bladeGlowTicks() { return get(BLADE_GLOW_TICKS); }
    public static double bladeSpiritEcho() { return get(BLADE_SPIRIT_ECHO); }
    public static double bladeSpiritEchoManifested() { return get(BLADE_SPIRIT_ECHO_MANIFESTED); }
    public static double bladeEarthKnockback() { return get(BLADE_EARTH_KNOCKBACK); }
    public static int bladeEarthSlownessLevel() { return get(BLADE_EARTH_SLOWNESS_LEVEL); }
    public static int bladeEarthSlownessLevelManifested() { return get(BLADE_EARTH_SLOWNESS_LEVEL_MANIFESTED); }
    public static int bladeEarthSlownessTicks() { return get(BLADE_EARTH_SLOWNESS_TICKS); }
    public static int bladeFireSeconds() { return get(BLADE_FIRE_SECONDS); }
    public static int bladeFireSecondsManifested() { return get(BLADE_FIRE_SECONDS_MANIFESTED); }
    public static double bladeFireDamage() { return get(BLADE_FIRE_DAMAGE); }
    public static double bladeFireDamageManifested() { return get(BLADE_FIRE_DAMAGE_MANIFESTED); }
    public static double bladeWaterHeal() { return get(BLADE_WATER_HEAL); }
    public static double bladeWaterHealManifested() { return get(BLADE_WATER_HEAL_MANIFESTED); }
    public static double bladeAirSweep() { return get(BLADE_AIR_SWEEP); }
    public static double bladeAirSweepManifested() { return get(BLADE_AIR_SWEEP_MANIFESTED); }
    public static double bladeLoomPull() { return get(BLADE_LOOM_PULL); }
    public static double bladeLoomPullManifested() { return get(BLADE_LOOM_PULL_MANIFESTED); }
    public static double axeWaterSaplingChance() { return get(AXE_WATER_SAPLING_CHANCE); }
    public static int axeSpiritGlowRange() { return get(AXE_SPIRIT_GLOW_RANGE); }
    public static int axeSpiritGlowTicks() { return get(AXE_SPIRIT_GLOW_TICKS); }
    public static int pickAirHasteTicks() { return get(PICK_AIR_HASTE_TICKS); }
    public static double shovelWaterClayChance() { return get(SHOVEL_WATER_CLAY_CHANCE); }
    public static double hoeSpiritBountyChance() { return get(HOE_SPIRIT_BOUNTY_CHANCE); }
    public static int hoeWaterMoistenRadius() { return get(HOE_WATER_MOISTEN_RADIUS); }
    public static int hoeWaterMoistenRadiusManifested() { return get(HOE_WATER_MOISTEN_RADIUS_MANIFESTED); }
    public static double shearsWaterRegrowChance() { return get(SHEARS_WATER_REGROW_CHANCE); }
    public static int shearsSpiritRegenTicks() { return get(SHEARS_SPIRIT_REGEN_TICKS); }
    public static int shearsSpiritGlowRange() { return get(SHEARS_SPIRIT_GLOW_RANGE); }
    public static int shearsSpiritGlowRangeManifested() { return get(SHEARS_SPIRIT_GLOW_RANGE_MANIFESTED); }
    public static int rattleVoiceTicks() { return get(RATTLE_VOICE_TICKS); }
    public static int rattleFireTicks() { return get(RATTLE_FIRE_TICKS); }
    public static double rattleWaterBonus() { return get(RATTLE_WATER_BONUS); }
    public static double boundSetDamageTaken() { return get(BOUND_SET_DAMAGE); }
    public static double manifestedSetDamageTaken() { return get(MANIFESTED_SET_DAMAGE); }
    public static double bladeBossBonusManifested() { return get(BLADE_BOSS_BONUS); }
    public static double bladeLifestealManifested() { return get(BLADE_LIFESTEAL); }
    public static double manifestedSpareChance() { return get(MANIFESTED_SPARE_CHANCE); }
    public static double earthBootsKnockbackKept() { return get(EARTH_BOOTS_KNOCKBACK); }
    public static double spiritBootsBounceFall() { return get(SPIRIT_BOOTS_BOUNCE_FALL); }
    public static double spiritBootsBounceSpeed() { return get(SPIRIT_BOOTS_BOUNCE_SPEED); }
    public static int spiritHoodGlowTicks() { return get(SPIRIT_HOOD_GLOW_TICKS); }
    public static int shearsSpiritGlowTicks() { return get(SHEARS_SPIRIT_GLOW_TICKS); }
    // staff
    public static int staffEarthCost() { return get(STAFF_EARTH_COST); }
    public static int staffFireCost() { return get(STAFF_FIRE_COST); }
    public static int staffWaterCost() { return get(STAFF_WATER_COST); }
    public static int staffAirCost() { return get(STAFF_AIR_COST); }
    public static int staffSpiritCost() { return get(STAFF_SPIRIT_COST); }
    public static int staffTetherCost() { return get(STAFF_TETHER_COST); }
    public static int staffStitchCost() { return get(STAFF_STITCH_COST); }
    public static double staffEarthDamage() { return get(STAFF_EARTH_DAMAGE); }
    public static double staffFireDamage() { return get(STAFF_FIRE_DAMAGE); }
    public static int staffEarthSlowTicks() { return get(STAFF_EARTH_SLOW_TICKS); }
    public static int staffWaterRegenTicks() { return get(STAFF_WATER_REGEN_TICKS); }
    public static int staffWaterBreathingTicks() { return get(STAFF_WATER_BREATHING_TICKS); }
    public static int staffAirSlowFallTicks() { return get(STAFF_AIR_SLOW_FALL_TICKS); }
    public static int staffSpiritSightTicks() { return get(STAFF_SPIRIT_SIGHT_TICKS); }
    public static int staffSpiritGlowTicks() { return get(STAFF_SPIRIT_GLOW_TICKS); }
    public static int staffCooldownTicks() { return get(STAFF_COOLDOWN); }
    public static int staffWaterCooldownTicks() { return get(STAFF_WATER_COOLDOWN); }
    public static int staffStitchCooldownTicks() { return get(STAFF_STITCH_COOLDOWN); }
    public static int staffFireIgniteSeconds() { return get(STAFF_FIRE_IGNITE_SECONDS); }
    public static double staffTetherPull() { return get(STAFF_TETHER_PULL); }
    public static int staffSpiritRadius() { return get(STAFF_SPIRIT_RADIUS); }
    public static int staffRange() { return get(STAFF_RANGE); }
    public static int staffStitchRange() { return get(STAFF_STITCH_RANGE); }
    // tools and rites
    public static int waystoneTier1Pulse() { return get(WAYSTONE_TIER1_PULSE); }
    public static int waystoneTier2Pulse() { return get(WAYSTONE_TIER2_PULSE); }
    public static int waystoneTier3Pulse() { return get(WAYSTONE_TIER3_PULSE); }
    public static int waystoneCooldownTicks() { return get(WAYSTONE_COOLDOWN); }
    public static int waystoneTier1Range() { return get(WAYSTONE_TIER1_RANGE); }
    public static int maulBlockPulse() { return get(MAUL_BLOCK_PULSE); }
    public static int maulCooldownTicks() { return get(MAUL_COOLDOWN); }
    public static int wandPulsePerBlock() { return get(WAND_PULSE_PER_BLOCK); }
    public static int wandMaxBlocks() { return get(WAND_MAX_BLOCKS); }
    public static int wrenchUsePulse() { return get(WRENCH_USE_PULSE); }
    public static int riteAmplifyPulse() { return get(RITE_AMPLIFY_PULSE); }
    // tribes
    public static int standingFavoured() { return get(STANDING_FAVOURED); }
    public static int standingReagent() { return get(STANDING_REAGENT); }
    public static int standingFood() { return get(STANDING_FOOD); }
    public static int standingPulsePer10() { return get(STANDING_PULSE_PER_10); }
    public static int hearthCellDrain() { return get(HEARTH_CELL_DRAIN); }
    public static int standingKill() { return get(STANDING_KILL); }
    public static int killCapPerDay() { return get(KILL_CAP_PER_DAY); }
    public static int offerCapPerDay() { return get(OFFER_CAP_PER_DAY); }
    public static int killRadius() { return get(KILL_RADIUS); }
    public static int standingTrade() { return get(STANDING_TRADE); }
    public static int standingHurtKin() { return get(STANDING_HURT_KIN); }
    public static int standingCampBlock() { return get(STANDING_CAMP_BLOCK); }
    public static int standingHearth() { return get(STANDING_HEARTH); }
    public static int hunterAngerTicks() { return get(HUNTER_ANGER_TICKS); }
    public static int kinDrumIntervalTicks() { return get(KIN_DRUM_INTERVAL); }
    public static int kinDrumRadius() { return get(KIN_DRUM_RADIUS); }
    public static int kinDrumPulse() { return get(KIN_DRUM_PULSE); }
    // familiars
    public static int familiarBoostRange() { return get(FAMILIAR_RANGE); }
    public static int familiarAttuneRange() { return get(FAMILIAR_ATTUNE_RANGE); }
    public static double familiarDiscount() { return get(FAMILIAR_DISCOUNT); }
    public static double familiarGearPerk() { return get(FAMILIAR_GEAR_PERK); }
    public static double familiarSongDamage() { return get(FAMILIAR_SONG_DAMAGE); }
    public static double familiarSongReach() { return get(FAMILIAR_SONG_REACH); }
    public static double familiarRiteBlessing() { return get(FAMILIAR_RITE_BLESSING); }
    public static int familiarBoonTicks() { return get(FAMILIAR_BOON_TICKS); }
    public static int foxSightRange() { return get(FOX_SIGHT_RANGE); }
    public static int foxOreRange() { return get(FOX_ORE_RANGE); }
    public static int foxLightPeriodTicks() { return get(FOX_LIGHT_PERIOD); }
    public static int impRange() { return get(IMP_RANGE); }
    public static int impRefund() { return get(IMP_REFUND); }
    public static int impTempoBonus() { return get(IMP_TEMPO_BONUS); }
    public static int bellRange() { return get(BELL_RANGE); }
    public static int mothClickPeriodTicks() { return get(MOTH_CLICK_PERIOD); }
    public static int mothClickTicks() { return get(MOTH_CLICK_TICKS); }
    public static int mothClickTrueTicks() { return get(MOTH_CLICK_TRUE_TICKS); }
    public static int weaverRange() { return get(WEAVER_RANGE); }
    public static int weaverGatherRange() { return get(WEAVER_GATHER_RANGE); }
    public static int houndTrackRange() { return get(HOUND_TRACK_RANGE); }
    public static double markSpeciesChance() { return get(MARK_SPECIES_CHANCE); }
    public static double markKinChance() { return get(MARK_KIN_CHANCE); }
    public static int brushCooldownTicks() { return get(BRUSH_COOLDOWN); }
    public static double bondChance() { return get(BOND_CHANCE); }
    public static double bondRemnantChance() { return get(BOND_REMNANT_CHANCE); }
    // echo
    public static int kilnSeconds() { return get(KILN_SECONDS); }
    public static int kilnPulse() { return get(KILN_PULSE); }
    public static int gritShatterSeconds() { return get(GRIT_SHATTER_SECONDS); }
    public static int gritShatterPulse() { return get(GRIT_SHATTER_PULSE); }
    public static int gearAttuneSeconds() { return get(GEAR_ATTUNE_SECONDS); }
    public static int gearAttunePulse() { return get(GEAR_ATTUNE_PULSE); }
    public static int gearBindSeconds() { return get(GEAR_BIND_SECONDS); }
    public static int gearBindPulse() { return get(GEAR_BIND_PULSE); }
    public static int gearManifestSeconds() { return get(GEAR_MANIFEST_SECONDS); }
    public static int gearManifestPulse() { return get(GEAR_MANIFEST_PULSE); }
    public static int machineAttuneSeconds() { return get(MACHINE_ATTUNE_SECONDS); }
    public static int machineAttunePulse() { return get(MACHINE_ATTUNE_PULSE); }
    public static int machineBindSeconds() { return get(MACHINE_BIND_SECONDS); }
    public static int machineBindPulse() { return get(MACHINE_BIND_PULSE); }
    public static int machineManifestSeconds() { return get(MACHINE_MANIFEST_SECONDS); }
    public static int machineManifestPulse() { return get(MACHINE_MANIFEST_PULSE); }
    /** What a machine's job time is multiplied by at rank 1, 2 or 3; 1.0 for rank 0 or anything else. */
    public static double machineRankTime(int rank) {
        return switch (rank) { case 1 -> get(MACHINE_RANK1_TIME); case 2 -> get(MACHINE_RANK2_TIME); case 3 -> get(MACHINE_RANK3_TIME); default -> 1.0; };
    }
    public static double machineRankGainBonus() { return get(MACHINE_RANK_GAIN); }
    /** One anointment's number, by the key it declares in {@link tk.darrow.tribalpower.song.Anointment}. */
    public static double anointing(tk.darrow.tribalpower.song.Anointment anointment, String key) {
        var value = ANOINTING.get(anointment).get(key);
        if (value == null) throw new IllegalArgumentException(anointment + " has no " + key);
        return get(value);
    }

    public static List<? extends String> gritDenyList() { return SPEC.isLoaded() ? GRIT_DENY_LIST.get() : List.of(); }

    /** Applies {@link #GENERATION_MULTIPLIER} without ever rounding a working generator down to nothing. */
    public static int scaleGeneration(int pulse) {
        if (pulse <= 0) return 0;
        double scaled = pulse * generationMultiplier();
        return scaled <= 0 ? 0 : Math.max(1, (int) Math.round(scaled));
    }

    /** Applies {@link #CONSUMPTION_MULTIPLIER}. Stone Font cobble must not call this. */
    public static int scaleConsumption(int pulse) {
        if (pulse <= 0) return 0;
        double scaled = pulse * consumptionMultiplier();
        return scaled <= 0 ? 0 : Math.max(1, (int) Math.round(scaled));
    }
}
