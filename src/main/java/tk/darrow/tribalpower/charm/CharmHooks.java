package tk.darrow.tribalpower.charm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritgearHelper;

import java.util.HashSet;
import java.util.Set;

/** Worn-charm effects, including creative-style flight from an Air voice. */
public final class CharmHooks {
    private static final java.util.Map<java.util.UUID, Boolean> PULSE_OK = new java.util.HashMap<>();

    private CharmHooks() {}

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        Set<Attunement> voices = new HashSet<>();
        int cost = 0;
        boolean flight = false;
        for (ItemStack charm : CharmSlots.equipped(player)) {
            voices.addAll(SpiritCharmItem.voices(charm));
            cost += SpiritCharmItem.pulseCost(charm);
            if (SpiritCharmItem.grantsFlight(charm)) flight = true;
        }
        boolean due = player.level().getGameTime() % 40 == 0;
        boolean last = PULSE_OK.getOrDefault(player.getUUID(), true);
        boolean paid;
        if (cost <= 0 || player.getAbilities().instabuild) {
            paid = true;
        } else if (!due && last) {
            paid = true;
        } else {
            paid = SpiritgearHelper.tryConsumePulse(player, cost);
            PULSE_OK.put(player.getUUID(), paid);
        }
        if (!paid) {
            setFlight(player, false);
            return;
        }
        if (player.level().getGameTime() % 80 == 0) apply(player, voices);
        applyKinds(player);
        if (voices.contains(Attunement.LOOM) && player.level().getGameTime() % 40 == 0
                && player.getRandom().nextFloat() < 0.30F && cost > 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof PulseCellItem) {
                    PulseCellItem.insertPulse(stack, Math.min(2, cost), false);
                    break;
                }
            }
        }
        setFlight(player, flight);
    }

    private static void applyKinds(Player player) {
        long time = player.level().getGameTime();
        for (ItemStack charm : CharmSlots.equipped(player)) {
            if (!(charm.getItem() instanceof SpiritCharmItem item)) continue;
            if (item.kind == CharmKind.HEARTH && time % 80 == 0) {
                player.getFoodData().eat(1, 0.4F);
            }
            if (item.kind == CharmKind.VEIL && player.isShiftKeyDown()) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
            }
        }
    }

    private static void apply(Player player, Set<Attunement> voices) {
        if (voices.contains(Attunement.FIRE))
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false, true));
        if (voices.contains(Attunement.WATER)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
        }
        if (voices.contains(Attunement.EARTH))
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, false, true));
        if (voices.contains(Attunement.SPIRIT))
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        if (voices.contains(Attunement.LOOM))
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false, true));
        if (voices.contains(Attunement.AIR) && !voices.contains(Attunement.FIRE))
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
    }

    private static void setFlight(Player player, boolean allow) {
        boolean creative = player.getAbilities().instabuild || player.isSpectator();
        boolean mayfly = creative || allow;
        if (player.getAbilities().mayfly == mayfly) {
            if (!mayfly) player.getAbilities().flying = false;
            return;
        }
        player.getAbilities().mayfly = mayfly;
        if (!mayfly) player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Set<Attunement> voices = new HashSet<>();
        boolean ward = false;
        for (ItemStack charm : CharmSlots.equipped(player)) {
            voices.addAll(SpiritCharmItem.voices(charm));
            if (charm.getItem() instanceof SpiritCharmItem item && item.kind == CharmKind.WARD) ward = true;
        }
        if (ward && voices.contains(Attunement.EARTH) && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && player.getRandom().nextFloat() < (voices.contains(Attunement.SPIRIT) ? 0.50F : 0.25F)) {
            event.setCanceled(true);
        }
        if (voices.contains(Attunement.FIRE) && event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.igniteForSeconds(3);
        }
    }

    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                || player.getAbilities().instabuild) return;
        CharmInventory inv = CharmSlots.of(player);
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            ItemStack stack = inv.removeItemNoUpdate(i);
            if (!stack.isEmpty()) player.drop(stack, true, false);
        }
        inv.clearContent();
    }
}
