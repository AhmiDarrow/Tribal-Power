package tk.darrow.tribalpower.wildlife;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
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
 * None of them can be carried in a bucket.
 */
public class MarchSwimmerEntity extends AbstractFish {
    public enum Kind { DRIFT_BELL, VEIL_RAY, SILT_EEL }

    public MarchSwimmerEntity(EntityType<? extends MarchSwimmerEntity> type, Level level) {
        super(type, level);
    }

    /** From the entity type, so it is known even while the constructor is still registering goals. */
    public Kind kind() {
        return switch (BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath()) {
            case "drift_bell" -> Kind.DRIFT_BELL;
            case "veil_ray" -> Kind.VEIL_RAY;
            default -> Kind.SILT_EEL;
        };
    }

    @Override
    protected void registerGoals() {
        switch (kind()) {
            case DRIFT_BELL -> goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 80));
            case VEIL_RAY -> {
                goalSelector.addGoal(0, new PanicGoal(this, 1.5));
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
        return InteractionResult.PASS;
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
        return kind() == Kind.DRIFT_BELL ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SALMON_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind() == Kind.DRIFT_BELL ? null : SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind() == Kind.DRIFT_BELL ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kind() == Kind.DRIFT_BELL ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SALMON_HURT;
    }
}
