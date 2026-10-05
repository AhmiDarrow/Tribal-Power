package tk.darrow.tribalpower.entity;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * How the creatures of the {@link CreatureHabitat#WATER water habitat} move.
 *
 * <p>They spawned only in water but were built as land beasts: walking navigation, a walking move control and
 * a float goal, so they bobbed at the surface, paddled at the edges and could drown. Every water dweller now swims
 * the way the vanilla fish do: water-bound navigation, a steering move control, neutral buoyancy while its eyes
 * are under (it sinks only once it breaks the surface, so it never bobs), and it never runs out of air in water.
 *
 * <p>The gentle swimmers (Silt Glider, Pale Drifter, Shoal Darter) are fish: on land they cannot walk, flop
 * toward the nearest water and dry out on the vanilla fish's clock. The two hunters (Brine Lurker, Drowned
 * Shade) are amphibious, like the vanilla Drowned: they hunt by biting and striking, so prey on a bank or a
 * jetty would otherwise be out of their reach, and the Tide Drummer calls its lurkers up around itself, on the
 * shore it is fought from as often as in the water. They swim in water, walk on land, breathe in both, and go
 * back to the water when idle.
 */
public final class CreatureSwimming {
    private CreatureSwimming() {}

    /** Thrust per tick at full speed: a creature reaches about 0.6 of its land speed attribute in blocks a tick. */
    private static final float THRUST = .06F;
    /** Matches the lift the move control gives while the eyes are under, so only a creature at the surface sinks. */
    private static final double BUOYANCY = .005;

    /** Whether a creature lives in water. */
    public static boolean swims(CreatureProfile profile) { return CreatureHabitat.of(profile) == CreatureHabitat.WATER; }

    /** Whether a water creature may also leave it: the hunters do, the gentle swimmers do not. */
    public static boolean amphibious(CreatureProfile profile) { return swims(profile) && !profile.animal; }

    public static PathNavigation navigation(Mob mob, Level level, CreatureProfile profile) {
        return amphibious(profile) ? new AmphibiousPathNavigation(mob, level) : new WaterBoundPathNavigation(mob, level);
    }

    /**
     * Moves a creature that is in water. Returns false when it is not, so the caller's ordinary travel runs (on
     * land, and on the client, which only animates).
     */
    public static boolean travel(LivingEntity mob, Vec3 input, boolean amphibious) {
        if (!mob.isControlledByLocalInstance() || !mob.isInWater()) return false;
        double startY = mob.getY();
        mob.moveRelative(THRUST, input);
        mob.move(MoverType.SELF, mob.getDeltaMovement());
        Vec3 motion = mob.getDeltaMovement().scale(.9).add(0, -BUOYANCY, 0);
        // A hunter swimming at a bank hops out onto it, the way anything in water climbs a one-block edge.
        if (amphibious && mob.horizontalCollision && mob.isFree(motion.x, motion.y + .6 - mob.getY() + startY, motion.z))
            motion = new Vec3(motion.x, .3, motion.z);
        mob.setDeltaMovement(motion);
        mob.calculateEntityAnimation(false);
        return true;
    }

    /**
     * After the base tick: a fish's air, as {@code WaterAnimal} keeps it. Full in water; out of it, one less each
     * tick, and once it runs out two points of drying out every second.
     */
    public static void breathe(LivingEntity mob, int airBefore) {
        if (!mob.isAlive()) return;
        if (mob.isInWaterOrBubble()) {
            mob.setAirSupply(mob.getMaxAirSupply());
            return;
        }
        mob.setAirSupply(airBefore - 1);
        if (mob.getAirSupply() == -20) {
            mob.setAirSupply(0);
            mob.hurt(mob.damageSources().dryOut(), 2F);
        }
    }

