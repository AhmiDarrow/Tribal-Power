package tk.darrow.tribalpower.wildlife;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A small schooling fish with lit fins: the March's common fish, and the one you can carry home in a bucket. */
public class GlimmerfinEntity extends AbstractSchoolingFish {
    public GlimmerfinEntity(EntityType<? extends GlimmerfinEntity> type, Level level) {
        super(type, level);
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
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }
}
