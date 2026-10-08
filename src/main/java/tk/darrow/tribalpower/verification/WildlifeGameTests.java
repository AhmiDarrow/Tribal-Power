package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.wildlife.Wildlife;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class WildlifeGameTests {
    private static void pool(GameTestHelper h) {
        // Room for a Veil Ray (1.4 blocks wide) to turn without being pushed out: a 5x5 pool, 4 deep, under glass.
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y < 5; y++) h.setBlock(x, y, z, x == 0 || z == 0 || x == 6 || z == 6 ? Blocks.GLASS : Blocks.WATER);
            h.setBlock(x, 5, z, Blocks.GLASS);
        }
    }

    /** Every water creature lives in a pool: it swims, keeps its air, and is not mistaken for a bucketable fish. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void marchWaterLifeLivesInWater(GameTestHelper h) {
        pool(h);
        var types = java.util.List.of(Wildlife.GLIMMERFIN.get(), Wildlife.DRIFT_BELL.get(), Wildlife.VEIL_RAY.get(), Wildlife.SILT_EEL.get());
        var spawned = types.stream().map(type -> (Mob) h.spawn(type, new BlockPos(3, 2, 3))).toList();
        h.runAfterDelay(100, () -> {
            for (Mob mob : spawned) {
                h.assertTrue(mob.isAlive(), mob.getType().getDescriptionId() + " must survive in water");
                h.assertTrue(mob.isInWater(), mob.getType().getDescriptionId() + " must stay in the water");
            }
            h.succeed();
        });
    }

    /**
     * A rod in the March lands the March's fish: every vanilla fish in a haul becomes a glimmerfin or an eel, junk
     * stays junk, a haul with no fish is left to vanilla, and an empty bucket in the other hand lands a live one.
     */
    @GameTest(template = "empty")
    public static void marchRodLandsMarchFish(GameTestHelper h) {
        var random = h.getLevel().random;
        var haul = java.util.List.of(new ItemStack(Items.COD), new ItemStack(Items.SALMON), new ItemStack(Items.STICK));
        for (int i = 0; i < 50; i++) {
            var landed = tk.darrow.tribalpower.wildlife.MarchFishing.marchCatch(haul, random, false);
            h.assertTrue(landed != null && landed.size() == 3, "a haul with fish is the March's to land");
            for (int n = 0; n < 2; n++)
                h.assertTrue(landed.get(n).is(Wildlife.RAW_GLIMMERFIN.get()) || landed.get(n).is(Wildlife.RAW_SILT_EEL.get()), "a vanilla fish comes up as March fish");
            h.assertTrue(landed.get(2).is(Items.STICK), "junk stays junk");
        }
        h.assertTrue(tk.darrow.tribalpower.wildlife.MarchFishing.marchCatch(java.util.List.of(new ItemStack(Items.STICK)), random, false) == null,
                "a haul with no fish is vanilla's");
        boolean live = false;
        for (int i = 0; i < 50 && !live; i++)
            for (ItemStack stack : tk.darrow.tribalpower.wildlife.MarchFishing.marchCatch(java.util.List.of(new ItemStack(Items.COD)), random, true))
                live |= stack.is(Wildlife.GLIMMERFIN_BUCKET.get());
        h.assertTrue(live, "an empty bucket in the other hand lands a live glimmerfin");
        h.succeed();
    }

    /**
     * Natural spawns land in open water, not in aquifers. The spawner tries heights from the bottom of the
     * world up, and when any water would do, most March swimmers spawned in water sealed under the land
     * where nobody could see them, filling the caps the meres share. A two-deep fen pool open to the sky
     * passes at every depth; the same water under a stone lid does not, though an egg may still go there.
     */
    @GameTest(template = "empty")
    public static void marchSwimmersSpawnInOpenWaterOnly(GameTestHelper h) {
        for (int x = 0; x < 7; x++) for (int z = 0; z < 4; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y < 3; y++) h.setBlock(x, y, z, x == 0 || z == 0 || x == 3 || x == 6 || z == 3 ? Blocks.STONE : Blocks.WATER);
        }
        for (int x = 4; x < 6; x++) for (int z = 1; z < 3; z++) h.setBlock(x, 3, z, Blocks.STONE);   // the aquifer's roof
        var level = h.getLevel();
        // the test's barrier ceiling would roof the pool too: open it to the sky over the pool
        for (int x = 1; x < 3; x++) for (int z = 1; z < 3; z++) {
            BlockPos column = h.absolutePos(new BlockPos(x, 3, z));
            for (int y = column.getY(); y < level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()); y++)
                level.setBlockAndUpdate(column.atY(y), Blocks.AIR.defaultBlockState());
        }
        var random = level.getRandom();
        var types = java.util.List.of(Wildlife.GLIMMERFIN.get(), Wildlife.DRIFT_BELL.get(), Wildlife.VEIL_RAY.get(), Wildlife.SILT_EEL.get());
        for (var type : types) {
            for (int y = 1; y < 3; y++) {
                BlockPos pool = h.absolutePos(new BlockPos(1, y, 1)), buried = h.absolutePos(new BlockPos(4, y, 1));
                h.assertTrue(net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(type, level, pool)
                                && net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, level, net.minecraft.world.entity.MobSpawnType.NATURAL, pool, random),
                        type.getDescriptionId() + " must spawn in an open fen pool at depth " + (3 - y));
                h.assertFalse(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, level, net.minecraft.world.entity.MobSpawnType.NATURAL, buried, random),
                        type.getDescriptionId() + " must not spawn naturally in water sealed under stone");
            }
            h.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, level, net.minecraft.world.entity.MobSpawnType.SPAWNER,
                    h.absolutePos(new BlockPos(4, 1, 1)), random), type.getDescriptionId() + " may still come from a spawner underground");
        }
        // The eel is a fish: it shares the fish's cap near the player, not the five water creatures spread over 128 blocks.
        h.assertTrue(Wildlife.SILT_EEL.get().getCategory() == net.minecraft.world.entity.MobCategory.WATER_AMBIENT,
                "Silt Eels must count with the fish");
        h.succeed();
    }

    /** Each March wood's sapling grows its own tree, built from its own logs, with its own leaves. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyMarchSaplingGrowsItsOwnWood(GameTestHelper h) {
        var woods = tk.darrow.tribalpower.world.MarchWoods.Wood.values();
        for (int i = 0; i < woods.length; i++) {
            if (woods[i] == tk.darrow.tribalpower.world.MarchWoods.Wood.STRIDER) continue; // stilts: grown in the survey instead
            var set = tk.darrow.tribalpower.world.MarchWoods.of(woods[i]);
            BlockPos at = new BlockPos(i * 40, 1, 0);
            h.setBlock(at.below(), tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get());
            h.setBlock(at, set.sapling.get());
            var abs = h.absolutePos(at);
            for (int k = 0; k < 2 && h.getLevel().getBlockState(abs).is(set.sapling.get()); k++)
                set.sapling.get().advanceTree(h.getLevel(), abs, h.getLevel().getBlockState(abs), h.getLevel().getRandom());
            h.assertTrue(h.getLevel().getBlockState(abs).is(set.log.get()), woods[i] + " sapling must grow into its own log, got "
                    + h.getLevel().getBlockState(abs));
            boolean leaves = false;
            for (BlockPos pos : BlockPos.betweenClosed(abs.offset(-14, 0, -14), abs.offset(14, 30, 14)))
                if (h.getLevel().getBlockState(pos).is(set.leaves.get())) { leaves = true; break; }
            h.assertTrue(leaves || woods[i] == tk.darrow.tribalpower.world.MarchWoods.Wood.CINDER, woods[i] + " must grow its own leaves");
            // An axe strips the log.
            var event = new net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent(h.getLevel().getBlockState(abs),
                    new net.minecraft.world.item.context.UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE),
                            new net.minecraft.world.phys.BlockHitResult(abs.getCenter(), net.minecraft.core.Direction.UP, abs, false)),
                    net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP, false);
            tk.darrow.tribalpower.world.MarchWoods.strip(event);
            h.assertTrue(event.getFinalState().is(set.strippedLog.get()), woods[i] + " log must strip");
        }
        h.succeed();
    }

    /** The March's water and cave decoration can stand where worldgen puts it. */
    @GameTest(template = "empty")
    public static void marchDecorationSurvivesItsPlaces(GameTestHelper h) {
        pool(h);
        var level = h.getLevel();
        h.setBlock(2, 4, 2, Blocks.AIR);
        var surface = h.absolutePos(new BlockPos(2, 4, 2));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.LILY_PAD.get().defaultBlockState().canSurvive(level, surface),
                "A lily pad must sit on still water, below is " + level.getBlockState(surface.below()));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.MOON_LILY.get().defaultBlockState().canSurvive(level, surface), "A moon lily too");
        var bed = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.RIBBON_WEED.get().defaultBlockState().canSurvive(level, bed), "Ribbon weed on a lake bed");
        h.succeed();
    }

    /** A water bucket scoops a Glimmerfin, but not the larger water life. */
    @GameTest(template = "empty")
    public static void onlyGlimmerfinGoesInABucket(GameTestHelper h) {
        pool(h);
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        var fish = h.spawn(Wildlife.GLIMMERFIN.get(), new BlockPos(2, 2, 2));
        var eel = h.spawn(Wildlife.SILT_EEL.get(), new BlockPos(2, 2, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        eel.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(eel.isAlive() && player.getMainHandItem().is(Items.WATER_BUCKET), "A Silt Eel must not go in a bucket");
        fish.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().is(Wildlife.GLIMMERFIN_BUCKET.get()) || player.getInventory().contains(new ItemStack(Wildlife.GLIMMERFIN_BUCKET.get())),
                "A water bucket must scoop a Glimmerfin");
        h.succeed();
    }
}
