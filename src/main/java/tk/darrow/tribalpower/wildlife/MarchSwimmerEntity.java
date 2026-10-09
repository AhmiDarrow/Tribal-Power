package tk.darrow.tribalpower.wildlife;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The March's larger water life, one class for three kinds:
 * <ul>
 * <li>Drift Bell: a slow, glowing jelly that drifts in still water. Harmless.</li>
 * <li>Veil Ray: a wide, shy ray that glides through lakes and flees anyone who comes close.</li>
 * <li>Silt Eel: a fen eel that bites swimmers who wade into its water, and anyone who strikes it.</li>
 * </ul>
 * None of them can be carried in a bucket. The bell and the ray follow ribbon weed or seagrass and breed on it
 * (see WildBreeding); the eel does neither.
 */
public class MarchSwimmerEntity extends AbstractFish implements WildBreeding.Breeder {
    public enum Kind { DRIFT_BELL, VEIL_RAY, SILT_EEL }

    private final WildBreeding.State breeding = new WildBreeding.State();

    @Override
    public WildBreeding.State breeding() {
        return breeding;
    }

    public MarchSwimmerEntity(EntityType<? extends MarchSwimmerEntity> type, Level level) {
        super(type, level);
    }

    /** Resolved on first ask (no initializer: the super constructor asks while registering goals, and an initializer would wipe it after). */
    private Kind kind;

    /** From the entity type, so it is known even while the constructor is still registering goals. */
    public Kind kind() {
        Kind known = kind;
        if (known == null) {
            known = switch (BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath()) {
                case "drift_bell" -> Kind.DRIFT_BELL;
                case "veil_ray" -> Kind.VEIL_RAY;
                default -> Kind.SILT_EEL;
            };
            kind = known;
        }
        return known;
    }

    @Override
    protected void registerGoals() {
        switch (kind()) {
            case DRIFT_BELL -> {
                goalSelector.addGoal(1, new TemptGoal(this, 1.0, stack -> tk.darrow.tribalpower.entity.BreedingFood.isFood(getType(), stack), false));
                goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 80));
            }
            case VEIL_RAY -> {
                goalSelector.addGoal(0, new PanicGoal(this, 1.5));
                // Ahead of its shyness: a ray comes to the hand that holds its food.
                goalSelector.addGoal(1, new TemptGoal(this, 1.2, stack -> tk.darrow.tribalpower.entity.BreedingFood.isFood(getType(), stack), false));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.2, 1.5));
                goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 30));
            }
            case SILT_EEL -> {
                goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
                goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 40));
                targetSelector.addGoal(1, new HurtByTargetGoal(this));
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                        target -> target.isInWater() && level().getDifficulty() != Difficulty.PEACEFUL));
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult fed = WildBreeding.feed(this, breeding, player, hand);
        return fed != null ? fed : InteractionResult.PASS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && kind() != Kind.SILT_EEL) {
            var mate = WildBreeding.tick(this, breeding);
            if (mate != null && tickCount % 10 == 0) getNavigation().moveTo(mate, 1.2);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        breeding.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breeding.load(tag);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasCustomName() && !isPersistenceRequired();
    }

    @Override
    protected SoundEvent getFlopSound() {
        return tk.darrow.tribalpower.sound.ModSounds.SPIRIT_FLOP.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return tk.darrow.tribalpower.sound.ModSounds.creature(kind() == Kind.DRIFT_BELL ? "drift_bell" : "march_swimmer","ambient");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return tk.darrow.tribalpower.sound.ModSounds.creature(kind() == Kind.DRIFT_BELL ? "drift_bell" : "march_swimmer","death");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return tk.darrow.tribalpower.sound.ModSounds.creature(kind() == Kind.DRIFT_BELL ? "drift_bell" : "march_swimmer","hurt");
    }
}
