package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.MarchRepopulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildlife returning to emptied March land. The tests run one attempt at a time at a chosen centre, with a
 * small counting box and a tight group spread so nothing strays into a neighbouring test.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class RepopulationGameTests {
    private static final BlockPos CENTRE = new BlockPos(8, 1, 8);

    private static BlockPos floor(GameTestHelper h, Block block) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(x, 0, z, block);
        return h.absolutePos(CENTRE);
    }

    private static EntityType<?> fox() { return CreatureEntities.type(CreatureProfile.LANTERN_FOX); }

    /** Foxes only, in groups of four. */
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> foxes() {
        return WeightedRandomList.create(new MobSpawnSettings.SpawnerData(fox(), 1, 4, 4));
    }

    /** Count within six blocks; no player exclusion, since other tests leave stand-in players about the level. */
    private static MarchRepopulation.Rules rules(int cap) {
        return new MarchRepopulation.Rules(32, 96, 6, cap, 0, 2);
    }

    private static List<Mob> tryUntilSomething(GameTestHelper h, BlockPos centre, WeightedRandomList<MobSpawnSettings.SpawnerData> list,
                                               MarchRepopulation.Rules rules, int attempts) {
        List<Mob> all = new ArrayList<>();
        for (int i = 0; i < attempts && all.isEmpty(); i++) all.addAll(MarchRepopulation.attemptAt(h.getLevel(), centre, list, rules));
        return all;
    }

    /** Under the local cap a group arrives from the list, as natural spawns, and never past the cap. */
    @GameTest(template = "empty")
    public static void wildlifeReturnsUnderTheCap(GameTestHelper h) {
        BlockPos centre = floor(h, ModBlocks.MARCH_GRASS.get());
        List<Mob> arrived = tryUntilSomething(h, centre, foxes(), rules(3), 20);
        try {
            h.assertTrue(!arrived.isEmpty(), "A fox group must return to empty March grass");
            h.assertTrue(arrived.size() <= 3, "A group of four must be trimmed to the room under a cap of three, got " + arrived.size());
            for (Mob mob : arrived) {
                h.assertTrue(mob.getType() == fox(), "Only listed creatures arrive, got " + mob.getType());
                h.assertTrue(mob.isAlive() && mob.isAddedToLevel(), "An arrival must be in the level");
                h.assertTrue(!mob.isPersistenceRequired(), "Arrivals are wild, like naturally spawned animals");
                h.assertTrue(tk.darrow.tribalpower.entity.MarchSpawns.turf(h.getLevel().getBlockState(mob.blockPosition().below())),
                        "Arrivals stand on the ground their spawn rules ask for");
            }
            h.assertTrue(MarchRepopulation.creaturesAround(h.getLevel(), centre, 6) <= 3, "The count must stay at or under the cap");
        } finally {
            arrived.forEach(Mob::discard);
        }
        h.succeed();
    }

    /** At the cap, nothing more arrives however many times it is tried. */
    @GameTest(template = "empty")
    public static void wildlifeWaitsAtTheCap(GameTestHelper h) {
        BlockPos centre = floor(h, ModBlocks.MARCH_GRASS.get());
        List<Mob> residents = new ArrayList<>();
        for (int i = 0; i < 3; i++) residents.add((Mob) h.spawn(fox(), new BlockPos(7 + i, 1, 8)));
        List<Mob> arrived = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) arrived.addAll(MarchRepopulation.attemptAt(h.getLevel(), centre, foxes(), rules(3)));
            h.assertTrue(arrived.isEmpty(), "Nothing may arrive where the cap is already met, got " + arrived.size());
        } finally {
            arrived.forEach(Mob::discard);
            residents.forEach(Mob::discard);
        }
        h.succeed();
    }

    /** Nothing arrives within 24 blocks of a player. */
    @GameTest(template = "empty")
    public static void wildlifeKeepsClearOfPlayers(GameTestHelper h) {
        BlockPos centre = floor(h, ModBlocks.MARCH_GRASS.get());
        var player = VerificationPlayers.inLevel(h);
        player.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0, 0);
        var near = new MarchRepopulation.Rules(32, 96, 6, 6, MarchRepopulation.PLAYER_EXCLUSION, 2);
        List<Mob> arrived = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) arrived.addAll(MarchRepopulation.attemptAt(h.getLevel(), centre, foxes(), near));
            h.assertTrue(arrived.isEmpty(), "Nothing may arrive beside a player, got " + arrived.size());
        } finally {
            arrived.forEach(Mob::discard);
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    /** With no list handed in, the attempt reads the spot's own biome creature list, as natural spawning does. */
    @GameTest(template = "empty")
    public static void wildlifeComesFromTheBiomeList(GameTestHelper h) {
        BlockPos centre = floor(h, Blocks.GRASS_BLOCK);
        var listed = MarchRepopulation.spawnsAt(h.getLevel(), centre);
        h.assertTrue(!listed.isEmpty(), "The test level's biome must list creatures for this test to mean anything");
        List<Mob> arrived = tryUntilSomething(h, centre, null, rules(6), 40);
        try {
            h.assertTrue(!arrived.isEmpty(), "A group from the biome's list must return to empty grass");
            for (Mob mob : arrived)
                h.assertTrue(listed.unwrap().stream().anyMatch(entry -> entry.type == mob.getType()),
                        mob.getType() + " is not on the biome's creature list");
        } finally {
            arrived.forEach(Mob::discard);
        }
        h.succeed();
    }
}