    /**
     * A beached swimmer, landing, flops again: up, and a little toward the nearest water within a few blocks, so
     * one stranded beside a pool works its way back in. Returns whether it flopped (to play the sound).
     */
    public static boolean flop(Mob mob) {
        if (mob.isInWater() || !mob.onGround() || !mob.verticalCollision) return false;
        var random = mob.getRandom();
        double x = (random.nextFloat() * 2 - 1) * .05, z = (random.nextFloat() * 2 - 1) * .05;
        Vec3 toward = nearestWater(mob);
        if (toward != null) {
            x += toward.x * .14;
            z += toward.z * .14;
            mob.setYRot((float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90F);
        }
        mob.setDeltaMovement(mob.getDeltaMovement().add(x, .4, z));
        mob.setOnGround(false);
        mob.hasImpulse = true;
        return true;
    }

    /** The flat direction to the closest water within four blocks, or null when there is none. */
    private static Vec3 nearestWater(Mob mob) {
        BlockPos water = nearestWater(mob, 4, 2);
        if (water == null) return null;
        Vec3 flat = new Vec3(water.getX() + .5 - mob.getX(), 0, water.getZ() + .5 - mob.getZ());
        return flat.lengthSqr() < 1.0E-4 ? null : flat.normalize();
    }

    /** The closest water block within a box around the creature, or null. */
    private static BlockPos nearestWater(Mob mob, int radius, int depth) {
        BlockPos at = mob.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double best = Double.MAX_VALUE;
        BlockPos found = null;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -depth; dy <= 1; dy++) {
                    cursor.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (!mob.level().getFluidState(cursor).is(FluidTags.WATER)) continue;
                    double distance = cursor.distToCenterSqr(mob.position());
                    if (distance < best) {
                        best = distance;
                        found = cursor.immutable();
                    }
                }
        return found;
    }

