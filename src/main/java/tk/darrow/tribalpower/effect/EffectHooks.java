package tk.darrow.tribalpower.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.camp.identity.CampStanding;
import tk.darrow.tribalpower.charm.CharmInventory;
import tk.darrow.tribalpower.charm.CharmKind;
import tk.darrow.tribalpower.charm.CharmSlots;
import tk.darrow.tribalpower.charm.SpiritCharmItem;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.item.SpiritgearHelper;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/** Where the spirit layer touches the rest of the game: the halves of blessings and boons that are moments, not attributes. */
public final class EffectHooks {
    public static final ResourceKey<Structure> SPIRIT_WELL = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "spirit_well"));

    private EffectHooks() {}

    // ---- every few seconds ---------------------------------------------------------------------------------------

    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (now % 100 == 0) standingBoons(player);
        if (now % 20 == 0 && player.isShiftKeyDown() && player.isInWater()) spiritWell(player);
    }

    /** Kin standing with a tribe carries its boon for as long as it lasts. */
    private static void standingBoons(ServerPlayer player) {
        if (!TribalConfig.boonsFromStanding()) return;
        int need = TribeRank.KIN.threshold();
        for (TribeDefinition tribe : TribeDefinition.values())
            if (CampStanding.effectiveStanding(player, tribe) >= need || tk.darrow.tribalpower.quest.QuestRegistry.carriesRelic(player, tribe))
                ModEffects.grantBoon(player, tribe, 140, true);
    }

    /**
     * Washing in a Spirit Well: sneak in its water with Pulse in a carried cell and every affliction is lifted,
     * Spirit Sickness included, for the Pulse the well asks.
     */
    private static void spiritWell(ServerPlayer player) {
        boolean afflicted = player.hasEffect(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_SICKNESS);
        for (var affliction : ModEffects.AFFLICTIONS.values()) afflicted |= player.hasEffect(affliction);
        if (!afflicted) return;
        ServerLevel level = player.serverLevel();
        var wells = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(SPIRIT_WELL);
        if (wells.isEmpty() || !level.structureManager().getStructureWithPieceAt(player.blockPosition(), wells.get().value()).isValid()) return;
        int price = TribalConfig.spiritWellPulse();
        if (!player.getAbilities().instabuild && price > 0 && !SpiritgearHelper.tryConsumePulse(player, price)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.spirit_well.need_pulse", price), true);
            return;
        }
        ModEffects.cleanse(player);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, player.getX(), player.getY(0.5), player.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
        player.displayClientMessage(Component.translatable("message.tribalpower.spirit_well.cleansed"), true);
    }

    // ---- blessings ----------------------------------------------------------------------------------------------

    /** Air falls softly; Claw's Edge-walkers never fall while they sneak. */
    public static void fall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (ModEffects.hasBoon(entity, TribeDefinition.CLAW) && entity.isShiftKeyDown()) {
            event.setCanceled(true);
            return;
        }
        int air = ModEffects.blessingLevel(entity, Attunement.AIR);
        if (air > 0) event.setDistance(Math.max(0, event.getDistance() - (float) TribalConfig.airBlessingFall() * air));
    }

    /** Earth digs faster. */
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        int earth = ModEffects.blessingLevel(event.getEntity(), Attunement.EARTH);
        if (earth > 0) event.setNewSpeed(event.getNewSpeed() * (float) (1 + TribalConfig.earthBlessingMining() * earth));
    }

    /** Fire's blows set the target alight. */
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) return;
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) return;
        int fire = ModEffects.blessingLevel(attacker, Attunement.FIRE);
        if (fire > 0 && !event.getEntity().fireImmune()) event.getEntity().igniteForSeconds((float) TribalConfig.fireBlessingBurn() * fire);
    }

    // ---- boons ----------------------------------------------------------------------------------------------------

    /** Stone's Grit-singers get a little more from the ore they break. */
    public static void blockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || !ModEffects.hasBoon(player, TribeDefinition.STONE)) return;
        if (!event.getState().is(Tags.Blocks.ORES) || event.getLevel().random.nextDouble() >= TribalConfig.stoneBoonChance()) return;
        for (ItemEntity drop : java.util.List.copyOf(event.getDrops())) {
            ItemStack extra = drop.getItem().copyWithCount(1);
            event.getDrops().add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), extra));
            break;
        }
    }

    /** Swarm's Colony-keepers are on good terms with every hive. */
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Bee && event.getNewAboutToBeSetTarget() instanceof Player player && ModEffects.hasBoon(player, TribeDefinition.SWARM)) {
            event.setCanceled(true);
            ((Bee) event.getEntity()).setRemainingPersistentAngerTime(0);
        }
    }

    /** Soil's Pad-keepers never trample a field. */
    public static void trample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof Player player && ModEffects.hasBoon(player, TribeDefinition.SOIL)) event.setCanceled(true);
    }

    /** Whether an Echo station within reach of a Clock boon should hurry. */
    public static boolean clockNear(ServerLevel level, BlockPos pos) {
        int reach = TribalConfig.clockBoonReach();
        return !level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(reach),
                p -> ModEffects.hasBoon(p, TribeDefinition.CLOCK)).isEmpty();
    }

    // ---- charms ---------------------------------------------------------------------------------------------------

    /** A worn Lantern charm keeps a blessing or boon lit longer. */
    public static void effectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var effect = event.getEffectInstance().getEffect().value();
        if (!(effect instanceof VoiceBlessingEffect) && !(effect instanceof TribeBoonEffect)) return;
        if (!wearsLantern(player)) return;
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.isInfiniteDuration()) return;
        int longer = (int) Math.round(instance.getDuration() * TribalConfig.lanternCharmStretch());
        instance.update(new MobEffectInstance(instance.getEffect(), longer, instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()));
    }

    private static boolean wearsLantern(Player player) {
        CharmInventory worn = CharmSlots.of(player);
        for (int i = 0; i < CharmInventory.SIZE; i++)
            if (worn.getItem(i).getItem() instanceof SpiritCharmItem charm && charm.kind == CharmKind.LANTERN) return true;
        return false;
    }
}
