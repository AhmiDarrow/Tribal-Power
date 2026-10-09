package tk.darrow.tribalpower.wildlife;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A small schooling fish with lit fins: the March's common fish, and the one you can carry home in a bucket.
 * Ribbon weed or seagrass tempts it and breeds it (see WildBreeding).
 */
public class GlimmerfinEntity extends AbstractSchoolingFish implements WildBreeding.Breeder {
    private final WildBreeding.State breeding = new WildBreeding.State();

    public GlimmerfinEntity(EntityType<? extends GlimmerfinEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public WildBreeding.State breeding() {
        return breeding;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Ahead of the fish's shyness of players, so the hand holding weed is followed, not fled.
        goalSelector.addGoal(1, new TemptGoal(this, 1.25, stack -> tk.darrow.tribalpower.entity.BreedingFood.isFood(getType(), stack), false));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult fed = WildBreeding.feed(this, breeding, player, hand);
        return fed != null ? fed : super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            var mate = WildBreeding.tick(this, breeding);
            if (mate != null && tickCount % 10 == 0) getNavigation().moveTo(mate, 1.25);
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
        return new ItemStack(Wildlife.GLIMMERFIN_BUCKET.get());
    }

    @Override
    public int getMaxSchoolSize() {
        return 8;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return tk.darrow.tribalpower.sound.ModSounds.SPIRIT_FLOP.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return tk.darrow.tribalpower.sound.ModSounds.creature("glimmerfin","ambient");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return tk.darrow.tribalpower.sound.ModSounds.creature("glimmerfin","death");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return tk.darrow.tribalpower.sound.ModSounds.creature("glimmerfin","hurt");
    }
}
