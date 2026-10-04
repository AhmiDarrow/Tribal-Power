package tk.darrow.tribalpower.wildlife;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import tk.darrow.tribalpower.entity.BreedingFood;

/**
 * Breeding for the March wildlife that has no young of its own: the fish, the jelly, the ray and the swifts.
 * Fed its food, one is in love for thirty seconds; two in love within a few blocks of each other make a third,
 * full grown (as a bucketed fish is), and both rest five minutes before they can again. The young never despawn,
 * so a stocked pond stays stocked.
 *
 * <p>NBT on the parent: {@code WildLove} and {@code WildBreedCooldown}, both ticks.
 */
public final class WildBreeding {
    public static final int LOVE_TICKS = 600, COOLDOWN_TICKS = 6000;
    /** Close enough to pair: the swifts never stop moving, so this is looser than an animal's three blocks. */
    private static final double PAIR_DISTANCE_SQR = 9.0, SEEK_RADIUS = 8.0;

    public static final class State {
        private int love, cooldown;

        public boolean inLove() {
            return love > 0;
        }

        public int cooldown() {
            return cooldown;
        }

        public void save(CompoundTag tag) {
            if (love > 0) tag.putInt("WildLove", love);
            if (cooldown > 0) tag.putInt("WildBreedCooldown", cooldown);
        }

        public void load(CompoundTag tag) {
            love = Math.clamp(tag.getInt("WildLove"), 0, LOVE_TICKS);
            cooldown = Math.clamp(tag.getInt("WildBreedCooldown"), 0, COOLDOWN_TICKS);
        }
    }

    /** Implemented by each wildlife class that can be bred. */
    public interface Breeder {
        State breeding();
    }

    private WildBreeding() {}

    /** Feeding: null when the hand holds nothing it eats, or it cannot fall in love yet; the caller then carries on. */
    public static InteractionResult feed(Mob mob, State state, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!BreedingFood.isFood(mob.getType(), stack) || state.love > 0 || state.cooldown > 0) return null;
        if (mob.level() instanceof ServerLevel level) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            state.love = LOVE_TICKS;
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
        return InteractionResult.sidedSuccess(mob.level().isClientSide);
    }

    /**
     * Server tick. Counts love and rest down, and when a partner in love is close enough, breeds. Returns a partner
     * still too far away, for the caller to steer toward, or null.
     */
    public static Mob tick(Mob mob, State state) {
        if (state.cooldown > 0) state.cooldown--;
        if (state.love <= 0 || !(mob.level() instanceof ServerLevel level)) return null;
        state.love--;
        if (mob.tickCount % 10 == 0)
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (var other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(SEEK_RADIUS),
                o -> o != mob && o.getType() == mob.getType() && o.isAlive() && o instanceof Breeder b && b.breeding().love > 0)) {
            double d = other.distanceToSqr(mob);
            if (d < best) {
                best = d;
                nearest = other;
            }
        }
        if (nearest == null) return null;
        if (best > PAIR_DISTANCE_SQR) return nearest;
        breed(level, mob, state, nearest, ((Breeder) nearest).breeding());
        return null;
    }

    private static void breed(ServerLevel level, Mob mob, State state, Mob mate, State mateState) {
        state.love = mateState.love = 0;
        state.cooldown = mateState.cooldown = COOLDOWN_TICKS;
        var child = mob.getType().create(level);
        if (!(child instanceof Mob young)) return;
        young.moveTo((mob.getX() + mate.getX()) / 2, (mob.getY() + mate.getY()) / 2, (mob.getZ() + mate.getZ()) / 2, mob.getYRot(), 0);
        // Bred stock is kept stock: it must not despawn like the wild fish and swifts it came from.
        young.setPersistenceRequired();
        // Born grown, so it waits out the same rest as its parents before it breeds.
        if (young instanceof Breeder b) b.breeding().cooldown = COOLDOWN_TICKS;
        level.addFreshEntityWithPassengers(young);
        level.sendParticles(ParticleTypes.HEART, young.getX(), young.getY() + young.getBbHeight() + 0.3, young.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
        level.addFreshEntity(new ExperienceOrb(level, mob.getX(), mob.getY(), mob.getZ(), 1 + mob.getRandom().nextInt(7)));
    }
}
