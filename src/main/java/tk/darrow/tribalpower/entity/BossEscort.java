package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.List;
import java.util.Map;

/**
 * What walks with a boss.
 *
 * <p>The Weeping Colossus is never found alone: it comes up out of the fen with a stand of smaller
 * wardens around it and the things that live in its water. Spawning the escort with the boss is what
 * makes it read as a grove that stood up rather than one large mob that wandered in, and it is the
 * difference between a fight and a lone health bar.
 *
 * <p>Escorts are spawned only for a natural spawn. Summoning the boss from an egg gives you the boss
 * and nothing else, which is what a creative-mode or test spawn should do.
 */
public final class BossEscort {

    /** boss id -> the creatures that come with it, and how many of each. */
    private static final Map<String, List<Member>> ESCORTS = Map.of(
            "colossus_warden", List.of(
                    new Member("weeping_warden", 2, 4),
                    new Member("marsh_fay", 1, 3),
                    new Member("bog_floater", 0, 2)));

    private record Member(String id, int low, int high) {}

    private BossEscort() {}

    /** Which boss a companion came with. */
    public static final String ESCORT_OF = "TribalPowerEscortOf";

    /**
     * A boss that leaves without dying -- despawned, say -- takes its retinue with it. The companions are gentle
     * creatures that never despawn on their own, so without this every visit of the boss left a few more behind.
     */
    public static void dismiss(Mob boss) {
        if (!(boss.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        java.util.UUID id = boss.getUUID();
        for (Mob companion : level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(48),
                m -> m.getPersistentData().hasUUID(ESCORT_OF) && id.equals(m.getPersistentData().getUUID(ESCORT_OF))
                        && !(m instanceof tk.darrow.tribalpower.familiar.Familiar familiar && familiar.isBonded())))
            companion.discard();
    }

    /** Spawns the retinue in a ring around the boss. Quietly does nothing for a mob with no escort. */
    public static void spawn(Mob boss, ServerLevelAccessor level, MobSpawnType reason) {
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return;
        List<Member> escort = ESCORTS.get(CreatureProfile.of(boss.getType()).id);
        if (escort == null) return;
        RandomSource random = level.getRandom();
        for (Member member : escort) {
            CreatureProfile profile = byId(member.id());
            if (profile == null) continue;
            int count = member.low() + random.nextInt(member.high() - member.low() + 1);
            for (int i = 0; i < count; i++) place(boss, level, profile, random);
        }
    }

    /** One companion, a few blocks out, on whatever ground is under that spot. */
    private static void place(Mob boss, ServerLevelAccessor level, CreatureProfile profile,
                              RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double range = 4 + random.nextDouble() * 5;
        BlockPos at = BlockPos.containing(
                boss.getX() + Math.cos(angle) * range,
                boss.getY(),
                boss.getZ() + Math.sin(angle) * range);
        // Follow the column up or down a little so a companion does not appear inside the bank.
        for (int drop = 0; drop <= 4; drop++) {
            BlockPos here = at.below(drop);
            if (!level.getBlockState(here).isAir() && level.getBlockState(here.above()).isAir()) {
                at = here.above();
                break;
            }
        }
        Mob mob = CreatureEntities.type(profile).create(level.getLevel());
        if (mob == null) return;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0F);
        mob.getPersistentData().putUUID(ESCORT_OF, boss.getUUID());
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
        level.addFreshEntity(mob);
    }

    private static CreatureProfile byId(String id) {
        for (CreatureProfile profile : CreatureProfile.values())
            if (profile.id.equals(id)) return profile;
        return null;
    }
}
