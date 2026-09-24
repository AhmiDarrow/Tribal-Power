package tk.darrow.tribalpower.healing;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.song.Note;
import tk.darrow.tribalpower.song.Reagents;

/**
 * Brewed remedies: which reagent and voice a stack carries, and what giving it to someone does. The reagent's Note
 * picks the {@link Remedy}; the voice of the totem the kettle sang beside shapes how strong or long it is; the
 * {@link Form} decides how it is taken.
 */
public final class Remedies {
    public static final String REAGENT = "Remedy", VOICE = "Voice";

    public enum Form {
        TINCTURE, SALVE, INCENSE;

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    private Remedies() {}

    public static ItemStack make(Form form, CreatureProfile reagent, @Nullable Attunement voice, int count) {
        ItemStack stack = new ItemStack(HealingRegistry.remedy(form), count);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(REAGENT, reagent.reagent);
            if (voice != null) tag.putString(VOICE, voice.getSerializedName());
        });
        return stack;
    }

    public static Optional<CreatureProfile> reagent(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        return Optional.ofNullable(Reagents.byId(data.getUnsafe().getString(REAGENT)));
    }

    public static Remedy remedy(ItemStack stack) {
        return reagent(stack).map(profile -> Remedy.of(Note.of(profile))).orElse(Remedy.MENDING);
    }

    public static @Nullable Attunement voice(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String name = data.getUnsafe().getString(VOICE);
        return name.isEmpty() ? null : Attunement.byName(name);
    }

    /**
     * Gives one dose. {@code seconds} is the form's own duration before the voice shapes it; {@code heal} is any
     * health the form restores at once.
     */
    public static void give(LivingEntity target, Remedy remedy, @Nullable Attunement voice, int seconds, float heal, boolean ambient) {
        int levels = voice == Attunement.WATER ? TribalConfig.voiceWaterLevels() : 0;
        double stretch = voice == Attunement.FIRE ? TribalConfig.voiceFireDuration()
                : voice == Attunement.LOOM ? TribalConfig.voiceLoomDuration() : 1.0;
        int ticks = (int) Math.round(seconds * 20 * stretch);
        if (remedy.effect != null && ticks > 0)
            target.addEffect(new MobEffectInstance(remedy.effect, ticks, voice == Attunement.LOOM ? 0 : levels, ambient, !ambient, true));
        for (var ailment : remedy.cures) target.removeEffect(ailment);
        if (TribalConfig.remediesCureSickness()) tk.darrow.tribalpower.effect.ModEffects.cleanse(target);
        if (voice == Attunement.EARTH && TribalConfig.voiceEarthAbsorption() > 0) {
            // absorption is capped by the effect's own attribute, so the points come as levels of the effect
            int level = Math.max(0, (int) Math.ceil(TribalConfig.voiceEarthAbsorption() / 4.0) - 1);
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(ticks, 20 * 60), level, ambient, !ambient, true));
        }
        if (voice == Attunement.AIR) heal += (float) TribalConfig.voiceAirHeal();
        if (voice == Attunement.SPIRIT && TribalConfig.voiceSpiritRegen() > 0)
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, TribalConfig.voiceSpiritRegen() * 20, 0, ambient, !ambient, true));
        if (heal > 0) target.heal(heal);
        if (!ambient && target.level() instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.7), target.getZ(), 6, 0.35, 0.35, 0.35, 0.02);
    }

    /** A tincture, drunk. */
    public static void drink(LivingEntity target, ItemStack tincture) {
        give(target, remedy(tincture), voice(tincture), TribalConfig.tinctureSeconds(), 0, false);
    }

    /** A salve, laid on: heals at once, lifts every harmful effect, and carries a shorter dose of the remedy. */
    public static void apply(LivingEntity target, ItemStack salve) {
        for (var active : java.util.List.copyOf(target.getActiveEffects()))
            if (!active.getEffect().value().isBeneficial()) target.removeEffect(active.getEffect());
        give(target, remedy(salve), voice(salve), (int) Math.round(TribalConfig.tinctureSeconds() * TribalConfig.salveEffectFraction()),
                (float) TribalConfig.salveHeal(), false);
    }

    /** One beat of burning incense on one body. */
    public static void breathe(LivingEntity target, ItemStack incense) {
        give(target, remedy(incense), voice(incense), TribalConfig.incenseSeconds(), 0, true);
    }
}
