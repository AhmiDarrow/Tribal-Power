package tk.darrow.tribalpower.item;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.healing.HealingRegistry;

/**
 * The Healer's Rattle. Hold it up and shake: every half second it mends whoever you look at -- a friend, a
 * familiar, a villager -- or yourself when you look at no one, spending Pulse from its own cell first as all
 * Spiritgear does. It ranks at the Echo stations; from Bound it also eases Spirit Sickness, and its totem voice
 * adds a gift of its own to every shake.
 */
public class SpiritgearRattleItem extends Item {
    private static final int SHAKE = 10;

    public SpiritgearRattleItem(Properties properties) {
        super(properties.stacksTo(1).durability(SpiritGear.TOOL_DURABILITY));
    }

    @Override public boolean isFoil(ItemStack stack) { return SpiritGear.foil(stack) || super.isFoil(stack); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof Player player) || level.isClientSide || (getUseDuration(stack, user) - remaining) % SHAKE != 0) return;
        if (!player.getAbilities().instabuild && !GearCell.spend(player, stack, TribalConfig.rattleCost())) {
            SpiritgearHelper.notifyStarved(player);
            player.stopUsingItem();
            return;
        }
        LivingEntity patient = patient(player);
        shake(player, stack, patient);
    }

    /** Whoever the healer is looking at within reach, or the healer. */
    public static LivingEntity patient(Player player) {
        double range = TribalConfig.rattleRange();
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1).scale(range));
        AABB sweep = player.getBoundingBox().expandTowards(player.getViewVector(1).scale(range)).inflate(1);
        var hit = ProjectileUtil.getEntityHitResult(player, eye, reach, sweep,
                e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                        && !tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(living)
                        && !(e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof tk.darrow.tribalpower.familiar.Familiar))
                        && player.hasLineOfSight(e), range * range);
        Entity found = hit == null ? null : hit.getEntity();
        return found instanceof LivingEntity living ? living : player;
    }

    /** One shake on one patient. */
    public static void shake(Player healer, ItemStack rattle, LivingEntity patient) {
        int rank = SpiritGear.rank(rattle);
        float heal = (float) (TribalConfig.rattleHeal() * (1 + rank * TribalConfig.rattleRankBonus()));
        Attunement voice = SpiritGear.voice(rattle).orElse(null);
        if (voice == Attunement.WATER) heal *= 1.5F;
        patient.heal(heal);
        if (rank >= 2) {
            var sickness = patient.getEffect(HealingRegistry.SPIRIT_SICKNESS);
            if (sickness != null) {
                patient.removeEffect(HealingRegistry.SPIRIT_SICKNESS);
                if (sickness.getAmplifier() > 0)
                    patient.addEffect(new MobEffectInstance(HealingRegistry.SPIRIT_SICKNESS, sickness.getDuration(), sickness.getAmplifier() - 1));
            }
        }
        if (voice != null) switch (voice) {
            case SPIRIT -> List.copyOf(patient.getActiveEffects()).stream()
                    .filter(effect -> !effect.getEffect().value().isBeneficial()).forEach(effect -> patient.removeEffect(effect.getEffect()));
            case EARTH -> patient.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
            case FIRE -> patient.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            case AIR -> patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
            case LOOM -> patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false, true));
            default -> {}
        }
        if (healer.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY(0.6), patient.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, healer.blockPosition(), SoundEvents.BAMBOO_WOOD_HIT, SoundSource.PLAYERS, 0.5F,
                    1.4F + server.random.nextFloat() * 0.3F);
        }
        SpiritgearHelper.notifyFueled(healer);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        tooltip.add(Component.translatable("item.tribalpower.spiritgear_rattle.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
