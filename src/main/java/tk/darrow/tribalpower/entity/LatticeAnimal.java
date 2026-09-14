package tk.darrow.tribalpower.entity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.familiar.FamiliarAbilities;
import tk.darrow.tribalpower.familiar.FamiliarData;
import tk.darrow.tribalpower.familiar.FamiliarFollowGoal;
import tk.darrow.tribalpower.familiar.FamiliarSitGoal;
import tk.darrow.tribalpower.familiar.FamiliarSlots;
import tk.darrow.tribalpower.familiar.MossbackMenu;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.world.ModDimensions;
/**
 * The three gentle March animals. Adults can be bonded with a Bonding Charm (design 3.0 §5); a bonded animal
 * stores {@code Owner} and {@code Sitting}, follows or waits, never despawns, ignores its owner's blows and yields
 * double reagent when brushed. Species abilities live in {@link FamiliarAbilities}. Threads and Marks live on
 * {@link FamiliarData} under NBT {@code Lattice}.
 * <p>NBT: {@code ForageCooldown} int, {@code Owner} UUID, {@code Sitting} boolean, {@code Saddlebag} compound with slot-indexed {@code Items} (Mossback), {@code LastLight} long (Lantern Fox), {@code Lattice} compound.
 */
public class LatticeAnimal extends Animal implements PlayerRideableJumping, Familiar {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER=SynchedEntityData.defineId(LatticeAnimal.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SITTING=SynchedEntityData.defineId(LatticeAnimal.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_STRIDE=SynchedEntityData.defineId(LatticeAnimal.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_LEAP=SynchedEntityData.defineId(LatticeAnimal.class,EntityDataSerializers.BOOLEAN);
    public static final int SADDLEBAG_SLOTS=FamiliarData.BASE_SADDLEBAG+FamiliarData.EXTRA_SADDLEBAG;
    private int forageCooldown,sitTicks;
    private final SimpleContainer saddlebag=new SimpleContainer(SADDLEBAG_SLOTS);
    private final FamiliarData lattice=new FamiliarData();
    private BlockPos lastLight;
    private float riderJumpScale;
    public LatticeAnimal(EntityType<? extends Animal> type,Level level) { super(type,level); }
    @Override public CreatureProfile profile() { return CreatureProfile.of(getType()); }
    @Override public FamiliarData lattice() { return lattice; }
    public void applyLattice() {
        lattice.apply(this,profile());
        entityData.set(DATA_STRIDE,(byte)lattice.phenotype(FamiliarData.Thread.STRIDE));
        entityData.set(DATA_LEAP,lattice.expressed(FamiliarData.Mark.LEAP));
    }
    public void ensureLattice(RandomSource random,boolean march) {
        if(lattice.rolled())return;
        lattice.rollWild(profile(),random,march);
        applyLattice();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER,Optional.empty());
        builder.define(DATA_SITTING,false);
        builder.define(DATA_STRIDE,(byte)1);
        builder.define(DATA_LEAP,false);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new FamiliarSitGoal(this));
        goalSelector.addGoal(2,new PanicGoal(this,1.3) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        });
        goalSelector.addGoal(3,new BreedGoal(this,1));
        goalSelector.addGoal(4,new TemptGoal(this,1,this::isFood,false) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        });
        goalSelector.addGoal(5,new FamiliarFollowGoal(this,1.1));
        goalSelector.addGoal(6,new AvoidEntityGoal<>(this,Monster.class,8,1,1.2) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        });
        goalSelector.addGoal(7,new FollowParentGoal(this,1));
        goalSelector.addGoal(8,new WaterAvoidingRandomStrollGoal(this,.8) {
            @Override public boolean canUse() { return !isBonded() && super.canUse(); }
        });
        goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(10,new RandomLookAroundGoal(this));
    }
    // ---- bonding state -------------------------------------------------------------------------------------
    @Override public Optional<UUID> ownerUUID() { return entityData.get(DATA_OWNER); }
    @Override public boolean isBonded() { return ownerUUID().isPresent(); }
    @Override public boolean isOwnedBy(Entity entity) { return entity!=null && ownerUUID().map(id->id.equals(entity.getUUID())).orElse(false); }
    @Override public Player getOwner() { return ownerUUID().map(id->level().getPlayerByUUID(id)).orElse(null); }
    @Override public void bond(Player owner) { entityData.set(DATA_OWNER,Optional.of(owner.getUUID()));setSitting(false);setPersistenceRequired(); }
    @Override public boolean isSitting() { return entityData.get(DATA_SITTING); }
    @Override public void setSitting(boolean sitting) { entityData.set(DATA_SITTING,sitting);if(sitting)getNavigation().stop(); }
    public SimpleContainer saddlebag() { return saddlebag; }
    public BlockPos lastLight() { return lastLight; }
    public void setLastLight(BlockPos pos) { lastLight=pos; }
    @Override public boolean unableToMoveToOwner() { return isSitting() || isPassenger() || isVehicle() || mayBeLeashed() || (getOwner()!=null && getOwner().isSpectator()); }
    @Override public boolean isAlliedTo(Entity other) {
        if(isBonded()) {
            if(other instanceof Player player && isOwnedBy(player))return true;
            if(other instanceof Familiar fam && fam.isBonded() && ownerUUID().equals(fam.ownerUUID()))return true;
        }
        return super.isAlliedTo(other);
    }
    @Override public boolean requiresCustomPersistence() { return super.requiresCustomPersistence() || isBonded(); }
    @Override public boolean removeWhenFarAway(double distance) { return !isBonded() && super.removeWhenFarAway(distance); }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isBonded() && isOwnedBy(source.getEntity()))return false;
        if(!level().isClientSide && isSitting() && amount>0)FamiliarSlots.tryFollow(this);
        return super.hurt(source,amount);
    }
    // ---- food, breeding, brushing ----------------------------------------------------------------------------
    @Override public boolean isFood(ItemStack s) { return s.is(switch(profile()) { case DAWN_STAG -> Items.WHEAT; case LANTERN_FOX -> Items.SWEET_BERRIES; default -> Items.SEAGRASS; }); }
    @Override public boolean canMate(Animal other) { return other.getType()==getType() && super.canMate(other); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob mate) {
        var child=CreatureEntities.ANIMALS.get(profile()).get().create(level);
        if(child!=null && mate instanceof LatticeAnimal other) {
            UUID a=ownerUUID().orElse(null),b=other.ownerUUID().orElse(null);
            child.lattice.copyFrom(FamiliarData.inherit(lattice,other.lattice,profile(),level.random,a,b));
            child.applyLattice();
        }
        return child;
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        ItemStack tool=player.getItemInHand(hand);
        if(tool.is(Items.BRUSH) && !isBaby()) {
            if(!level().isClientSide && forageCooldown==0) {
                spawnAtLocation(new ItemStack(CreatureItems.REAGENTS.get(profile()).get(),isBonded()?2:1));
                forageCooldown=1200;
                tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
                level().playSound(null,blockPosition(),SoundEvents.BRUSH_GENERIC,SoundSource.NEUTRAL,.7F,1.1F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(isBonded() && isOwnedBy(player) && !isFood(tool)) {
            if(player.isSecondaryUseActive()) {
                // Sneak-use: a Mossback with an empty hand opens its saddlebag; anything else toggles stay/follow.
                if(profile()==CreatureProfile.MOSSBACK && tool.isEmpty()) {
                    if(player instanceof net.minecraft.server.level.ServerPlayer server)server.openMenu(new SimpleMenuProvider((id,inv,p)->new MossbackMenu(id,inv,saddlebag,this),getDisplayName()));
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
            if(profile()==CreatureProfile.DAWN_STAG && !isBaby() && !isVehicle()) {
                if(!level().isClientSide) { setSitting(false);player.startRiding(this); }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        else if(isBonded() && !isOwnedBy(player) && profile()==CreatureProfile.DAWN_STAG && !isFood(tool) && !tool.is(tk.darrow.tribalpower.familiar.FamiliarRegistry.BONDING_CHARM.get())) {
            if(!level().isClientSide)player.displayClientMessage(Component.translatable("message.tribalpower.familiar.not_yours",getDisplayName()),true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    // ---- ticking ---------------------------------------------------------------------------------------------
    @Override public void aiStep() {
        super.aiStep();
        if(!level().isClientSide) {
            if(forageCooldown>0)forageCooldown--;
            ensureLattice(getRandom(),level() instanceof ServerLevel server && server.dimension().equals(ModDimensions.THE_MARCH));
            if(isSitting()) {
                sitTicks++;
                if(lattice.expressed(FamiliarData.Mark.DRIFT) && sitTicks>=FamiliarData.DRIFT_SIT_TICKS) {
                    if(!FamiliarSlots.tryFollow(this))sitTicks=0;
                    else {
                        var owner=getOwner();
                        if(owner!=null)owner.displayClientMessage(Component.translatable("message.tribalpower.familiar.restless",getDisplayName()),true);
                    }
                }
            } else sitTicks=0;
            if(isBonded() && !isSitting())FamiliarSlots.enforceCap(this);
            if(lattice.sparked() && level() instanceof ServerLevel server && server.getGameTime()%40==0)
                server.sendParticles(ParticleTypes.END_ROD,getX(),getY()+getBbHeight()*.6,getZ(),2,.2,.2,.2,.01);
            if(isBonded())FamiliarAbilities.tick(this);
        }
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        var result=super.finalizeSpawn(level,difficulty,reason,data);
        ensureLattice(level.getRandom(),level.getLevel().dimension().equals(ModDimensions.THE_MARCH));
        return result;
    }
    @Override public void remove(RemovalReason reason) {
        if(!level().isClientSide && reason!=RemovalReason.UNLOADED_TO_CHUNK && reason!=RemovalReason.UNLOADED_WITH_PLAYER)FamiliarAbilities.clearLight(this);
        super.remove(reason);
    }
    /** Portals and cross-dimension teleports bypass {@link #remove}: the old copy is dropped through this hook instead. */
    @Override protected void removeAfterChangingDimensions() {
        FamiliarAbilities.clearLight(this);
        super.removeAfterChangingDimensions();
    }
    /** The copy that arrives in the new dimension must not inherit a light position from the old one. */
    @Override public void restoreFrom(Entity source) {
        super.restoreFrom(source);
        lastLight=null;
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if(!level().isClientSide) { Containers.dropContents(level(),blockPosition(),saddlebag);saddlebag.clearContent(); }
    }
    // ---- riding (Dawn Stag) ------------------------------------------------------------------------------------
    @Override public LivingEntity getControllingPassenger() {
        if(profile()==CreatureProfile.DAWN_STAG && isBonded() && !isBaby() && getFirstPassenger() instanceof Player rider && isOwnedBy(rider))return rider;
        return super.getControllingPassenger();
    }
    @Override protected void tickRidden(Player rider,Vec3 travel) {
        super.tickRidden(rider,travel);
        setRot(rider.getYRot(),rider.getXRot()*.5F);
        yRotO=yBodyRot=yHeadRot=getYRot();
        if(isControlledByLocalInstance() && onGround()) {
            if(riderJumpScale>0) {
                var motion=getDeltaMovement();
                double lift=.42*(entityData.get(DATA_LEAP)?1.21:1);
                setDeltaMovement(motion.x,lift*Math.max(.4,riderJumpScale)+.06,motion.z);
                hasImpulse=true;
                net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
            }
            riderJumpScale=0;
        }
    }
    @Override protected Vec3 getRiddenInput(Player rider,Vec3 travel) {
        float side=rider.xxa*.5F,forward=rider.zza;
        if(forward<=0)forward*=.25F;
        return new Vec3(side,0,forward);
    }
    @Override protected float getRiddenSpeed(Player rider) {
        int stride=entityData.get(DATA_STRIDE);
        return (float)(.32*(1.0+(stride-1)*0.12));
    }
    public boolean rideLeap() { return entityData.get(DATA_LEAP); }
    public int rideStride() { return entityData.get(DATA_STRIDE); }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger,EntityDimensions dimensions,float partialTick) { return new Vec3(0,isBaby()?.6:1.05,0); }
    @Override public void onPlayerJump(int power) { if(canJump())riderJumpScale=power>=90?1:.4F+.4F*power/90F; }
    @Override public boolean canJump() { return profile()==CreatureProfile.DAWN_STAG && isBonded() && getControllingPassenger()!=null; }
    @Override public void handleStartJump(int power) { playSound(SoundEvents.GOAT_LONG_JUMP,.5F,1.2F); }
    @Override public void handleStopJump() {}
    @Override protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if(!level().isClientSide && isBonded())FamiliarSlots.tryFollow(this);
    }
    // ---- persistence ------------------------------------------------------------------------------------------
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ForageCooldown",forageCooldown);
        ownerUUID().ifPresent(id->tag.putUUID("Owner",id));
        tag.putBoolean("Sitting",isSitting());
        if(!saddlebag.isEmpty()) {
            var items=net.minecraft.core.NonNullList.withSize(SADDLEBAG_SLOTS,ItemStack.EMPTY);
            for(int i=0;i<SADDLEBAG_SLOTS;i++)items.set(i,saddlebag.getItem(i));
            CompoundTag bag=new CompoundTag();ContainerHelper.saveAllItems(bag,items,registryAccess());tag.put("Saddlebag",bag);
        }
        if(lastLight!=null)tag.putLong("LastLight",lastLight.asLong());
        if(lattice.rolled())tag.put("Lattice",lattice.save());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        forageCooldown=Math.clamp(tag.getInt("ForageCooldown"),0,1200);
        entityData.set(DATA_OWNER,tag.hasUUID("Owner")?Optional.of(tag.getUUID("Owner")):Optional.empty());
        setSitting(tag.getBoolean("Sitting") && isBonded());
        saddlebag.clearContent();
        if(tag.contains("Saddlebag",Tag.TAG_COMPOUND)) {
            var items=net.minecraft.core.NonNullList.withSize(SADDLEBAG_SLOTS,ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Saddlebag"),items,registryAccess());
            for(int i=0;i<SADDLEBAG_SLOTS;i++)saddlebag.setItem(i,items.get(i));
        }
        else if(tag.contains("Saddlebag",Tag.TAG_LIST))saddlebag.fromTag(tag.getList("Saddlebag",Tag.TAG_COMPOUND),registryAccess());
        lastLight=tag.contains("LastLight",Tag.TAG_LONG)?BlockPos.of(tag.getLong("LastLight")):null;
        if(tag.contains("Lattice",Tag.TAG_COMPOUND)) { lattice.load(tag.getCompound("Lattice"));applyLattice(); }
        if(isBonded())setPersistenceRequired();
    }
    public int forageCooldown() { return forageCooldown; }
    public static boolean canSpawn(EntityType<LatticeAnimal> type,LevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return MarchSpawns.animal(type,level,reason,pos,random);
    }
    @Override protected SoundEvent getAmbientSound() { return profile()==CreatureProfile.LANTERN_FOX?SoundEvents.FOX_AMBIENT:SoundEvents.GOAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.FOX_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.FOX_DEATH; }
}
