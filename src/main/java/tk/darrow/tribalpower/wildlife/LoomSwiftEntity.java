package tk.darrow.tribalpower.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Small, quick birds that wheel over the March in loose flocks. No pathfinding: each bird picks a point in
 * the open sky ahead and steers for it, now and then leaning toward where its neighbours are headed. That is
 * cheap enough to keep a sky busy without costing a server anything worth measuring.
 *
 * <p>Hold out seeds and nearby swifts wheel in close around you, where they can be fed and bred (see WildBreeding).
 */
public class LoomSwiftEntity extends AmbientCreature implements WildBreeding.Breeder {
    private static final double TEMPT_RANGE = 16;
    private final WildBreeding.State breeding = new WildBreeding.State();
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
    public WildBreeding.State breeding() {
        return breeding;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult fed = WildBreeding.feed(this, breeding, player, hand);
        return fed != null ? fed : super.mobInteract(player, hand);
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
    protected void customServerAiStep() {
        super.customServerAiStep();
        // A mate in love comes first, then a player holding seeds; either replaces the open-sky point.
        Mob mate = WildBreeding.tick(this, breeding);
        if (mate != null) {
            target = mate.position();
            retarget = 10;
        } else if (tickCount % 10 == 0) {
            Player feeder = level().getNearestPlayer(getX(), getY(), getZ(), TEMPT_RANGE,
                    p -> !p.isSpectator() && (tk.darrow.tribalpower.entity.BreedingFood.isFood(getType(), ((Player) p).getMainHandItem())
                            || tk.darrow.tribalpower.entity.BreedingFood.isFood(getType(), ((Player) p).getOffhandItem())));
            if (feeder != null) {
                target = feeder.getEyePosition().add(random.nextGaussian() * 1.5, 0.6 + random.nextDouble(), random.nextGaussian() * 1.5);
                retarget = 20;
            }
        }
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
        return random.nextInt(4) == 0 ? tk.darrow.tribalpower.sound.ModSounds.creature("loom_swift","ambient") : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + random.nextFloat() * 0.3F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return tk.darrow.tribalpower.sound.ModSounds.creature("loom_swift","hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return tk.darrow.tribalpower.sound.ModSounds.creature("loom_swift","death");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
