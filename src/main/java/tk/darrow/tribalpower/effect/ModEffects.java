package tk.darrow.tribalpower.effect;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * The spirit layer: six voice blessings, one per Attunement; nine tribe boons, one per tribe; and the March's
 * afflictions. Songs, rites, standing and food hand them out; remedies and the Spirit Well take the bad ones
 * away. Every number is read live from the {@code effects} section of the common config.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TribalPower.MOD_ID);

    public static final Map<Attunement, DeferredHolder<MobEffect, VoiceBlessingEffect>> BLESSINGS = new EnumMap<>(Attunement.class);
    public static final Map<TribeDefinition, DeferredHolder<MobEffect, TribeBoonEffect>> BOONS = new EnumMap<>(TribeDefinition.class);
    public static final Map<AfflictionEffect.Kind, DeferredHolder<MobEffect, AfflictionEffect>> AFFLICTIONS = new EnumMap<>(AfflictionEffect.Kind.class);

    static {
        for (Attunement voice : Attunement.values())
            BLESSINGS.put(voice, EFFECTS.register(voice.getSerializedName() + "_voice_blessing", () -> new VoiceBlessingEffect(voice)));
        for (TribeDefinition tribe : TribeDefinition.values())
            BOONS.put(tribe, EFFECTS.register(tribe.id() + "_boon", () -> new TribeBoonEffect(tribe)));
        for (AfflictionEffect.Kind kind : AfflictionEffect.Kind.values())
            AFFLICTIONS.put(kind, EFFECTS.register(kind.id(), () -> new AfflictionEffect(kind)));
    }

    private ModEffects() {}

    public static Holder<MobEffect> blessing(Attunement voice) { return BLESSINGS.get(voice); }
    public static Holder<MobEffect> boon(TribeDefinition tribe) { return BOONS.get(tribe); }
    public static Holder<MobEffect> affliction(AfflictionEffect.Kind kind) { return AFFLICTIONS.get(kind); }

    public static boolean blessed(LivingEntity entity, Attunement voice) { return entity.hasEffect(blessing(voice)); }
    public static boolean hasBoon(LivingEntity entity, TribeDefinition tribe) { return entity.hasEffect(boon(tribe)); }
    public static boolean afflicted(LivingEntity entity, AfflictionEffect.Kind kind) { return entity.hasEffect(affliction(kind)); }

    /** Level (amplifier + 1) of a blessing, or 0. */
    public static int blessingLevel(LivingEntity entity, Attunement voice) {
        MobEffectInstance active = entity.getEffect(blessing(voice));
        return active == null ? 0 : active.getAmplifier() + 1;
    }

    /** Grants a blessing for {@code ticks}; a stronger or longer one already held is kept. */
    public static void bless(LivingEntity entity, Attunement voice, int ticks, int amplifier, boolean ambient) {
        entity.addEffect(new MobEffectInstance(blessing(voice), ticks, amplifier, ambient, !ambient, true));
    }

    public static void grantBoon(LivingEntity entity, TribeDefinition tribe, int ticks, boolean ambient) {
        entity.addEffect(new MobEffectInstance(boon(tribe), ticks, 0, ambient, !ambient, true));
    }

    public static void afflict(LivingEntity entity, AfflictionEffect.Kind kind, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(affliction(kind), ticks, amplifier));
    }

    /** Lifts every affliction the spirit layer knows, including the healing set's Spirit Sickness. Returns how many went. */
    public static int cleanse(LivingEntity entity) {
        int lifted = 0;
        for (var affliction : AFFLICTIONS.values()) if (entity.removeEffect(affliction)) lifted++;
        if (entity.removeEffect(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_SICKNESS)) lifted++;
        return lifted;
    }

    /** Whether the player is hushed: songs and staff voices refuse until it lifts. Tells them why. */
    public static boolean hushed(Player player) {
        if (!afflicted(player, AfflictionEffect.Kind.UNSUNG_HUSH)) return false;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tribalpower.hushed"), true);
        return true;
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(EffectHooks::playerTick);
        NeoForge.EVENT_BUS.addListener(EffectHooks::fall);
        NeoForge.EVENT_BUS.addListener(EffectHooks::breakSpeed);
        NeoForge.EVENT_BUS.addListener(EffectHooks::blockDrops);
        NeoForge.EVENT_BUS.addListener(EffectHooks::changeTarget);
        NeoForge.EVENT_BUS.addListener(EffectHooks::incomingDamage);
        NeoForge.EVENT_BUS.addListener(EffectHooks::effectAdded);
        NeoForge.EVENT_BUS.addListener(EffectHooks::trample);
    }
}
