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
