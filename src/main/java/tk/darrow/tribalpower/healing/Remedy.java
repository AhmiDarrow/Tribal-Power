package tk.darrow.tribalpower.healing;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.song.Note;

/**
 * What a remedy does, by the Note of the reagent brewed into it: an effect to grant and ailments to lift. Every
 * remedy also lifts Spirit Sickness unless the config says otherwise.
 */
public enum Remedy {
    MENDING(Note.QUIET, MobEffects.REGENERATION),
    KINDLING(Note.EMBER, MobEffects.DAMAGE_BOOST),
    QUICKENING(Note.BOLT, MobEffects.MOVEMENT_SPEED),
    COOLING(Note.CHILL, MobEffects.FIRE_RESISTANCE),
    ANTIDOTE(Note.VENOM, null, MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER, MobEffects.CONFUSION),
    FEATHERFALL(Note.GUST, MobEffects.SLOW_FALLING),
    ROOTEDNESS(Note.ROOT, MobEffects.DAMAGE_RESISTANCE),
    BULWARK(Note.SHOVE, MobEffects.ABSORPTION),
    CLEAR_SIGHT(Note.WEAVE, MobEffects.NIGHT_VISION, MobEffects.BLINDNESS, MobEffects.DARKNESS),
    RESTORING(Note.WEAKEN, MobEffects.DIG_SPEED, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN),
    VIGOUR(Note.HOUND, MobEffects.HEALTH_BOOST);

    public final Note note;
    public final @Nullable Holder<MobEffect> effect;
    public final List<Holder<MobEffect>> cures;

    @SafeVarargs
    Remedy(Note note, @Nullable Holder<MobEffect> effect, Holder<MobEffect>... cures) {
        this.note = note;
        this.effect = effect;
        this.cures = List.of(cures);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Remedy of(Note note) {
        for (Remedy remedy : values()) if (remedy.note == note) return remedy;
        return MENDING;
    }
}
