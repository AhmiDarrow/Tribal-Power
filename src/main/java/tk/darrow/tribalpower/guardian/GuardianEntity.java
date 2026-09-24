package tk.darrow.tribalpower.guardian;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.quest.QuestEvents;

/**
 * One class for all eight guardians. Which one it is comes from its entity type; what it does comes from
 * {@link Guardian#ability}. Every guardian: a boss bar in its tribe's colour, no knockback, no leash, no urn, a
 * second phase at half health that sharpens its attack and calls its biome's creatures (who drop nothing), a
 * reset when left alone, and on death the trial of its tribe for everyone who fought it.
 */
public class GuardianEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WARDED = SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String SUMMONED_TAG = "tribalpower:summoned";
    public static final int RESET_RANGE = 48;
    private static final int SNARE_RANGE = 12, BOLT_RANGE = 24;

    private Guardian guardian;
    private final ServerBossEvent bossEvent;
    private final List<UUID> adds = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private BlockPos altarPos;
    private int abilityTicks, addTicks, wardTicks, chargeTicks, awayTicks;

    public GuardianEntity(EntityType<? extends GuardianEntity> type, Level level) {
        super(type, level);
        Guardian guardian = guardian();
        this.bossEvent = new ServerBossEvent(Component.translatable(guardian.nameKey()), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 120;
        setPersistenceRequired();
        if (guardian.flying) {
            this.moveControl = new FlyingMoveControl(this, 12, true);
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Guardian g) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, g.health * TribalConfig.guardianHealthScale())
                .add(Attributes.ARMOR, g.armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, g.speed)
                .add(Attributes.FLYING_SPEED, g.speed * 1.6)
                .add(Attributes.ATTACK_DAMAGE, g.damage * TribalConfig.guardianDamageScale())
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(ATTACK, 0);
        builder.define(WARDED, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (!guardian().flying) return super.createNavigation(level);
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override public boolean canUse() { return !isWarded() && chargeTicks <= 0 && super.canUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    /** Which guardian this is; resolved from the type, because the super constructor asks before any field is set. */
    public Guardian guardian() {
        if (guardian == null) guardian = GuardianRegistry.guardianOf(getType());
        return guardian;
    }

    public int phase() { return entityData.get(PHASE); }
    public int attackTime() { return entityData.get(ATTACK); }
    public boolean isWarded() { return entityData.get(WARDED); }
    public void bindAltar(BlockPos pos) { altarPos = pos; }
    public BlockPos altar() { return altarPos; }
    public int addsAlive() {
        if (!(level() instanceof ServerLevel server)) return adds.size();
        adds.removeIf(id -> !(server.getEntity(id) instanceof LivingEntity living) || !living.isAlive());
        return adds.size();
    }

    private void markAttack(ServerLevel server) { entityData.set(ATTACK, (int) server.getGameTime()); }

    // ---- the fight -----------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (phase() == 1 && getHealth() <= getMaxHealth() * 0.5F) enterSecondPhase(server);
        tickReset(server);
        if (guardian().flying) hover(server);
        if (wardTicks > 0 && --wardTicks == 0) entityData.set(WARDED, false);
        if (chargeTicks > 0) tickCharge(server);
        tickBolts(server);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        int interval = Math.max(20, (int) (TribalConfig.guardianAbilityInterval() * (phase() == 2 ? 0.6 : 1.0)));
        if (++abilityTicks >= interval) {
            abilityTicks = 0;
            useAbility(server, target);
        }
        if (phase() == 2 && ++addTicks >= TribalConfig.guardianAddsInterval()) {
            addTicks = 0;
            summonAdds(server);
        }
        if (phase() == 2 && guardian().ability == Guardian.Ability.SWEEP && wardTicks == 0 && server.random.nextInt(200) == 0) ward(server);
    }

    private void enterSecondPhase(ServerLevel server) {
        entityData.set(PHASE, 2);
        bossEvent.setName(Component.translatable(guardian().nameKey()).append(" — ").append(Component.translatable("boss.tribalpower.guardian.phase2")));
        server.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2F, 0.6F);
        SpiritEffects.ring(server, position().add(0, 1, 0), guardian().tribe.attunement(), 5, 24);
        for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RESET_RANGE)))
            player.displayClientMessage(Component.translatable("message.tribalpower.guardian." + guardian().id + ".phase2").withStyle(ChatFormatting.RED), false);
        summonAdds(server);
    }

    /** Public so the trial can be proven without a whole fight. */
    public void useAbility(ServerLevel server, LivingEntity target) {
        boolean sharp = phase() == 2;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (guardian().ability) {
            case SLAM -> {
                markAttack(server);
                double radius = sharp ? 8 : 6;
                for (int r = 2; r <= (int) radius; r += 2) SpiritEffects.ring(server, position().add(0, 0.3, 0), Attunement.FIRE, r, 6 * r);
                server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1F, 0.5F);
                for (Player player : players(server, radius)) {
                    boolean braced = player.isShiftKeyDown();
                    player.hurt(damageSources().mobAttack(this), braced ? damage * 0.3F : damage * 0.7F);
                    if (!braced) { player.knockback(1.2, getX() - player.getX(), getZ() - player.getZ()); player.hurtMarked = true; }
                    if (sharp) player.igniteForSeconds(4);
                }
            }
            case SNARE -> {
                if (distanceToSqr(target) > SNARE_RANGE * SNARE_RANGE) return;
                markAttack(server);
                SpiritEffects.beam(server, position().add(0, 1.5, 0), target.position().add(0, 1, 0), Attunement.EARTH);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, sharp ? 3 : 2));
                if (sharp) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                Vec3 pull = position().subtract(target.position()).normalize().scale(sharp ? 0.9 : 0.6);
                target.push(pull.x, 0.3, pull.z);
                target.hurtMarked = true;
                target.hurt(damageSources().mobAttack(this), damage * 0.4F);
                server.playSound(null, blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.HOSTILE, 1.5F, 0.5F);
            }
            case FROST -> {
                markAttack(server);
                Vec3 look = getLookAngle();
                double reach = sharp ? 10 : 8;
                for (int i = 1; i <= (int) reach; i++)
                    server.sendParticles(ParticleTypes.SNOWFLAKE, getX() + look.x * i, getY() + 1.5 + look.y * i, getZ() + look.z * i, 6, 0.5, 0.5, 0.5, 0.02);
                for (LivingEntity hit : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach), e -> e != this && !(e instanceof GuardianEntity))) {
                    if (hit instanceof Player player && (player.isSpectator() || player.isCreative())) continue;
                    if (hit.getTags().contains(SUMMONED_TAG)) continue;
                    Vec3 to = hit.position().subtract(position()).normalize();
                    if (to.dot(look) < 0.5) continue;
                    hit.hurt(damageSources().freeze(), damage * 0.5F);
                    hit.setTicksFrozen(Math.max(hit.getTicksFrozen(), sharp ? 200 : 140));
                    hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                }
                server.playSound(null, blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.5F, 0.5F);
            }
            case BOLT -> {
                if (distanceToSqr(target) > BOLT_RANGE * BOLT_RANGE) return;
                markAttack(server);
                Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
                Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
                bolts.add(new Bolt(from, dir.scale(0.5), damage * (sharp ? 0.8F : 0.5F)));
                if (sharp) bolts.add(new Bolt(from, dir.yRot(0.25F).scale(0.5), damage * 0.4F));
                server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F, 0.6F);
            }
            case SWEEP -> {
                markAttack(server);
                double radius = sharp ? 5 : 4;
                SpiritEffects.ring(server, position().add(0, 1, 0), Attunement.SPIRIT, radius, 24);
                server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1F, 0.5F);
                for (Player player : players(server, radius)) {
                    player.hurt(damageSources().mobAttack(this), damage * 0.6F);
                    player.knockback(sharp ? 2.2 : 1.6, getX() - player.getX(), getZ() - player.getZ());
                    player.hurtMarked = true;
                }
            }
            case TIDE -> {
                markAttack(server);
                double radius = sharp ? 10 : 8;
                for (int r = 2; r <= (int) radius; r += 2) SpiritEffects.ring(server, position().add(0, 0.3, 0), Attunement.WATER, r, 6 * r);
                server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 3F, 0.5F);
                for (Player player : players(server, radius)) {
                    Vec3 pull = position().subtract(player.position()).normalize().scale(sharp ? 1.1 : 0.8);
                    player.push(pull.x, 0.25, pull.z);
                    player.hurtMarked = true;
                    player.hurt(damageSources().mobAttack(this), damage * 0.4F);
                    if (sharp) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            }
            case CHARGE -> {
                markAttack(server);
                chargeTicks = sharp ? 30 : 22;
                server.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.7F);
            }
            case SWOOP -> {
                if (distanceToSqr(target) > 14 * 14) return;
                markAttack(server);
                Vec3 dash = target.position().subtract(position()).normalize().scale(1.4);
                setDeltaMovement(dash.x, dash.y * 0.5, dash.z);
                hurtMarked = true;
                for (LivingEntity hit : server.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5), e -> e != this)) {
                    if (hit instanceof Player player && (player.isSpectator() || player.isCreative())) continue;
                    if (hit.getTags().contains(SUMMONED_TAG)) continue;
                    hit.hurt(damageSources().mobAttack(this), damage * 0.5F);
                    hit.push(0, sharp ? 1.3 : 1.0, 0);
                    hit.hurtMarked = true;
                }
                server.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 2F, 0.6F);
                if (sharp) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                    if (bolt != null) {
                        bolt.moveTo(target.position());
                        bolt.setVisualOnly(true);
                        server.addFreshEntity(bolt);
                        for (Player player : server.getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(3)))
                            if (!player.isCreative() && !player.isSpectator()) player.hurt(damageSources().lightningBolt(), damage * 0.4F);
                    }
                }
            }
        }
    }

    private List<Player> players(ServerLevel server, double radius) {
        List<Player> out = new ArrayList<>();
        for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius))) {
            if (player.isSpectator() || player.isCreative()) continue;
            double dx = player.getX() - getX(), dz = player.getZ() - getZ();
            if (dx * dx + dz * dz <= radius * radius) out.add(player);
        }
        return out;
    }

    private void tickCharge(ServerLevel server) {
        chargeTicks--;
        LivingEntity target = getTarget();
        if (target == null) { chargeTicks = 0; return; }
        Vec3 dir = target.position().subtract(position()).normalize();
        setDeltaMovement(dir.x * 1.1, getDeltaMovement().y, dir.z * 1.1);
        hurtMarked = true;
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 4, 0.6, 0.1, 0.6, 0.02);
        for (LivingEntity hit : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8), e -> e != this && !e.getTags().contains(SUMMONED_TAG))) {
            if (hit instanceof Player player && (player.isSpectator() || player.isCreative())) continue;
            hit.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            hit.knockback(2.0, getX() - hit.getX(), getZ() - hit.getZ());
            hit.hurtMarked = true;
            chargeTicks = 0;
        }
    }

    private void hover(ServerLevel server) {
        LivingEntity target = getTarget();
        double wantY = (target != null ? target.getY() : getY()) + 3.5;
        if (target == null && altarPos != null) wantY = altarPos.getY() + 4;
        double dy = wantY - getY();
        if (Math.abs(dy) > 0.5 && chargeTicks <= 0) setDeltaMovement(getDeltaMovement().add(0, Math.signum(dy) * 0.04, 0));
        if (server.getGameTime() % 10 == 0) server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 2, 0.8, 0.2, 0.8, 0.0);
    }

    private void ward(ServerLevel server) {
        wardTicks = 60;
        entityData.set(WARDED, true);
        server.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.4F);
        SpiritEffects.ring(server, position().add(0, 1, 0), Attunement.SPIRIT, 2.5, 16);
    }

    /** Calls its biome's creatures to its side; they drop nothing and go when it does. */
    public void summonAdds(ServerLevel server) {
        int max = TribalConfig.guardianMaxAdds();
        int alive = addsAlive();
        for (int i = 0; i < TribalConfig.guardianAddsPerWave() && alive < max; i++) {
            Mob add = CreatureEntities.type(guardian().adds).create(server);
            if (add == null) return;
            double angle = server.random.nextDouble() * Math.PI * 2;
            add.moveTo(getX() + Math.cos(angle) * 3, getY() + 0.5, getZ() + Math.sin(angle) * 3, server.random.nextFloat() * 360F, 0);
            add.addTag(SUMMONED_TAG);
            add.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            add.setPersistenceRequired();
            server.addFreshEntity(add);
            adds.add(add.getUUID());
            alive++;
            server.sendParticles(ParticleTypes.SOUL, add.getX(), add.getY() + 0.5, add.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        }
    }

    private void tickBolts(ServerLevel server) {
        if (bolts.isEmpty()) return;
        bolts.removeIf(bolt -> {
            bolt.pos = bolt.pos.add(bolt.vel);
            if (++bolt.life > 60) return true;
            server.sendParticles(ParticleTypes.END_ROD, bolt.pos.x, bolt.pos.y, bolt.pos.z, 2, 0.05, 0.05, 0.05, 0.0);
            for (Player player : server.getEntitiesOfClass(Player.class, new AABB(bolt.pos, bolt.pos).inflate(0.9))) {
                if (player.isSpectator() || player.isCreative()) continue;
                player.hurt(damageSources().mobAttack(this), bolt.damage);
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                return true;
            }
            return !server.getBlockState(BlockPos.containing(bolt.pos)).isAir() && server.getBlockState(BlockPos.containing(bolt.pos)).isSolid();
        });
    }

    private void tickReset(ServerLevel server) {
        boolean anyone = !server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RESET_RANGE), p -> !p.isSpectator()).isEmpty();
        awayTicks = anyone ? 0 : awayTicks + 1;
        if (awayTicks >= TribalConfig.guardianResetSeconds() * 20) {
            dismissAdds(server);
            if (altarPos != null && server.getBlockEntity(altarPos) instanceof GuardianAltarBlockEntity altar) altar.onGuardianGone();
            discard();
        }
    }

    private void dismissAdds(ServerLevel server) {
        for (UUID id : adds) if (server.getEntity(id) instanceof Mob add) add.discard();
        adds.clear();
    }

    // ---- the rules every boss keeps ------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isWarded() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.5, getZ(), 8, 0.5, 0.5, 0.5, 0.1);
            return false;
        }
        if (source.getEntity() != null && source.getEntity().getTags().contains(SUMMONED_TAG)) return false;
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) {}
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean isPreventingPlayerRest(Player player) { return true; }
    @Override public void travel(Vec3 input) {
        if (guardian().flying && !level().isClientSide) {
            moveRelative(0.1F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            calculateEntityAnimation(false);
        } else super.travel(input);
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }
    @Override public void remove(RemovalReason reason) { super.remove(reason); bossEvent.removeAllPlayers(); }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!dead || !(level() instanceof ServerLevel server)) return;
        dismissAdds(server);
        SpiritEffects.ring(server, position().add(0, 1, 0), guardian().tribe.attunement(), 6, 24);
        server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 60, 1.5, 1.5, 1.5, 0.05);
        if (altarPos != null && server.getBlockEntity(altarPos) instanceof GuardianAltarBlockEntity altar) altar.onGuardianGone();
        java.util.Set<ServerPlayer> fought = new java.util.LinkedHashSet<>(server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(RESET_RANGE)));
        if (source.getEntity() instanceof ServerPlayer killer) fought.add(killer);
        for (ServerPlayer player : fought) {
            player.displayClientMessage(Component.translatable("message.tribalpower.guardian." + guardian().id + ".fallen").withStyle(ChatFormatting.GOLD), false);
            QuestEvents.trial(player, guardian().tribe);
            tk.darrow.tribalpower.camp.CampHooks.award(server, player.getUUID(), "march/guardian_" + guardian().id);
        }
    }

    @Override protected SoundEvent getAmbientSound() {
        return switch (guardian().ability) {
            case SLAM -> SoundEvents.BLAZE_AMBIENT;
            case SNARE -> SoundEvents.RAVAGER_AMBIENT;
            case FROST -> SoundEvents.STRAY_AMBIENT;
            case BOLT -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case SWEEP -> SoundEvents.WARDEN_HEARTBEAT;
            case TIDE -> SoundEvents.DROWNED_AMBIENT;
            case CHARGE -> SoundEvents.HOGLIN_AMBIENT;
            case SWOOP -> SoundEvents.PHANTOM_AMBIENT;
        };
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return guardian().ability == Guardian.Ability.SWEEP ? SoundEvents.ANVIL_PLACE : SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override protected float getSoundVolume() { return 1.5F; }
    @Override public float getVoicePitch() { return 0.6F; }

    // ---- persistence ---------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (altarPos != null) tag.putLong("Altar", altarPos.asLong());
        tag.putInt("Phase", phase());
        tag.putInt("Away", awayTicks);
        ListTag list = new ListTag();
        for (UUID id : adds) list.add(NbtUtils.createUUID(id));
        tag.put("Adds", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Altar")) altarPos = BlockPos.of(tag.getLong("Altar"));
        entityData.set(PHASE, Math.max(1, tag.getInt("Phase")));
        awayTicks = tag.getInt("Away");
        adds.clear();
        for (Tag id : tag.getList("Adds", Tag.TAG_INT_ARRAY)) adds.add(NbtUtils.loadUUID(id));
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    private static final class Bolt {
        Vec3 pos, vel; final float damage; int life;
        Bolt(Vec3 pos, Vec3 vel, float damage) { this.pos = pos; this.vel = vel; this.damage = damage; }
    }
}
