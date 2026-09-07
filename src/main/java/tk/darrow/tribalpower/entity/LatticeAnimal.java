package tk.darrow.tribalpower.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import tk.darrow.tribalpower.item.CreatureItems;
public class LatticeAnimal extends Animal {
    private int forageCooldown;
    public LatticeAnimal(EntityType<? extends Animal> type,Level level) { super(type,level); }
    public CreatureProfile profile() { return CreatureProfile.of(getType()); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.3));
        goalSelector.addGoal(2,new BreedGoal(this,1));
        goalSelector.addGoal(3,new TemptGoal(this,1,this::isFood,false));
        goalSelector.addGoal(4,new AvoidEntityGoal<>(this,Monster.class,8,1,1.2));
        goalSelector.addGoal(5,new FollowParentGoal(this,1));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override public boolean isFood(ItemStack s) { return s.is(switch(profile()) { case DAWN_STAG -> Items.WHEAT; case LANTERN_FOX -> Items.SWEET_BERRIES; default -> Items.SEAGRASS; }); }
    @Override public boolean canMate(Animal other) { return other.getType()==getType() && super.canMate(other); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob mate) { return CreatureEntities.ANIMALS.get(profile()).get().create(level); }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        ItemStack tool=player.getItemInHand(hand);
        if(tool.is(Items.BRUSH) && !isBaby()) {
            if(!level().isClientSide && forageCooldown==0) {
                spawnAtLocation(new ItemStack(CreatureItems.REAGENTS.get(profile()).get()));
                forageCooldown=1200;
                tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
                level().playSound(null,blockPosition(),SoundEvents.BRUSH_GENERIC,SoundSource.NEUTRAL,.7F,1.1F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override public void aiStep() { super.aiStep();if(!level().isClientSide && forageCooldown>0) forageCooldown--; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putInt("ForageCooldown",forageCooldown); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);forageCooldown=Math.clamp(tag.getInt("ForageCooldown"),0,1200); }
    public int forageCooldown() { return forageCooldown; }
    public static boolean canSpawn(EntityType<LatticeAnimal> type,LevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        var below=level.getBlockState(pos.below());
        return (below.is(BlockTags.DIRT) || below.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get()) || below.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_MOSS.get()))
            && level.getRawBrightness(pos,0)>8 && level.getFluidState(pos).isEmpty();
    }
    @Override protected SoundEvent getAmbientSound() { return profile()==CreatureProfile.LANTERN_FOX?SoundEvents.FOX_AMBIENT:SoundEvents.GOAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.FOX_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.FOX_DEATH; }
}
