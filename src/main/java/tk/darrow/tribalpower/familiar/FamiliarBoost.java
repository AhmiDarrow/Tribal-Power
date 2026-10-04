package tk.darrow.tribalpower.familiar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.camp.CampHooks;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Familiars and totems lean on each other. A bonded familiar takes a voice at a Resonance Totem: bring it within
 * {@link #ATTUNE_RANGE} blocks and sneak-use a Bonding Charm on the totem. While it stays within {@link #RANGE} blocks
 * of its keeper, that voice's magic is cheaper and stronger: Spirit Charms, Spiritgear, songs and rites. Near a totem
 * of its own voice it keeps the totem answered. Without an attuned familiar every helper here hands back what it was
 * given, so no magic is weaker than it was before familiars took voices.
 */
public final class FamiliarBoost {
    /** Keeper to familiar, for the boost. */
    public static final int RANGE = 16;
    /** Totem to familiar, when attuning. */
    public static final int ATTUNE_RANGE = 6;
    /** Familiar to the totems of its voice that it keeps answered. */
    public static final int KEEP_RANGE = LatticeNetwork.DEFAULT_RADIUS;
    public static final int KEEP_PERIOD = 200, SHIMMER_PERIOD = 60;
    /** Share of the Pulse an attuned familiar takes off its voice's charms and Spiritgear. */
    public static final float DISCOUNT = 0.4F;
    /** Spiritgear voice perks of the familiar's voice fire this much more often. */
    public static final float GEAR_PERK = 1.5F;
    /** Songs of the voice strike this much harder, and a call reaches this much further. */
    public static final float SONG_DAMAGE = 1.3F;
    public static final double SONG_REACH = 1.5;
    /** Rite blessings of the voice last this much longer. */
    public static final float RITE_BLESSING = 1.5F;
    /** A charm's extra effects outlast the 4-second refresh, so they linger a few seconds after you part. */
    public static final int BOON_TICKS = 200;

    /** The voices near a keeper, trusted for a second: the charm upkeep asks every tick. */
    private static final int REFRESH = 20;
    private record Seen(long at, ResourceKey<Level> where, Set<Attunement> voices) {}
    private static final Map<Player, Seen> SEEN = new WeakHashMap<>();

    private FamiliarBoost() {}

    // ---- what the keeper has near ----------------------------------------------------------------------------

    /** Voices of the keeper's attuned familiars within {@link #RANGE}. Always empty on the client. */
    public static Set<Attunement> voices(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return Set.of();
        long now = level.getGameTime();
        Seen seen = SEEN.get(player);
        if (seen != null && seen.where() == level.dimension() && now >= seen.at() && now - seen.at() < REFRESH) return seen.voices();
        Set<Attunement> found = EnumSet.noneOf(Attunement.class);
        for (Familiar familiar : owned(level, player, player.position(), RANGE))
            if (familiar.lattice().voice() != null) found.add(familiar.lattice().voice());
        Set<Attunement> voices = found.isEmpty() ? Set.of() : Collections.unmodifiableSet(found);
        SEEN.put(player, new Seen(now, level.dimension(), voices));
        return voices;
    }

    public static boolean boosts(Player player, Attunement voice) {
        return voice != null && voices(player).contains(voice);
    }

    /** Drops the remembered voices, so the next ask looks again (attuning, tests). */
    public static void forget(Player player) {
        SEEN.remove(player);
    }

    /** The keeper's bonded familiars within {@code range} of {@code at}. */
    public static List<Familiar> owned(ServerLevel level, Player player, Vec3 at, double range) {
        List<Familiar> out = new ArrayList<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(range),
                m -> m.isAlive() && m instanceof Familiar f && f.isBonded() && f.isOwnedBy(player) && m.distanceToSqr(at) <= range * range))
            out.add((Familiar) mob);
        return out;
    }

    // ---- the boosts -----------------------------------------------------------------------------------------

    public static int discounted(int cost) {
        return cost <= 0 ? cost : Math.max(1, Math.round(cost * (1 - DISCOUNT)));
    }

    /** One worn charm's upkeep: cheaper when any of its voices is one a familiar near you carries. */
    public static int charmCost(Player player, Set<Attunement> charmVoices, int cost) {
        if (cost <= 0) return cost;
        for (Attunement voice : voices(player)) if (charmVoices.contains(voice)) return discounted(cost);
        return cost;
    }

    /** What a Spiritgear piece spends: cheaper when its linked voice is one a familiar near you carries. */
    public static int gearCost(Player player, ItemStack gear, int amount) {
        if (amount <= 0) return amount;
        return boosts(player, SpiritGear.voice(gear).orElse(null)) ? discounted(amount) : amount;
    }

    /** A Spiritgear voice perk's odds: higher when its linked voice is one a familiar near you carries. */
    public static float gearChance(Player player, ItemStack gear, float base) {
        return player != null && boosts(player, SpiritGear.voice(gear).orElse(null)) ? Math.min(1F, base * GEAR_PERK) : base;
    }

    public static float songDamage(Player player, Attunement voice, float damage) {
        return boosts(player, voice) ? damage * SONG_DAMAGE : damage;
    }

    public static double songReach(Player player, Attunement voice, double radius) {
        return boosts(player, voice) ? radius + SONG_REACH : radius;
    }

    public static int riteBlessing(Player player, Attunement voice, int ticks) {
        return boosts(player, voice) ? Math.round(ticks * RITE_BLESSING) : ticks;
    }

    /** Fire's worn charms burn an attacker twice as long when a Fire familiar is near. */
    public static int emberSeconds(Player player, int seconds) {
        return boosts(player, Attunement.FIRE) ? seconds * 2 : seconds;
    }

    /**
     * Worn charms' extra effects, for each worn voice a familiar near you shares. Called with the charms' own
     * effects, every four seconds.
     */
    public static void charmBoons(Player player, Set<Attunement> worn) {
        Set<Attunement> kin = voices(player);
        if (kin.isEmpty()) return;
        for (Attunement voice : worn) {
            if (!kin.contains(voice)) continue;
            switch (voice) {
                case EARTH -> player.addEffect(boon(MobEffects.DAMAGE_RESISTANCE, 1));
                case WATER -> player.addEffect(boon(MobEffects.CONDUIT_POWER, 0));
                case AIR -> player.addEffect(boon(MobEffects.MOVEMENT_SPEED, 0));
                case LOOM -> player.addEffect(boon(MobEffects.LUCK, 1));
                case FIRE -> player.addEffect(boon(MobEffects.DIG_SPEED, 0));
                case SPIRIT -> {
                    // the lantern's sight: hostile things nearby show through walls
                    for (LivingEntity foe : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE), FamiliarRoster::hostile))
                        foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
                }
            }
        }
    }

    private static MobEffectInstance boon(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, BOON_TICKS, amplifier, true, false, true);
    }

    // ---- attuning at a totem --------------------------------------------------------------------------------

    /**
     * Gives the totem's voice to the keeper's nearest grown familiar within {@link #ATTUNE_RANGE} that does not
     * already carry it. Returns the familiar, or null with a message saying why not.
     */
    public static Familiar attune(ServerLevel level, Player player, BlockPos base, Attunement voice) {
        Vec3 at = base.getCenter();
        List<Familiar> near = owned(level, player, at, ATTUNE_RANGE);
        near.removeIf(f -> f.asMob().isBaby());
        if (near.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.familiar.attune_none", ATTUNE_RANGE), true);
            return null;
        }
        near.sort(Comparator.comparingDouble(f -> f.asMob().distanceToSqr(at)));
        Familiar pick = near.stream().filter(f -> f.lattice().voice() != voice).findFirst().orElse(null);
        if (pick == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.familiar.attune_same",
                    near.getFirst().asMob().getDisplayName(), voiceName(voice)), true);
            return null;
        }
        Mob mob = pick.asMob();
        pick.ensureLattice(mob.getRandom(), level.dimension().equals(ModDimensions.THE_MARCH));
        pick.lattice().setVoice(voice);
        pick.applyLattice();
        if (level.getBlockEntity(base) instanceof ResonanceTotemBlockEntity totem) Keeping.relight(totem, level);
        SpiritEffects.beam(level, at.add(0, 0.6, 0), mob.getBoundingBox().getCenter(), voice);
        SpiritEffects.ring(level, mob.position().add(0, 0.2, 0), voice, Math.max(0.5, mob.getBbWidth()), 16);
        level.playSound(null, mob.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.9F, 1.1F);
        player.displayClientMessage(Component.translatable("message.tribalpower.familiar.attuned", mob.getDisplayName(), voiceName(voice)), false);
        CampHooks.award(level, player.getUUID(), "kindred_voice");
        forget(player);
        return pick;
    }

    /** A plain click on a totem: who keeps it, or how the keeper's familiar standing by could. */
    public static void report(ServerLevel level, Player player, BlockPos base, Attunement voice) {
        List<Component> keepers = new ArrayList<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(KEEP_RANGE),
                m -> m.isAlive() && m instanceof Familiar f && f.isBonded() && f.lattice().voice() == voice))
            keepers.add(mob.getDisplayName());
        if (!keepers.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.familiar.keeps_totem",
                    ComponentUtils.formatList(keepers, Component.literal(", ")), voiceName(voice)), false);
            return;
        }
        // only a familiar close enough to attune right now earns a hint, so plain clicks do not fill the chat
        owned(level, player, base.getCenter(), ATTUNE_RANGE).stream().filter(f -> !f.asMob().isBaby()).findFirst().ifPresent(f ->
                player.displayClientMessage(Component.translatable("message.tribalpower.familiar.attune_hint",
                        f.asMob().getDisplayName(), voiceName(voice)), false));
    }

    // ---- the familiar's own beat ----------------------------------------------------------------------------

    /** Bonded familiars, every tick: an attuned one shimmers in its voice and keeps its voice's totems answered. */
    public static void tick(Familiar familiar) {
        Attunement voice = familiar.lattice().voice();
        Mob mob = familiar.asMob();
        if (voice == null || !(mob.level() instanceof ServerLevel level)) return;
        long beat = mob.tickCount + mob.getId();
        if (beat % SHIMMER_PERIOD == 0)
            SpiritEffects.ring(level, mob.position().add(0, 0.15, 0), voice, Math.max(0.35, mob.getBbWidth() * 0.6), 6);
        if (beat % KEEP_PERIOD == 0)
            for (ResonanceTotemBlockEntity totem : LatticeNetwork.findNearbyTotems(level, mob.blockPosition(), KEEP_RANGE))
                if (totem.getAttunement() == voice) totem.feed();
    }

    // ---- reading it -----------------------------------------------------------------------------------------

    public static Component voiceName(Attunement voice) {
        return Component.translatable("attunement.tribalpower." + voice.getSerializedName());
    }

    public static int rgb(Attunement voice) {
        var c = SpiritEffects.color(voice);
        return ((int) (c.x() * 255) << 16) | ((int) (c.y() * 255) << 8) | (int) (c.z() * 255);
    }

    /**
     * One line on what this creature is to you: its voice, or that it has none yet, or whether and how it can be
     * bonded. {@code voice} is the one to show (the synced copy on the client). Null for the young.
     */
    public static Component status(Familiar familiar, Attunement voice) {
        if (familiar.isBonded()) return voice != null
                ? Component.translatable("gui.tribalpower.familiar.voice", voiceName(voice)).withColor(rgb(voice))
                : Component.translatable("gui.tribalpower.familiar.no_voice").withStyle(ChatFormatting.GRAY);
        if (familiar.asMob().isBaby()) return null;
        if (!FamiliarRoster.tameable(familiar.profile()))
            return Component.translatable("gui.tribalpower.familiar.wild").withStyle(ChatFormatting.DARK_GRAY);
        TribeDefinition tribe = FamiliarRoster.voiceTribe(familiar.profile());
        return tribe == null
                ? Component.translatable("gui.tribalpower.familiar.bondable").withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.tribalpower.familiar.bondable_voice", tribe.displayNameComponent()).withStyle(ChatFormatting.YELLOW);
    }
}
