package tk.darrow.tribalpower.boss;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.world.structure.SilentDrumBlockEntity;

/**
 * The Unsung — an ancestor spirit shaped like a hollow standing drum (design §3).
 * <p>Phases by health: <b>Beat</b> (100–66%): drumbeat shockwaves every 3 s, melee swipes.
 * <b>Chorus</b> (66–33%): summons Echo Weavers every 8 s (max 6 alive), shockwaves every 2.5 s.
 * <b>Silence</b> (33–0%): invulnerable, floats over the Silent Drum and fires slow Echo bolts; the four-beat
 * rhythm on the drum resyncs it — stunned {@link #STUN_TICKS} taking double damage, then silence resumes
 * {@link #RESYNC_WINDOW} after the resync. Resets and despawns when no player stays within 48 blocks for 30 s.
 */
public class TheUnsungEntity extends Monster {
    public static final int PHASE_BEAT = 1, PHASE_CHORUS = 2, PHASE_SILENCE = 3;
    public static final int STUN_TICKS = 160, RESYNC_WINDOW = 240;
    public static final int SHOCK_INTERVAL_BEAT = 60, SHOCK_INTERVAL_CHORUS = 50;
    public static final int SUMMON_INTERVAL = 160, MAX_WEAVERS = 6, BOLT_INTERVAL = 40;
    public static final double SHOCK_RADIUS = 8, RESET_RANGE = 48;
    public static final int RESET_TICKS = 600, XP = 200;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(TheUnsungEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STUNNED = SynchedEntityData.defineId(TheUnsungEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SWIPE = SynchedEntityData.defineId(TheUnsungEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAT = SynchedEntityData.defineId(TheUnsungEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.tribalpower.the_unsung"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private BlockPos drumPos;
    private int shockTimer = 40, summonTimer = 60, boltTimer = 30, stunTicks, vulnerableTicks, awayTicks, lastPhase;
    private final List<UUID> weavers = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();

    public TheUnsungEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = XP;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 400).add(Attributes.ARMOR, 8).add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_BEAT);
        builder.define(STUNNED, false);
        builder.define(SWIPE, -100);
        builder.define(BEAT, -100);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SwipeGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------ state

    public void bindDrum(BlockPos pos) { drumPos = pos == null ? null : pos.immutable(); }
    public BlockPos drumPos() { return drumPos; }
    public int phase() { return entityData.get(PHASE); }
    public boolean isStunned() { return entityData.get(STUNNED); }
    public boolean inSilence() { return phase() == PHASE_SILENCE; }
    /** Game time of the last swipe / shockwave, for client animation. */
    public int swipeTime() { return entityData.get(SWIPE); }
    public int beatTime() { return entityData.get(BEAT); }
    public boolean vulnerable() { return !inSilence() || vulnerableTicks > 0; }

    public static int phaseFor(float health, float max) {
        float f = health / Math.max(1F, max);
        return f > 0.66F ? PHASE_BEAT : f > 0.33F ? PHASE_CHORUS : PHASE_SILENCE;
    }

    /** Four-beat rhythm on the Silent Drum: during Silence this stuns it and opens the double-damage window. */
    public boolean resync() {
        if (!inSilence() || stunTicks > 0 || vulnerableTicks > 0) return false;
        stunTicks = STUN_TICKS;
        vulnerableTicks = RESYNC_WINDOW;
        entityData.set(STUNNED, true);
        bolts.clear();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2F, 0.5F);
            server.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2F, 0.7F);
            SpiritEffects.ring(server, position().add(0, 2, 0), Attunement.LOOM, 3, 24);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5, getZ(), 40, 1.2, 1.5, 1.2, 0.05);
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RESET_RANGE)))
                player.displayClientMessage(Component.translatable("message.tribalpower.unsung.resync"), false);
        }
        return true;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) level();
        int phase = phaseFor(getHealth(), getMaxHealth());
        if (phase != entityData.get(PHASE)) entityData.set(PHASE, phase);
        if (phase != lastPhase) enterPhase(server, phase);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        tickReset(server);
        if (stunTicks > 0) {
            stunTicks--;
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            getNavigation().stop();
            if (tickCount % 5 == 0) server.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 3.5, getZ(), 3, 0.8, 0.4, 0.8, 0.02);
            if (stunTicks == 0) entityData.set(STUNNED, false);
        }
        if (vulnerableTicks > 0) vulnerableTicks--;
        tickBolts(server);
        switch (phase) {
            case PHASE_BEAT -> { setNoGravity(false); if (stunTicks == 0 && --shockTimer <= 0) { shockwave(server); shockTimer = SHOCK_INTERVAL_BEAT; } }
            case PHASE_CHORUS -> {
                setNoGravity(false);
                if (stunTicks == 0 && --shockTimer <= 0) { shockwave(server); shockTimer = SHOCK_INTERVAL_CHORUS; }
                if (--summonTimer <= 0) { summonWeavers(server); summonTimer = SUMMON_INTERVAL; }
            }
            default -> {
                setNoGravity(true);
                getNavigation().stop();
                if (stunTicks == 0) {
                    floatToDrum();
                    if (--boltTimer <= 0) { fireBolt(server); boltTimer = BOLT_INTERVAL; }
                }
                if (vulnerableTicks == 0 && tickCount % 4 == 0)
                    server.sendParticles(new DustParticleOptions(SpiritEffects.color(Attunement.SPIRIT), 1.2F), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 3, 1, 0, 0, 0, 0);
            }
        }
        if (tickCount % 20 == 0 && phase != PHASE_SILENCE) {
            SpiritEffects.ring(server, position().add(0, 0.2, 0), Attunement.SPIRIT, 1.8, 10);
        }
    }

    private void enterPhase(ServerLevel server, int phase) {
        lastPhase = phase;
        String key = switch (phase) { case PHASE_BEAT -> "beat"; case PHASE_CHORUS -> "chorus"; default -> "silence"; };
        bossEvent.setName(Component.translatable("entity.tribalpower.the_unsung").append(" — ").append(Component.translatable("boss.tribalpower.unsung.phase." + key)));
        if (tickCount > 1) {
            server.playSound(null, blockPosition(), phase == PHASE_SILENCE ? SoundEvents.WITHER_AMBIENT : SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2F, phase == PHASE_SILENCE ? 0.4F : 0.6F);
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RESET_RANGE)))
                player.displayClientMessage(Component.translatable("message.tribalpower.unsung.phase." + key), false);
        }
        if (phase == PHASE_SILENCE) { bolts.clear(); boltTimer = 30; }
    }

    private void tickReset(ServerLevel server) {
        Player near = server.getNearestPlayer(this, RESET_RANGE);
        if (near == null || near.isSpectator()) awayTicks++; else awayTicks = 0;
        if (awayTicks >= RESET_TICKS) resetAndDespawn(server);
    }

    /** No one stayed: the drum falls silent again and may be struck anew. */
    public void resetAndDespawn(ServerLevel server) {
        for (UUID id : weavers) if (server.getEntity(id) instanceof Mob weaver) weaver.discard();
        weavers.clear();
        if (drumPos != null && server.getBlockEntity(drumPos) instanceof SilentDrumBlockEntity drum) drum.onBossReset();
        server.playSound(null, blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1F, 0.4F);
        discard();
    }

    private void shockwave(ServerLevel server) {
        entityData.set(BEAT, (int) server.getGameTime());
        Vec3 centre = position();
        for (int r = 2; r <= (int) SHOCK_RADIUS; r += 2) SpiritEffects.ring(server, centre.add(0, 0.3, 0), Attunement.SPIRIT, r, 6 * r);
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2.5, getZ(), 1, 0, 0, 0, 0);
        server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 3F, 0.45F);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 0.5F);
        for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SHOCK_RADIUS))) {
            if (player.isSpectator() || player.isCreative()) continue;
            double dx = player.getX() - getX(), dz = player.getZ() - getZ();
            if (dx * dx + dz * dz > SHOCK_RADIUS * SHOCK_RADIUS) continue;
            boolean braced = player.isShiftKeyDown() || player.isCrouching();
            player.hurt(damageSources().mobAttack(this), braced ? 3F : 6F);
            if (!braced) { player.knockback(1.3, -dx, -dz); player.hurtMarked = true; }
            else player.displayClientMessage(Component.translatable("message.tribalpower.unsung.braced"), true);
        }
    }

    /** Living Echo Weavers this Unsung summoned (stale ids are dropped). */
    public int weaversAlive(ServerLevel server) {
        weavers.removeIf(id -> !(server.getEntity(id) instanceof LivingEntity living) || !living.isAlive());
        return weavers.size();
    }

    private void summonWeavers(ServerLevel server) {
        int alive = weaversAlive(server);
        for (int i = 0; i < 2 && alive < MAX_WEAVERS; i++) {
            Mob weaver = CreatureEntities.type(CreatureProfile.ECHO_WEAVER).create(server);
            if (weaver == null) return;
            double a = random.nextDouble() * Math.PI * 2, d = 3 + random.nextDouble() * 2;
            weaver.moveTo(getX() + Math.cos(a) * d, getY() + 0.5, getZ() + Math.sin(a) * d, random.nextFloat() * 360F, 0);
            weaver.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            weaver.setPersistenceRequired();
            if (getTarget() != null) weaver.setTarget(getTarget());
            server.addFreshEntity(weaver);
            weavers.add(weaver.getUUID());
            alive++;
            server.sendParticles(ParticleTypes.SOUL, weaver.getX(), weaver.getY() + 0.5, weaver.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        }
        server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2F, 0.5F);
    }

    private void floatToDrum() {
        Vec3 target = drumPos != null ? Vec3.atBottomCenterOf(drumPos).add(0, 1.2, 0) : position();
        Vec3 delta = target.subtract(position());
        Vec3 motion = delta.length() > 0.2 ? delta.normalize().scale(Math.min(0.12, delta.length() * 0.1)) : Vec3.ZERO;
        setDeltaMovement(motion.add(0, Math.sin(tickCount * 0.08) * 0.01, 0));
        hurtMarked = true;
    }

    private void fireBolt(ServerLevel server) {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) target = server.getNearestPlayer(this, RESET_RANGE);
        if (target == null) return;
        entityData.set(SWIPE, (int) server.getGameTime());
        Vec3 from = position().add(0, 2.8, 0);
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
        bolts.add(new Bolt(from, dir.scale(0.32)));
        server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    private void tickBolts(ServerLevel server) {
        if (bolts.isEmpty()) return;
        DustParticleOptions dust = new DustParticleOptions(SpiritEffects.color(Attunement.LOOM), 1.1F);
        var it = bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            bolt.pos = bolt.pos.add(bolt.vel);
            bolt.life++;
            server.sendParticles(dust, bolt.pos.x, bolt.pos.y, bolt.pos.z, 2, 0.08, 0.08, 0.08, 0);
            if (bolt.life % 2 == 0) server.sendParticles(ParticleTypes.END_ROD, bolt.pos.x, bolt.pos.y, bolt.pos.z, 1, 0, 0, 0, 0);
            boolean hit = false;
            for (Player player : server.getEntitiesOfClass(Player.class, new AABB(bolt.pos, bolt.pos).inflate(1.0))) {
                if (player.isSpectator()) continue;
                if (player.hurt(damageSources().indirectMagic(this, this), 5F)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                }
                hit = true;
            }
            if (hit || bolt.life > 120 || server.getBlockState(BlockPos.containing(bolt.pos)).isSolid()) {
                SpiritEffects.ring(server, bolt.pos, Attunement.LOOM, 0.8, 8);
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ combat

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (super.isInvulnerableTo(source)) return true;
        return inSilence() && vulnerableTicks <= 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isInvulnerableTo(source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel server && source.getEntity() instanceof Player player) {
                server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1F, 0.4F);
                player.displayClientMessage(Component.translatable("message.tribalpower.unsung.silent"), true);
            }
            return false;
        }
        if (stunTicks > 0) amount *= 2F;
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) entityData.set(SWIPE, (int) level().getGameTime());
        return hit;
    }

    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) { }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean canRide(Entity vehicle) { return false; }
    @Override public boolean addEffect(MobEffectInstance effect, Entity source) { return false; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean isPersistenceRequired() { return true; }

    // ------------------------------------------------------------------ boss bar & sounds

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }
    @Override public void remove(RemovalReason reason) { super.remove(reason); bossEvent.removeAllPlayers(); }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!dead) return; // a cancelled LivingDeathEvent leaves it alive: keep its Weavers and say nothing
        if (level() instanceof ServerLevel server) {
            for (UUID id : weavers) if (server.getEntity(id) instanceof Mob weaver) weaver.discard();
            weavers.clear();
            SpiritEffects.ring(server, position().add(0, 1, 0), Attunement.LOOM, 6, 24);
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2, getZ(), 60, 1.5, 1.5, 1.5, 0.05);
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RESET_RANGE)))
                player.displayClientMessage(Component.translatable("message.tribalpower.unsung.stilled"), false);
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_HEARTBEAT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.NOTE_BLOCK_BASEDRUM.value(); }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override protected float getSoundVolume() { return 1.6F; }
    @Override public float getVoicePitch() { return 0.55F; }
    @Override public int getAmbientSoundInterval() { return 60; }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (drumPos != null) tag.putLong("Drum", drumPos.asLong());
        tag.putInt("Stun", stunTicks);
        tag.putInt("Vulnerable", vulnerableTicks);
        tag.putInt("Away", awayTicks);
        ListTag list = new ListTag();
        for (UUID id : weavers) list.add(NbtUtils.createUUID(id));
        tag.put("Weavers", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        drumPos = tag.contains("Drum") ? BlockPos.of(tag.getLong("Drum")) : null;
        stunTicks = Math.max(0, tag.getInt("Stun"));
        vulnerableTicks = Math.max(0, tag.getInt("Vulnerable"));
        awayTicks = Math.max(0, tag.getInt("Away"));
        weavers.clear();
        for (Tag id : tag.getList("Weavers", Tag.TAG_INT_ARRAY)) weavers.add(NbtUtils.loadUUID(id));
        entityData.set(STUNNED, stunTicks > 0);
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    private static final class Bolt {
        Vec3 pos, vel; int life;
        Bolt(Vec3 pos, Vec3 vel) { this.pos = pos; this.vel = vel; }
    }

    /** Melee arm swipe, only while it still walks (Beat and Chorus) and is not stunned. */
    private final class SwipeGoal extends MeleeAttackGoal {
        SwipeGoal() { super(TheUnsungEntity.this, 1.0, true); setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        @Override public boolean canUse() { return !inSilence() && stunTicks == 0 && super.canUse(); }
        @Override public boolean canContinueToUse() { return !inSilence() && stunTicks == 0 && super.canContinueToUse(); }
    }
}
