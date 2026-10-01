package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.world.ModDimensions;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildlife finding its way back into the March.
 *
 * <p>Animals mostly appear when land is first generated. The periodic creature pass only runs while the
 * creature count near a player is under the global cap, and the March's own animals stay in the world, so
 * once a stretch of land is explored that cap is full and nothing ever returns to ground that was hunted
 * or emptied. This pass replaces the global cap with a local one: every so often, near each player in the
 * March, it picks a spot a fair walk away and, if few animals live around it, lets a small group from that
 * spot's biome wander in.
 *
 * <p>A spawn here goes through the same gates as a natural one: the biome's creature list (including what
 * other mods add to it), the creature's placement and spawn rules, its own position checks, and the spawn
 * events, all as {@link MobSpawnType#NATURAL}. Only the {@link MobCategory#CREATURE} category is touched;
 * monsters already come and go on their own. Every number is in the {@code wildlife} section of the common
 * config.
 */
public final class MarchRepopulation {
    /** No natural spawn lands within this many blocks of a player (the vanilla rule). */
    public static final double PLAYER_EXCLUSION = 24.0;
    /** How far apart a group's members land: the vanilla group spread. */
    public static final int VANILLA_SPREAD = 6;
    /** Tries per group member before it is given up, as vanilla does. */
    private static final int TRIES = 4;
    /** How far the floor search falls from a random height before giving up. */
    private static final int FLOOR_SEARCH = 24;

    /**
     * The dials for one attempt.
     *
     * @param minDistance closest an attempt lands to its player, horizontally
     * @param maxDistance farthest
     * @param radius      half-width of the box creatures are counted in, around the attempt's centre
     * @param cap         no group arrives while this many creatures already live in that box
     * @param exclusion   no creature lands within this many blocks of any player
     * @param spread      group spread: each member steps up to {@code spread - 1} blocks from the last
     */
    public record Rules(int minDistance, int maxDistance, int radius, int cap, double exclusion, int spread) {
        public static Rules fromConfig() {
            int min = TribalConfig.marchRepopulateMinDistance(), max = TribalConfig.marchRepopulateMaxDistance();
            return new Rules(Math.min(min, max), Math.max(min, max), TribalConfig.marchRepopulateRadius(),
                    TribalConfig.marchRepopulateCap(), PLAYER_EXCLUSION, VANILLA_SPREAD);
        }
    }

    private MarchRepopulation() {}

    // ---- the clock ---------------------------------------------------------------------------------------------

    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(ModDimensions.THE_MARCH)) return;
        if (level.getGameTime() % (Math.max(1, TribalConfig.marchRepopulateSeconds()) * 20L) != 0) return;
        if (level.players().isEmpty() || !allowed(level)) return;
        Rules rules = Rules.fromConfig();
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isSpectator()) continue;
            try {
                attemptNear(level, player, rules);
            } catch (RuntimeException error) {
                // A creature from another mod that throws while spawning must not take the level tick with it.
                TribalPower.LOGGER.warn("March repopulation attempt failed near {}", player.getScoreboardName(), error);
            }
        }
    }

    /** Whether animals may arrive at all: the config switch, doMobSpawning, and the server's spawn-animals setting. */
    public static boolean allowed(ServerLevel level) {
        return TribalConfig.marchRepopulate()
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && level.getServer().isSpawningAnimals();
    }

    // ---- one attempt -------------------------------------------------------------------------------------------

    /** One attempt somewhere in the ring around a player. Never loads or generates a chunk. */
    public static List<Mob> attemptNear(ServerLevel level, Player player, Rules rules) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = rules.minDistance() + level.random.nextDouble() * Math.max(0, rules.maxDistance() - rules.minDistance());
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance), z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        BlockPos column = new BlockPos(x, player.getBlockY(), z);
        if (!level.isPositionEntityTicking(column) || !level.getWorldBorder().isWithinBounds(column)) return List.of();
        return attemptAt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column), null, rules);
    }

    /**
     * One attempt at a given centre. With {@code spawns} null the centre's biome creature list is used, exactly
     * as natural spawning reads it; tests may hand in their own list. Returns the creatures that arrived.
     */
    public static List<Mob> attemptAt(ServerLevel level, BlockPos centre, @Nullable WeightedRandomList<MobSpawnSettings.SpawnerData> spawns,
                                      Rules rules) {
        if (!level.isPositionEntityTicking(centre)) return List.of();
        if (nearPlayer(level, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, rules.exclusion())) return List.of();
        int room = rules.cap() - creaturesAround(level, centre, rules.radius());
        if (room <= 0) return List.of();
        var list = spawns != null ? spawns : spawnsAt(level, centre);
        var picked = list.getRandom(level.random);
        if (picked.isEmpty()) return List.of();
        MobSpawnSettings.SpawnerData entry = picked.get();
        EntityType<?> type = entry.type;
        if (type.getCategory() != MobCategory.CREATURE || !type.canSummon()) return List.of();
        int size = Math.min(room, entry.minCount + level.random.nextInt(1 + Math.max(0, entry.maxCount - entry.minCount)));

        List<Mob> arrived = new ArrayList<>();
        SpawnGroupData[] group = {null};   // carried from member to member, as natural spawning does
        int x = centre.getX(), z = centre.getZ();
        int spread = Math.max(1, rules.spread());
        for (int member = 0; member < size; member++) {
            for (int attempt = 0; attempt < TRIES; attempt++) {
                x += level.random.nextInt(spread) - level.random.nextInt(spread);
                z += level.random.nextInt(spread) - level.random.nextInt(spread);
                // the surface first; a later try may look lower down, so cave and lava dwellers can return too
                BlockPos pos = attempt % 2 == 0 ? surface(level, type, x, z) : floorBelow(level, type, x, z);
                if (pos == null) continue;
                Mob mob = place(level, type, pos, rules, group);
                if (mob == null) continue;
                arrived.add(mob);
                break;
            }
            if (!arrived.isEmpty()) {
                Mob last = arrived.get(arrived.size() - 1);
                if (arrived.size() >= EventHooks.getMaxSpawnClusterSize(last) || last.isMaxGroupSizeReached(arrived.size())) break;
            }
        }
        return arrived;
    }

    /** The creature list natural spawning would read here, with the potential-spawns event other mods hook. */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> spawnsAt(ServerLevel level, BlockPos pos) {
        var listed = level.getChunkSource().getGenerator().getMobsAt(level.getBiome(pos), level.structureManager(), MobCategory.CREATURE, pos);
        return EventHooks.getPotentialSpawns(level, MobCategory.CREATURE, pos, listed);
    }

    /** Creatures of the creature category living in the box around a position. */
    public static int creaturesAround(ServerLevel level, BlockPos centre, int radius) {
        return level.getEntitiesOfClass(Mob.class, new AABB(centre).inflate(radius),
                mob -> mob.isAlive() && mob.getType().getCategory() == MobCategory.CREATURE).size();
    }

    // ---- placing one creature ----------------------------------------------------------------------------------

    /** Sets one creature down at a position if every natural-spawn rule allows it, or returns null. */
    @Nullable
    private static Mob place(ServerLevel level, EntityType<?> type, BlockPos pos, Rules rules, SpawnGroupData[] group) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (!level.isPositionEntityTicking(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
        if (nearPlayer(level, x, y, z, rules.exclusion())) return null;
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)
                || !SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random)
                || !level.noCollision(type.getSpawnAABB(x, y, z))) return null;
        Entity entity;
        try {
            entity = type.create(level);
        } catch (RuntimeException error) {
            TribalPower.LOGGER.warn("March repopulation could not create {}", EntityType.getKey(type), error);
            return null;
        }
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            return null;
        }
        mob.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        // as natural spawning: a creature that would despawn at this distance is not spawned at it
        Player nearest = level.getNearestPlayer(x, y, z, -1.0, false);
        if (nearest != null) {
            double distance = nearest.distanceToSqr(x, y, z), despawn = type.getCategory().getDespawnDistance();
            if (distance > despawn * despawn && mob.removeWhenFarAway(distance)) return null;
        }
        if (!EventHooks.checkSpawnPosition(mob, level, MobSpawnType.NATURAL)) return null;
        group[0] = EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, group[0]);
        level.addFreshEntityWithPassengers(mob);
        // a finalize hook may have cancelled the spawn, in which case the level refused it
        return mob.isAddedToLevel() && !mob.isRemoved() ? mob : null;
    }

    private static boolean nearPlayer(ServerLevel level, double x, double y, double z, double exclusion) {
        return exclusion > 0 && level.getNearestPlayer(x, y, z, exclusion, false) != null;
    }

    /** The first open spot on top of the column, by the creature's own heightmap, as natural spawning finds it. */
    private static BlockPos surface(ServerLevel level, EntityType<?> type, int x, int z) {
        int top = level.getHeight(SpawnPlacements.getHeightmapType(type), x, z);
        return SpawnPlacements.getPlacementType(type).adjustSpawnPosition(level, new BlockPos(x, top, z));
    }

    /**
     * A random height down the column, settled onto the floor beneath it: how natural spawning reaches caves,
     * lava ledges and water. Null when the height lands inside solid ground.
     */
    @Nullable
    private static BlockPos floorBelow(ServerLevel level, EntityType<?> type, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int bottom = level.getMinBuildHeight() + 1;
        if (top <= bottom) return null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Mth.randomBetweenInclusive(level.random, bottom, top), z);
        if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return null;
        if (level.getFluidState(cursor).isEmpty())
            for (int fall = 0; fall < FLOOR_SEARCH && cursor.getY() > bottom
                    && level.getBlockState(cursor.below()).getCollisionShape(level, cursor.below()).isEmpty()
                    && level.getFluidState(cursor.below()).isEmpty(); fall++)
                cursor.move(0, -1, 0);
        return SpawnPlacements.getPlacementType(type).adjustSpawnPosition(level, cursor.immutable());
    }
}
