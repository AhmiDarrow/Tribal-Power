package tk.darrow.tribalpower.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/**
 * Original short hits for the 3.1 devices (design 3.1 section 15) plus the streamed Drum Circle disc.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TribalPower.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_DRUM_CIRCLE =
            register("music_disc.drum_circle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUMHEART_TEMPO = register("drumheart_tempo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUMHEART_OFF_TEMPO = register("drumheart_off_tempo");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMBER_HORN_ROAR = register("ember_horn_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_HARP_STRING = register("wind_harp_string");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAVE_DRUM_SLAP = register("wave_drum_slap");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAKE_BELL_TOLL = register("wake_bell_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_HUM = register("gate_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_TRANSIT = register("gate_transit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MESH_SIFT = register("mesh_sift");
    public static final DeferredHolder<SoundEvent, SoundEvent> FONT_FORM = register("font_form");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHALK_DRAW = register("chalk_draw");
    /** The Wind Charm: a gust through all five rods, or one rod knocked (tools-side synth, D major pentatonic). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_CHARM_GUST = register("block.wind_charm.gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_CHARM_ROD = register("block.wind_charm.rod");
    /** The Gate Rite's music, one track per rite (see tools/generate_gate_rite.py). */
    public static final java.util.List<DeferredHolder<SoundEvent, SoundEvent>> GATE_RITE =
            java.util.stream.IntStream.rangeClosed(1, 6).mapToObj(i -> register("gate_rite_" + i)).toList();

    /** The March's music (tools/generate_march_music.py): a track per biome, a theme per boss and the finale, stingers, and the guardians' voices. */
    public static final java.util.List<String> MARCH_BIOMES = java.util.List.of("march_steppe", "march_highlands", "march_glimmer_ridge", "march_snow_fields",
            "march_ember_wastes", "march_reed_fen", "march_crystal_fields", "march_shallows");
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> BIOME_MUSIC = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> BOSS_THEMES = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> GUARDIAN_VOICES = new java.util.LinkedHashMap<>();
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_FESTIVAL = register("stinger.festival");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_SURGE = register("stinger.surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_WEATHER = register("stinger.weather");
    static {
        for (String biome : MARCH_BIOMES) BIOME_MUSIC.put(biome, register("music." + biome));
        for (String boss : java.util.List.of("the_unsung", "colossus_warden", "finale")) BOSS_THEMES.put(boss, register("boss." + boss));
        for (var guardian : tk.darrow.tribalpower.guardian.Guardian.values()) {
            BOSS_THEMES.put(guardian.id, register("boss." + guardian.id));
            GUARDIAN_VOICES.put(guardian.id, register("guardian." + guardian.id));
        }
    }

    /** Every creature profile's own voice (tools/generate_creature_voices.py): creature.&lt;id&gt;.ambient / hurt / death. */
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> CREATURE = new java.util.HashMap<>();
    public static final java.util.List<String> CREATURE_KINDS = java.util.List.of("ambient", "hurt", "death");
    /** The wildlife and the people share the same scheme under their own names. */
    public static final java.util.List<String> NAMED_VOICES = java.util.List.of("glimmerfin", "march_swimmer", "drift_bell", "loom_swift");
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> KIN = new java.util.HashMap<>();
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> UNSUNG = new java.util.HashMap<>();
    public static final DeferredHolder<SoundEvent, SoundEvent> GUARDIAN_HURT = register("guardian.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIN_DRUM = register("kin.drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIN_LOOM = register("kin.loom");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIN_YES = register("kin.yes");
    /** Short spirit effects the mobs play instead of vanilla's blocks and beasts. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_CAST = register("spirit.cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_CHIME = register("spirit.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_IMPACT = register("spirit.impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_SWOOP = register("spirit.swoop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_FROST = register("spirit.frost");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_THORN = register("spirit.thorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_SUMMON = register("spirit.summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_DRUM = register("spirit.drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_FLOP = register("spirit.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_BRUSH = register("spirit.brush");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_JUMP = register("spirit.jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIRIT_SNIFF = register("spirit.sniff");
    static {
        for (var profile : tk.darrow.tribalpower.entity.CreatureProfile.values())
            for (String kind : CREATURE_KINDS) CREATURE.put(profile.id + "." + kind, register("creature." + profile.id + "." + kind));
        for (String name : NAMED_VOICES)
            for (String kind : CREATURE_KINDS) CREATURE.put(name + "." + kind, register("creature." + name + "." + kind));
        for (String kind : CREATURE_KINDS) {
            KIN.put(kind, register("kin." + kind));
            UNSUNG.put(kind, register("unsung." + kind));
        }
    }

    /** A creature's voice of the given kind; {@code id} is a profile id or one of {@link #NAMED_VOICES}. */
    public static SoundEvent creature(String id, String kind) {
        var holder = CREATURE.get(id + "." + kind);
        return holder == null ? CREATURE.get("ridge_grazer." + kind).get() : holder.get();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, name)));
    }

    public static void play(Level level, BlockPos pos, DeferredHolder<SoundEvent, SoundEvent> sound,
                            float volume, float pitch) {
        if (level == null || level.isClientSide) return;
        level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, volume, pitch);
    }

    private ModSounds() {}
}
