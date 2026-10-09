package tk.darrow.tribalpower.charm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritgearHelper;

import java.util.HashSet;
import java.util.Set;

/** Worn-charm effects, including creative-style flight from an Air voice. */
public final class CharmHooks {
    private static final java.util.Map<java.util.UUID, Boolean> PULSE_OK = new java.util.HashMap<>();

    private CharmHooks() {}

    /**
     * Whether the player's worn charms are paid for right now: their last upkeep came out of a cell (or they need
     * none). An unpaid charm gives nothing, the Lantern's longer blessings included.
     */
    public static boolean upkeepPaid(Player player) {
        return player.getAbilities().instabuild || PULSE_OK.getOrDefault(player.getUUID(), true);
    }

    /** Forget a player's last Pulse verdict and last reading of their slots when they log out, so the maps cannot grow without bound. */
    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        PULSE_OK.remove(event.getEntity().getUUID());
        WORN.remove(event.getEntity().getUUID());
    }

    /**
     * What a player's worn charms came to when they were last read. voices() parses each charm's tag, and
     * reading three slots for every player every tick was the cost of this handler, so the slots are read
     * again only on the upkeep beat, or sooner when a charm is put on or taken off (the slot then holds a
     * different stack). A voice bound onto a worn charm between beats is counted from the next beat.
     */
    private static final class Worn {
        final ItemStack[] slots = new ItemStack[CharmInventory.SIZE];
        Set<Attunement> voices;   // null when nothing is worn
        int cost;
        boolean flight;
        int gathering;

        boolean sameSlots(CharmInventory worn) {
            for (int i = 0; i < CharmInventory.SIZE; i++) if (worn.getItem(i) != slots[i]) return false;
            return true;
        }
    }
    private static final java.util.Map<java.util.UUID, Worn> WORN = new java.util.HashMap<>();

    private static Worn read(Player player, CharmInventory worn) {
        Worn out = new Worn();
        int perVoice = TribalConfig.charmUpkeepPerVoice();
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            ItemStack charm = worn.getItem(i);
            out.slots[i] = charm;
            if (charm.isEmpty()) continue;
            Set<Attunement> theirs = SpiritCharmItem.voices(charm);
            // Cost and pull radius count every voice; the shared effects skip a Gathering Charm's own Air.
            Set<Attunement> effects = charm.getItem() instanceof SpiritCharmItem item
                    ? SpiritCharmItem.effectVoices(item.kind, theirs) : theirs;
            if (out.voices == null) out.voices = new HashSet<>();
            out.voices.addAll(effects);
            // an attuned familiar near you carries part of its voice's upkeep (FamiliarBoost)
            out.cost += tk.darrow.tribalpower.familiar.FamiliarBoost.charmCost(player, theirs, perVoice * Math.max(1, theirs.size()));
            if (effects.contains(Attunement.AIR)) out.flight = true;
            if (charm.getItem() instanceof SpiritCharmItem item && item.kind == CharmKind.GATHERING)
                out.gathering = Math.max(out.gathering, Math.max(1, theirs.size()));
        }
        return out;
    }

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        CharmInventory worn = CharmSlots.of(player);
        long time = player.level().getGameTime();
        boolean due = time % TribalConfig.charmUpkeepBeatTicks() == 0;
        Worn read = WORN.get(player.getUUID());
        if (read == null || due || !read.sameSlots(worn)) {
            read = read(player, worn);
            WORN.put(player.getUUID(), read);
        }
        Set<Attunement> voices = read.voices;
        if (voices == null) {
            // Nothing worn: no Pulse to spend, no effects to apply, and flight to take back if it was given.
            setFlight(player, false);
            return;
        }
        int cost = read.cost;
        if (cost > 0 && tk.darrow.tribalpower.effect.ModEffects.hasBoon(player, tk.darrow.tribalpower.tribe.TribeDefinition.SIGIL))
            cost = Math.max(1, (int) Math.round(cost * (1 - TribalConfig.sigilBoonDiscount())));
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
        if (time % 80 == 0) apply(player, voices);
        applyKinds(player, worn);
        if (read.gathering > 0) gather(player, read.gathering);
        if (voices.contains(Attunement.LOOM) && due && cost > 0
                && player.getRandom().nextFloat() < TribalConfig.charmLoomRefundChance()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof PulseCellItem) {
                    PulseCellItem.insertPulse(stack, Math.min(TribalConfig.charmLoomRefundCap(), cost), false);
                    break;
                }
            }
        }
        setFlight(player, read.flight);
    }

    private static void applyKinds(Player player, CharmInventory worn) {
        long time = player.level().getGameTime();
        if (time % 80 != 0 && !player.isShiftKeyDown()) return;
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            ItemStack charm = worn.getItem(i);
            if (!(charm.getItem() instanceof SpiritCharmItem item)) continue;
            if (item.kind == CharmKind.HEARTH && time % 80 == 0) {
                player.getFoodData().eat(TribalConfig.hearthCharmNutrition(),
                        (float) TribalConfig.hearthCharmSaturation());
            }
            if (item.kind == CharmKind.VEIL && player.isShiftKeyDown()) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
            }
        }
    }

    // Cross-mod convention (conveyors, other magnets): an entity carrying this flag must not be moved remotely.
    private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";

    /** Gathering Charm reach: the base with one voice, a step more per extra voice, never past the cap (all in config). */
    public static double gatherRadius(int voices) {
        return Math.min(TribalConfig.gatherReachCap(),
                TribalConfig.gatherReach() + TribalConfig.gatherReachPerVoice() * (Math.max(1, voices) - 1));
    }

    /**
     * Gathering Charm: draw dropped items and experience toward the wearer. One box query every other tick,
     * staggered by entity id so players do not all scan on the same tick. Sneaking pauses it so drops stay put.
     * The query only sees the wearer's own level, so nothing in another dimension is touched.
     */
    private static void gather(Player player, int voices) {
        if (player.isShiftKeyDown() || player.isSpectator()) return;
        if ((player.level().getGameTime() + player.getId()) % 2 != 0) return;
        double radius = gatherRadius(voices);
        Vec3 target = player.position().add(0, 0.5, 0);
        java.util.UUID wearer = player.getUUID();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(radius), e -> gatherable(e, wearer))) {
            Vec3 to = target.subtract(entity.position());
            double distance = to.length();
            // Close enough is left to vanilla pickup; pushing on would only shove it through the player.
            if (distance > radius || distance < 0.75) continue;
            // Full pace from afar, easing off in the last couple of blocks so it settles rather than overshoots.
            entity.setDeltaMovement(to.scale(Math.min(0.45, distance * 0.25) / distance));
            entity.hasImpulse = true;   // send the new motion to clients now, not on the item's slow update
        }
    }

    private static boolean gatherable(Entity entity, java.util.UUID wearer) {
        // Type first: getPersistentData() creates NeoForge's tag on whatever it is asked of, so ask only our two kinds.
        if (!entity.isAlive()) return false;
        if (entity instanceof ItemEntity item) {
            // A pickup delay means it was just thrown (or is never to be picked up): leave it where it lands.
            if (item.hasPickUpDelay()) return false;
            // A stack kept for someone else (a /give overflow, say) is theirs to collect, not ours to take.
            if (item.getTarget() != null && !item.getTarget().equals(wearer)) return false;
        } else if (!(entity instanceof ExperienceOrb)) {
            return false;
        }
        // The flag's presence is the request, whatever value it carries.
        return !entity.getPersistentData().contains(PREVENT_REMOTE_MOVEMENT);
    }

    private static MobEffectInstance timed(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks) {
        return new MobEffectInstance(effect, ticks, 0, true, false, true);
    }

    private static void apply(Player player, Set<Attunement> voices) {
        if (voices.contains(Attunement.FIRE))
            player.addEffect(timed(MobEffects.FIRE_RESISTANCE, TribalConfig.charmFireResistanceTicks()));
        if (voices.contains(Attunement.WATER)) {
            player.addEffect(timed(MobEffects.WATER_BREATHING, TribalConfig.charmWaterBreathingTicks()));
            player.addEffect(timed(MobEffects.DOLPHINS_GRACE, TribalConfig.charmDolphinsGraceTicks()));
        }
        if (voices.contains(Attunement.EARTH))
            player.addEffect(timed(MobEffects.DAMAGE_RESISTANCE, TribalConfig.charmResistanceTicks()));
        if (voices.contains(Attunement.SPIRIT))
            player.addEffect(timed(MobEffects.NIGHT_VISION, TribalConfig.charmNightVisionTicks()));
        if (voices.contains(Attunement.LOOM))
            player.addEffect(timed(MobEffects.LUCK, TribalConfig.charmLuckTicks()));
        if (voices.contains(Attunement.AIR) && !voices.contains(Attunement.FIRE))
            player.addEffect(timed(MobEffects.SLOW_FALLING, TribalConfig.charmSlowFallingTicks()));
        tk.darrow.tribalpower.familiar.FamiliarBoost.charmBoons(player, voices);
    }

    // Persisted so a restart mid-flight still revokes; flight granted by anything else is left alone.
    private static final String GRANTED_FLIGHT = "tribalpower:charm_flight";

    private static void setFlight(Player player, boolean allow) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        boolean granted = data.getBoolean(GRANTED_FLIGHT);
        if (allow) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                data.putBoolean(GRANTED_FLIGHT, true);
            }
            return;
        }
        if (!granted) return;
        data.remove(GRANTED_FLIGHT);
        if (player.getAbilities().instabuild || player.isSpectator() || !player.getAbilities().mayfly) return;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Unpaid upkeep switches every other charm effect off in playerTick; the reactive ones follow suit.
        if (!PULSE_OK.getOrDefault(player.getUUID(), true)) return;
        Set<Attunement> voices = new HashSet<>();
        boolean ward = false;
        for (ItemStack charm : CharmSlots.equipped(player)) {
            voices.addAll(SpiritCharmItem.voices(charm));
            if (charm.getItem() instanceof SpiritCharmItem item && item.kind == CharmKind.WARD) ward = true;
        }
        if (ward && voices.contains(Attunement.EARTH) && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && player.getRandom().nextFloat() < (voices.contains(Attunement.SPIRIT)
                        ? TribalConfig.wardBlockChanceSpirit()
                        : TribalConfig.wardBlockChance())) {
            event.setCanceled(true);
        }
        if (voices.contains(Attunement.FIRE) && event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.igniteForSeconds(tk.darrow.tribalpower.familiar.FamiliarBoost.emberSeconds(player,
                    TribalConfig.charmEmberSeconds()));
        }
    }

    public static void drops(LivingDropsEvent event) {
        // A death whose drops another mod already waved off keeps its charms where they are (the slots survive death).
        if (event.isCanceled() || !(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                || player.getAbilities().instabuild) return;
        CharmInventory inv = CharmSlots.of(player);
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            ItemStack stack = inv.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
            item.setPickUpDelay(40);
            float speed = player.getRandom().nextFloat() * 0.5F;
            float angle = player.getRandom().nextFloat() * Mth.TWO_PI;
            item.setDeltaMovement(-Mth.sin(angle) * speed, 0.2F, Mth.cos(angle) * speed);
            event.getDrops().add(item);
        }
        inv.clearContent();
    }
}