    /** The nearest water block, preferring one with water on all four sides (room for a wide creature). */
    private static BlockPos openWater(Mob mob, int radius, int depth) {
        BlockPos at = mob.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double best = Double.MAX_VALUE;
        BlockPos found = null;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -depth; dy <= 1; dy++) {
                    cursor.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (!mob.level().getFluidState(cursor).is(FluidTags.WATER)) continue;
                    double score = cursor.distToCenterSqr(mob.position());
                    for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL)
                        if (!mob.level().getFluidState(cursor.relative(side)).is(FluidTags.WATER)) score += 16;
                    if (score < best) {
                        best = score;
                        found = cursor.immutable();
                    }
                }
        return found;
    }

    /** Whether water standing at these feet would cover the creature's eyes. */
    static boolean covers(Mob mob, BlockPos feet) {
        return mob.level().getFluidState(BlockPos.containing(feet.getX() + .5, feet.getY() + mob.getEyeHeight() + .1, feet.getZ() + .5))
                .is(FluidTags.WATER);
    }

    /**
     * Wandering, as vanilla's random swimming, but to a spot deep enough to cover its eyes where the water is that
     * deep: a goal at the very top of the water would hold a tall swimmer with its head out. Only while allowed
     * (a bonded one follows or waits instead). Out of water it finds water within reach, so a hunter left on land
     * drifts back to it.
     */
    public static final class Roam extends RandomSwimmingGoal {
        private final BooleanSupplier allowed;

        public Roam(PathfinderMob mob, BooleanSupplier allowed) {
            super(mob, 1, 40);
            this.allowed = allowed;
        }

        @Override
        public boolean canUse() {
            return allowed.getAsBoolean() && super.canUse();
        }

        @Override
        protected Vec3 getPosition() {
            Vec3 target = super.getPosition();
            if (target == null) return null;
            BlockPos.MutableBlockPos feet = BlockPos.containing(target).mutable();
            for (int i = 0; i < 3 && !covers(mob, feet) && mob.level().getFluidState(feet.below()).is(FluidTags.WATER); i++)
                feet.move(0, -1, 0);
            return Vec3.atBottomCenterOf(feet);
        }
    }

    /**
     * A hunter on land with nothing to chase walks back to the nearest water within eight blocks: it came out
     * after prey, and its home is the water. It makes for open water rather than the margin, and once its path is
     * done walks straight in: a path for a creature over a block wide can end on the brink, with the creature
     * standing over the bank and the water at once.
     */
    public static final class ReturnToWater extends Goal {
        /** Gives up after this long, to look again later. */
        private static final int PATIENCE = 200;
        private final PathfinderMob mob;
        private final BooleanSupplier allowed;
        private BlockPos water;
        private int ticks;

        public ReturnToWater(PathfinderMob mob, BooleanSupplier allowed) {
            this.mob = mob;
            this.allowed = allowed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.isInWater() || mob.getTarget() != null || !allowed.getAsBoolean() || mob.getRandom().nextInt(reducedTickDelay(20)) != 0)
                return false;
            water = openWater(mob, 8, 3);
            return water != null;
        }

        @Override
        public void start() {
            ticks = 0;
            mob.getNavigation().moveTo(water.getX() + .5, water.getY(), water.getZ() + .5, 1);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (mob.getNavigation().isDone())
                mob.getMoveControl().setWantedPosition(water.getX() + .5, water.getY(), water.getZ() + .5, 1);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.isInWater() && mob.getTarget() == null && allowed.getAsBoolean() && ++ticks < PATIENCE;
        }

        @Override
        public void stop() {
            water = null;
            mob.getNavigation().stop();
        }
    }

    /**
     * Steering for swimmers, after vanilla's smooth swimming control: in water it turns toward the next waypoint
     * and thrusts along the line to it, climbing or diving as it goes. It keeps that pitch to itself rather than
     * writing the creature's head pitch, so the look control alone poses the head the rigs turn by name.
     * Out of water a hunter walks with the ordinary control (which steps and jumps); a fish cannot.
     */
    public static final class SwimMoveControl extends MoveControl {
        private final boolean amphibious;

        public SwimMoveControl(Mob mob, boolean amphibious) {
            super(mob);
            this.amphibious = amphibious;
        }

        @Override
        public void tick() {
            boolean wet = mob.isInWater();
            if (!wet && !amphibious) {
                halt();
                return;
            }
            // The last few blocks to prey in the water it goes straight at it: a path ends on the block grid, which
            // can leave a hunter hanging a block above prey on the bottom, or a wide one standing on the brink.
            LivingEntity target = mob.getTarget();
            boolean closing = target != null && target.isAlive() && (wet || target.isInWater())
                    && mob.distanceToSqr(target) < 9 && mob.hasLineOfSight(target);
            if (closing) setWantedPosition(target.getX(), target.getY(), target.getZ(), speedModifier > 0 ? speedModifier : 1);
            if (!wet) {
                super.tick();
                return;
            }
            if (mob.isEyeInFluid(FluidTags.WATER)) mob.setDeltaMovement(mob.getDeltaMovement().add(0, BUOYANCY, 0));
            if (!closing && (operation != Operation.MOVE_TO || mob.getNavigation().isDone())) {
                halt();
                return;
            }
            double dx = wantedX - mob.getX(), dy = wantedY - mob.getY(), dz = wantedZ - mob.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat * flat + dy * dy < 2.5E-7) {
                mob.setZza(0);
                mob.setYya(0);
                return;
            }
            if (flat > 1.0E-5) {
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
                mob.setYRot(rotlerp(mob.getYRot(), yaw, 30F));
                mob.yBodyRot = mob.getYRot();
            }
            float speed = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            mob.setSpeed(speed);
            float pitch = (float) Mth.atan2(dy, flat);
            pitch = Mth.clamp(pitch, -85F * Mth.DEG_TO_RAD, 85F * Mth.DEG_TO_RAD);
            mob.zza = Mth.cos(pitch) * speed;
            mob.yya = Mth.sin(pitch) * speed;
        }

        private void halt() {
            mob.setSpeed(0);
            mob.setXxa(0);
            mob.setYya(0);
            mob.setZza(0);
        }
    }
}
