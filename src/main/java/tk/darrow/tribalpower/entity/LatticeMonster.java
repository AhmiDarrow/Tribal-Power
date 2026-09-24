package tk.darrow.tribalpower.entity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.familiar.FamiliarAbilities;
import tk.darrow.tribalpower.familiar.FamiliarData;
import tk.darrow.tribalpower.familiar.FamiliarFollowGoal;
import tk.darrow.tribalpower.familiar.FamiliarOwnerTargetGoals;
import tk.darrow.tribalpower.familiar.FamiliarRoster;
import tk.darrow.tribalpower.familiar.FamiliarSitGoal;
import tk.darrow.tribalpower.familiar.FamiliarSlots;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.world.ModDimensions;

public class LatticeMonster extends Monster implements Familiar {
    public static final int POUCH_SLOTS=5;
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER=SynchedEntityData.defineId(LatticeMonster.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SITTING=SynchedEntityData.defineId(LatticeMonster.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BABY=SynchedEntityData.defineId(LatticeMonster.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STATS=SynchedEntityData.defineId(LatticeMonster.class,EntityDataSerializers.INT);
    private final FamiliarData lattice=new FamiliarData();
    private final SimpleContainer pouch=new SimpleContainer(POUCH_SLOTS);
    private int age,inLove,forageCooldown,sitTicks;
    private UUID lastOwnerAttacker;
    private BlockPos lastClick;

    public LatticeMonster(EntityType<? extends Monster> type,Level level) {
        super(type,level);xpReward=profile().health>=35?8:5;
        if(profile().flying) { moveControl=new FlyingMoveControl(this,12,true);setNoGravity(true); }
    }
    @Override public CreatureProfile profile() { return CreatureProfile.of(getType()); }
    @Override public FamiliarData lattice() { return lattice; }
    public void applyLattice() { lattice.apply(this,profile());entityData.set(DATA_STATS,lattice.pack()); }
    @Override public int syncedStats() { return entityData.get(DATA_STATS); }
    public void ensureLattice(RandomSource random,boolean march) {
        if(lattice.rolled())return;
        lattice.rollWild(profile(),random,march);
        applyLattice();
    }
    public SimpleContainer pouch() { return pouch; }
    public UUID lastOwnerAttacker() { return lastOwnerAttacker; }
    public void setLastOwnerAttacker(UUID id) { lastOwnerAttacker=id; }
    public BlockPos lastClick() { return lastClick; }
    public void setLastClick(BlockPos pos) { lastClick=pos; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER,Optional.empty());
        builder.define(DATA_SITTING,false);
        builder.define(DATA_BABY,false);
        builder.define(DATA_STATS,0);
    }
    @Override protected PathNavigation createNavigation(Level level) {
        return profile().flying?new FlyingPathNavigation(this,level):super.createNavigation(level);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new FamiliarSitGoal(this));
        goalSelector.addGoal(2,profile().ranged()?new SpiritCastGoal():new MeleeAttackGoal(this,1,false) {
            @Override public boolean canUse() { return !isBaby() && (!isBonded() || FamiliarRoster.combat(profile())) && super.canUse(); }
        });
        goalSelector.addGoal(3,new FamiliarFollowGoal(this,1.1));
        goalSelector.addGoal(5,profile().flying?new WaterAvoidingRandomFlyingGoal(this,.8) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        }:new WaterAvoidingRandomStrollGoal(this,.8) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        });
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new FamiliarOwnerTargetGoals.HurtBy(this));
        targetSelector.addGoal(2,new FamiliarOwnerTargetGoals.OwnerHurtBy(this));
        targetSelector.addGoal(3,new FamiliarOwnerTargetGoals.OwnerHurt(this));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,Player.class,true) {
            // Bred stock stays peaceful (persistent), but a name tag also sets persistence and must not tame a wild hostile.
            @Override public boolean canUse() {
                return !isBonded() && !isBaby() && (!isPersistenceRequired() || hasCustomName()) && super.canUse();
            }
        });
    }

    @Override public Optional<UUID> ownerUUID() { return entityData.get(DATA_OWNER); }
    @Override public boolean isBonded() { return ownerUUID().isPresent(); }
    @Override public boolean isOwnedBy(Entity entity) { return entity!=null && ownerUUID().map(id->id.equals(entity.getUUID())).orElse(false); }
    @Override public Player getOwner() { return ownerUUID().map(id->level().getPlayerByUUID(id)).orElse(null); }
    @Override public void bond(Player owner) { entityData.set(DATA_OWNER,Optional.of(owner.getUUID()));setSitting(false);setPersistenceRequired();setTarget(null); }
    @Override public boolean isSitting() { return entityData.get(DATA_SITTING); }
    @Override public void setSitting(boolean sitting) {
        entityData.set(DATA_SITTING,sitting);
        if(sitting) { getNavigation().stop();setTarget(null); }
    }
    @Override public boolean unableToMoveToOwner() {
        return isSitting() || isPassenger() || isVehicle() || mayBeLeashed() || (getOwner()!=null && getOwner().isSpectator());
    }
    @Override public boolean isBaby() { return entityData.get(DATA_BABY); }
    public void setBabyFlag(boolean baby) {
        entityData.set(DATA_BABY,baby);
        if(baby && age>=0)age=-24000;
        if(!baby && age<0)age=0;
        if(baby)setPersistenceRequired();
        refreshDimensions();
    }
    @Override public boolean isPersistenceRequired() { return super.isPersistenceRequired() || isBonded() || isBaby(); }
    @Override public boolean requiresCustomPersistence() { return super.requiresCustomPersistence() || isBonded() || isBaby(); }
    /** Elites are worth triple; read from their saved modifier so it survives a reload. */
    @Override protected int getBaseExperienceReward() {
        var health=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        int base=super.getBaseExperienceReward();
        return health!=null && health.hasModifier(MarchThreat.ELITE) ? base*tk.darrow.tribalpower.config.TribalConfig.eliteXpMultiplier() : base;
    }
    @Override public boolean removeWhenFarAway(double distance) { return !isPersistenceRequired() && super.removeWhenFarAway(distance); }
    /** A boss's summoned creatures drop nothing: the fight is the reward, not the adds. */
    @Override protected boolean shouldDropLoot() { return !getTags().contains(tk.darrow.tribalpower.guardian.GuardianEntity.SUMMONED_TAG) && super.shouldDropLoot(); }
    @Override public boolean shouldDropExperience() { return !getTags().contains(tk.darrow.tribalpower.guardian.GuardianEntity.SUMMONED_TAG) && super.shouldDropExperience(); }
    @Override protected boolean shouldDespawnInPeaceful() { return !isPersistenceRequired(); }
    /** Caps a spawn attempt's pack, whatever the spawn table asks for. */
    @Override public int getMaxSpawnClusterSize() { return tk.darrow.tribalpower.config.TribalConfig.maxGroupSize(); }
    /** By default only a spirit already hunting you keeps you from sleep; one wandering past the wall does not. */
    @Override public boolean isPreventingPlayerRest(Player player) {
        return !isPersistenceRequired() && (!tk.darrow.tribalpower.config.TribalConfig.onlyHuntersBlockSleep() || getTarget() == player || profile().boss())
                && super.isPreventingPlayerRest(player);
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions=super.getDefaultDimensions(pose);
        return isBaby()?dimensions.scale(0.5F):dimensions;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isBonded() && isOwnedBy(source.getEntity()))return false;
        if(!level().isClientSide && isSitting() && amount>0)FamiliarSlots.tryFollow(this);
        return super.hurt(source,amount);
    }
    @Override public boolean doHurtTarget(Entity entity) {
        if(entity instanceof LivingEntity living && FamiliarOwnerTargetGoals.forbidden(this,living))return false;
        boolean hit=super.doHurtTarget(entity);
        if(hit && entity instanceof LivingEntity target) {
            applyVoice(target);
            if(!isBonded())tk.darrow.tribalpower.healing.HealingHooks.afflict(this,target);
        }
        return hit;
    }
    @Override public boolean isAlliedTo(Entity other) {
        if(isBonded()) {
            if(other instanceof Player player && isOwnedBy(player))return true;
            if(other instanceof Familiar fam && fam.isBonded() && ownerUUID().equals(fam.ownerUUID()))return true;
        }
        return super.isAlliedTo(other);
    }

    public boolean canCastAt(LivingEntity target) { return target.isAlive() && distanceToSqr(target)<=144 && hasLineOfSight(target); }
    /** What this spirit's attack leaves on whoever it hits. Public so tests can prove it without a fight. */
    public void applyVoice(LivingEntity target) {
        switch(profile().attack) {
            case "ember","bolt" -> target.igniteForSeconds(2);
            case "root","chill" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
            case "weave" -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
                // Loom-torn: a weaver's touch frays what holds you together.
                if(target instanceof Player && !isBonded())tk.darrow.tribalpower.effect.ModEffects.afflict(target,
                        tk.darrow.tribalpower.effect.AfflictionEffect.Kind.FRAYED,tk.darrow.tribalpower.config.TribalConfig.frayedSeconds()*20,0);
            }
            case "venom" -> target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
            case "weaken" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,0));
            case "shove","gust" -> { target.knockback(.45,getX()-target.getX(),getZ()-target.getZ());target.hurtMarked=true; }
            default -> { }
        }
    }

    public boolean isFood(ItemStack stack) { return !stack.isEmpty() && stack.is(CreatureItems.REAGENTS.get(profile()).get()); }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        ItemStack tool=player.getItemInHand(hand);
        // a Ley Lens read comes before any sneak-use of the creature's own: the game asks the creature first
        if(player.isSecondaryUseActive() && tool.getItem() instanceof tk.darrow.tribalpower.ley.LeyLensItem)return InteractionResult.PASS;
        if(tool.is(Items.BRUSH) && isBonded() && !isBaby()) {
            if(!level().isClientSide && forageCooldown==0) {
                spawnAtLocation(new ItemStack(CreatureItems.REAGENTS.get(profile()).get(),2));
                forageCooldown=1200;
                tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
                level().playSound(null,blockPosition(),SoundEvents.BRUSH_GENERIC,SoundSource.NEUTRAL,.7F,1.1F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(isFood(tool) && !isBaby()) {
            // Its own reagent tames a wild remnant in time (Voice permitting) and heals a hurt companion.
            InteractionResult fed=tk.darrow.tribalpower.familiar.FamiliarCare.feed(this,player,hand,tool);
            if(fed!=null)return fed;
        }
        if(isFood(tool) && isBaby()) {
            if(!level().isClientSide) {
                if(!player.getAbilities().instabuild){tool.shrink(1);if(tool.isEmpty())player.getInventory().removeItem(tool);}
                int grow=Math.max(1,(int)((-age / 20) * 0.1F));
                age=Math.min(0,age+grow*20);
                if(age==0)setBabyFlag(false);
                level().broadcastEntityEvent(this,(byte)18);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(isBonded() && isOwnedBy(player)) {
            if(isFood(tool) && !isBaby()) {
                if(!level().isClientSide && inLove<=0 && age==0) {
                    if(!player.getAbilities().instabuild){tool.shrink(1);if(tool.isEmpty())player.getInventory().removeItem(tool);}
                    inLove=600;
                    level().broadcastEntityEvent(this,(byte)18);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if(player.isSecondaryUseActive() && !isFood(tool)) {
                if(profile()==CreatureProfile.ECHO_WEAVER && tool.isEmpty() && !pouch.isEmpty()) {
                    if(!level().isClientSide)dumpPouch(player);
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
                if(!level().isClientSide) {
                    if(isSitting()) {
                        boolean followed=FamiliarSlots.tryFollow(this);
                        player.displayClientMessage(Component.translatable(followed?"message.tribalpower.familiar.follow":"message.tribalpower.familiar.stay",getDisplayName()),false);
                    } else {
                        setSitting(true);
                        player.displayClientMessage(Component.translatable("message.tribalpower.familiar.stay",getDisplayName()),false);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        return super.mobInteract(player,hand);
    }
    public void dumpPouch(Player player) {
        for(int i=0;i<POUCH_SLOTS;i++) {
            ItemStack stack=pouch.getItem(i);
            if(stack.isEmpty())continue;
            player.getInventory().add(stack);
            if(stack.isEmpty())pouch.setItem(i,ItemStack.EMPTY);
        }
    }

    @Override public void aiStep() {
        super.aiStep();
        if(!level().isClientSide) {
            if(forageCooldown>0)forageCooldown--;
            if(inLove>0)inLove--;
            if(age<0) { age++; if(age==0)setBabyFlag(false); }
            else if(age>0)age--;
            ensureLattice(getRandom(),level() instanceof ServerLevel server && server.dimension().equals(ModDimensions.THE_MARCH));
            if(isSitting()) {
                sitTicks++;
                if(lattice.expressed(FamiliarData.Mark.DRIFT) && sitTicks>=FamiliarData.DRIFT_SIT_TICKS) {
                    if(!FamiliarSlots.tryFollow(this) )sitTicks=0;
                    else {
                        var owner=getOwner();
                        if(owner!=null)owner.displayClientMessage(Component.translatable("message.tribalpower.familiar.restless",getDisplayName()),true);
                    }
                }
            } else sitTicks=0;
            if(isBonded() && !isSitting())FamiliarSlots.enforceCap(this);
            if(lattice.sparked() && level() instanceof ServerLevel server && server.getGameTime()%40==0)
                server.sendParticles(ParticleTypes.END_ROD,getX(),getY()+getBbHeight()*.6,getZ(),2,.2,.2,.2,.01);
            if(isBonded() && inLove>0)tryBreed();
            if(isBonded())FamiliarAbilities.tickMonster(this);
            fadeAtDawn();
        }
    }
    /** Wild spirits under open sky thin out once the March's day comes, so night is the danger and day the work. */
    private void fadeAtDawn() {
        if(!(level() instanceof ServerLevel server) || !server.dimension().equals(ModDimensions.THE_MARCH)) return;
        if(!tk.darrow.tribalpower.config.TribalConfig.dawnFade() || isPersistenceRequired() || profile().boss() || !server.isDay()
                || getRandom().nextInt(tk.darrow.tribalpower.config.TribalConfig.dawnFadeOneIn())!=0) return;
        if(getTarget()!=null || !server.canSeeSky(blockPosition().above())) return;
        server.sendParticles(ParticleTypes.SOUL,getX(),getY()+getBbHeight()*.5,getZ(),12,.3,getBbHeight()*.4,.3,.02);
        discard();
    }
    private void tryBreed() {
        if(!(level() instanceof ServerLevel server) || isBaby() || age>0)return;
        for(LatticeMonster other:server.getEntitiesOfClass(LatticeMonster.class,getBoundingBox().inflate(8),
                m->m!=this && m.getType()==getType() && m.isBonded() && !m.isBaby() && m.inLove>0 && m.age==0)) {
            var child=CreatureEntities.MONSTERS.get(profile()).get().create(server);
            if(child==null)return;
            child.copyPosition(this);
            child.lattice.copyFrom(FamiliarData.inherit(lattice,other.lattice,profile(),server.random,ownerUUID().orElse(null),other.ownerUUID().orElse(null)));
            child.applyLattice();
            child.setBabyFlag(true);
            child.setPersistenceRequired();
            // As with wolves, the young of two companions of one owner are born into that owner's company.
            Player owner=getOwner();
            if(owner!=null && other.isOwnedBy(owner))child.bond(owner);
            server.addFreshEntity(child);
            if(owner!=null && other.isOwnedBy(owner))tk.darrow.tribalpower.familiar.FamiliarSlots.afterBond(child,owner);
            inLove=0;other.inLove=0;
            age=6000;other.age=6000;
            server.broadcastEntityEvent(this,(byte)18);
            return;
        }
    }
    private net.minecraft.server.level.ServerBossEvent bossBar;
    /** Created on demand, so the ordinary hostiles carry nothing extra. */
    private net.minecraft.server.level.ServerBossEvent bossBar() {
        if(bossBar==null) bossBar=new net.minecraft.server.level.ServerBossEvent(getDisplayName(),
                net.minecraft.world.BossEvent.BossBarColor.GREEN,net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
        return bossBar;
    }
    @Override public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        if(profile().boss()) bossBar().addPlayer(player);
    }
    @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if(bossBar!=null) bossBar.removePlayer(player);
    }
    @Override public void customServerAiStep() {
        super.customServerAiStep();
        if(profile().boss()) bossBar().setProgress(getHealth()/getMaxHealth());
    }
    @Override public void handleEntityEvent(byte id) {
        if(id==18) {
            for(int i=0;i<7;i++)
                level().addParticle(ParticleTypes.HEART,getX()+getRandom().nextGaussian()*0.3,getY()+getBbHeight()*0.5,getZ()+getRandom().nextGaussian()*0.3,0,0,0);
        } else super.handleEntityEvent(id);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        var result=super.finalizeSpawn(level,difficulty,reason,data);
        boolean march=level.getLevel().dimension().equals(ModDimensions.THE_MARCH);
        ensureLattice(level.getRandom(),march);
        if(march && (reason==MobSpawnType.NATURAL || reason==MobSpawnType.CHUNK_GENERATION)) MarchThreat.empower(this,level,difficulty);
        if(profile().boss()) BossEscort.spawn(this,level,reason);
        return result;
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if(!level().isClientSide) { Containers.dropContents(level(),blockPosition(),pouch);pouch.clearContent(); }
        FamiliarAbilities.clearClick(this);
    }
    @Override public void remove(RemovalReason reason) {
        if(!level().isClientSide && reason==RemovalReason.DISCARDED && profile().boss()) BossEscort.dismiss(this);
        if(!level().isClientSide && reason!=RemovalReason.UNLOADED_TO_CHUNK && reason!=RemovalReason.UNLOADED_WITH_PLAYER)
            FamiliarAbilities.clearClick(this);
        if(bossBar!=null) bossBar.removeAllPlayers();
        super.remove(reason);
    }
    @Override protected void removeAfterChangingDimensions() {
        FamiliarAbilities.clearClick(this);
        super.removeAfterChangingDimensions();
    }
    @Override public void restoreFrom(Entity source) {
        super.restoreFrom(source);
        lastClick=null;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUUID().ifPresent(id->tag.putUUID("Owner",id));
        tag.putBoolean("Sitting",isSitting());
        tag.putInt("Age",age);
        tag.putInt("InLove",inLove);
        tag.putInt("ForageCooldown",forageCooldown);
        if(lattice.rolled())tag.put("Lattice",lattice.save());
        if(!pouch.isEmpty()) {
            var items=net.minecraft.core.NonNullList.withSize(POUCH_SLOTS,ItemStack.EMPTY);
            for(int i=0;i<POUCH_SLOTS;i++)items.set(i,pouch.getItem(i));
            CompoundTag bag=new CompoundTag();ContainerHelper.saveAllItems(bag,items,registryAccess());tag.put("Pouch",bag);
        }
        if(lastOwnerAttacker!=null)tag.putUUID("LastOwnerAttacker",lastOwnerAttacker);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_OWNER,tag.hasUUID("Owner")?Optional.of(tag.getUUID("Owner")):Optional.empty());
        setSitting(tag.getBoolean("Sitting") && isBonded());
        age=tag.getInt("Age");
        setBabyFlag(age<0);
        inLove=Math.max(0,tag.getInt("InLove"));
        forageCooldown=Math.max(0,tag.getInt("ForageCooldown"));
        if(tag.contains("Lattice",Tag.TAG_COMPOUND)) {
            lattice.load(tag.getCompound("Lattice"));applyLattice();
            // The lattice health bonus is transient, so the saved Health was clamped to the base maximum on load
            // (and then read as "full"). Restore the real value now the bonus is back.
            if(tag.contains("Health",Tag.TAG_ANY_NUMERIC))setHealth(Math.min(tag.getFloat("Health"),getMaxHealth()));
        }
        pouch.clearContent();
        if(tag.contains("Pouch",Tag.TAG_COMPOUND)) {
            var items=net.minecraft.core.NonNullList.withSize(POUCH_SLOTS,ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Pouch"),items,registryAccess());
            for(int i=0;i<POUCH_SLOTS;i++)pouch.setItem(i,items.get(i));
        }
        lastOwnerAttacker=tag.hasUUID("LastOwnerAttacker")?tag.getUUID("LastOwnerAttacker"):null;
        if(isBonded() || isBaby())setPersistenceRequired();
    }

    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return !profile().flying && super.causeFallDamage(distance,multiplier,source); }
    @Override protected SoundEvent getAmbientSound() { return profile().ranged()?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.SOUL_SAND_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.AMETHYST_BLOCK_BREAK; }

    private final class SpiritCastGoal extends Goal {
        private int cooldown,windup;
        SpiritCastGoal() { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            return !isBaby() && (!isBonded() || FamiliarRoster.combat(profile())) && getTarget()!=null && getTarget().isAlive() && !isSitting();
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { windup=0;getNavigation().stop(); }
        @Override public void tick() {
            var target=getTarget();if(target==null || isSitting())return;
            if(cooldown>0)cooldown--;
            getLookControl().setLookAt(target,30,30);
            if(!canCastAt(target)) { windup=0;getNavigation().moveTo(target,1);return; }
            getNavigation().stop();if(cooldown>0)return;
            if(level() instanceof ServerLevel server && windup%4==0)server.sendParticles(ParticleTypes.END_ROD,getX(),getEyeY(),getZ(),2,.12,.12,.12,.01);
            if(++windup<16)return;
            windup=0;cooldown=70;
            if(FamiliarOwnerTargetGoals.forbidden(LatticeMonster.this,target))return;
            if(target.hurt(damageSources().indirectMagic(LatticeMonster.this,LatticeMonster.this),(float)(profile().damage*lattice.multiplier(FamiliarData.Thread.FANG))))applyVoice(target);
            level().playSound(null,blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.HOSTILE,.7F,.8F);
        }
    }
}
