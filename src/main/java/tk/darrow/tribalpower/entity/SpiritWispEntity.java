package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;

/**
 * Ambient March spirit — drifts, sheds motes, flees when startled.
 */
public class SpiritWispEntity extends PathfinderMob {
    public SpiritWispEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 0.9D, 1.15D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        if (this.level().isClientSide && this.tickCount % 4 == 0) {
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.2, this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.02,
                    0.02,
                    (this.random.nextDouble() - 0.5) * 0.02
            );
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.FLYING_SPEED, 0.35D);
    }

    public static boolean checkSpawnRules(
            EntityType<SpiritWispEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
    ) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean footing = below.is(ModBlocks.MARCH_GRASS.get())
                || below.is(ModBlocks.MARCH_MOSS.get())
                || below.is(ModBlocks.MARCH_SOIL.get())
                || below.is(ModBlocks.MARCH_STONE.get())
                || below.is(ModBlocks.MARCH_COBBLE.get())
                || below.is(ModBlocks.MARCH_CRYSTAL.get());
        if (!footing) {
            return false;
        }
        // Cluster near crystals more often; otherwise require open sky-ish air above.
        if (below.is(ModBlocks.MARCH_CRYSTAL.get())) {
            return true;
        }
        return level.getBlockState(pos.above()).isAir() && random.nextInt(3) != 0;
    }
}
