package tk.darrow.tribalpower.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureHabitat;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.CreatureSwimming;
import tk.darrow.tribalpower.entity.LatticeAnimal;
import tk.darrow.tribalpower.familiar.BondingCharmItem;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.familiar.FamiliarFollowGoal;
import tk.darrow.tribalpower.familiar.FamiliarRegistry;

/**
 * The Shallows' own creatures swim. They spawned only in water but were built as land beasts, walking and
 * floating: they bobbed at the surface, could not dive, and could not even be spawned there naturally.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class WaterCreatureGameTests {
    private static final List<CreatureProfile> SWIMMERS = List.of(CreatureProfile.SILT_GLIDER, CreatureProfile.PALE_DRIFTER,
            CreatureProfile.SHOAL_DARTER, CreatureProfile.BRINE_LURKER, CreatureProfile.DROWNED_SHADE);

    /** A 12 x 12 pool, four deep and open to the air, walled in glass one block above the water. */
    private static void deepPool(GameTestHelper h) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) {
            boolean wall = x == 0 || z == 0 || x == 13 || z == 13;
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= 5; y++) h.setBlock(x, y, z, wall ? Blocks.GLASS : y <= 4 ? Blocks.WATER : Blocks.AIR);
        }
    }

    /** Dry stone ground (its top at y 3) with a 5 x 5 pool two deep let into it at its west end, flush with the ground. */
    private static void bankAndPool(GameTestHelper h) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 7; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            boolean pool = x >= 1 && x <= 5 && z >= 1 && z <= 5;
            for (int y = 1; y <= 2; y++) h.setBlock(x, y, z, pool ? Blocks.WATER : Blocks.STONE);
        }
    }

    /** A wild creature of the profile, its threads rolled and its health full, that never despawns. */
    private static Mob put(GameTestHelper h, CreatureProfile profile, double x, double y, double z) {
        Mob mob = CreatureEntities.type(profile).create(h.getLevel());
        Vec3 at = h.absoluteVec(new Vec3(x, y, z));
        mob.moveTo(at.x, at.y, at.z, 0, 0);
        mob.setPersistenceRequired();
        ((Familiar) mob).ensureLattice(h.getLevel().getRandom(), false);
        mob.setHealth(mob.getMaxHealth());
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    /** The water creatures are exactly the five the habitat table puts in water. */
    @GameTest(template = "empty")
    public static void everyWaterHabitatCreatureSwims(GameTestHelper h) {
        for (var profile : CreatureProfile.values())
            h.assertTrue(CreatureSwimming.swims(profile) == SWIMMERS.contains(profile)
                            && CreatureSwimming.swims(profile) == (CreatureHabitat.of(profile) == CreatureHabitat.WATER),
                    profile.id + ": swims exactly when its habitat is water");
        // Breathing under water is a tag in 1.21 (LivingEntity#canBreatheUnderwater is final): it must name all five.
        for (var profile : SWIMMERS) {
            Mob mob = CreatureEntities.type(profile).create(h.getLevel());
            h.assertTrue(mob.canBreatheUnderwater(), profile.id + " must breathe under water");
            mob.discard();
        }
        h.assertTrue(CreatureSwimming.amphibious(CreatureProfile.BRINE_LURKER) && CreatureSwimming.amphibious(CreatureProfile.DROWNED_SHADE)
                && !CreatureSwimming.amphibious(CreatureProfile.SILT_GLIDER) && !CreatureSwimming.amphibious(CreatureProfile.PALE_DRIFTER)
                && !CreatureSwimming.amphibious(CreatureProfile.SHOAL_DARTER), "The two hunters leave the water; the gentle swimmers do not");
        h.succeed();
    }

    /**
     * Natural spawning may set a swimmer down in water. Mob refuses any spawn touching liquid, so although the
     * habitat rule chose water for these five, the spawner threw every one of them back.
     */
    @GameTest(template = "empty")
    public static void waterCreaturesMaySpawnInWater(GameTestHelper h) {
        deepPool(h);
        var level = h.getLevel();
        for (var profile : SWIMMERS) {
            Mob mob = CreatureEntities.type(profile).create(level);
            Vec3 at = h.absoluteVec(new Vec3(6.5, 1, 6.5));
            mob.moveTo(at.x, at.y, at.z, 0, 0);
            h.assertTrue(mob.checkSpawnObstruction(level), profile.id + " must be allowed to spawn in water");
            if (profile.animal)
                h.assertTrue(mob.checkSpawnRules(level, MobSpawnType.NATURAL), profile.id + " must find water good ground to spawn on");
            mob.discard();
        }
        h.succeed();
    }

    /**
     * Each water creature left in a pool for 400 ticks cruises about under the surface rather than bobbing on it,
     * never leaves the water, and keeps its full air and health.
     */
    @GameTest(template = "empty", timeoutTicks = 460)
    public static void swimmersCruiseUnderTheSurfaceAndKeepTheirAir(GameTestHelper h) {
        deepPool(h);
        double[][] starts = {{3.5, 1, 3.5}, {10.5, 1, 3.5}, {3.5, 1, 10.5}, {10.5, 1, 10.5}, {6.5, 2, 6.5}};
        List<Mob> swimmers = new ArrayList<>();
        for (int i = 0; i < SWIMMERS.size(); i++)
            swimmers.add(put(h, SWIMMERS.get(i), starts[i][0], starts[i][1], starts[i][2]));
        int samples = 20;
        int[] submerged = new int[swimmers.size()], wet = new int[swimmers.size()], moves = new int[swimmers.size()];
        double[] farthest = new double[swimmers.size()];
        Vec3[] first = swimmers.stream().map(Mob::position).toArray(Vec3[]::new), last = first.clone();
        for (int s = 1; s <= samples; s++) {
            boolean end = s == samples;
            h.runAfterDelay(s * 20L, () -> {
                for (int i = 0; i < swimmers.size(); i++) {
                    Mob mob = swimmers.get(i);
                    if (mob.isUnderWater()) submerged[i]++;
                    if (mob.isInWater()) wet[i]++;
                    if (mob.position().distanceTo(last[i]) > .2) moves[i]++;
                    farthest[i] = Math.max(farthest[i], mob.position().distanceTo(first[i]));
                    last[i] = mob.position();
                }
                if (!end) return;
                for (int i = 0; i < swimmers.size(); i++) {
                    Mob mob = swimmers.get(i);
                    String id = SWIMMERS.get(i).id;
                    h.assertTrue(mob.isAlive() && wet[i] == samples, id + " must stay in the water: in it " + wet[i] + " of " + samples);
                    h.assertTrue(submerged[i] >= samples * 4 / 5, id + " must swim under the surface, not bob on it: under "
                            + submerged[i] + " of " + samples);
                    h.assertTrue(farthest[i] >= 1.5 && moves[i] >= 3, id + " must swim about: farthest " + farthest[i] + ", moved in "
                            + moves[i] + " of " + samples);
                    h.assertTrue(mob.getAirSupply() == mob.getMaxAirSupply(), id + " must keep its air under water: " + mob.getAirSupply());
                    h.assertTrue(mob.getHealth() == mob.getMaxHealth(), id + " must come to no harm in water: " + mob.getHealth());
                }
                swimmers.forEach(Mob::discard);
                h.succeed();
            });
        }
    }

    /**
     * Beached beside a pool, a gentle swimmer flops its way back in, drying out as a fish does while it is out; a
     * hunter breathes on land and walks back to the water itself.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void strandedSwimmersGetBackToTheWater(GameTestHelper h) {
        bankAndPool(h);
        Mob glider = put(h, CreatureProfile.SILT_GLIDER, 8.5, 3, 2.5);
        Mob lurker = put(h, CreatureProfile.BRINE_LURKER, 9.5, 3, 4.5);
        boolean[] checked = {false};
        h.runAfterDelay(10, () -> {
            h.assertTrue(!glider.isInWater() && glider.getAirSupply() < glider.getMaxAirSupply(),
                    "A beached fish dries out: air " + glider.getAirSupply());
            h.assertTrue(lurker.getAirSupply() == lurker.getMaxAirSupply(), "A hunter breathes on land: air " + lurker.getAirSupply());
            checked[0] = true;
        });
        h.succeedWhen(() -> {
            h.assertTrue(checked[0], "The beached pair are weighed first");
            h.assertTrue(glider.isAlive() && glider.isInWater(), "The beached Silt Glider must flop back into the pool");
            h.assertTrue(lurker.isAlive() && lurker.isInWater(), "The Brine Lurker must walk back to the pool: at " + lurker.position()
                    + (lurker.getNavigation().getPath() == null ? ", no path" : ", path to " + lurker.getNavigation().getPath().getTarget()
                    + (lurker.getNavigation().getPath().isDone() ? ", done" : ", next " + lurker.getNavigation().getPath().getNextNode())));
            h.assertTrue(glider.getHealth() == glider.getMaxHealth(), "Back in time, the fish took no harm");
        });
    }

    /** A water hunter dives across the pool to prey resting on the bottom of the far corner. */
    private static void huntsInWater(GameTestHelper h, CreatureProfile profile) {
        deepPool(h);
        Mob prey = put(h, CreatureProfile.SHOAL_DARTER, 11.5, 1, 11.5);
        prey.setNoAi(true);
        Mob hunter = put(h, profile, 2.5, 3, 2.5);
        boolean[] pathed = {false};
        // After a tick, once the hunter knows it is in water.
        h.runAfterDelay(2, () -> {
            var path = hunter.getNavigation().createPath(prey, 0);
            h.assertTrue(path != null && path.getEndNode().distanceTo(prey.blockPosition()) <= 1.5F, profile.id + " must find a path through the water to its prey: "
                    + (path == null ? "no path" : "ends at " + path.getEndNode() + ", " + path.getNodeCount() + " nodes"));
            hunter.setTarget(prey);
            pathed[0] = true;
        });
        h.succeedWhen(() -> {
            h.assertTrue(pathed[0], profile.id + " has not been set on its prey yet");
            h.assertTrue(hunter.isAlive() && hunter.isInWater(), profile.id + " must hunt in the water");
            h.assertTrue(hunter.isWithinMeleeAttackRange(prey) || prey.getHealth() < prey.getMaxHealth(),
                    profile.id + " must reach its prey: " + Math.round(hunter.distanceTo(prey) * 10) / 10.0 + " blocks off");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void drownedShadeHuntsUnderWater(GameTestHelper h) {
        huntsInWater(h, CreatureProfile.DROWNED_SHADE);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void brineLurkerHuntsUnderWater(GameTestHelper h) {
        huntsInWater(h, CreatureProfile.BRINE_LURKER);
    }

    /**
     * A bonded swimmer is never set down on dry land beside its owner, only in water; told to stay, it waits where
     * it is in the water instead of drifting off or surfacing.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bondedSwimmerFollowsByWaterAndStays(GameTestHelper h) {
        bankAndPool(h);
        var player = VerificationPlayers.inLevel(h);
        Vec3 far = h.absoluteVec(new Vec3(13.5, 3, 5.5));
        player.moveTo(far.x, far.y, far.z, 0, 0);
        var glider = (LatticeAnimal) put(h, CreatureProfile.SILT_GLIDER, 2.5, 1, 2.5);
        player.getAbilities().instabuild = false;
        var charm = new ItemStack(FamiliarRegistry.BONDING_CHARM.get(), 4);
        h.assertTrue(BondingCharmItem.attempt(h.getLevel(), player, glider, charm, true) && glider.isBonded(), "A forced attempt must bond");
        h.assertFalse(FamiliarFollowGoal.teleportToOwner(glider, player), "No water near its owner: a swimmer is not set down on land");
        h.runAfterDelay(40, () -> {
            h.assertTrue(glider.isAlive() && glider.isInWater(), "Following an owner on land, a swimmer keeps to its water");
            Vec3 bank = h.absoluteVec(new Vec3(7.5, 3, 3.5));
            player.moveTo(bank.x, bank.y, bank.z, 0, 0);
            boolean moved = false;
            for (int i = 0; i < 60 && !moved; i++) moved = FamiliarFollowGoal.teleportToOwner(glider, player);
            h.assertTrue(moved && h.getLevel().getFluidState(glider.blockPosition()).is(net.minecraft.tags.FluidTags.WATER),
                    "Beside water, its owner's swimmer is brought into the water");
            glider.setSitting(true);
            // It glides to a halt on the way it was swimming, then waits there.
            h.runAfterDelay(30, () -> {
                Vec3 waiting = glider.position();
                h.runAfterDelay(60, () -> {
                    h.assertTrue(glider.isSitting() && glider.isInWater() && glider.position().distanceTo(waiting) < .5,
                            "A waiting swimmer stays put in the water: moved " + glider.position().distanceTo(waiting));
                    h.assertTrue(glider.getAirSupply() == glider.getMaxAirSupply() && glider.getHealth() == glider.getMaxHealth(),
                            "A waiting swimmer keeps its air and health");
                    glider.discard();
                    h.succeed();
                });
            });
        });
    }
}
