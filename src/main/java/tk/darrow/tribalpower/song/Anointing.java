package tk.darrow.tribalpower.song;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.familiar.FamiliarRoster;

/**
 * A reagent worked into a weapon. The weapon keeps it -- one at a time -- until another reagent is worked in over
 * it at the Song Bench. What it does is the reagent's Note ({@link Anointment}); how strongly is the config's.
 *
 * <p>Stored on {@link DataComponents#CUSTOM_DATA} as {@code Anointed}: the reagent's id. Which weapons take one is
 * the {@code tribalpower:anointable} item tag: Spiritgear weapons, and nothing of vanilla's or another mod's
 * unless a pack opens the tag.
 */
public final class Anointing {
    public static final String KEY = "Anointed";
    public static final TagKey<Item> ANOINTABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "anointable"));

    /** Set while an anointment's own follow-up blow lands, so it cannot set itself off again. */
    private static final ThreadLocal<Boolean> ECHOING = ThreadLocal.withInitial(() -> false);

    private Anointing() {}

    public static boolean canAnoint(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ANOINTABLE);
    }

    public static Optional<CreatureProfile> reagent(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        String id = data.getUnsafe().getString(KEY);
        return id.isEmpty() ? Optional.empty() : Optional.ofNullable(Reagents.byId(id));
    }

    public static Optional<Anointment> anointment(ItemStack stack) {
        return reagent(stack).map(profile -> Anointment.of(Note.of(profile)));
    }

    /** Works a reagent in, replacing whatever was there. */
    public static void anoint(ItemStack stack, CreatureProfile profile) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, profile.reagent));
    }

    private static double value(Anointment anointment, String key) {
        return TribalConfig.anointing(anointment, key);
    }

    /** The anointed weapon behind a melee blow, or empty. Arrows and spells are not the weapon's doing. */
    private static Optional<Player> striker(net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return Optional.of(player);
        return Optional.empty();
    }

    /** Anointments that change the blow itself: fire on the edge, and weight that goes through armour. */
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (ECHOING.get()) return;
        Player player = striker(event.getSource()).orElse(null);
        if (player == null) return;
        Anointment anointment = anointment(player.getMainHandItem()).orElse(null);
        if (anointment == null) return;
        LivingEntity target = event.getEntity();
        switch (anointment) {
            case SEARING -> event.setAmount(event.getAmount() + (float) value(anointment, "bonusDamage"));
            case SUNDERING -> event.setAmount(event.getAmount() + (float) (target.getArmorValue() * value(anointment, "armorFraction")));
            default -> {}
        }
    }

    /** Anointments that follow a blow that landed. */
    public static void dealtDamage(LivingDamageEvent.Post event) {
        if (ECHOING.get() || event.getNewDamage() <= 0) return;
        Player player = striker(event.getSource()).orElse(null);
        if (player == null || player.level().isClientSide) return;
        ItemStack weapon = player.getMainHandItem();
        Anointment anointment = anointment(weapon).orElse(null);
        if (anointment == null) return;
        LivingEntity target = event.getEntity();
        var random = player.getRandom();
        switch (anointment) {
            case SEARING -> target.igniteForSeconds((float) value(anointment, "burnSeconds"));
            case STORMCALL -> {
                if (random.nextDouble() < value(anointment, "chance")) arc(player, target, anointment);
            }
            case FROSTBITE -> {
                int ticks = ticks(value(anointment, "seconds"));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level(value(anointment, "slownessLevel"))), player);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
            }
            case VENOM -> target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks(value(anointment, "seconds")),
                    level(value(anointment, "poisonLevel"))), player);
            case GALE -> {
                double push = value(anointment, "knockback");
                if (push > 0) target.knockback(push, player.getX() - target.getX(), player.getZ() - target.getZ());
                target.setDeltaMovement(target.getDeltaMovement().add(0, value(anointment, "lift"), 0));
                target.hurtMarked = true;
            }
            case ROOTING -> {
                if (random.nextDouble() < value(anointment, "chance")) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks(value(anointment, "seconds")), 9), player);
                    target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                    target.hurtMarked = true;
                    particles(target, ParticleTypes.SPORE_BLOSSOM_AIR);
                }
            }
            case ECHO -> {
                if (random.nextDouble() < value(anointment, "chance")) {
                    float again = (float) (event.getNewDamage() * value(anointment, "fraction"));
                    if (again > 0) echo(() -> {
                        target.invulnerableTime = 0;
                        target.hurt(player.damageSources().playerAttack(player), again);
                    });
                    particles(target, ParticleTypes.ENCHANTED_HIT);
                }
            }
            case SAPPING -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks(value(anointment, "seconds")),
                    level(value(anointment, "weaknessLevel"))), player);
            case BLOODTHIRST -> {
                float heal = (float) (event.getNewDamage() * value(anointment, "lifeSteal"));
                if (heal > 0 && player.getHealth() < player.getMaxHealth()) {
                    player.heal(heal);
                    particles(target, ParticleTypes.DAMAGE_INDICATOR);
                }
            }
            case MENDING -> {
                int repair = (int) Math.round(value(anointment, "repairPerHit"));
                if (repair > 0 && weapon.isDamaged()) weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - repair));
            }
        }
    }

    /** Lightning from the struck target to its nearest hostile neighbours. */
    private static void arc(Player player, LivingEntity from, Anointment anointment) {
        float damage = (float) value(anointment, "arcDamage");
        int targets = (int) Math.round(value(anointment, "arcTargets"));
        double range = value(anointment, "arcRange");
        if (damage <= 0 || targets <= 0) return;
        var near = player.level().getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(range),
                e -> e != player && e != from && e.isAlive() && FamiliarRoster.hostile(e));
        near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        echo(() -> {
            for (LivingEntity next : near.subList(0, Math.min(targets, near.size()))) {
                next.hurt(player.damageSources().indirectMagic(player, player), damage);
                if (player.level() instanceof ServerLevel server) {
                    var a = from.position().add(0, from.getBbHeight() * 0.6, 0);
                    var b = next.position().add(0, next.getBbHeight() * 0.6, 0);
                    for (int i = 0; i <= 8; i++) {
                        var at = a.lerp(b, i / 8.0);
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
        });
    }

    private static void echo(Runnable blow) {
        ECHOING.set(true);
        try {
            blow.run();
        } finally {
            ECHOING.set(false);
        }
    }

    private static void particles(LivingEntity target, net.minecraft.core.particles.SimpleParticleType type) {
        if (target.level() instanceof ServerLevel server)
            server.sendParticles(type, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
    }

    private static int ticks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20));
    }

    /** A config level (1 is Level I) as an effect amplifier. */
    private static int level(double level) {
        return Math.max(0, (int) Math.round(level) - 1);
    }

    /** Every anointable weapon shows what it carries, or that it can carry one. */
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!canAnoint(stack)) return;
        var lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        reagent(stack).ifPresentOrElse(profile -> {
            Anointment anointment = Anointment.of(Note.of(profile));
            lines.add(at, Component.translatable("anointment.tribalpower." + anointment.id() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(at, Component.translatable("item.tribalpower.anointed",
                    Component.translatable("anointment.tribalpower." + anointment.id()),
                    Component.translatable("item.tribalpower." + profile.reagent)).withStyle(ChatFormatting.GOLD));
        }, () -> {
            if (event.getFlags().isAdvanced())
                lines.add(at, Component.translatable("item.tribalpower.anointable").withStyle(ChatFormatting.DARK_GRAY));
        });
    }
}
