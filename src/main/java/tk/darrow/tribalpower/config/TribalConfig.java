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

    /** Per weapon kind: attack damage, attacks per second, extra reach, and its trait number. */
    public record WeaponValues(ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue speed,
                               ModConfigSpec.DoubleValue reach, ModConfigSpec.DoubleValue trait, ModConfigSpec.DoubleValue weight) {}
    private static final java.util.Map<tk.darrow.tribalpower.item.WeaponKind, WeaponValues> WEAPONS =
            new java.util.EnumMap<>(tk.darrow.tribalpower.item.WeaponKind.class);
    public static final ModConfigSpec.IntValue ANOINT_REAGENT_COST;
    public static final ModConfigSpec.IntValue DISH_BOON;
    public static final ModConfigSpec.DoubleValue HEARTH_SCALE;
    public static final ModConfigSpec.BooleanValue HEARTH_HEAT;
    public static final ModConfigSpec.IntValue FEAST_NUTRITION;
    public static final ModConfigSpec.DoubleValue FEAST_SATURATION;
    public static final ModConfigSpec.IntValue FEAST_BLESSING;
    public static final ModConfigSpec.IntValue DISH_STANDING;
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
    public static final ModConfigSpec.IntValue URN_WOVEN;
    public static final ModConfigSpec.IntValue URN_COPPER;
    public static final ModConfigSpec.IntValue URN_MANIFESTED;
    public static final ModConfigSpec.IntValue DYE_YIELD;
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
    private static final java.util.Map<tk.darrow.tribalpower.song.Anointment, java.util.Map<String, ModConfigSpec.DoubleValue>> ANOINTING =
            new java.util.EnumMap<>(tk.darrow.tribalpower.song.Anointment.class);

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
        b.comment("Camp kit: the Spirit Door, Vine Lifts, the Pulse Crossbow, Soul Urns and reagent dyes.").push("kit");
        SPIRIT_DOOR_BLOCKS = b.comment("Whether a Spirit Door stops hostile monsters and spirits while letting everyone else through.").define("spiritDoorBlocksHostiles", true);
        LIFT_RANGE = b.comment("Blocks up or down a Vine Lift looks for the next lift in its column.").defineInRange("liftRange", 48, 2, 384);
        CROSSBOW_LOAD = b.comment("Ticks the Pulse Crossbow takes to load (a Pulse Bow reaches full draw in 20).").defineInRange("crossbowLoadTicks", 25, 1, 200);
        CROSSBOW_DAMAGE = b.comment("Damage of a crossbow bolt against a fully drawn Pulse Bow bolt.").defineInRange("crossbowDamageMultiplier", 1.5, 0.1, 10.0);
        CROSSBOW_SPEED = b.comment("Speed a crossbow bolt leaves at (a full bow draw is 3.2).").defineInRange("crossbowVelocity", 3.6, 0.5, 10.0);
        CROSSBOW_PULSE = b.comment("Pulse loading the crossbow spends; a verse arrow adds the bow's verse cost.").defineInRange("crossbowPulse", 10, 0, 1000);
        URN_WOVEN = b.comment("Uses a Woven Soul Urn has. Capturing and releasing are one use each.").defineInRange("wovenUrnUses", 2, 2, 10000);
        URN_COPPER = b.comment("Uses a Copper Soul Urn has.").defineInRange("copperUrnUses", 10, 2, 10000);
        URN_MANIFESTED = b.comment("Uses a Manifested Soul Urn has. A Resonant Soul Urn never wears out.").defineInRange("manifestedUrnUses", 20, 2, 10000);
        DYE_YIELD = b.comment("Dye one reagent ground with chalk in a bowl makes.").defineInRange("dyeYield", 2, 1, 64);
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

    public static double weaponDamage(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).damage()); }
    public static double weaponSpeed(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).speed()); }
    public static double weaponReach(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).reach()); }
    public static double weaponTrait(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).trait()); }
    public static double weaponWeight(tk.darrow.tribalpower.item.WeaponKind kind) { return get(WEAPONS.get(kind).weight()); }
    public static int anointReagentCost() { return get(ANOINT_REAGENT_COST); }
    public static int dishBoonMinutes() { return get(DISH_BOON); }
    public static double hearthCookScale() { return get(HEARTH_SCALE); }
    public static boolean hearthNeedsHeat() { return get(HEARTH_HEAT); }
    public static int feastNutrition() { return get(FEAST_NUTRITION); }
    public static double feastSaturation() { return get(FEAST_SATURATION); }
    public static int feastBlessingMinutes() { return get(FEAST_BLESSING); }
    public static int dishStanding() { return get(DISH_STANDING); }
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
    public static int wovenUrnUses() { return get(URN_WOVEN); }
    public static int copperUrnUses() { return get(URN_COPPER); }
    public static int manifestedUrnUses() { return get(URN_MANIFESTED); }
    public static int dyeYield() { return get(DYE_YIELD); }
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
