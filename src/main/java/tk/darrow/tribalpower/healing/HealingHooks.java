package tk.darrow.tribalpower.healing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.MarchThreat;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.world.ModDimensions;

/** Where shamanic healing meets the rest of the game. */
public final class HealingHooks {
    private HealingHooks() {}

    // ---- Spirit Sickness --------------------------------------------------------------------------------------

    /** Called when a March spirit lands a blow: elites and night spirits may leave the struck player sick. */
    public static void afflict(Mob spirit, LivingEntity target) {
        if (!(target instanceof Player player) || !(spirit.level() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(ModDimensions.THE_MARCH)) return;
        var health = spirit.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        boolean elite = health != null && health.hasModifier(MarchThreat.ELITE);
        boolean night = level.isNight();
        if (!(elite && TribalConfig.sicknessFromElites()) && !(night && TribalConfig.sicknessFromNight())) return;
        if (level.random.nextDouble() >= TribalConfig.sicknessChance()) return;
        sicken(player);
    }

    /** Adds a level of Spirit Sickness, up to the configured most, unless the player is blessed. */
    public static boolean sicken(LivingEntity target) {
        if (target.hasEffect(HealingRegistry.SPIRIT_BLESSING)) return false;
        MobEffectInstance current = target.getEffect(HealingRegistry.SPIRIT_SICKNESS);
        int level = current == null ? 0 : Math.min(TribalConfig.sicknessMaxLevel() - 1, current.getAmplifier() + 1);
        if (current != null) target.removeEffect(HealingRegistry.SPIRIT_SICKNESS);
        target.addEffect(new MobEffectInstance(HealingRegistry.SPIRIT_SICKNESS, TribalConfig.sicknessSeconds() * 20, level));
        if (target instanceof Player player && current == null)
            player.displayClientMessage(Component.translatable("message.tribalpower.spirit_sickness"), true);
        return true;
    }

    // ---- salves on others -------------------------------------------------------------------------------------

    /** A salve used on someone else is laid on them: a friend, a villager, a familiar. Before the mob sees the click. */
    public static void salveOthers(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof RemedyItem remedy) || remedy.form() != Remedies.Form.SALVE) return;
        if (!(event.getTarget() instanceof LivingEntity patient) || !patient.isAlive()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        Remedies.apply(patient, stack);
        event.getLevel().playSound(null, patient.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
        stack.consume(1, event.getEntity());
    }

    // ---- Spirit Remnants --------------------------------------------------------------------------------------

    /** A bonded familiar that falls leaves its remnant where it fell. */
    public static void remnant(LivingDeathEvent event) {
        if (event.isCanceled() || !TribalConfig.familiarRemnants() || event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Familiar familiar) || !familiar.isBonded() || !(event.getEntity() instanceof Mob mob)) return;
        ItemStack remnant = SpiritRemnantItem.of(mob);
        ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ(), remnant);
        drop.setUnlimitedLifetime();
        drop.setGlowingTag(true);
        mob.level().addFreshEntity(drop);
        if (familiar.getOwner() instanceof Player owner)
            owner.displayClientMessage(Component.translatable("message.tribalpower.spirit_remnant.left", mob.getDisplayName()), false);
    }

    // ---- the Sweat Lodge --------------------------------------------------------------------------------------

    /** Sleeping the night through near hot Sweat Stones under a roof cleanses and blesses. */
    public static void wake(PlayerWakeUpEvent event) {
        // Only the morning wake counts: "Leave Bed" and being woken early pass updateLevel or wakeImmediately.
        if (event.wakeImmediately() || event.updateLevel() || !(event.getEntity().level() instanceof ServerLevel level)) return;
        Player player = event.getEntity();
        BlockPos bed = player.getSleepingPos().orElse(null);
        if (bed == null || !lodge(level, bed)) return;
        bless(player);
    }

    /** Whether a bed stands in a sweat lodge: hot stones within reach, and a roof when the config asks for one. */
    public static boolean lodge(ServerLevel level, BlockPos bed) {
        if (TribalConfig.lodgeNeedsRoof() && !roofed(level, bed)) return false;
        int range = TribalConfig.lodgeRange();
        for (BlockPos at : BlockPos.betweenClosed(bed.offset(-range, -2, -range), bed.offset(range, 2, range))) {
            var state = level.getBlockState(at);
            if (state.getBlock() instanceof SweatStonesBlock && state.getValue(SweatStonesBlock.HOT)) return true;
        }
        return false;
    }

    /** A lodge's roof: something solid low over the bed. Read from the blocks, not sky light, which lags a tick. */
    private static boolean roofed(ServerLevel level, BlockPos bed) {
        for (int up = 2; up <= 5; up++)
            if (level.getBlockState(bed.above(up)).blocksMotion()) return true;
        return false;
    }

    public static void bless(LivingEntity body) {
        for (var effect : java.util.List.copyOf(body.getActiveEffects()))
            if (!effect.getEffect().value().isBeneficial()) body.removeEffect(effect.getEffect());
        tk.darrow.tribalpower.effect.ModEffects.cleanse(body);
        int ticks = TribalConfig.lodgeBlessingMinutes() * 60 * 20;
        if (ticks > 0) {
            body.addEffect(new MobEffectInstance(HealingRegistry.SPIRIT_BLESSING, ticks, 0));
            body.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.min(ticks, 600), 0));
        }
        body.setHealth(body.getMaxHealth());
        if (body instanceof Player player) player.displayClientMessage(Component.translatable("message.tribalpower.sweat_lodge"), true);
    }

    // ---- the Healing Circle -----------------------------------------------------------------------------------

    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) HealingCircles.tick(level);
    }
}
