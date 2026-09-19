package tk.darrow.tribalpower.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Small, quick birds that wheel over the March in loose flocks. No pathfinding: each bird picks a point in
 * the open sky ahead and steers for it, now and then leaning toward where its neighbours are headed. That is
 * cheap enough to keep a sky busy without costing a server anything worth measuring.
 */
public class LoomSwiftEntity extends AmbientCreature {
    private Vec3 target;
    private int retarget;

    public LoomSwiftEntity(EntityType<? extends LoomSwiftEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    protected void pushEntities() {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (target == null || --retarget <= 0 || target.distanceToSqr(position()) < 4 || horizontalCollision || verticalCollision) pick();
        Vec3 heading = target.subtract(position()).normalize().scale(0.32);
        Vec3 motion = getDeltaMovement().add(heading.subtract(getDeltaMovement()).scale(0.08));
        setDeltaMovement(motion);
        float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.35F);
        yBodyRot = getYRot();
    }

    /** A new point in open sky ahead, drawn a little toward the flock. */
    private void pick() {
        retarget = 60 + random.nextInt(80);
        Vec3 ahead = getDeltaMovement().lengthSqr() > 0.001 ? getDeltaMovement().normalize().scale(10) : Vec3.ZERO;
        double x = getX() + ahead.x + random.nextGaussian() * 9, z = getZ() + ahead.z + random.nextGaussian() * 9;
        var flock = level().getEntitiesOfClass(LoomSwiftEntity.class, getBoundingBox().inflate(12), bird -> bird != this && bird.target != null);
        if (!flock.isEmpty()) {
            Vec3 other = flock.get(random.nextInt(flock.size())).target;
            x = Mth.lerp(0.6, x, other.x);
            z = Mth.lerp(0.6, z, other.z);
        }
        // An unloaded column reports the bottom of the world; keep to the loaded sky instead of diving for it.
        if (!level().hasChunkAt(BlockPos.containing(x, getY(), z))) {
            x = getX() - ahead.x;
            z = getZ() - ahead.z;
        }
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        double y = Math.max(ground + 5 + random.nextInt(12), Math.min(getY() + random.nextGaussian() * 3, ground + 26));
        target = new Vec3(x, y, z);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? SoundEvents.PARROT_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + random.nextFloat() * 0.3F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
