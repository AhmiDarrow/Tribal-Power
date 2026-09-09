package tk.darrow.tribalpower.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tribal Kin: one entity type, tribe + role data. Neutral, never despawns, lives within 16 blocks of its camp anchor.
 * NBT: {@code Tribe} int 0-8, {@code Role} string ELDER|DRUMMER|HUNTER|WEAVER, {@code Anchor} long (optional).
 */
public class TribalKinEntity extends PathfinderMob implements Merchant {
    public static final String NBT_TRIBE = TribeDefinition.NBT_KEY;
    public static final String NBT_ROLE = "Role";
    public static final String NBT_ANCHOR = "Anchor";
    public static final int WANDER_RADIUS = 16;
    public static final int HUNT_RADIUS = 12;
    public static final int DRUM_INTERVAL = 120;
    public static final int DRUM_RADIUS = 8;
    public static final int DRUM_PULSE = 2;
    public static final byte EVENT_BEAT = 64;

    private static final EntityDataAccessor<Integer> TRIBE = SynchedEntityData.defineId(TribalKinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(TribalKinEntity.class, EntityDataSerializers.INT);

    @Nullable private BlockPos anchor;
    private int drumTimer;
    @Nullable private UUID angerTarget;
    private long angerUntil;
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    /** Rank the current {@link #offers} were built for; offers are rebuilt only when the customer's rank differs. */
    @Nullable private TribeRank offersRank;
    /** Uses spent per entry of {@link TribeDefinition#trades()}; persisted so closing the screen does not restock. */
    private int[] tradeUses = new int[0];
    /** Minecraft day of the last restock: like villagers, Elders restock once per day. */
    private long restockDay = -1;
    /** Client-side beat animation countdown. */
    public int beatTicks;

    public TribalKinEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRIBE, 0);
        builder.define(ROLE, KinRole.WEAVER.ordinal());
    }

    // ---- data ----

    public TribeDefinition tribe() { return TribeDefinition.byOrdinal(entityData.get(TRIBE)); }
    public void setTribe(TribeDefinition tribe) { entityData.set(TRIBE, tribe.ordinal()); }
    public KinRole role() { return KinRole.byOrdinal(entityData.get(ROLE)); }
    public void setRole(KinRole role) { entityData.set(ROLE, role.ordinal()); }
    public BlockPos anchor() { return anchor == null ? blockPosition() : anchor; }
    public void setAnchor(BlockPos pos) { anchor = pos.immutable(); restrictTo(anchor, WANDER_RADIUS); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true) {
            @Override public boolean canUse() { return role() == KinRole.HUNTER && super.canUse(); }
        });
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new AngerTargetGoal());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> role() == KinRole.HUNTER && e instanceof Enemy && e.distanceToSqr(this) <= HUNT_RADIUS * HUNT_RADIUS));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data) {
        if (anchor == null) setAnchor(blockPosition());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (beatTicks > 0) beatTicks--;
            return;
        }
        if (anchor == null) setAnchor(blockPosition());
        else if (!hasRestriction()) restrictTo(anchor, WANDER_RADIUS);
        if (role() == KinRole.DRUMMER && ++drumTimer >= DRUM_INTERVAL) {
            drumTimer = random.nextInt(20);
            drum();
        }
        if (angerTarget != null && level().getGameTime() > angerUntil) { angerTarget = null; setTarget(null); }
    }

    /** Drummer beat: basedrum note, tribe-coloured ring and 2 Pulse into each generator within 8 blocks. */
    public void drum() {
        if (!(level() instanceof ServerLevel server)) return;
        server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.NEUTRAL, 0.8F, 0.85F);
        server.broadcastEntityEvent(this, EVENT_BEAT);
        DustParticleOptions dust = new DustParticleOptions(tribe().particleColour(), 0.9F);
        double phase = server.getGameTime() * 0.05;
        for (int i = 0; i < 12; i++) {
            double a = phase + i * Math.PI * 2 / 12;
            server.sendParticles(dust, getX() + Math.cos(a) * 1.4, getY() + 0.3, getZ() + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
        }
        BlockPos origin = blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -DRUM_RADIUS; dx <= DRUM_RADIUS; dx++)
            for (int dy = -DRUM_RADIUS; dy <= DRUM_RADIUS; dy++)
                for (int dz = -DRUM_RADIUS; dz <= DRUM_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = server.hasChunkAt(cursor) ? server.getBlockEntity(cursor) : null;
                    if (be instanceof PulseHandler handler && (be instanceof DrumheartBlockEntity
                            || be instanceof LeyCollectorBlockEntity || be instanceof PulseResonatorBlockEntity))
                        handler.insertPulse(DRUM_PULSE, false);
                }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BEAT) beatTicks = 10;
        else super.handleEntityEvent(id);
    }

    /** 0..1 beat animation progress for the model. */
    public float beatProgress(float partial) {
        return beatTicks <= 0 ? 0 : Math.max(0, (beatTicks - partial) / 10F);
    }

    // ---- anger: hurting Kin turns the camp's Hunters on the attacker for 60 s ----

    public void setAngryAt(@Nullable LivingEntity target, int ticks) {
        if (target == null) { angerTarget = null; return; }
        angerTarget = target.getUUID();
        angerUntil = level().getGameTime() + ticks;
        if (role() == KinRole.HUNTER) setTarget(target);
    }

    public boolean isAngryAt(Entity entity) {
        return angerTarget != null && angerTarget.equals(entity.getUUID()) && level().getGameTime() <= angerUntil;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof ServerPlayer player && !player.isCreative()) {
            TribeStanding.add(player, tribe(), TribeStanding.LOSS_HURT_KIN);
            for (TribalKinEntity kin : level().getEntitiesOfClass(TribalKinEntity.class, getBoundingBox().inflate(24),
                    k -> k.tribe() == tribe()))
                kin.setAngryAt(player, TribeStanding.HUNTER_ANGER_TICKS);
        }
        return hurt;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return (role() == KinRole.HUNTER || isAngryAt(target)) && super.canAttack(target);
    }

    // ---- interaction ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        TribeDefinition tribe = tribe();
        if (!TribeHooks.hasMet(sp, tribe)) {
            TribeHooks.markMet(sp, tribe);
            sp.sendSystemMessage(Component.translatable("message.tribalpower.kin.greeting", tribe.displayNameComponent())
                    .withStyle(TribeStanding.colour(tribe)));
            sp.sendSystemMessage(tribe.marginComponent().withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.GRAY));
        }
        TribeRank rank = TribeRank.of(tk.darrow.tribalpower.camp.identity.CampStanding.effectiveStanding(sp, tribe));
        if (role() == KinRole.ELDER) {
            if (rank == TribeRank.VOICE) {
                TribeStandingSavedData data = TribeStandingSavedData.get(sp.server);
                if (!data.hasMark(sp.getUUID(), tribe)) {
                    data.grantMark(sp.getUUID(), tribe);
                    ItemStack mark = tribe.stamped(TribeRegistry.TRIBE_MARK.get());
                    if (!sp.getInventory().add(mark)) sp.drop(mark, false);
                    sp.sendSystemMessage(Component.translatable("message.tribalpower.kin.mark", tribe.displayNameComponent())
                            .withStyle(TribeStanding.colour(tribe)));
                    tk.darrow.tribalpower.camp.CampHooks.award(sp.serverLevel(), sp.getUUID(), "tribes/mark");
                }
            }
            if (getTradingPlayer() == null) {
                MerchantOffers offers = offersFor(rank);
                if (offers.isEmpty()) {
                    sp.displayClientMessage(Component.translatable("message.tribalpower.kin.stranger", tribe.displayNameComponent()), true);
                    return InteractionResult.CONSUME;
                }
                setTradingPlayer(sp);
                openTradingScreen(sp, Component.translatable("entity.tribalpower.tribal_kin.elder", tribe.displayNameComponent()), rank.ordinal());
            }
            return InteractionResult.CONSUME;
        }
        sp.displayClientMessage(Component.translatable("message.tribalpower.kin.role." + role().id(),
                tribe.displayNameComponent(), Component.translatable(rank.translationKey())), true);
        return InteractionResult.CONSUME;
    }

    // ---- Merchant ----

    /** Fresh offers for {@code rank} with no uses spent (does not touch the Elder's persisted stock). */
    public MerchantOffers buildOffers(TribeRank rank) {
        return buildOffers(rank, new int[0]);
    }

    private MerchantOffers buildOffers(TribeRank rank, int[] uses) {
        MerchantOffers list = new MerchantOffers();
        List<TribeDefinition.Offer> all = tribe().trades();
        for (int i = 0; i < all.size(); i++) {
            TribeDefinition.Offer spec = all.get(i);
            if (rank.ordinal() < spec.rank().ordinal()) continue;
            ItemStack cost = spec.cost().get();
            Optional<ItemCost> costB = spec.costB() == null ? Optional.empty()
                    : Optional.of(new ItemCost(spec.costB().get().getItem(), spec.costB().get().getCount()));
            int used = i < uses.length ? Math.min(uses[i], spec.maxUses()) : 0;
            list.add(new MerchantOffer(new ItemCost(cost.getItem(), cost.getCount()), costB, spec.result().get(), used, spec.maxUses(), 2, 0.05F));
        }
        return list;
    }

    /**
     * The Elder's stock for a customer of {@code rank}: uses spent survive closing the screen, saving and reloading,
     * and rank changes (the same six offers are just filtered); stock restocks once per Minecraft day.
     */
    public MerchantOffers offersFor(TribeRank rank) {
        int count = tribe().trades().size();
        if (tradeUses.length != count) tradeUses = Arrays.copyOf(tradeUses, count);
        long day = level().getDayTime() / 24000L;
        if (restockDay != day) { restockDay = day; Arrays.fill(tradeUses, 0); offers = null; }
        if (offers == null || offersRank != rank) { offers = buildOffers(rank, tradeUses); offersRank = rank; }
        return offers;
    }

    /** Index into {@link TribeDefinition#trades()} of a live offer, or -1. */
    private int tradeIndex(MerchantOffer offer) {
        if (offers == null || offersRank == null) return -1;
        int slot = offers.indexOf(offer);
        if (slot < 0) return -1;
        List<TribeDefinition.Offer> all = tribe().trades();
        for (int i = 0, seen = 0; i < all.size(); i++) {
            if (offersRank.ordinal() < all.get(i).rank().ordinal()) continue;
            if (seen++ == slot) return i;
        }
        return -1;
    }

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) return offersFor(offersRank == null ? TribeRank.STRANGER : offersRank);
        return offers;
    }

    @Override public void overrideOffers(MerchantOffers offers) { this.offers = offers; }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        int index = tradeIndex(offer);
        if (index >= 0 && index < tradeUses.length) tradeUses[index] = offer.getUses();
        if (tradingPlayer instanceof ServerPlayer sp) {
            TribeStanding.add(sp, tribe(), TribeStanding.GAIN_TRADE);
        }
    }

    @Override public void notifyTradeUpdated(ItemStack stack) { ambientSoundTime = -getAmbientSoundInterval(); }
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide; }

    // ---- persistence ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_TRIBE, tribe().ordinal());
        tag.putString(NBT_ROLE, role().name());
        if (anchor != null) tag.putLong(NBT_ANCHOR, anchor.asLong());
        tag.putIntArray("TradeUses", tradeUses);
        tag.putLong("RestockDay", restockDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTribe(TribeDefinition.byOrdinal(tag.getInt(NBT_TRIBE)));
        if (tag.contains(NBT_ROLE, net.minecraft.nbt.Tag.TAG_STRING)) setRole(KinRole.byName(tag.getString(NBT_ROLE)));
        else if (tag.contains(NBT_ROLE)) setRole(KinRole.byOrdinal(tag.getInt(NBT_ROLE)));
        if (tag.contains(NBT_ANCHOR)) setAnchor(BlockPos.of(tag.getLong(NBT_ANCHOR)));
        tradeUses = tag.getIntArray("TradeUses").clone();
        restockDay = tag.contains("RestockDay") ? tag.getLong("RestockDay") : -1;
        offers = null;
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean isPersistenceRequired() { return true; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }
    @Override public int getAmbientSoundInterval() { return 240; }
    @Override protected SoundEvent getAmbientSound() { return role() == KinRole.DRUMMER ? null : SoundEvents.VILLAGER_AMBIENT; }

    @Override
    public Component getTypeName() {
        return Component.translatable("entity.tribalpower.tribal_kin." + role().id(), tribe().displayNameComponent());
    }

    // ---- goals ----

    /** Elders stand still and face their customer while a trade screen is open. */
    private final class TradeGoal extends Goal {
        TradeGoal() { setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE)); }
        @Override public boolean canUse() {
            Player p = getTradingPlayer();
            return isAlive() && p != null && p.isAlive() && !p.isSpectator() && p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu
                    && distanceToSqr(p) <= 16 && !isInWater();
        }
        @Override public void start() { getNavigation().stop(); }
        @Override public void stop() { setTradingPlayer(null); }
        @Override public void tick() { Player p = getTradingPlayer(); if (p != null) getLookControl().setLookAt(p, 30F, 30F); }
    }

    /** Targets the player the camp is angry at while the anger timer runs. */
    private final class AngerTargetGoal extends TargetGoal {
        AngerTargetGoal() { super(TribalKinEntity.this, false, false); setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            if (role() != KinRole.HUNTER || angerTarget == null || level().getGameTime() > angerUntil) return false;
            Player p = level().getPlayerByUUID(angerTarget);
            if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator() || distanceToSqr(p) > 24 * 24) return false;
            return TargetingConditions.forCombat().range(24).test(TribalKinEntity.this, p);
        }
        @Override public void start() { setTarget(angerTarget == null ? null : level().getPlayerByUUID(angerTarget)); super.start(); }
        @Override public boolean canContinueToUse() { return canUse() && super.canContinueToUse(); }
    }

    /** Camp-wide search helper used by hooks. */
    public static AABB campBox(BlockPos center, double radius) { return new AABB(center).inflate(radius); }
}
